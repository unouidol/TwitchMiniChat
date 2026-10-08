package com.fs.twitchminichat.probe

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.fs.twitchminichat.AccountRepository
import com.fs.twitchminichat.chat.TimelineSpan
import java.io.File
import java.io.RandomAccessFile
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.LockSupport

/**
 * SCAFFOLD for experiment/startup-cost-attribution. Never merged.
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
 * - "full": frames, phase boundaries, rows, visibility, emote requests, per-thread CPU.
 */
object StartupProbe {

    const val MODE_OFF = 0
    const val MODE_YARDSTICK = 1
    const val MODE_FULL = 2

    private const val TAG = "TMC_PROBE"

    const val WINDOW_NS = 10_000_000_000L
    const val CLOSE_NS = 14_000_000_000L
    private const val SNAP_PERIOD_NS = 1_000_000_000L
    private const val LONG_FRAME_NS = 32_000_000L
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

    private const val PAGES = 2
    private const val ROW_CAP = 3072
    private const val FRAME_CAP = 4096
    private const val SNAP_CAP = 420
    private const val THREADS_PER_SNAP = 192

    @JvmField var anchorNs = 0L
    @JvmField var mode = MODE_OFF
    @JvmField var inject = ""
    @JvmField var injectArg = 0L
    @JvmField var harness = false
    @Volatile private var closed = false

    private lateinit var appContext: Context
    private var processStartRelNs = 0L
    private var mainTid = 0

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
    private val pageFrameEvaluators = arrayOfNulls<() -> Unit>(PAGES)
    private val pageScrollers = arrayOfNulls<() -> Unit>(PAGES)

    /* Rows: every appendChatLine call, and every other appendChatView call. */
    private lateinit var rowPage: IntArray
    private lateinit var rowSource: IntArray
    private lateinit var rowOrdinal: IntArray
    private lateinit var rowAbort: IntArray
    private lateinit var rowBegin: LongArray
    private lateinit var rowMark1: LongArray
    private lateinit var rowMark2: LongArray
    private lateinit var rowBuildIn: LongArray
    private lateinit var rowBuildOut: LongArray
    private lateinit var rowInsertIn: LongArray
    private lateinit var rowInsertOut: LongArray
    private lateinit var rowReqBuild: IntArray
    private lateinit var rowReqLater: IntArray
    private lateinit var rowSeenAt: LongArray
    private lateinit var rowInViewportAt: LongArray
    private lateinit var rowSeenFrameAt: LongArray
    private lateinit var rowView: Array<View?>
    private val rowOfView = IdentityHashMap<View, Int>(ROW_CAP * 2)
    private var rowCount = 0
    private var currentRow = -1
    private var buildingRow = -1
    private var reqUnattributed = 0
    private var pendingSource = SRC_LIVE
    private var pendingSourceSlot = -1

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

    /* Qualification only: when the injected delay ran. */
    private var injectStartNs = -1L
    private var injectEndNs = -1L
    private var injectThreadTid = 0

    private var activityRef: WeakReference<Activity>? = null

    /** Offset of now from the anchor. */
    @JvmStatic
    fun rel(): Long = System.nanoTime() - anchorNs

