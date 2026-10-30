package com.fs.twitchminichat.probe

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import com.fs.twitchminichat.AccountRepository
import java.io.File
import java.io.RandomAccessFile
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.LockSupport

/**
 * SCAFFOLD for the 5d closing measurement. Never merged.
 *
 * The probe of experiment/startup-cost-attribution (a091809), ported onto the two builds the 5d
 * measurement compares: the pre-5d control (c8a9f99) and the 5d head. This file is the same, byte for
 * byte, on both; only where its hooks are called from differs, as the code they sit in differs.
 *
 * Records the cold start of the chat pages against one monotonic anchor, taken in
 * [ProbeApplication.attachBaseContext], into arrays allocated before the first activity
 * exists. Nothing is written anywhere while the window is open: the main thread only
 * stores numbers. The window closes at anchor + [CLOSE_NS]; a background thread then
 * serialises everything to files/startup-probe/run-<ms>.json.
 *
 * Enabled by files/startup-probe/flag, read once in attachBaseContext:
 * - absent: nothing is recorded at all;
 * - "yardstick": only the Choreographer frame times, so the total can be compared with
 *   and without the rest of the instrument;
 * - "full": frames, phase boundaries, rows, view builds, emote requests, main-thread messages,
 *   per-thread CPU.
 * Words after the first switch components: "nosnap" and "nolooper" turn the CPU snapshots and
 * the message log off, as at a091809; "visibility" turns on the viewport walk, off by default
 * since the port (see [viewport]); "calibrate" times the probe's own hooks after the dump.
 *
 * What changed from a091809, each for a reason the 5d report gives:
 * - [viewport] walks only with "visibility". Its call site, the emote gate's viewport reconcile,
 *   exists only in the control, and the walk cost 4 to 13 ms inside the span on 2026-10-08: left on,
 *   it would make the control's span longer by work the 5d build never does. It still counts its
 *   calls, and times itself.
 * - View building has its own record, [buildIn] and [buildOut]. Before 5d a row's view was built
 *   inside its own insert; in 5d views are built and bound by the list's adapter, outside any row.
 *   Each build records where it ran, and for which row when it ran inside one.
 * - Two marks inside appendChatView, 3 and 4, around the timeline insert: before it, the composer
 *   snapshot and the follow decision; after it, the follow-or-count bookkeeping and the composer
 *   restore. Both builds call them at the same two places.
 * - The inflation harness (fragment_chat with and without GeckoView) is gone: it answered its
 *   question on 2026-10-08, and no quantity of this measurement uses it.
 */
object StartupProbe {

    const val MODE_OFF = 0
    const val MODE_YARDSTICK = 1
    const val MODE_FULL = 2

    private const val TAG = "TMC_PROBE"

    const val WINDOW_NS = 10_000_000_000L
    const val CLOSE_NS = 14_000_000_000L
    /* A snapshot after a stall long enough to be a line of the startup burst: 30 frames at 90 Hz. */
    private const val LONG_FRAME_NS = 330_000_000L
    private const val REASON_LONG_FRAME_END = 1L shl 62

    /* Boundaries. Page boundaries are PAGE_BASE + slot * PAGE_STRIDE + K_*. */
    const val B_ANCHOR = 0
    const val B_APP_IN = 1
    const val B_APP_OUT = 2
    const val B_ACT_IN = 3
    const val B_ACT_OUT = 4
    private const val PAGE_BASE = 5
    private const val PAGE_STRIDE = 7
    const val K_INFLATE_IN = 0
    const val K_INFLATE_OUT = 1
    const val K_VIEW_IN = 2
    const val K_VIEW_OUT = 3
    const val K_HISTORY = 4
    const val K_FIRST_ROW = 5
    const val K_LAST_ROW = 6
    const val B_TEN_S = 19
    const val B_CLOSE = 20
    private const val B_COUNT = 21
    private val BOUNDARY_NAMES = arrayOf(
        "anchor", "app_in", "app_out", "act_in", "act_out",
        "p0_inflate_in", "p0_inflate_out", "p0_view_in", "p0_view_out", "p0_history", "p0_first_row", "p0_last_row",
        "p1_inflate_in", "p1_inflate_out", "p1_view_in", "p1_view_out", "p1_history", "p1_first_row", "p1_last_row",
        "anchor_10s", "close"
    )

    const val SRC_HISTORY = 1
    const val SRC_LIVE = 2
    const val SRC_OTHER = 3
    const val ABORT_DEDUP = 1
    const val ABORT_HIDDEN = 2

    /* The marks of a row. 0-2 in appendChatLine, as at a091809; 3 and 4 in appendChatView, around the timeline insert. */
    const val MARK_MENTION = 0
    const val MARK_RECONCILE = 1
    const val MARK_BUDDY = 2
    const val MARK_BEFORE_INSERT = 3
    const val MARK_AFTER_INSERT = 4
    private const val MARKS = 5

    /* What a view build was. */
    const val BUILD_CREATE_AND_BIND = 1
    const val BUILD_CREATE = 2
    const val BUILD_BIND = 3

