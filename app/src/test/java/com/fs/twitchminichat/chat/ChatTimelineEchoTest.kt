package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import com.fs.twitchminichat.OutgoingChatMessageTracker
import com.fs.twitchminichat.PendingOutgoingChatMessage
import com.fs.twitchminichat.chat.PendingEchoEvent.NOTICE
import com.fs.twitchminichat.chat.PendingEchoEvent.TIMEOUT
import com.fs.twitchminichat.chat.PendingEchoEvent.USERSTATE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Drives the pending outgoing echo through [ChatTimelineViewSync] over a list standing in
 * for the layout, and checks that the echo is found by its local id, that a change of
 * status rebinds the view already there - never adds, removes or rebuilds one - and that
 * the canonical message takes the echo's sequence.
 *
 * The list behaves as PendingEchoView does: an echo starts as sending, and a rebind sets
 * its status line, hidden when there is none, and its opacity, nothing else.
 *
 * The last test replays random sending, answers and canonical messages against a
 * transcription of ChatFragment's echo code at 4f90dbd. Today's side decides which echo each
 * answer names through the real OutgoingChatMessageTracker; the transcription, through its own
 * copy of the tracker as it was at 4f90dbd.
 */
class ChatTimelineEchoTest {

    private var clockMillis = 1_000_000L
    private val views = EchoViews()
    private val timeline = ChatTimeline(currentTimeMillis = { clockMillis })
    private val sync = ChatTimelineViewSync(views, timeline)

    // ---------------------------------------------------------------------------
    // A change of status rebinds; it never rebuilds
    // ---------------------------------------------------------------------------

    @Test
    fun aChangeOfStatus_rebindsTheSameView_andAddsOrRemovesNothing() {
        echo("e1", "local-1", 10.0)
        message("bot", "bot", 11.0)
        val addsAfterSending = views.adds

        sync.applyEchoEvent("local-1", TIMEOUT)
        sync.applyEchoEvent("local-1", USERSTATE)

        assertEquals(addsAfterSending, views.adds)
        assertEquals(0, views.removes)
        assertEquals(listOf("e1", "bot"), views.views)
        assertEquals(
            listOf(
                Rebind("e1", ChatTimelineChange.ECHO_STATUS, PendingEchoStatus.UNCONFIRMED),
                Rebind("e1", ChatTimelineChange.ECHO_STATUS, PendingEchoStatus.CONFIRMED)
            ),
            views.rebinds
        )
    }

    @Test
    fun theEchoLooksAsItsStatusSays_afterEachEvent() {
        echo("e1", "local-1", 10.0)
        assertEquals(Look(PendingEchoStatusLine.SENDING, true, 0.72f), views.looks["e1"])

        sync.applyEchoEvent("local-1", TIMEOUT)
        assertEquals(Look(PendingEchoStatusLine.UNCONFIRMED, true, 0.62f), views.looks["e1"])

        sync.applyEchoEvent("local-1", NOTICE)
        assertEquals(Look(PendingEchoStatusLine.REJECTED, true, 0.5f), views.looks["e1"])
    }

    @Test
    fun theTimelineKeepsTheEchosNewStatus_atTheSamePosition() {
        echo("e1", "local-1", 10.0)
        val position = timeline.rows.single().position

        sync.applyEchoEvent("local-1", USERSTATE)

        val echo = timeline.rows.single() as PendingEchoRow
        assertEquals(PendingEchoStatus.CONFIRMED, echo.status)
        assertEquals(position, echo.position)
    }

    @Test
    fun anEventThatLeavesTheStatusAsItWas_rebindsNothing_andSaysSo() {
        echo("e1", "local-1", 10.0)
        sync.applyEchoEvent("local-1", USERSTATE)

        assertFalse(sync.applyEchoEvent("local-1", TIMEOUT))
        assertFalse(sync.applyEchoEvent("local-1", NOTICE))
        assertEquals(1, views.rebinds.size)
    }

    // ---------------------------------------------------------------------------
    // Finding the echo by its local id
    // ---------------------------------------------------------------------------

    @Test
    fun theEchoIsFoundByItsLocalId_amongOtherEchoesAndMessages() {
        echo("e1", "local-1", 10.0)
        message("bot", "bot", 11.0)
        echo("e2", "local-2", 12.0)

        assertTrue(sync.applyEchoEvent("local-2", NOTICE))

        assertEquals(listOf(Rebind("e2", ChatTimelineChange.ECHO_STATUS, PendingEchoStatus.REJECTED)), views.rebinds)
        assertEquals(Look(PendingEchoStatusLine.SENDING, true, 0.72f), views.looks["e1"])
    }

