"""Build M: remove every qualification injection, and the per-frame sampler that only qualification used."""
import re

ROOT = "app/src/main/java/com/fs/twitchminichat/"


def edit(name, pairs):
    path = ROOT + name
    raw = open(path, "rb").read().decode("utf-8")
    nl = "\r\n" if "\r\n" in raw else "\n"
    s = raw.replace("\r\n", "\n")
    for old, new in pairs:
        n = s.count(old)
        assert n == 1, (name, n, old[:100])
        s = s.replace(old, new)
    open(path, "wb").write(s.replace("\n", nl).encode("utf-8"))
    print("stripped:", name, len(pairs))


P = "com.fs.twitchminichat.probe.StartupProbe"

edit("probe/ProbeApplication.kt", [
    ("        StartupProbe.inject(\"app\")\n", ""),
])

edit("probe/StartupProbe.kt", [
    ("    @JvmField var inject = \"\"\n    @JvmField var injectArg = 0L\n", ""),
    ("    private val pageFrameEvaluators = arrayOfNulls<() -> Unit>(PAGES)\n    private val pageScrollers = arrayOfNulls<() -> Unit>(PAGES)\n", ""),
    ("    private lateinit var rowSeenFrameAt: LongArray\n", ""),
    ("    /* Qualification only: when the injected delay ran. */\n    private var injectStartNs = -1L\n    private var injectEndNs = -1L\n    private var injectThreadTid = 0\n\n", ""),
    ("                word.startsWith(\"inject=\") -> inject = word.removePrefix(\"inject=\")\n"
     "                word.startsWith(\"arg=\") -> injectArg = word.removePrefix(\"arg=\").toLongOrNull() ?: 0L\n", ""),
    ("        rowSeenFrameAt = LongArray(ROW_CAP) { -1L }\n", ""),
    ("        if (inject == \"stall\") {\n            Handler(Looper.getMainLooper()).postAtTime(\n                { injected { Thread.sleep(50) } },\n"
     "                (anchorNs + injectArg * 1_000_000L) / 1_000_000L\n            )\n        }\n"
     "        if (inject == \"scroll\") {\n            Handler(Looper.getMainLooper()).postAtTime(\n"
     "                { injected { pageScrollers.forEachIndexed { slot, scroller -> if (pageEverCurrent[slot]) scroller?.invoke() } } },\n"
     "                (anchorNs + 8_000_000_000L) / 1_000_000L\n            )\n        }\n", ""),
    ("            if (inject == \"everyframe\") pageFrameEvaluators.forEach { evaluator -> evaluator?.invoke() }\n", ""),
    ("    fun registerPage(slot: Int, frameEvaluator: () -> Unit, scroller: () -> Unit) {\n        if (slot !in 0 until PAGES) return\n"
     "        pageFrameEvaluators[slot] = frameEvaluator\n        pageScrollers[slot] = scroller\n    }\n\n", ""),
    ("    /** Qualification only: the same test at every frame, to see what the reconcile sampling misses. */\n"
     "    fun viewportAtFrame(\n        slot: Int, top: Int, bottom: Int, current: Boolean, started: Boolean,\n"
     "        container: ViewGroup, spanOf: (View) -> TimelineSpan?\n    ) {\n"
     "        if (mode != MODE_FULL || closed || slot !in 0 until PAGES || !current || !started) return\n"
     "        val now = rel()\n        if (now > WINDOW_NS) return\n"
     "        forEachIntersecting(container, top, bottom, spanOf) { row ->\n"
     "            if (rowSeenFrameAt[row] < 0L) rowSeenFrameAt[row] = now\n        }\n    }\n\n", ""),
    ("    // ---------------------------------------------------------------------------------\n"
     "    // Qualification injections (build Q only)\n"
     "    // ---------------------------------------------------------------------------------\n\n"
     "    /** Runs [delay] if [name] is the injection this run asked for, on [slot] 0 only. */\n"
     "    fun inject(name: String, slot: Int = 0, ordinal: Int = -1, delay: () -> Unit = { Thread.sleep(50) }) {\n"
     "        if (mode != MODE_FULL || inject != name || slot != 0) return\n"
     "        if (ordinal >= 0 && ordinal.toLong() != (if (injectArg > 0) injectArg else 100L)) return\n"
     "        injected(delay)\n    }\n\n"
     "    fun historyOrdinalOfCurrentRow(): Int = if (currentRow >= 0) rowOrdinal[currentRow] else -1\n\n"
     "    private inline fun injected(delay: () -> Unit) {\n        if (injectStartNs >= 0L) return\n"
     "        injectThreadTid = Process.myTid()\n        injectStartNs = rel()\n        delay()\n        injectEndNs = rel()\n    }\n\n"
     "    /** A busy loop of [ms] milliseconds of CPU, for the CPU instrument's qualification. */\n"
     "    fun spin(ms: Long) {\n        val end = System.nanoTime() + ms * 1_000_000L\n        var x = 0L\n"
     "        while (System.nanoTime() < end) x += 1\n        if (x == -1L) Log.d(TAG, \"unreachable\")\n    }\n\n", ""),
])