    /** What a span lambda returns for a view outside the timeline. */
    const val NO_SPAN = Long.MIN_VALUE

    private const val PAGES = 2
    private const val ROW_CAP = 3072
    private const val BUILD_CAP = 4096
    private const val FRAME_CAP = 4096
    private const val SNAP_CAP = 96
    private const val THREADS_PER_SNAP = 128

    @JvmField var anchorNs = 0L
    @JvmField var mode = MODE_OFF
    /* Components that can be left off, or turned on, so that each one's cost can be measured on its own. */
    @JvmField var snapshots = true
    @JvmField var looperLog = true
    @JvmField var visibility = false
    @JvmField var calibrate = false
    @Volatile private var closed = false

    private lateinit var appContext: Context
    private var processStartRelNs = 0L
    private var mainTid = 0
    private var flagText = ""

    private val boundaryNs = LongArray(B_COUNT) { -1L }
    private val boundaryTid = IntArray(B_COUNT)
    private val pendingSnap = AtomicLong(0L)
    private var probeThread: Thread? = null

    /* Pages, by the order in which ChatFragment instances first create their view. */
    private val pageOwners = arrayOfNulls<Any>(PAGES)
    private val pageAccount = arrayOfNulls<String>(PAGES)
    private val pageChannel = arrayOfNulls<String>(PAGES)
    private val pageHistoryCount = IntArray(PAGES) { -1 }
    private val pageHistorySec = LongArray(PAGES) { -1L }
    private val pageHistoryTid = IntArray(PAGES)
    private val pageHistoryRequestAt = LongArray(PAGES) { -1L }
    private val pageHistoryRequests = AtomicIntegerArray(PAGES)
    private val pagePostsQueued = AtomicIntegerArray(PAGES)
    private val pagePostsRun = IntArray(PAGES)
    private val pageHistoryRowsBegun = IntArray(PAGES)
    private val pageScrollPosts = IntArray(PAGES)
    private val pageScrollPostsInSpan = IntArray(PAGES)
    private val pageEverCurrent = BooleanArray(PAGES)
    private val pageViewportCalls = IntArray(PAGES)
    private val pageViewportNs = LongArray(PAGES)

    /* Rows: every appendChatLine call, and every other appendChatView call. */
    private lateinit var rowPage: IntArray
    private lateinit var rowSource: IntArray
    private lateinit var rowOrdinal: IntArray
    private lateinit var rowAbort: IntArray
    private lateinit var rowBegin: LongArray
    private lateinit var rowMarks: Array<LongArray>
    private lateinit var rowBuildIn: LongArray
    private lateinit var rowBuildOut: LongArray
    private lateinit var rowInsertIn: LongArray
    private lateinit var rowInsertOut: LongArray
    private lateinit var rowReqBuild: IntArray
    private lateinit var rowReqLater: IntArray
    private lateinit var rowSeenAt: LongArray
    private lateinit var rowInViewportAt: LongArray
    private val rowOfView = IdentityHashMap<View, Int>(ROW_CAP * 2)
    private var rowCount = 0
    private var currentRow = -1
    private var pendingSource = SRC_LIVE
    private var pendingSourceSlot = -1

    /* View builds: every build or bind of a row's view, wherever it ran. */
    private lateinit var buildKind: IntArray
    private lateinit var buildRow: IntArray
    private lateinit var buildStart: LongArray
    private lateinit var buildEnd: LongArray
    private lateinit var buildReq: IntArray
    private lateinit var buildReqLater: IntArray
    private val buildOfView = IdentityHashMap<View, Int>(BUILD_CAP)
    private var buildCount = 0
    private var building = -1
    private var reqUnattributed = 0

    /* Main-thread messages, from the Looper's own dispatch log: start and end of each. */
    private const val MSG_CAP = 16384
    private lateinit var msgStart: LongArray
    private lateinit var msgEnd: LongArray
    private var msgCount = 0
    private var msgOpen = -1L
    private val looperPrinter = android.util.Printer { line ->
        if (closed || line.isEmpty()) return@Printer
        if (line[0] == '>') {
            msgOpen = rel()
        } else if (line[0] == '<' && msgOpen >= 0L) {
            val i = msgCount
            if (i < MSG_CAP) {
                msgStart[i] = msgOpen
                msgEnd[i] = rel()
                msgCount = i + 1
            }
            msgOpen = -1L
        }
    }

    /* Frames: Choreographer vsync time, and when the callback actually ran. */
    private lateinit var frameVsync: LongArray
    private lateinit var frameRan: LongArray
    private var frameCount = 0

    /* Per-thread CPU: one row per snapshot, its threads in [snapStart, snapStart + snapLen). */
    private lateinit var snapAt: LongArray
    private lateinit var snapDone: LongArray
    private lateinit var snapReason: LongArray
    private lateinit var snapProcUt: LongArray
    private lateinit var snapProcSt: LongArray
    private lateinit var snapStart: IntArray
    private lateinit var snapLen: IntArray
    private lateinit var snapTid: IntArray
    private lateinit var snapUt: LongArray
    private lateinit var snapSt: LongArray
    private var snapCount = 0
    private var snapThreadsUsed = 0
    private val threadNames = HashMap<Int, String>()
    private var snapCostNs = 0L