    @Test
    fun anEventForALocalIdNotInTheTimeline_changesNothing_andSaysSo() {
        echo("e1", "local-1", 10.0)

        assertFalse(sync.applyEchoEvent("local-9", USERSTATE))
        assertEquals(emptyList<Rebind>(), views.rebinds)
    }

    @Test
    fun aMessageIsNeverTakenForAnEcho_evenWithTheSameName() {
        message("local-1", "local-1", 10.0)

        assertFalse(sync.applyEchoEvent("local-1", USERSTATE))
        assertNull(sync.removeEcho("local-1"))
    }

    @Test
    fun theEchoIsFoundPastAMessageFromAUserNamedLikeItsLocalId() {
        message("m1", "local-1", 9.0)
        echo("e1", "local-1", 10.0)

        assertTrue(sync.applyEchoEvent("local-1", TIMEOUT))
        assertEquals(listOf(Rebind("e1", ChatTimelineChange.ECHO_STATUS, PendingEchoStatus.UNCONFIRMED)), views.rebinds)
    }

    @Test
    fun ifTwoEchoesEverSharedALocalId_theEarlierOneWouldBeTheOneMoved() {
        echo("e1", "local-1", 10.0)
        echo("e2", "local-1", 11.0)

        sync.applyEchoEvent("local-1", NOTICE)

        assertEquals(listOf(Rebind("e1", ChatTimelineChange.ECHO_STATUS, PendingEchoStatus.REJECTED)), views.rebinds)
    }

    @Test
    fun aSecondTimeout_rebindsNothing() {
        echo("e1", "local-1", 10.0)

        assertTrue(sync.applyEchoEvent("local-1", TIMEOUT))
        assertFalse(sync.applyEchoEvent("local-1", TIMEOUT))
        assertEquals(1, views.rebinds.size)
    }

    @Test
    fun aRejectedEcho_isNotReboundByALaterUserState() {
        echo("e1", "local-1", 10.0)
        sync.applyEchoEvent("local-1", NOTICE)

        assertFalse(sync.applyEchoEvent("local-1", USERSTATE))
        assertEquals(listOf(Rebind("e1", ChatTimelineChange.ECHO_STATUS, PendingEchoStatus.REJECTED)), views.rebinds)
    }

    @Test
    fun theRightEchoIsRebound_afterRowsBeforeItAreRemoved() {
        echo("e0", "local-0", 1.0)
        message("m2", "alice", 2.0)
        echo("e1", "local-1", 3.0)
        /* The row before the echo leaves as rows leave the page: an earlier echo replaced by its canonical message. */
        assertEquals(ChatTimelinePosition(timestampMillis = 1_000L, sequence = 0L), sync.removeEcho("local-0"))

        sync.applyEchoEvent("local-1", TIMEOUT)

        assertEquals(listOf(Rebind("e1", ChatTimelineChange.ECHO_STATUS, PendingEchoStatus.UNCONFIRMED)), views.rebinds)
    }

    // ---------------------------------------------------------------------------
    // The canonical message
    // ---------------------------------------------------------------------------

    @Test
    fun theCanonicalMessage_ofTheSameSecond_takesTheEchosPlace_beforeALaterBotReply() {
        echo("e1", "local-1", 10.0)
        message("bot", "bot", 10.0)

        val position = sync.removeEcho("local-1")
        message("canonical", "me", 10.0, preserved = position)

        assertEquals(listOf("canonical", "bot"), views.views)
    }

    @Test
    fun theCanonicalMessage_landsWhereItsOwnTimestampSays_whenItDisagreesWithTheEcho() {
        echo("e1", "local-1", 10.0)
        message("bot", "bot", 10.5)

        val position = sync.removeEcho("local-1")
        message("canonical", "me", 11.0, preserved = position)

        assertEquals(listOf("bot", "canonical"), views.views)
        assertEquals(position?.sequence, timeline.rows[1].position.sequence)
    }

    @Test
    fun removeEcho_ofALocalIdNotInTheTimeline_returnsNull_andChangesNothing() {
        echo("e1", "local-1", 10.0)

        assertNull(sync.removeEcho("local-9"))
        assertEquals(listOf("e1"), views.views)
        assertEquals(0, views.removes)
    }

    // ---------------------------------------------------------------------------
    // The echo among the rest of the timeline
    // ---------------------------------------------------------------------------

