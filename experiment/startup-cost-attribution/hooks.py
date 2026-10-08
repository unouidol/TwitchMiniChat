"""Wire StartupProbe into the app. Every anchor text must occur exactly once."""
import re

ROOT = "app/src/main/java/com/fs/twitchminichat/"


def edit(name, pairs):
    path = ROOT + name
    raw = open(path, "rb").read().decode("utf-8")
    if "probe.StartupProbe" in raw:
        print("already hooked:", name)
        return
    nl = "\r\n" if "\r\n" in raw else "\n"
    s = raw.replace("\r\n", "\n")
    for old, new in pairs:
        n = s.count(old)
        assert n == 1, (name, n, old[:90])
        s = s.replace(old, new)
    open(path, "wb").write(s.replace("\n", nl).encode("utf-8"))
    print("hooked:", name, len(pairs))


P = "com.fs.twitchminichat.probe.StartupProbe"

edit("MainActivity.kt", [
    ("    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n",
     "    override fun onCreate(savedInstanceState: Bundle?) {\n"
     "        %s.boundary(%s.B_ACT_IN)\n"
     "        super.onCreate(savedInstanceState)\n" % (P, P)),
    ("        RemoteDeletionChecker.checkOnceOnAppOpen(\n            context = this,\n            onNoDeletionDetected = {\n"
     "                continueStartupAfterDeletionCheck()\n            }\n        )\n    }\n",
     "        RemoteDeletionChecker.checkOnceOnAppOpen(\n            context = this,\n            onNoDeletionDetected = {\n"
     "                continueStartupAfterDeletionCheck()\n            }\n        )\n"
     "        %s.activityCreated(this)\n"
     "        %s.boundary(%s.B_ACT_OUT)\n    }\n" % (P, P, P)),
])

edit("TwitchEmoteImageLoader.kt", [
    ("        session.track(target)\n        requestManager\n            .asGif()\n",
     "        session.track(target)\n        %s.emoteRequest(session.probeView)\n        requestManager\n            .asGif()\n" % P),
    ("        session.track(target)\n        requestManager\n            .asDrawable()\n",
     "        session.track(target)\n        %s.emoteRequest(session.probeView)\n        requestManager\n            .asDrawable()\n" % P),
    ("        private val targets = mutableSetOf<Target<*>>()\n",
     "        private val targets = mutableSetOf<Target<*>>()\n\n        /* SCAFFOLD: the row's view, for StartupProbe. */\n        val probeView: View get() = textView\n"),
])

