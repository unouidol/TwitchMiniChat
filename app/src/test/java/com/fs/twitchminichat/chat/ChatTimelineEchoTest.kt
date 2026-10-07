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
 * The last test replays random sending, answers and canonical messages, through the real
 * OutgoingChatMessageTracker, against a transcription of ChatFragment's echo code at
 * 4f90dbd.
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
    // Against ChatFragment at 4f90dbd
    // ---------------------------------------------------------------------------

    @Test
    fun randomSendingAndAnswers_produceTheSameEchoes_asChatFragmentAt4f90dbd() {
        val texts = listOf("hi", "gg", "pog", "/me waves")

        val coverage = OracleCoverage("4f90dbd", seeds = 200, operations = 300)
        for (seed in 1..200) {
            val random = Random(seed)
            var nowSec = 1_000.0
            val tracker = OutgoingChatMessageTracker()
            val clock = { (nowSec * 1000.0).toLong() }
            val old = Fragment4f90dbd(clock)
            val views = EchoViews()
            val sync = ChatTimelineViewSync(views, ChatTimeline(clock))
            /* The 10 s timeouts still armed: page state, the same on both sides. */
            val armed = mutableListOf<String>()
            var nextView = 0

            repeat(300) { step ->
                nowSec += random.nextDouble(0.0, 4.0)
                val where = "seed $seed, step $step"

                when (random.nextInt(11)) {
                    in 0..1 -> {
                        val pending = tracker.register("#chan", "me", texts[random.nextInt(texts.size)], nowSec)
                        val view = "v${nextView++}"
                        old.appendPendingOutgoingMessage(view, pending)
                        sync.insert(view, pending.sentAtSec, null) { position -> echoRow(position, pending) }
                        armed += pending.localId
                    }
                    2 -> if (armed.isNotEmpty()) {
                        val localId = armed.removeAt(random.nextInt(armed.size))
                        /* The runnable returns at once when the tracker no longer holds the write. */
                        if (tracker.contains(localId)) {
                            old.timeout(localId)
                            sync.applyEchoEvent(localId, TIMEOUT)
                        }
                    }
                    3 -> tracker.confirmOldestFromUserState("#chan", nowSec)?.let { confirmed ->
                        armed -= confirmed.localId
                        old.confirmFromUserState(confirmed.localId)
                        sync.applyEchoEvent(confirmed.localId, USERSTATE)
                    }
                    4 -> tracker.removeNewestAwaiting("#chan")?.let { rejected ->
                        armed -= rejected.localId
                        old.reject(rejected.localId)
                        sync.applyEchoEvent(rejected.localId, NOTICE)
                    }
                    in 5..6 -> {
                        val text = texts[random.nextInt(texts.size)]
                        val timestampSec = nowSec - random.nextDouble(0.0, 6.0)
                        val confirmed = tracker.confirmCanonical("#chan", "me", text, timestampSec)
                        if (confirmed != null) armed -= confirmed.localId
                        val oldPosition = confirmed?.let { old.reconcile(it.localId) }
                        val newPosition = confirmed?.let { sync.removeEcho(it.localId) }
                        assertEquals(where, oldPosition, newPosition)
                    if (newPosition != null) coverage.removed(echo = true, hidden = false) else if (confirmed != null) coverage.missed()

                        val view = "v${nextView++}"
                        old.appendChatLine(view, "me", timestampSec, oldPosition)
                        sync.insert(view, timestampSec, newPosition) { position -> messageRow(position, "me") }
                    }
                    in 7..8 -> {
                        val view = "v${nextView++}"
                        val timestampSec = nowSec - random.nextDouble(0.0, 3.0)
                        old.appendChatLine(view, "bot", timestampSec, null)
                        sync.insert(view, timestampSec, null) { position -> messageRow(position, "bot") }
                    }
                    9 -> {
                        /* clearPendingOutgoingState, as forgetPendingSends and onDestroyView call it. */
                        armed.clear()
                        tracker.clear()
                        old.clearPendingOutgoingState()
                    }
                    else -> if (random.nextInt(4) == 0) {
                        /* A channel change: forgetPendingSends, then clearTimeline. */
                        armed.clear()
                        tracker.clear()
                        old.clearPendingOutgoingState()
                        coverage.cleared(old.views.size)
                        old.clearTimeline()
                        sync.clear()
                    }
                }

                assertEquals(where, old.views, views.views)
                assertEquals(where, old.echoLooks(), views.echoLooks())
                coverage.step(timelineRows = views.views.size, hiddenViews = views.hidden.size)
            }
        }
        println(coverage)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun echo(view: String, localId: String, sentAtSec: Double) {
        sync.insert(view, sentAtSec, null) { position ->
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
        sync.insert(view, timestampSec, preserved) { position -> messageRow(position, user) }
    }

    /** What an echo view shows: its status line, kept while hidden, whether it is shown, and its opacity. */
    private data class Look(val line: PendingEchoStatusLine?, val lineShown: Boolean, val alpha: Float)

    private data class Rebind(val view: String, val change: ChatTimelineChange, val status: PendingEchoStatus)

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

        override fun indexOf(view: String): Int = views.indexOf(view)

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
     * ChatFragment's echo code at 4f90dbd, with the layout replaced by a list: the
     * pendingOutgoingViews map from local id to view, the status line and alpha set on
     * that view, and the echo removed by its view reference when the canonical arrives.
     * Its timeline orders the views as ChatTimelineController did.
     */
    private class Fragment4f90dbd(clock: () -> Long) {
        private val order = OrderViews()
        private val timeline = ChatTimelineViewSync(order, ChatTimeline(clock))
        private val pendingOutgoingViews = LinkedHashMap<String, String>()
        private val looks = HashMap<String, Look>()

        val views: List<String> get() = order.views

        fun echoLooks(): Map<String, Look> = views.filter { view -> view in looks }.associateWith { view -> looks.getValue(view) }

        fun appendPendingOutgoingMessage(view: String, pending: PendingOutgoingChatMessage) {
            looks[view] = Look(PendingEchoStatusLine.SENDING, true, 0.72f)
            pendingOutgoingViews[pending.localId] = view
            timeline.insert(view, pending.sentAtSec, null) { position -> SystemLineRow(position, "echo") }
        }

        fun timeout(localId: String) {
            pendingOutgoingViews[localId]?.let { view ->
                looks[view] = looks.getValue(view).copy(line = PendingEchoStatusLine.UNCONFIRMED, alpha = 0.62f)
            }
        }

        fun confirmFromUserState(localId: String) {
            pendingOutgoingViews[localId]?.let { view ->
                looks[view] = looks.getValue(view).copy(lineShown = false, alpha = 1f)
            }
        }

        fun reject(localId: String) {
            pendingOutgoingViews[localId]?.let { view ->
                looks[view] = looks.getValue(view).copy(line = PendingEchoStatusLine.REJECTED, alpha = 0.5f)
            }
        }

        fun reconcile(localId: String): ChatTimelinePosition? {
            return pendingOutgoingViews.remove(localId)?.let { view ->
                timeline.removeAndTakePosition(view)?.also { looks -= view }
            }
        }

        fun appendChatLine(view: String, user: String, timestampSec: Double, preserved: ChatTimelinePosition?) {
            timeline.insert(view, timestampSec, preserved) { position -> messageRow(position, user) }
        }

        /* removeViews was false at every caller: the map is forgotten, the views stay. */
        fun clearPendingOutgoingState() {
            pendingOutgoingViews.clear()
        }

        fun clearTimeline() {
            timeline.clear()
            looks.clear()
        }
    }

    /** Only the order of the views, for the transcription. */
    private class OrderViews : ChatTimelineViews<String> {
        val views = mutableListOf<String>()

        override fun indexOf(view: String): Int = views.indexOf(view)
        override fun add(view: String, row: ChatTimelineRow, index: Int) = views.add(index, view)
        override fun removeAt(index: Int) {
            views.removeAt(index)
        }
        override fun removeAll() = views.clear()
        override fun setShown(index: Int, shown: Boolean) = error("the transcription hides nothing")
        override fun rebind(index: Int, row: ChatTimelineRow, change: ChatTimelineChange) = error("the transcription rebinds nothing")
    }

    private companion object {
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