    @Test
    fun theEchoIsNeverHidden_evenWhenItsUserIs() {
        echo("e1", "local-1", 10.0)
        message("m1", "me", 11.0)

        sync.hideUser("me")

        assertEquals(listOf("e1"), views.visible())
    }

    @Test
    fun clear_removesTheEcho_andAnEventAfterwardsChangesNothing() {
        echo("e1", "local-1", 10.0)

        sync.clear()

        assertFalse(sync.applyEchoEvent("local-1", USERSTATE))
        assertNull(sync.removeEcho("local-1"))
        assertEquals(emptyList<String>(), views.views)
    }

    // ---------------------------------------------------------------------------
    // What is rebound in place
    // ---------------------------------------------------------------------------

    /**
     * The invariant behind both preconditions on a rebind in place - ChatRowViewBinder.rebind's,
     * that the view can be rebound, and PendingEchoView.rebind's, that the row is an echo: the sync
     * rebinds in place only a view it created for an echo, and only with that same echo's row, over
     * every operation it has, users hidden among them.
     */
    @Test
    fun onlyAViewCreatedForAnEcho_isEverReboundInPlace_andOnlyWithThatEchosRow() {
        val users = listOf("me", "alice", "bob")
        val events = PendingEchoEvent.values()
        var rebinds = 0
        for (seed in 1..100) {
            val random = Random(seed)
            val layout = KindLayout()
            val sync = ChatTimelineViewSync(layout, ChatTimeline(currentTimeMillis = { 1_000_000L }))
            var nextView = 0

            repeat(300) {
                when (random.nextInt(12)) {
                    in 0..2 -> {
                        layout.nextView = "v${nextView++}"
                        sync.insert(random.nextInt(1, 30).toDouble(), null) { position ->
                            messageRow(position, users[random.nextInt(users.size)])
                        }
                    }
                    3 -> {
                        layout.nextView = "v${nextView++}"
                        sync.insert(random.nextInt(1, 30).toDouble(), null) { position -> SystemLineRow(position, "line") }
                    }
                    in 4..5 -> {
                        val localId = "local-${random.nextInt(0, 6)}"
                        layout.nextView = "v${nextView++}"
                        sync.insert(random.nextInt(1, 30).toDouble(), null) { position ->
                            PendingEchoRow(position, localId, "me", "hello", null, null, 1.0, PendingEchoStatus.SENDING)
                        }
                    }
                    in 6..7 -> sync.applyEchoEvent("local-${random.nextInt(0, 7)}", events[random.nextInt(events.size)])
                    8 -> sync.removeEcho("local-${random.nextInt(0, 7)}")
                    9 -> sync.hideUser(users[random.nextInt(users.size)])
                    10 -> sync.setHiddenUsers(users.filter { random.nextBoolean() }.toSet())
                    else -> if (random.nextInt(8) == 0) sync.clear()
                }
            }
            rebinds += layout.rebinds
        }
        /* Not vacuous: the invariant was checked on every one of these rebinds. */
        assertTrue("only $rebinds rebinds happened, too few to check anything", rebinds > 1_000)
        println("rebinds in place checked: $rebinds")
    }

    // ---------------------------------------------------------------------------
    // Against ChatFragment at 4f90dbd
    // ---------------------------------------------------------------------------