    private var activityRef: WeakReference<Activity>? = null

    /** Offset of now from the anchor. */
    @JvmStatic
    fun rel(): Long = System.nanoTime() - anchorNs

    /** First thing the process runs in its own code. */
    fun onAttach(base: Context) {
        anchorNs = System.nanoTime()
        val flag = File(base.filesDir, "startup-probe/flag")
        if (!flag.exists()) return
        flagText = flag.readText().trim()
        val words = flagText.split(Regex("\\s+"))
        mode = when (words.firstOrNull()) {
            "full" -> MODE_FULL
            "yardstick" -> MODE_YARDSTICK
            else -> MODE_OFF
        }
        if (mode == MODE_OFF) return
        for (word in words.drop(1)) {
            when (word) {
                "nosnap" -> snapshots = false
                "nolooper" -> looperLog = false
                "visibility" -> visibility = true
                "calibrate" -> calibrate = true
            }
        }
        processStartRelNs = Process.getStartUptimeMillis() * 1_000_000L - anchorNs
        mainTid = Process.myTid()
        allocate()
        boundaryNs[B_ANCHOR] = 0L
        boundaryTid[B_ANCHOR] = mainTid
    }

    private fun allocate() {
        frameVsync = LongArray(FRAME_CAP)
        frameRan = LongArray(FRAME_CAP)
        if (mode != MODE_FULL) return
        msgStart = LongArray(MSG_CAP)
        msgEnd = LongArray(MSG_CAP)
        rowPage = IntArray(ROW_CAP)
        rowSource = IntArray(ROW_CAP)
        rowOrdinal = IntArray(ROW_CAP)
        rowAbort = IntArray(ROW_CAP)
        rowBegin = LongArray(ROW_CAP)
        rowMarks = Array(MARKS) { LongArray(ROW_CAP).also { it.fill(-1L) } }
        rowBuildIn = LongArray(ROW_CAP).also { it.fill(-1L) }
        rowBuildOut = LongArray(ROW_CAP).also { it.fill(-1L) }
        rowInsertIn = LongArray(ROW_CAP).also { it.fill(-1L) }
        rowInsertOut = LongArray(ROW_CAP).also { it.fill(-1L) }
        rowReqBuild = IntArray(ROW_CAP)
        rowReqLater = IntArray(ROW_CAP)
        rowSeenAt = LongArray(ROW_CAP).also { it.fill(-1L) }
        rowInViewportAt = LongArray(ROW_CAP).also { it.fill(-1L) }
        buildKind = IntArray(BUILD_CAP)
        buildRow = IntArray(BUILD_CAP)
        buildStart = LongArray(BUILD_CAP)
        buildEnd = LongArray(BUILD_CAP).also { it.fill(-1L) }
        buildReq = IntArray(BUILD_CAP)
        buildReqLater = IntArray(BUILD_CAP)
        snapAt = LongArray(SNAP_CAP)
        snapDone = LongArray(SNAP_CAP)
        snapReason = LongArray(SNAP_CAP)
        snapProcUt = LongArray(SNAP_CAP)
        snapProcSt = LongArray(SNAP_CAP)
        snapStart = IntArray(SNAP_CAP)
        snapLen = IntArray(SNAP_CAP)
        snapTid = IntArray(SNAP_CAP * THREADS_PER_SNAP)
        snapUt = LongArray(SNAP_CAP * THREADS_PER_SNAP)
        snapSt = LongArray(SNAP_CAP * THREADS_PER_SNAP)
    }

    /** From Application.onCreate, after its own work: starts the frame recorder and the probe thread. */
    fun start(context: Context) {
        if (mode == MODE_OFF) return
        appContext = context.applicationContext
        Choreographer.getInstance().postFrameCallback(frameCallback)
        if (mode == MODE_FULL && looperLog) Looper.getMainLooper().setMessageLogging(looperPrinter)
        val thread = Thread({ probeLoop() }, "tmc-probe")
        probeThread = thread
        thread.start()
    }