    /** First thing the process runs in its own code. */
    fun onAttach(base: Context) {
        anchorNs = System.nanoTime()
        val flag = File(base.filesDir, "startup-probe/flag")
        if (!flag.exists()) return
        val words = flag.readText().trim().split(Regex("\\s+"))
        mode = when (words.firstOrNull()) {
            "full" -> MODE_FULL
            "yardstick" -> MODE_YARDSTICK
            else -> MODE_OFF
        }
        if (mode == MODE_OFF) return
        for (word in words.drop(1)) {
            when {
                word.startsWith("inject=") -> inject = word.removePrefix("inject=")
                word.startsWith("arg=") -> injectArg = word.removePrefix("arg=").toLongOrNull() ?: 0L
                word == "harness" -> harness = true
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
        rowPage = IntArray(ROW_CAP)
        rowSource = IntArray(ROW_CAP)
        rowOrdinal = IntArray(ROW_CAP)
        rowAbort = IntArray(ROW_CAP)
        rowBegin = LongArray(ROW_CAP)
        rowMark1 = LongArray(ROW_CAP) { -1L }
        rowMark2 = LongArray(ROW_CAP) { -1L }
        rowBuildIn = LongArray(ROW_CAP) { -1L }
        rowBuildOut = LongArray(ROW_CAP) { -1L }
        rowInsertIn = LongArray(ROW_CAP) { -1L }
        rowInsertOut = LongArray(ROW_CAP) { -1L }
        rowReqBuild = IntArray(ROW_CAP)
        rowReqLater = IntArray(ROW_CAP)
        rowSeenAt = LongArray(ROW_CAP) { -1L }
        rowInViewportAt = LongArray(ROW_CAP) { -1L }
        rowSeenFrameAt = LongArray(ROW_CAP) { -1L }
        rowView = arrayOfNulls(ROW_CAP)
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
        val thread = Thread({ probeLoop() }, "tmc-probe")
        probeThread = thread
        thread.start()
        if (inject == "stall") {
            Handler(Looper.getMainLooper()).postAtTime(
                { injected { Thread.sleep(50) } },
                (anchorNs + injectArg * 1_000_000L) / 1_000_000L
            )
        }
        if (inject == "scroll") {
            Handler(Looper.getMainLooper()).postAtTime(
                { injected { pageScrollers.forEachIndexed { slot, scroller -> if (pageEverCurrent[slot]) scroller?.invoke() } } },
                (anchorNs + 8_000_000_000L) / 1_000_000L
            )
        }
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
            if (inject == "everyframe") pageFrameEvaluators.forEach { evaluator -> evaluator?.invoke() }
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

    fun registerPage(slot: Int, frameEvaluator: () -> Unit, scroller: () -> Unit) {
        if (slot !in 0 until PAGES) return
        pageFrameEvaluators[slot] = frameEvaluator
        pageScrollers[slot] = scroller
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

    /** Inside appendChatLine before the build: 1 after the echo reconcile, 2 after the buddy capture. */
    fun rowMark(row: Int, mark: Int) {
        if (row < 0) return
        if (mark == 1) rowMark1[row] = rel() else rowMark2[row] = rel()
    }

    fun rowBuildIn(row: Int) {
        if (row < 0) return
        rowBuildIn[row] = rel()
        buildingRow = row
    }

    fun rowBuildOut(row: Int, view: View) {
        if (row < 0) return
        buildingRow = -1
        rowBuildOut[row] = rel()
        rowView[row] = view
        rowOfView[view] = row
    }

    /** At the start of appendChatView. A call outside appendChatLine becomes a row of its own. */
    fun insertIn(slot: Int, view: View): Int {
        if (mode != MODE_FULL || closed || slot !in 0 until PAGES) return -1
        var row = currentRow
        if (row < 0) {
            val i = rowCount
            if (i >= ROW_CAP) return -1
            rowPage[i] = slot
            rowSource[i] = SRC_OTHER
            rowBegin[i] = rel()
            rowView[i] = view
            rowOfView[view] = i
            rowCount = i + 1
            row = i
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

    /** One Glide request, from TwitchEmoteImageLoader, for the row whose view is [view]. */
    fun emoteRequest(view: View) {
        if (mode != MODE_FULL || closed) return
        val building = buildingRow
        if (building >= 0) {
            rowReqBuild[building]++
            return
        }
        val row = rowOfView[view]
        if (row != null) rowReqLater[row]++ else reqUnattributed++
    }

    /**
     * On the emote gate's own reconcile: which rows of [slot] intersect the viewport, by
     * the gate's own rule, and whether the page is started and current, as the gate requires.
     */
    fun viewport(
        slot: Int, top: Int, bottom: Int, current: Boolean, started: Boolean,
        container: ViewGroup, spanOf: (View) -> TimelineSpan?
    ) {
        if (mode != MODE_FULL || closed || slot !in 0 until PAGES) return
        val now = rel()
        if (now > WINDOW_NS) return
        val seen = current && started
        if (seen) pageEverCurrent[slot] = true
        forEachIntersecting(container, top, bottom, spanOf) { row ->
            if (rowInViewportAt[row] < 0L) rowInViewportAt[row] = now
            if (seen && rowSeenAt[row] < 0L) rowSeenAt[row] = now
        }
        pageViewportCalls[slot]++
        pageViewportNs[slot] += rel() - now
    }

    /** Qualification only: the same test at every frame, to see what the reconcile sampling misses. */
    fun viewportAtFrame(
        slot: Int, top: Int, bottom: Int, current: Boolean, started: Boolean,
        container: ViewGroup, spanOf: (View) -> TimelineSpan?
    ) {
        if (mode != MODE_FULL || closed || slot !in 0 until PAGES || !current || !started) return
        val now = rel()
        if (now > WINDOW_NS) return
        forEachIntersecting(container, top, bottom, spanOf) { row ->
            if (rowSeenFrameAt[row] < 0L) rowSeenFrameAt[row] = now
        }
    }

    /*
     * The recorded rows whose span intersects [top, bottom), by the gate's own rule. The
     * container's children are laid out in order, so the walk starts from the nearer end
     * and stops at the first shown child past the viewport. A child that is gone has no
     * current bounds and is neither visible nor used to stop the walk.
     */
    private inline fun forEachIntersecting(
        container: ViewGroup, top: Int, bottom: Int,
        spanOf: (View) -> TimelineSpan?, mark: (Int) -> Unit
    ) {
        val count = container.childCount
        if (count == 0) return
        val lastSpan = spanOf(container.getChildAt(count - 1))
        val fromEnd = lastSpan == null || top >= lastSpan.bottom / 2
        var k = if (fromEnd) count - 1 else 0
        while (k in 0 until count) {
            val child = container.getChildAt(k)
            k += if (fromEnd) -1 else 1
            if (child.visibility == View.GONE) continue
            val span = spanOf(child) ?: continue
            if (span.bottom > top && span.top < bottom) {
                val row = rowOfView[child]
                if (row != null) mark(row)
            } else if (if (fromEnd) span.bottom <= top else span.top >= bottom) {
                break
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Qualification injections (build Q only)
    // ---------------------------------------------------------------------------------

    /** Runs [delay] if [name] is the injection this run asked for, on [slot] 0 only. */
    fun inject(name: String, slot: Int = 0, ordinal: Int = -1, delay: () -> Unit = { Thread.sleep(50) }) {
        if (mode != MODE_FULL || inject != name || slot != 0) return
        if (ordinal >= 0 && ordinal.toLong() != (if (injectArg > 0) injectArg else 100L)) return
        injected(delay)
    }

    fun historyOrdinalOfCurrentRow(): Int = if (currentRow >= 0) rowOrdinal[currentRow] else -1

    private inline fun injected(delay: () -> Unit) {
        if (injectStartNs >= 0L) return
        injectThreadTid = Process.myTid()
        injectStartNs = rel()
        delay()
        injectEndNs = rel()
    }

    /** A busy loop of [ms] milliseconds of CPU, for the CPU instrument's qualification. */
    fun spin(ms: Long) {
        val end = System.nanoTime() + ms * 1_000_000L
        var x = 0L
        while (System.nanoTime() < end) x += 1
        if (x == -1L) Log.d(TAG, "unreachable")
    }

    // ---------------------------------------------------------------------------------
    // The probe thread: CPU snapshots, the 10 s boundary, the close and the dump
    // ---------------------------------------------------------------------------------

    private fun probeLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
        var nextPeriodic = rel()
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
                if (reason != 0L || now >= nextPeriodic) {
                    snapshot(reason, buffer)
                    if (now >= nextPeriodic) nextPeriodic = now + SNAP_PERIOD_NS
                }
            }
            val wait = minOf(if (mode == MODE_FULL) nextPeriodic else CLOSE_NS, if (tenDone) CLOSE_NS else WINDOW_NS) - rel()
            if (wait > 0L) LockSupport.parkNanos(wait)
        }
        if (mode == MODE_FULL) {
            boundaryNs[B_CLOSE] = rel()
            snapshot(1L shl B_CLOSE, buffer)
        }
        /* Stop the main thread recording, then serialise what it recorded. */
        val latch = java.util.concurrent.CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            closed = true
            latch.countDown()
        }
        latch.await()
        val name = "run-${System.currentTimeMillis()}"
        try {
            val dir = File(appContext.filesDir, "startup-probe").apply { mkdirs() }
            File(dir, "$name.json").writeText(toJson())
            File(dir, "latest").writeText(name)
            Log.i(TAG, "dumped $name rows=$rowCount frames=$frameCount snaps=$snapCount")
        } catch (e: Exception) {
            Log.e(TAG, "dump failed", e)
        }
        if (harness) runHarness(name)
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
    // The optional inflation harness: fragment_chat with and without its GeckoView node
    // ---------------------------------------------------------------------------------

    private fun runHarness(name: String) {
        val activity = activityRef?.get() ?: return
        val results = StringBuilder()
        val latch = java.util.concurrent.CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            try {
                val inflater = LayoutInflater.from(activity)
                val parent = FrameLayout(activity)
                val withGecko = LongArray(20)
                val withoutGecko = LongArray(20)
                for (k in 0 until 20) {
                    var t = SystemClock.elapsedRealtimeNanos()
                    inflater.inflate(com.fs.twitchminichat.R.layout.fragment_chat, parent, false)
                    withGecko[k] = SystemClock.elapsedRealtimeNanos() - t
                    t = SystemClock.elapsedRealtimeNanos()
                    inflater.inflate(com.fs.twitchminichat.R.layout.fragment_chat_nogecko, parent, false)
                    withoutGecko[k] = SystemClock.elapsedRealtimeNanos() - t
                }
                results.append("{\"withGeckoNs\":").append(withGecko.joinToString(",", "[", "]"))
                    .append(",\"withoutGeckoNs\":").append(withoutGecko.joinToString(",", "[", "]")).append("}")
            } catch (e: Exception) {
                results.append("{\"error\":\"").append(e.javaClass.simpleName).append("\"}")
            }
            latch.countDown()
        }
        latch.await()
        File(File(appContext.filesDir, "startup-probe"), "$name-harness.json").writeText(results.toString())
        Log.i(TAG, "harness $name")
    }

    // ---------------------------------------------------------------------------------
    // Serialisation, after the window
    // ---------------------------------------------------------------------------------

    private fun toJson(): String {
        val b = StringBuilder(1 shl 20)
        b.append("{\"mode\":").append(mode)
        b.append(",\"inject\":\"").append(inject).append("\",\"injectArg\":").append(injectArg)
        b.append(",\"injectStartNs\":").append(injectStartNs).append(",\"injectEndNs\":").append(injectEndNs)
        b.append(",\"injectTid\":").append(injectThreadTid)
        b.append(",\"anchorNs\":").append(anchorNs)
        b.append(",\"processStartRelNs\":").append(processStartRelNs)
        b.append(",\"mainTid\":").append(mainTid)
        b.append(",\"frames\":{\"vsync\":").longs(frameVsync, frameCount).append(",\"ran\":").longs(frameRan, frameCount).append("}")
        if (mode == MODE_FULL) {
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
            b.append(",\"rows\":{\"n\":").append(rowCount)
            b.append(",\"page\":").ints(rowPage, rowCount)
            b.append(",\"source\":").ints(rowSource, rowCount)
            b.append(",\"ordinal\":").ints(rowOrdinal, rowCount)
            b.append(",\"abort\":").ints(rowAbort, rowCount)
            b.append(",\"begin\":").longs(rowBegin, rowCount)
            b.append(",\"mark1\":").longs(rowMark1, rowCount)
            b.append(",\"mark2\":").longs(rowMark2, rowCount)
            b.append(",\"buildIn\":").longs(rowBuildIn, rowCount)
            b.append(",\"buildOut\":").longs(rowBuildOut, rowCount)
            b.append(",\"insertIn\":").longs(rowInsertIn, rowCount)
            b.append(",\"insertOut\":").longs(rowInsertOut, rowCount)
            b.append(",\"reqBuild\":").ints(rowReqBuild, rowCount)
            b.append(",\"reqLater\":").ints(rowReqLater, rowCount)
            b.append(",\"seenAt\":").longs(rowSeenAt, rowCount)
            b.append(",\"inViewportAt\":").longs(rowInViewportAt, rowCount)
            b.append(",\"seenFrameAt\":").longs(rowSeenFrameAt, rowCount)
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