    @Test
    fun randomSendingAndAnswers_produceTheSameEchoes_asChatFragmentAt4f90dbd() {
        val texts = listOf("hi", "gg", "pog", "/me waves")

        val coverage = OracleCoverage("4f90dbd", seeds = 200, operations = 300)
        for (seed in 1..200) {
            val random = Random(seed)
            var nowSec = 1_000.0
            val clock = { (nowSec * 1000.0).toLong() }
            val old = Fragment4f90dbd(clock)
            /* Today's page: the live tracker deciding which echo each answer names, and the sync. */
            val tracker = OutgoingChatMessageTracker()
            val views = EchoViews()
            val sync = ChatTimelineViewSync(views, ChatTimeline(clock))
            /* Today's page's 10 s timeouts still armed, by local id, and each echo's view. */
            val armed = mutableListOf<String>()
            val echoViewOf = HashMap<String, String>()
            var nextView = 0

            repeat(300) { step ->
                nowSec += random.nextDouble(0.0, 4.0)
                val where = "seed $seed, step $step"

                when (random.nextInt(11)) {
                    in 0..1 -> {
                        val text = texts[random.nextInt(texts.size)]
                        val view = "v${nextView++}"
                        old.send(view, text, nowSec)
                        val pending = tracker.register("#chan", "me", text, nowSec)
                        views.nextView = view
                        sync.insert(pending.sentAtSec, null) { position -> echoRow(position, pending) }
                        armed += pending.localId
                        echoViewOf[pending.localId] = view
                    }
                    2 -> if (armed.isNotEmpty()) {
                        val localId = armed.removeAt(random.nextInt(armed.size))
                        /* The same echo's timeout runs on both pages; each returns at once when its tracker no longer holds the write. */
                        old.runTimeout(echoViewOf.getValue(localId))
                        if (tracker.contains(localId)) sync.applyEchoEvent(localId, TIMEOUT)
                    }
                    3 -> {
                        old.onUserState(nowSec)
                        tracker.confirmOldestFromUserState("#chan", nowSec)?.let { confirmed ->
                            armed -= confirmed.localId
                            sync.applyEchoEvent(confirmed.localId, USERSTATE)
                        }
                    }
                    4 -> {
                        old.onNotice()
                        tracker.removeNewestAwaiting("#chan")?.let { rejected ->
                            armed -= rejected.localId
                            sync.applyEchoEvent(rejected.localId, NOTICE)
                        }
                    }
                    in 5..6 -> {
                        val text = texts[random.nextInt(texts.size)]
                        val timestampSec = nowSec - random.nextDouble(0.0, 6.0)
                        val view = "v${nextView++}"
                        val oldPosition = old.appendChatLine(view, "me", text, timestampSec)
                        val confirmed = tracker.confirmCanonical("#chan", "me", text, timestampSec)
                        if (confirmed != null) armed -= confirmed.localId
                        val newPosition = confirmed?.let { sync.removeEcho(it.localId) }
                        assertEquals("$where canonical position", oldPosition, newPosition)
                        if (newPosition != null) coverage.removed(echo = true, hidden = false) else if (confirmed != null) coverage.missed()
                        views.nextView = view
                        sync.insert(timestampSec, newPosition) { position -> messageRow(position, "me") }
                    }
                    in 7..8 -> {
                        val view = "v${nextView++}"
                        val timestampSec = nowSec - random.nextDouble(0.0, 3.0)
                        /* Every chat line is offered to the tracker first, a bot's too, on both pages. */
                        val oldPosition = old.appendChatLine(view, "bot", BOT_TEXT, timestampSec)
                        val newPosition = tracker.confirmCanonical("#chan", "bot", BOT_TEXT, timestampSec)?.let { sync.removeEcho(it.localId) }
                        assertEquals("$where bot line position", oldPosition, newPosition)
                        views.nextView = view
                        sync.insert(timestampSec, newPosition) { position -> messageRow(position, "bot") }
                    }
                    9 -> {
                        /* clearPendingOutgoingState, as forgetPendingSends and onDestroyView call it. */
                        old.forgetPendingSends()
                        armed.clear()
                        tracker.clear()
                    }
                    else -> if (random.nextInt(4) == 0) {
                        /* A channel change: forgetPendingSends, then clearTimeline. */
                        coverage.cleared(old.views.size)
                        old.forgetPendingSends()
                        old.clearTimeline()
                        armed.clear()
                        tracker.clear()
                        sync.clear()
                    }
                }

                assertEquals("$where views", old.views, views.views)
                assertEquals("$where looks", old.echoLooks(), views.echoLooks())
                /* Which timeout runs next is drawn from today's page, so both pages must hold the same ones. */
                assertEquals("$where armed timeouts", old.armedTimeoutViews, armed.map { localId -> echoViewOf.getValue(localId) }.toSet())
                /* Why the 4f90dbd page's removeAndTakePosition never misses: every echo view it still maps is in its timeline. */
                assertEquals("$where mapped echo views outside the timeline", emptySet<String>(), old.mappedEchoViews - old.views.toSet())
                coverage.step(timelineRows = views.views.size, hiddenViews = views.hidden.size)
            }
        }
        println(coverage)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun echo(view: String, localId: String, sentAtSec: Double) {
        views.nextView = view
        sync.insert(sentAtSec, null) { position ->
            PendingEchoRow(
                position = position,
                localId = localId,
                user = "me",
                messageText = "hello",
                emotesRaw = null,
                replyParentUserLogin = null,
                sentAtSec = sentAtSec,
                status = PendingEchoStatus.SENDING
            )
        }
    }

    private fun message(view: String, user: String, timestampSec: Double, preserved: ChatTimelinePosition? = null) {
        views.nextView = view
        sync.insert(timestampSec, preserved) { position -> messageRow(position, user) }
    }

    /** What an echo view shows: its status line, kept while hidden, whether it is shown, and its opacity. */
    private data class Look(val line: PendingEchoStatusLine?, val lineShown: Boolean, val alpha: Float)

    private data class Rebind(val view: String, val change: ChatTimelineChange, val status: PendingEchoStatus)

    /** Stands in for the layout, and checks on every rebind in place which row the view was created for. */
    private class KindLayout : ChatTimelineViews<String> {
        private val views = mutableListOf<String>()
        private val rowOf = HashMap<String, ChatTimelineRow>()
        var rebinds = 0

        /* The name the next view takes, set by the test before each insert and taken by create. */
        var nextView: String? = null

        override fun create(row: ChatTimelineRow): String {
            val view = checkNotNull(nextView) { "a view was created that the test did not name: one create per insert" }
            nextView = null
            rowOf[view] = row
            return view
        }

        override fun add(view: String, row: ChatTimelineRow, index: Int) {
            views.add(index, view)
        }

        override fun removeAt(index: Int) {
            rowOf -= views.removeAt(index)
        }

        override fun removeAll() {
            views.clear()
            rowOf.clear()
        }

        override fun setShown(index: Int, shown: Boolean) = Unit

        override fun rebind(index: Int, row: ChatTimelineRow, change: ChatTimelineChange) {
            val view = views[index]
            val shown = rowOf.getValue(view)
            assertTrue("$view was rebound in place, but was created for a ${shown.javaClass.simpleName}", shown is PendingEchoRow)
            assertTrue("$view, an echo's view, was rebound with a ${row.javaClass.simpleName}", row is PendingEchoRow)
            assertEquals("$view was rebound with another echo", (shown as PendingEchoRow).localId, (row as PendingEchoRow).localId)
            assertEquals("$view was rebound with another echo's position", shown.position, row.position)
            rowOf[view] = row
            rebinds++
        }
    }

    /** Stands in for the layout, with echo views that behave as PendingEchoView. */
    private class EchoViews : ChatTimelineViews<String> {
        val views = mutableListOf<String>()
        val hidden = mutableSetOf<String>()
        val looks = HashMap<String, Look>()
        val rebinds = mutableListOf<Rebind>()
        var adds = 0
        var removes = 0

        fun visible(): List<String> = views.filter { view -> view !in hidden }

        fun echoLooks(): Map<String, Look> = views.filter { view -> view in looks }.associateWith { view -> looks.getValue(view) }

        /* The name the next view takes, set by the test before each insert and taken by create. */
        var nextView: String? = null

        override fun create(row: ChatTimelineRow): String {
            val view = checkNotNull(nextView) { "a view was created that the test did not name: one create per insert" }
            nextView = null
            return view
        }

        override fun add(view: String, row: ChatTimelineRow, index: Int) {
            views.add(index, view)
            adds++
            if (row is PendingEchoRow) {
                looks[view] = Look(PendingEchoStatusLine.SENDING, true, 0.72f)
            }
        }

        override fun removeAt(index: Int) {
            val view = views.removeAt(index)
            hidden -= view
            looks -= view
            removes++
        }

        override fun removeAll() {
            views.clear()
            hidden.clear()
            looks.clear()
            removes++
        }

        override fun setShown(index: Int, shown: Boolean) {
            if (shown) hidden -= views[index] else hidden += views[index]
        }

        override fun rebind(index: Int, row: ChatTimelineRow, change: ChatTimelineChange) {
            val view = views[index]
            val echo = row as PendingEchoRow
            rebinds += Rebind(view, change, echo.status)

            val presentation = echo.status.presentation
            val before = looks.getValue(view)
            looks[view] = if (presentation.statusLine == null) {
                before.copy(lineShown = false, alpha = presentation.alpha)
            } else {
                Look(presentation.statusLine, true, presentation.alpha)
            }
        }
    }

    /**
     * Frozen at 4f90dbd: the echo code, the timeline order below, and the tracker that decides
     * which echo each answer or canonical message names. Read from 4f90dbd: ChatFragment's echo
     * code and OutgoingChatMessageTracker as they were there. Not frozen: what
     * removeAndTakePosition did for a view no longer in the timeline, which at 4f90dbd was to
     * return null. No step of this driver reaches that case, because every echo view the page
     * still maps is in its timeline - the driver asserts it after every step - so this copy fails
     * there instead of carrying a branch that has never run. Not exercised: where a row goes
     * against one whose position equals its own, the one case where an upper bound and a lower
     * bound differ, so a pass here says nothing about it. No row this page inserts holds the
     * sequence of a row still present, whatever the timestamps. An echo always takes a new
     * sequence from the counter, which gives each once until clearTimeline empties the timeline
     * with it. The only position a chat line preserves is the one removeAndTakePosition has just
     * taken out of the timeline together with the echo that held it, so the row that held that
     * sequence is gone before the line takes it, and the line keeps its own timestamp.
     *
     * ChatFragment's echo code at 4f90dbd, with the layout replaced by a list: the
     * pendingOutgoingViews map from local id to view, the status line and alpha set on
     * that view, the echo removed by its view reference when the canonical arrives, and the
     * 10 s timeouts it armed and cancelled. Each entry point is what the page did when that event
     * reached it: a write that succeeded, a timeout that ran, a USERSTATE, a NOTICE, a chat line,
     * forgetPendingSends and clearTimeline.
     * The views are placed as the timeline placed them at 4f90dbd, written out: ChatTimeline's
     * sequence counter, its fallback to the clock and its scan for the first newer position.
     *
     * Self-contained: nothing here calls ChatTimeline, ChatTimelineOrderer, the sync or
     * OutgoingChatMessageTracker, so a later change to any of them cannot change this oracle
     * with it.
     */
    private class Fragment4f90dbd(private val clock: () -> Long) {
        private val tracker = Tracker4f90dbd()
        private val order = ArrayList<String>()
        private val positions = ArrayList<ChatTimelinePosition>()
        private var nextSequence = 0L
        private val pendingOutgoingViews = LinkedHashMap<String, String>()
        private val looks = HashMap<String, Look>()

        /*
         * ChatFragment.pendingOutgoingTimeouts at 4f90dbd, from local id to the echo's view. A
         * timeout that has run is dropped here; at 4f90dbd it stayed in the map until cancelled,
         * when removing a callback that could not run again changed nothing.
         */
        private val pendingOutgoingTimeouts = LinkedHashMap<String, String>()

        /* Every echo view this page made, so a timeout for a view it never made fails instead of doing nothing. */
        private val echoViews = HashSet<String>()

        val views: List<String> get() = order

        /* The echo views whose timeout is posted and has neither run nor been cancelled. */
        val armedTimeoutViews: Set<String> get() = pendingOutgoingTimeouts.values.toSet()

        /* The echo views still mapped by local id: the ones a canonical message can remove. */
        val mappedEchoViews: Set<String> get() = pendingOutgoingViews.values.toSet()

        fun echoLooks(): Map<String, Look> = views.filter { view -> view in looks }.associateWith { view -> looks.getValue(view) }

        /* The Written branch of sendMessage at 4f90dbd: the tracker registers the write, then appendPendingOutgoingMessage. */
        fun send(view: String, message: String, sentAtSec: Double) {
            val pending = tracker.register(CHANNEL, USERNAME, message, sentAtSec)
            appendPendingOutgoingMessage(view, pending)
        }

        /* appendPendingOutgoingMessage at 4f90dbd: the echo, its view in pendingOutgoingViews, and its timeout posted. */
        private fun appendPendingOutgoingMessage(view: String, pending: Tracker4f90dbd.Pending) {
            looks[view] = Look(PendingEchoStatusLine.SENDING, true, 0.72f)
            pendingOutgoingViews[pending.localId] = view
            insert(view, pending.sentAtSec, null)
            pendingOutgoingTimeouts[pending.localId] = view
            echoViews += view
        }

        /*
         * The timeout runnable of the echo shown in [view], when the handler runs it: nothing if it
         * was cancelled, nothing if the tracker no longer holds the write, else unconfirmed.
         */
        fun runTimeout(view: String) {
            check(view in echoViews) { "No echo view named $view was ever made by the 4f90dbd page." }
            val localId = pendingOutgoingTimeouts.entries.firstOrNull { (_, armedView) -> armedView == view }?.key ?: return
            pendingOutgoingTimeouts.remove(localId)
            if (!tracker.contains(localId)) return
            pendingOutgoingViews[localId]?.let { pendingView ->
                looks[pendingView] = looks.getValue(pendingView).copy(line = PendingEchoStatusLine.UNCONFIRMED, alpha = 0.62f)
            }
        }

        /* confirmOldestPendingOutgoingFromUserState at 4f90dbd. */
        fun onUserState(confirmedAtSec: Double) {
            val confirmed = tracker.confirmOldestFromUserState(CHANNEL, confirmedAtSec) ?: return
            cancelPendingOutgoingTimeout(confirmed.localId)
            pendingOutgoingViews[confirmed.localId]?.let { view ->
                looks[view] = looks.getValue(view).copy(lineShown = false, alpha = 1f)
            }
        }

        /* rejectNewestPendingOutgoing at 4f90dbd. The composer it refills is not modelled. */
        fun onNotice() {
            val rejected = tracker.removeNewestAwaiting(CHANNEL) ?: return
            cancelPendingOutgoingTimeout(rejected.localId)
            pendingOutgoingViews[rejected.localId]?.let { view ->
                looks[view] = looks.getValue(view).copy(line = PendingEchoStatusLine.REJECTED, alpha = 0.5f)
            }
        }

        /*
         * appendChatLine at 4f90dbd, for a line that is not suppressed and whose user is not hidden:
         * reconcilePendingOutgoingMessage first, then the line at the position it preserved.
         * Returns that position.
         */
        fun appendChatLine(view: String, user: String, message: String, timestampSec: Double): ChatTimelinePosition? {
            val preserved = reconcilePendingOutgoingMessage(user, message, timestampSec)
            insert(view, timestampSec, preserved)
            return preserved
        }

        /* reconcilePendingOutgoingMessage at 4f90dbd: the echo's view by its local id, then removal by that view. */
        private fun reconcilePendingOutgoingMessage(user: String, message: String, timestampSec: Double): ChatTimelinePosition? {
            val confirmed = tracker.confirmCanonical(CHANNEL, user, message, timestampSec) ?: return null
            cancelPendingOutgoingTimeout(confirmed.localId)
            return pendingOutgoingViews.remove(confirmed.localId)?.let { view ->
                removeAndTakePosition(view).also { looks -= view }
            }
        }

        /* clearPendingOutgoingState(removeViews = false) at 4f90dbd, as forgetPendingSends called it: the views stay. */
        fun forgetPendingSends() {
            pendingOutgoingTimeouts.clear()
            tracker.clear()
            pendingOutgoingViews.clear()
        }

        fun clearTimeline() {
            order.clear()
            positions.clear()
            nextSequence = 0L
            looks.clear()
        }

        /* cancelPendingOutgoingTimeout at 4f90dbd. */
        private fun cancelPendingOutgoingTimeout(localId: String) {
            pendingOutgoingTimeouts.remove(localId)
        }

        /* ChatTimeline.insert at 4f90dbd, with the view put at the index it gives. */
        private fun insert(view: String, timestampSec: Double?, preserved: ChatTimelinePosition?) {
            val millis = timestampSec?.takeIf { timestamp -> timestamp.isFinite() && timestamp > 0.0 }?.times(1000.0)?.toLong() ?: clock()
            val position = ChatTimelinePosition(millis, preserved?.sequence ?: nextSequence++)
            val found = positions.indexOfFirst { existing ->
                position.timestampMillis < existing.timestampMillis ||
                    (position.timestampMillis == existing.timestampMillis && position.sequence < existing.sequence)
            }
            val index = if (found >= 0) found else positions.size
            positions.add(index, position)
            order.add(index, view)
        }

        /*
         * ChatTimelineViewSync.removeAndTakePosition at 4f90dbd: the view's index, both removed there.
         * At 4f90dbd a view missing from the timeline gave null; here it cannot be missing, so it fails.
         */
        private fun removeAndTakePosition(view: String): ChatTimelinePosition {
            val index = order.indexOf(view)
            check(index >= 0) {
                "Echo view $view is still mapped by local id but is not in the timeline. Invariant: every " +
                    "mapped echo view is in the timeline, because only a canonical message and clearTimeline " +
                    "remove one, and this driver runs forgetPendingSends, which forgets the map, before every " +
                    "clearTimeline."
            }
            order.removeAt(index)
            return positions.removeAt(index)
        }

        private companion object {
            /* What the page passed: currentChannelNormalized() and the account's login. */
            const val CHANNEL = "#chan"
            const val USERNAME = "me"
        }
    }

    /**
     * OutgoingChatMessageTracker at 4f90dbd, written out with the defaults ChatFragment
     * constructed it with. Its record is its own, so nothing here reads or builds
     * PendingOutgoingChatMessage.
     */
    private class Tracker4f90dbd {
        data class Pending(
            val localId: String,
            val channel: String,
            val username: String,
            val message: String,
            val sentAtSec: Double,
            val transportConfirmed: Boolean = false
        )

        private val canonicalMatchWindowSec = 45.0
        private val userStateConfirmWindowSec = 10.0
        private val maximumPendingMessages = 50
        private val pendingById = LinkedHashMap<String, Pending>()
        private var nextSequence = 0L

        fun register(channel: String, username: String, message: String, sentAtSec: Double): Pending {
            nextSequence += 1L
            val item = Pending(
                localId = "pending:${(sentAtSec * 1000.0).toLong()}:$nextSequence",
                channel = normalizeChannel(channel),
                username = normalizeUsername(username),
                message = normalizeMessage(message),
                sentAtSec = sentAtSec
            )
            pendingById[item.localId] = item
            trimToCapacity()
            return item
        }

        fun confirmOldestFromUserState(channel: String, confirmedAtSec: Double): Pending? {
            val normalizedChannel = normalizeChannel(channel)
            val entry = pendingById.entries.firstOrNull { (_, item) ->
                !item.transportConfirmed &&
                    item.channel == normalizedChannel &&
                    confirmedAtSec >= item.sentAtSec &&
                    confirmedAtSec - item.sentAtSec <= userStateConfirmWindowSec
            } ?: return null
            val confirmed = entry.value.copy(transportConfirmed = true)
            pendingById[entry.key] = confirmed
            return confirmed
        }

        fun confirmCanonical(channel: String, username: String, message: String, messageTimestampSec: Double): Pending? {
            val normalizedChannel = normalizeChannel(channel)
            val normalizedUsername = normalizeUsername(username)
            val normalizedMessage = normalizeMessage(message)
            val entry = pendingById.entries
                .asSequence()
                .filter { (_, item) ->
                    item.channel == normalizedChannel &&
                        item.username == normalizedUsername &&
                        item.message == normalizedMessage &&
                        abs(item.sentAtSec - messageTimestampSec) <= canonicalMatchWindowSec
                }
                .minByOrNull { (_, item) -> abs(item.sentAtSec - messageTimestampSec) } ?: return null
            pendingById.remove(entry.key)
            return entry.value
        }

        fun removeNewestAwaiting(channel: String): Pending? {
            val normalizedChannel = normalizeChannel(channel)
            val entry = pendingById.entries
                .toList()
                .asReversed()
                .firstOrNull { (_, item) -> item.channel == normalizedChannel && !item.transportConfirmed } ?: return null
            pendingById.remove(entry.key)
            return entry.value
        }

        fun contains(localId: String): Boolean = pendingById.containsKey(localId)

        fun clear() {
            pendingById.clear()
        }

        private fun trimToCapacity() {
            while (pendingById.size > maximumPendingMessages) {
                val oldestId = pendingById.keys.firstOrNull() ?: return
                pendingById.remove(oldestId)
            }
        }

        private fun normalizeChannel(value: String): String = value.trim().removePrefix("#").lowercase()

        private fun normalizeUsername(value: String): String = value.trim().lowercase()

        private fun normalizeMessage(value: String): String {
            val trimmed = value.trim()
            if (trimmed.startsWith("\u0001ACTION ") && trimmed.endsWith("\u0001") && trimmed.length > 8) {
                return trimmed.substring(8, trimmed.length - 1).trim()
            }
            if (trimmed.startsWith("/me ", ignoreCase = true)) {
                return trimmed.substring(4).trim()
            }
            return trimmed
        }
    }

    private companion object {
        /* What a bot's line says. It never names an echo: the bot is not the sender. */
        const val BOT_TEXT = "text"

        fun echoRow(position: ChatTimelinePosition, pending: PendingOutgoingChatMessage) = PendingEchoRow(
            position = position,
            localId = pending.localId,
            user = "me",
            messageText = pending.message,
            emotesRaw = null,
            replyParentUserLogin = null,
            sentAtSec = pending.sentAtSec,
            status = PendingEchoStatus.SENDING
        )

        fun messageRow(position: ChatTimelinePosition, user: String) = ChatMessageRow(
            position = position,
            user = user,
            usernameLower = user.lowercase(),
            messageId = null,
            messageText = "text",
            messageTimestampSec = position.timestampMillis / 1000.0,
            emotesRaw = null,
            replyParentUserLogin = null
        )
    }
}