    fun activityCreated(activity: Activity) {
        if (mode == MODE_OFF) return
        activityRef = WeakReference(activity)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (closed) return
            val i = frameCount
            if (i < FRAME_CAP) {
                frameVsync[i] = frameTimeNanos - anchorNs
                frameRan[i] = rel()
                frameCount = i + 1
                /*
                 * The frame after a long one may begin the first stable window, which is
                 * known only 2 s later: take the CPU snapshot now, in case it does.
                 */
                if (mode == MODE_FULL && i > 0 && frameVsync[i] - frameVsync[i - 1] > LONG_FRAME_NS) {
                    pendingSnap.getAndUpdate { bits -> bits or REASON_LONG_FRAME_END }
                    probeThread?.let(LockSupport::unpark)
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    // ---------------------------------------------------------------------------------
    // Boundaries
    // ---------------------------------------------------------------------------------

    fun boundary(id: Int) {
        if (mode != MODE_FULL || closed) return
        if (boundaryNs[id] >= 0L) return
        boundaryNs[id] = rel()
        boundaryTid[id] = Process.myTid()
        pendingSnap.getAndUpdate { bits -> bits or (1L shl id) }
        probeThread?.let(LockSupport::unpark)
    }

    fun pageBoundary(slot: Int, k: Int) {
        if (slot in 0 until PAGES) boundary(PAGE_BASE + slot * PAGE_STRIDE + k)
    }

    /** The slot of a ChatFragment, assigned the first time it creates its view; -1 past two. */
    fun pageSlot(owner: Any): Int {
        if (mode != MODE_FULL) return -1
        for (slot in 0 until PAGES) {
            if (pageOwners[slot] === owner) return slot
            if (pageOwners[slot] == null) {
                pageOwners[slot] = owner
                return slot
            }
        }
        return -1
    }

    fun pageAccount(slot: Int, accountId: String, channel: String?) {
        if (slot !in 0 until PAGES) return
        pageAccount[slot] = accountId
        pageChannel[slot] = channel
    }

    // ---------------------------------------------------------------------------------
    // History (background thread, then the posts it makes)
    // ---------------------------------------------------------------------------------

    fun historyRequestStarted(slot: Int) {
        if (slot !in 0 until PAGES) return
        if (pageHistoryRequests.incrementAndGet(slot) == 1) {
            pageHistoryTid[slot] = Process.myTid()
            pageHistoryRequestAt[slot] = rel()
        }
    }

    fun historyArrived(slot: Int, messageCount: Int, requestedSec: Long) {
        if (slot !in 0 until PAGES || pageHistoryCount[slot] >= 0) return
        pageHistoryCount[slot] = messageCount
        pageHistorySec[slot] = requestedSec
        pageBoundary(slot, K_HISTORY)
    }

    /** Before each post of one history message to the main thread. Returns its ordinal. */
    fun historyPostQueued(slot: Int): Int {
        if (slot !in 0 until PAGES || mode != MODE_FULL) return -1
        return pagePostsQueued.incrementAndGet(slot)
    }

    /** As the posted history runnable starts on the main thread. */
    fun historyPostRunBegin(slot: Int) {
        if (slot !in 0 until PAGES || mode != MODE_FULL || closed) return
        pendingSource = SRC_HISTORY
        pendingSourceSlot = slot
    }

    /** As the posted history runnable ends: the last one of the page closes the insertion phase. */
    fun historyPostRunEnd(slot: Int) {
        if (slot !in 0 until PAGES || mode != MODE_FULL || closed) return
        pendingSource = SRC_LIVE
        pendingSourceSlot = -1
        pagePostsRun[slot]++
        if (pagePostsRun[slot] == pageHistoryCount[slot]) pageBoundary(slot, K_LAST_ROW)
    }

    // ---------------------------------------------------------------------------------
    // Rows (main thread)
    // ---------------------------------------------------------------------------------

    /** At the start of appendChatLine. Returns the row, or -1 when nothing is recorded. */
    fun rowBegin(slot: Int): Int {
        if (mode != MODE_FULL || closed || slot !in 0 until PAGES) return -1
        val i = rowCount
        if (i >= ROW_CAP) return -1
        val history = pendingSource == SRC_HISTORY && pendingSourceSlot == slot
        rowPage[i] = slot
        rowSource[i] = if (history) SRC_HISTORY else SRC_LIVE
        rowBegin[i] = rel()
        if (history) {
            rowOrdinal[i] = ++pageHistoryRowsBegun[slot]
            if (rowOrdinal[i] == 1) pageBoundary(slot, K_FIRST_ROW)
        }
        rowCount = i + 1
        currentRow = i
        return i
    }

    fun rowAbort(row: Int, reason: Int) {
        if (row < 0) return
        rowAbort[row] = reason
        if (currentRow == row) currentRow = -1
    }

    /**
     * Inside a row: [MARK_MENTION] after addMentionUser, [MARK_RECONCILE] after the echo reconcile,
     * [MARK_BUDDY] after the buddy capture, in appendChatLine; [MARK_BEFORE_INSERT] just before the
     * timeline insert and [MARK_AFTER_INSERT] just after it, in appendChatView.
     */
    fun rowMark(row: Int, mark: Int) {
        if (row < 0) return
        rowMarks[mark][row] = rel()
    }

    /** At the start of appendChatView. A call outside appendChatLine becomes a row of its own. */
    fun insertIn(slot: Int): Int {
        if (mode != MODE_FULL || closed || slot !in 0 until PAGES) return -1
        var row = currentRow
        if (row < 0) {
            val i = rowCount
            if (i >= ROW_CAP) return -1
            rowPage[i] = slot
            rowSource[i] = SRC_OTHER
            rowBegin[i] = rel()
            rowCount = i + 1
            row = i
            currentRow = i
        }
        rowInsertIn[row] = rel()
        return row
    }

    fun insertOut(row: Int) {
        if (row < 0) return
        rowInsertOut[row] = rel()
        if (currentRow == row) currentRow = -1
    }

    fun scrollPost(slot: Int) {
        if (mode != MODE_FULL || closed || slot !in 0 until PAGES) return
        pageScrollPosts[slot]++
        if (currentRow >= 0 && rowSource[currentRow] == SRC_HISTORY) pageScrollPostsInSpan[slot]++
    }

    // ---------------------------------------------------------------------------------
    // View builds (main thread)
    // ---------------------------------------------------------------------------------

    /**
     * As a row's view starts being built or bound, [kind] saying which. Inside a row - before 5d
     * the timeline insert built the view - the build is also that row's. Returns the build, or -1.
     */
    fun buildIn(kind: Int): Int {
        if (mode != MODE_FULL || closed) return -1
        val i = buildCount
        if (i >= BUILD_CAP) return -1
        buildKind[i] = kind
        buildRow[i] = currentRow
        buildStart[i] = rel()
        if (currentRow >= 0 && rowBuildIn[currentRow] < 0L) rowBuildIn[currentRow] = buildStart[i]
        buildCount = i + 1
        building = i
        return i
    }

    /** As the build [build] ends, with the view it built or bound. */
    fun buildOut(build: Int, view: View) {
        if (build < 0) return
        building = -1
        buildEnd[build] = rel()
        buildOfView[view] = build
        val row = buildRow[build]
        if (row >= 0) {
            rowBuildOut[row] = buildEnd[build]
            rowOfView[view] = row
        }
    }

    /** One Glide request, from TwitchEmoteImageLoader, for the view [view]. */
    fun emoteRequest(view: View) {
        if (mode != MODE_FULL || closed) return
        val during = building
        if (during >= 0) {
            buildReq[during]++
            val row = buildRow[during]
            if (row >= 0) rowReqBuild[row]++
            return
        }
        val row = rowOfView[view]
        if (row != null) {
            rowReqLater[row]++
            return
        }
        val build = buildOfView[view]
        if (build != null) buildReqLater[build]++ else reqUnattributed++
    }

    // ---------------------------------------------------------------------------------
    // Visibility (control only, and only with "visibility")
    // ---------------------------------------------------------------------------------

    /** A row's extent from [top] to [bottom], as a span lambda returns it to [viewport]. */
    fun span(top: Int, bottom: Int): Long = (top.toLong() shl 32) or (bottom.toLong() and 0xFFFFFFFFL)

    /**
     * On the emote gate's own reconcile, which only the control has: which rows of [slot] intersect
     * the viewport, by the gate's own rule, and whether the page is started and current, as the gate
     * requires. Without "visibility" it only counts and times its call: both builds' measurement runs
     * leave the walk off, so that neither pays for it.
     */
    fun viewport(
        slot: Int, top: Int, bottom: Int, current: Boolean, started: Boolean,
        container: ViewGroup, spanOf: (View) -> Long
    ) {
        if (mode != MODE_FULL || closed || slot !in 0 until PAGES) return
        val now = rel()
        pageViewportCalls[slot]++
        if (visibility && now <= WINDOW_NS) {
            val seen = current && started
            if (seen) pageEverCurrent[slot] = true
            forEachIntersecting(container, top, bottom, spanOf) { row ->
                if (rowInViewportAt[row] < 0L) rowInViewportAt[row] = now
                if (seen && rowSeenAt[row] < 0L) rowSeenAt[row] = now
            }
        }
        pageViewportNs[slot] += rel() - now
    }

    /*
     * The recorded rows whose span intersects [top, bottom), by the gate's own rule. The
     * container's children are laid out in order, so the walk starts from the nearer end
     * and stops at the first shown child past the viewport. A child that is gone has no
     * current bounds and is neither visible nor used to stop the walk.
     */
    private inline fun forEachIntersecting(
        container: ViewGroup, top: Int, bottom: Int,
        spanOf: (View) -> Long, mark: (Int) -> Unit
    ) {
        val count = container.childCount
        if (count == 0) return
        val lastSpan = spanOf(container.getChildAt(count - 1))
        val fromEnd = lastSpan == NO_SPAN || top >= bottomOf(lastSpan) / 2
        var k = if (fromEnd) count - 1 else 0
        while (k in 0 until count) {
            val child = container.getChildAt(k)
            k += if (fromEnd) -1 else 1
            if (child.visibility == View.GONE) continue
            val span = spanOf(child)
            if (span == NO_SPAN) continue
            if (bottomOf(span) > top && topOf(span) < bottom) {
                val row = rowOfView[child]
                if (row != null) mark(row)
            } else if (if (fromEnd) bottomOf(span) <= top else topOf(span) >= bottom) {
                break
            }
        }
    }

    private fun topOf(span: Long): Int = (span shr 32).toInt()

    private fun bottomOf(span: Long): Int = span.toInt()

    // ---------------------------------------------------------------------------------
    // The probe thread: CPU snapshots, the 10 s boundary, the close and the dump
    // ---------------------------------------------------------------------------------

    private fun probeLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
        var tenDone = false
        val buffer = ByteArray(1024)
        while (true) {
            val now = rel()
            if (now >= CLOSE_NS) break
            if (!tenDone && now >= WINDOW_NS) {
                tenDone = true
                if (mode == MODE_FULL) {
                    boundaryNs[B_TEN_S] = now
                    pendingSnap.getAndUpdate { bits -> bits or (1L shl B_TEN_S) }
                }
            }
            if (mode == MODE_FULL) {
                val reason = pendingSnap.getAndSet(0L)
                if (reason != 0L && snapshots) snapshot(reason, buffer)
            }
            val wait = (if (tenDone) CLOSE_NS else WINDOW_NS) - rel()
            if (wait > 0L) LockSupport.parkNanos(wait)
        }
        if (mode == MODE_FULL) {
            boundaryNs[B_CLOSE] = rel()
            if (snapshots) snapshot(1L shl B_CLOSE, buffer)
        }
        /* Stop the main thread recording, then serialise what it recorded. */
        val latch = java.util.concurrent.CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            closed = true
            if (mode == MODE_FULL) Looper.getMainLooper().setMessageLogging(null)
            latch.countDown()
        }
        latch.await()
        val name = "run-${System.currentTimeMillis()}"
        try {
            val dir = File(appContext.filesDir, "startup-probe").apply { mkdirs() }
            File(dir, "$name.json").writeText(toJson())
            File(dir, "latest").writeText(name)
            Log.i(TAG, "dumped $name rows=$rowCount builds=$buildCount frames=$frameCount snaps=$snapCount")
        } catch (e: Exception) {
            Log.e(TAG, "dump failed", e)
        }
        if (calibrate && mode == MODE_FULL) runCalibration(name)
    }

    private fun snapshot(reason: Long, buffer: ByteArray) {
        val i = snapCount
        if (i >= SNAP_CAP) return
        val started = rel()
        snapAt[i] = started
        snapReason[i] = reason
        val pid = Process.myPid()
        readStat(0, "/proc/$pid/stat", buffer)
        snapProcUt[i] = lastUt
        snapProcSt[i] = lastSt
        snapStart[i] = snapThreadsUsed
        val tids = File("/proc/$pid/task").list() ?: emptyArray()
        var n = 0
        for (entry in tids) {
            if (n >= THREADS_PER_SNAP) break
            val tid = entry.toIntOrNull() ?: continue
            if (!readStat(tid, "/proc/$pid/task/$tid/stat", buffer)) continue
            val at = snapThreadsUsed + n
            snapTid[at] = tid
            snapUt[at] = lastUt
            snapSt[at] = lastSt
            n++
        }
        snapLen[i] = n
        snapThreadsUsed += n
        snapDone[i] = rel()
        snapCount = i + 1
        snapCostNs += snapDone[i] - started
    }

    private var lastUt = 0L
    private var lastSt = 0L
    /* One open file per thread, read again from its start: no open or close per snapshot. */
    private val statFiles = HashMap<Int, RandomAccessFile>()

    /*
     * Fields 14 and 15 of /proc/.../stat, after the parenthesised name. The name is at most
     * 15 bytes and may itself hold a ')', so the last ')' within 16 bytes of the '(' ends it.
     * [key] is the thread id, or 0 for the process; its name is kept the first time only.
     */
    private fun readStat(key: Int, path: String, buffer: ByteArray): Boolean {
        val length = try {
            val file = statFiles[key] ?: RandomAccessFile(path, "r").also { opened -> statFiles[key] = opened }
            file.seek(0L)
            file.read(buffer)
        } catch (e: Exception) {
            statFiles.remove(key)?.let { stale -> try { stale.close() } catch (_: Exception) {} }
            return false
        }
        if (length <= 0) return false
        var open = -1
        for (k in 0 until minOf(length, 12)) {
            if (buffer[k] == '('.code.toByte()) {
                open = k
                break
            }
        }
        if (open < 0) return false
        var close = -1
        for (k in minOf(length - 1, open + 16) downTo open + 1) {
            if (buffer[k] == ')'.code.toByte()) {
                close = k
                break
            }
        }
        if (close < 0) return false
        if (key != 0 && !threadNames.containsKey(key)) threadNames[key] = String(buffer, open + 1, close - open - 1)
        /* After ") ": field 3 is index 0, so utime (14) is index 11 and stime (15) index 12. */
        var field = 0
        var k = close + 2
        var value = 0L
        var ut = -1L
        while (k <= length) {
            val c = if (k < length) buffer[k].toInt() else ' '.code
            if (c == ' '.code) {
                if (field == 11) ut = value
                if (field == 12) {
                    lastUt = ut
                    lastSt = value
                    return true
                }
                field++
                value = 0L
            } else if (c in '0'.code..'9'.code) {
                value = value * 10 + (c - '0'.code)
            }
            k++
        }
        return false
    }

    // ---------------------------------------------------------------------------------
    // Calibration, after the window: what one hook call and one logged message cost
    // ---------------------------------------------------------------------------------

    /*
     * Times, on the main thread and after the dump, the two costs the probe adds per event: a hook
     * call that records a time (rowMark on a scratch row), and the Looper's dispatch log for one
     * message, measured as a chain of empty messages with the log on and then off. Each is taken
     * [CALIBRATION_ROUNDS] times. The report multiplies them by each run's own counts.
     */
    private fun runCalibration(name: String) {
        val hookNs = LongArray(CALIBRATION_ROUNDS)
        val withLogNs = LongArray(CALIBRATION_ROUNDS)
        val withoutLogNs = LongArray(CALIBRATION_ROUNDS)
        val handler = Handler(Looper.getMainLooper())
        for (round in 0 until CALIBRATION_ROUNDS) {
            val latch = java.util.concurrent.CountDownLatch(1)
            handler.post {
                /* Row 0's mark, overwritten: the run is already written out. */
                val start = System.nanoTime()
                for (k in 0 until CALIBRATION_HOOK_CALLS) rowMark(0, MARK_AFTER_INSERT)
                hookNs[round] = System.nanoTime() - start
                latch.countDown()
            }
            latch.await()
            withLogNs[round] = messageChain(handler, logged = true)
            withoutLogNs[round] = messageChain(handler, logged = false)
        }
        val out = StringBuilder()
        out.append("{\"hookCalls\":").append(CALIBRATION_HOOK_CALLS)
        out.append(",\"hookNs\":").append(hookNs.joinToString(",", "[", "]"))
        out.append(",\"messages\":").append(CALIBRATION_MESSAGES)
        out.append(",\"withLogNs\":").append(withLogNs.joinToString(",", "[", "]"))
        out.append(",\"withoutLogNs\":").append(withoutLogNs.joinToString(",", "[", "]")).append('}')
        try {
            File(File(appContext.filesDir, "startup-probe"), "$name-calibration.json").writeText(out.toString())
            Log.i(TAG, "calibrated $name")
        } catch (e: Exception) {
            Log.e(TAG, "calibration failed", e)
        }
    }

    /* [CALIBRATION_MESSAGES] empty messages, each posting the next, with the dispatch log on or off: their total time. */
    private fun messageChain(handler: Handler, logged: Boolean): Long {
        val latch = java.util.concurrent.CountDownLatch(1)
        var left = CALIBRATION_MESSAGES
        var start = 0L
        var elapsed = 0L
        val step = object : Runnable {
            override fun run() {
                left--
                if (left > 0) {
                    handler.post(this)
                } else {
                    elapsed = System.nanoTime() - start
                    Looper.getMainLooper().setMessageLogging(null)
                    latch.countDown()
                }
            }
        }
        handler.post {
            /* The printer drops every line once closed, as during the window it records them. */
            Looper.getMainLooper().setMessageLogging(if (logged) looperPrinter else null)
            start = System.nanoTime()
            handler.post(step)
        }
        latch.await()
        return elapsed
    }

    private const val CALIBRATION_ROUNDS = 5
    private const val CALIBRATION_HOOK_CALLS = 100_000
    private const val CALIBRATION_MESSAGES = 5_000

    // ---------------------------------------------------------------------------------
    // Serialisation, after the window
    // ---------------------------------------------------------------------------------

    private fun toJson(): String {
        val b = StringBuilder(1 shl 20)
        b.append("{\"mode\":").append(mode)
        b.append(",\"commit\":\"").append(com.fs.twitchminichat.BuildConfig.GIT_SHA).append('"')
        b.append(",\"flag\":\"").append(flagText.replace("\"", "'")).append('"')
        b.append(",\"anchorNs\":").append(anchorNs)
        b.append(",\"processStartRelNs\":").append(processStartRelNs)
        b.append(",\"mainTid\":").append(mainTid)
        b.append(",\"frames\":{\"vsync\":").longs(frameVsync, frameCount).append(",\"ran\":").longs(frameRan, frameCount).append("}")
        if (mode == MODE_FULL) {
            b.append(",\"visibility\":").append(visibility)
            b.append(",\"boundaryNames\":").append(BOUNDARY_NAMES.joinToString(",", "[", "]") { "\"$it\"" })
            b.append(",\"boundaries\":").longs(boundaryNs, B_COUNT)
            b.append(",\"boundaryTid\":").ints(boundaryTid, B_COUNT)
            val accounts = try {
                AccountRepository(appContext).loadAccounts().map { account -> account.id }
            } catch (e: Exception) {
                emptyList()
            }
            b.append(",\"accounts\":").append(accounts.joinToString(",", "[", "]") { "\"$it\"" })
            b.append(",\"pages\":[")
            for (slot in 0 until PAGES) {
                if (slot > 0) b.append(',')
                b.append("{\"slot\":").append(slot)
                b.append(",\"accountId\":\"").append(pageAccount[slot].orEmpty()).append('"')
                b.append(",\"channelFp\":\"").append(fingerprint(pageChannel[slot])).append('"')
                b.append(",\"historyCount\":").append(pageHistoryCount[slot])
                b.append(",\"historySec\":").append(pageHistorySec[slot])
                b.append(",\"historyTid\":").append(pageHistoryTid[slot])
                b.append(",\"historyRequestAtNs\":").append(pageHistoryRequestAt[slot])
                b.append(",\"historyRequests\":").append(pageHistoryRequests.get(slot))
                b.append(",\"postsQueued\":").append(pagePostsQueued.get(slot))
                b.append(",\"postsRun\":").append(pagePostsRun[slot])
                b.append(",\"historyRowsBegun\":").append(pageHistoryRowsBegun[slot])
                b.append(",\"scrollPosts\":").append(pageScrollPosts[slot])
                b.append(",\"scrollPostsFromHistoryRows\":").append(pageScrollPostsInSpan[slot])
                b.append(",\"everCurrent\":").append(pageEverCurrent[slot])
                b.append(",\"viewportCalls\":").append(pageViewportCalls[slot])
                b.append(",\"viewportNs\":").append(pageViewportNs[slot])
                b.append('}')
            }
            b.append(']')
            b.append(",\"reqUnattributed\":").append(reqUnattributed)
            b.append(",\"msgs\":{\"start\":").longs(msgStart, msgCount).append(",\"end\":").longs(msgEnd, msgCount).append('}')
            b.append(",\"rows\":{\"n\":").append(rowCount)
            b.append(",\"page\":").ints(rowPage, rowCount)
            b.append(",\"source\":").ints(rowSource, rowCount)
            b.append(",\"ordinal\":").ints(rowOrdinal, rowCount)
            b.append(",\"abort\":").ints(rowAbort, rowCount)
            b.append(",\"begin\":").longs(rowBegin, rowCount)
            for (mark in 0 until MARKS) b.append(",\"mark").append(mark).append("\":").longs(rowMarks[mark], rowCount)
            b.append(",\"buildIn\":").longs(rowBuildIn, rowCount)
            b.append(",\"buildOut\":").longs(rowBuildOut, rowCount)
            b.append(",\"insertIn\":").longs(rowInsertIn, rowCount)
            b.append(",\"insertOut\":").longs(rowInsertOut, rowCount)
            b.append(",\"reqBuild\":").ints(rowReqBuild, rowCount)
            b.append(",\"reqLater\":").ints(rowReqLater, rowCount)
            b.append(",\"seenAt\":").longs(rowSeenAt, rowCount)
            b.append(",\"inViewportAt\":").longs(rowInViewportAt, rowCount)
            b.append('}')
            b.append(",\"builds\":{\"n\":").append(buildCount)
            b.append(",\"kind\":").ints(buildKind, buildCount)
            b.append(",\"row\":").ints(buildRow, buildCount)
            b.append(",\"start\":").longs(buildStart, buildCount)
            b.append(",\"end\":").longs(buildEnd, buildCount)
            b.append(",\"req\":").ints(buildReq, buildCount)
            b.append(",\"reqLater\":").ints(buildReqLater, buildCount)
            b.append('}')
            b.append(",\"snaps\":{\"n\":").append(snapCount).append(",\"costNs\":").append(snapCostNs)
            b.append(",\"at\":").longs(snapAt, snapCount)
            b.append(",\"done\":").longs(snapDone, snapCount)
            b.append(",\"reason\":").longs(snapReason, snapCount)
            b.append(",\"procUt\":").longs(snapProcUt, snapCount)
            b.append(",\"procSt\":").longs(snapProcSt, snapCount)
            b.append(",\"start\":").ints(snapStart, snapCount)
            b.append(",\"len\":").ints(snapLen, snapCount)
            b.append(",\"tid\":").ints(snapTid, snapThreadsUsed)
            b.append(",\"ut\":").longs(snapUt, snapThreadsUsed)
            b.append(",\"st\":").longs(snapSt, snapThreadsUsed)
            b.append(",\"names\":{")
            threadNames.entries.forEachIndexed { k, (tid, comm) ->
                if (k > 0) b.append(',')
                b.append('"').append(tid).append("\":\"").append(comm.replace("\\", "\\\\").replace("\"", "'")).append('"')
            }
            b.append("}}")
        }
        b.append('}')
        return b.toString()
    }

    /* Enough to tell two channels apart without writing either name down. */
    private fun fingerprint(channel: String?): String {
        val normalized = channel?.trim()?.removePrefix("#")?.lowercase().orEmpty()
        if (normalized.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.take(4).joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun StringBuilder.longs(values: LongArray, n: Int): StringBuilder {
        append('[')
        for (k in 0 until n) {
            if (k > 0) append(',')
            append(values[k])
        }
        return append(']')
    }

    private fun StringBuilder.ints(values: IntArray, n: Int): StringBuilder {
        append('[')
        for (k in 0 until n) {
            if (k > 0) append(',')
            append(values[k])
        }
        return append(']')
    }
}