F = "ChatFragment.kt"
edit(F, [
    # The slot field, next to accountId.
    ("    private var accountId: String = \"\"\n",
     "    private var accountId: String = \"\"\n\n    /* SCAFFOLD: this page's slot in StartupProbe; -1 when it records nothing. */\n    private var probeSlot = -1\n"),
    # Inflation: the Fragment(R.layout.fragment_chat) constructor inflates in super.onCreateView.
    ("    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {\n        super.onViewCreated(view, savedInstanceState)\n",
     "    override fun onCreateView(\n        inflater: android.view.LayoutInflater,\n        container: ViewGroup?,\n        savedInstanceState: Bundle?\n    ): View? {\n"
     "        probeSlot = %s.pageSlot(this)\n"
     "        %s.pageBoundary(probeSlot, %s.K_INFLATE_IN)\n"
     "        %s.inject(\"inflate\", probeSlot)\n"
     "        val inflated = super.onCreateView(inflater, container, savedInstanceState)\n"
     "        %s.pageBoundary(probeSlot, %s.K_INFLATE_OUT)\n"
     "        return inflated\n    }\n\n"
     "    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {\n"
     "        %s.pageBoundary(probeSlot, %s.K_VIEW_IN)\n"
     "        try {\n            onViewCreatedProbed(view, savedInstanceState)\n"
     "            %s.inject(\"viewcreated\", probeSlot)\n"
     "            %s.inject(\"spin\", probeSlot) { %s.spin(50) }\n"
     "        } finally {\n"
     "            %s.pageAccount(probeSlot, accountId, cfg?.channel)\n"
     "            %s.registerPage(\n                probeSlot,\n"
     "                frameEvaluator = { probeViewportAtFrame() },\n"
     "                scroller = { if (this::scrollChat.isInitialized) scrollChat.scrollTo(0, 0) }\n            )\n"
     "            %s.pageBoundary(probeSlot, %s.K_VIEW_OUT)\n        }\n    }\n\n"
     "    private fun onViewCreatedProbed(view: View, savedInstanceState: Bundle?) {\n"
     "        super.onViewCreated(view, savedInstanceState)\n"
     % ((P,) * 15)),
    # History: the request, its arrival, each post and each posted run.
    ("        thread {\n            val result = backendHistoryClient.load(\n",
     "        val probeSlotForHistory = probeSlot\n"
     "        thread {\n            %s.historyRequestStarted(probeSlotForHistory)\n"
     "            %s.inject(\"history\", probeSlotForHistory)\n"
     "            val result = backendHistoryClient.load(\n" % (P, P)),
    ("                is BackendHistoryResult.Success -> {\n                    /*\n                     * The account and the channel name the user",
     "                is BackendHistoryResult.Success -> {\n"
     "                    %s.historyArrived(probeSlotForHistory, result.messages.size, seconds.toLong())\n"
     "                    %s.inject(\"spinbg\", probeSlotForHistory) { %s.spin(50) }\n"
     "                    /*\n                     * The account and the channel name the user" % (P, P, P)),
    ("                    result.messages.forEach { message ->\n                        /* History is also a spawn source",
     "                    result.messages.forEach { message ->\n"
     "                        val probeOrdinal = %s.historyPostQueued(probeSlotForHistory)\n"
     "                        if (probeOrdinal == 200) {\n"
     "                            android.os.Handler(android.os.Looper.getMainLooper()).post {\n"
     "                                %s.inject(\"gap\", probeSlotForHistory)\n                            }\n                        }\n"
     "                        /* History is also a spawn source" % (P, P)),
    ("                        runUiIfAlive {\n                            appendChatLine(\n                                user = message.user,\n",
     "                        runUiIfAlive {\n"
     "                            %s.historyPostRunBegin(probeSlotForHistory)\n"
     "                            appendChatLine(\n                                user = message.user,\n" % P),
    ("                            if (spawnIngestion.snapshotChanged) {\n                                refreshOpenQuickCatchMenuIfNeeded()\n                            }\n                        }\n",
     "                            if (spawnIngestion.snapshotChanged) {\n                                refreshOpenQuickCatchMenuIfNeeded()\n                            }\n"
     "                            %s.historyPostRunEnd(probeSlotForHistory)\n                        }\n" % P),
    # Rows: begin, the two early returns, the build, then the insert inside appendChatView.
    ("        if (chatMessageDeduplicator.shouldSuppress(dedupKey)) return\n",
     "        val probeRow = %s.rowBegin(probeSlot)\n"
     "        if (chatMessageDeduplicator.shouldSuppress(dedupKey)) {\n"
     "            %s.rowAbort(probeRow, %s.ABORT_DEDUP)\n            return\n        }\n" % (P, P, P)),
    ("        if (isUserHidden(user)) {\n            Log.d(\"CHAT_HIDE\", \"skip message from hidden user\")\n            return\n        }\n\n        val tv = createMessageTextView(\n",
     "        if (isUserHidden(user)) {\n            Log.d(\"CHAT_HIDE\", \"skip message from hidden user\")\n"
     "            %s.rowAbort(probeRow, %s.ABORT_HIDDEN)\n            return\n        }\n\n"
     "        %s.rowBuildIn(probeRow)\n"
     "        if (%s.historyOrdinalOfCurrentRow() >= 0) %s.inject(\"build\", probeSlot, %s.historyOrdinalOfCurrentRow())\n"
     "        val tv = createMessageTextView(\n" % ((P,) * 6)),
    ("        attachSwipeReplyAction(\n            tv = tv,\n            messageId = msgId,\n            user = user,\n            message = message\n        )\n\n        appendChatView(\n",
     "        attachSwipeReplyAction(\n            tv = tv,\n            messageId = msgId,\n            user = user,\n            message = message\n        )\n"
     "        %s.rowBuildOut(probeRow, tv)\n\n        appendChatView(\n" % P),
    ("        row: (ChatTimelinePosition) -> ChatTimelineRow\n    ) {\n        val hadComposerFocus",
     "        row: (ChatTimelinePosition) -> ChatTimelineRow\n    ) {\n"
     "        val probeRow = %s.insertIn(probeSlot, view)\n"
     "        if (%s.historyOrdinalOfCurrentRow() >= 0) %s.inject(\"row\", probeSlot, %s.historyOrdinalOfCurrentRow())\n"
     "        try {\n            appendChatViewProbed(view, forceScroll, countAsUnread, messageTimestampSec, preservedTimelinePosition, row)\n"
     "        } finally {\n            %s.insertOut(probeRow)\n        }\n    }\n\n"
     "    private fun appendChatViewProbed(\n        view: View,\n        forceScroll: Boolean,\n        countAsUnread: Boolean,\n"
     "        messageTimestampSec: Double?,\n        preservedTimelinePosition: ChatTimelinePosition?,\n"
     "        row: (ChatTimelinePosition) -> ChatTimelineRow\n    ) {\n        val hadComposerFocus" % ((P,) * 5)),
    # Scroll posts.
    ("    private fun scrollToBottom() {\n        scrollChat.post {\n",
     "    private fun scrollToBottom() {\n        %s.scrollPost(probeSlot)\n        scrollChat.post {\n" % P),
    # Visibility: on the gate's own reconcile, with the gate's own viewport and spanOf.
    ("        val top = scrollChat.scrollY - chatContainer.top\n        gate.onViewport(top = top, bottom = top + scrollChat.height)\n    }\n",
     "        val top = scrollChat.scrollY - chatContainer.top\n        gate.onViewport(top = top, bottom = top + scrollChat.height)\n"
     "        %s.viewport(\n            probeSlot, top, top + scrollChat.height,\n"
     "            current = isCurrentChatPage(),\n"
     "            started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)\n"
     "        ) { rowView -> timelineSpanOf(rowView) }\n    }\n\n"
     "    /* SCAFFOLD, qualification only: the same viewport test at every frame. */\n"
     "    private fun probeViewportAtFrame() {\n"
     "        if (!this::scrollChat.isInitialized || !this::chatContainer.isInitialized) return\n"
     "        val top = scrollChat.scrollY - chatContainer.top\n"
     "        %s.viewportAtFrame(\n            probeSlot, top, top + scrollChat.height,\n"
     "            current = isCurrentChatPage(),\n"
     "            started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)\n"
     "        ) { rowView -> timelineSpanOf(rowView) }\n    }\n" % (P, P)),
])