s = open(ROOT + "probe/StartupProbe.kt", "rb").read().decode("utf-8").replace("\r\n", "\n")
# The JSON lines that wrote the injection fields and the per-frame sample.
pairs = []
for line in s.split("\n"):
    if "\\\"inject" in line or "seenFrameAt" in line:
        pairs.append((line + "\n", ""))
edit("probe/StartupProbe.kt", pairs)

edit("ChatFragment.kt", [
    ("        %s.inject(\"inflate\", probeSlot)\n" % P, ""),
    ("            %s.inject(\"viewcreated\", probeSlot)\n" % P, ""),
    ("            %s.inject(\"spin\", probeSlot) { %s.spin(50) }\n" % (P, P), ""),
    ("            %s.registerPage(\n                probeSlot,\n"
     "                frameEvaluator = { probeViewportAtFrame() },\n"
     "                scroller = { if (this::scrollChat.isInitialized) scrollChat.scrollTo(0, 0) }\n            )\n" % P, ""),
    ("            %s.inject(\"history\", probeSlotForHistory)\n" % P, ""),
    ("                    %s.inject(\"spinbg\", probeSlotForHistory) { %s.spin(50) }\n" % (P, P), ""),
    ("                        val probeOrdinal = %s.historyPostQueued(probeSlotForHistory)\n"
     "                        if (probeOrdinal == 200) {\n"
     "                            android.os.Handler(android.os.Looper.getMainLooper()).post {\n"
     "                                %s.inject(\"gap\", probeSlotForHistory)\n                            }\n                        }\n" % (P, P),
     "                        %s.historyPostQueued(probeSlotForHistory)\n" % P),
    ("        if (%s.historyOrdinalOfCurrentRow() >= 0) %s.inject(\"row\", probeSlot, %s.historyOrdinalOfCurrentRow())\n" % (P, P, P), ""),
    ("        if (%s.historyOrdinalOfCurrentRow() >= 0) %s.inject(\"build\", probeSlot, %s.historyOrdinalOfCurrentRow())\n" % (P, P, P), ""),
    ("\n    /* SCAFFOLD, qualification only: the same viewport test at every frame. */\n"
     "    private fun probeViewportAtFrame() {\n"
     "        if (!this::scrollChat.isInitialized || !this::chatContainer.isInitialized) return\n"
     "        val top = scrollChat.scrollY - chatContainer.top\n"
     "        %s.viewportAtFrame(\n            probeSlot, top, top + scrollChat.height,\n"
     "            current = isCurrentChatPage(),\n"
     "            started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),\n"
     "            container = chatContainer\n"
     "        ) { rowView -> timelineSpanOf(rowView) }\n    }\n" % P, ""),
])
