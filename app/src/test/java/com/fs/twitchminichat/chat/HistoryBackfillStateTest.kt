package com.fs.twitchminichat.chat

import com.fs.twitchminichat.BackendHistoryResult
import com.fs.twitchminichat.BackendHistoryMessage
import com.fs.twitchminichat.chat.HistoryBackfillDecision.Request
import com.fs.twitchminichat.chat.HistoryBackfillDecision.Skip
import com.fs.twitchminichat.chat.HistoryBackfillResultOutcome.Applied
import com.fs.twitchminichat.chat.HistoryBackfillResultOutcome.Discarded
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.BACKFILL_IN_FLIGHT
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.RECENT_BACKFILL
import com.fs.twitchminichat.chat.HistoryBackfillSource.FIRST_CONNECT
import com.fs.twitchminichat.chat.HistoryBackfillSource.RECONNECT_OFFLINE
import com.fs.twitchminichat.chat.HistoryBackfillSource.RESUME
import com.fs.twitchminichat.chat.HistoryBackfillSource.RESUME_NO_PAUSE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives [HistoryBackfillState] through the transitions ChatFragment makes, and
 * asserts what the next decision is - never the fields themselves.
 *
 * Each step goes through [HistoryBackfillPolicy] exactly as the chat page does:
 * capture the inputs, decide, apply, send, hand the result back. A line deleted from
 * the state's handling of any of those steps changes a later decision, and one of
 * these tests sees it.
 */
class HistoryBackfillStateTest {

    private val state = HistoryBackfillState()

    @Test
    fun aFailedRequest_isReachedBackToByTheNextRequest() {
        openWithASuccess()

        /* Home for a minute; the reconnect asks for it and fails. */
        state.onPaused(T1 - 300L)
        state.onStopped(T1)
        val reconnect = connect(T1 + 60_000L) as Request
        assertEquals(RECONNECT_OFFLINE, reconnect.source)
        val reconnectSend = sent(reconnect)
        assertNotNull(fail(reconnectSend, T1 + 60_004L))

        /* IRC stays up. A minute later the user switches pages for two seconds. */
        state.onPaused(T1 + 120_000L)
        val resumedAt = T1 + 122_000L
        val next = resume(resumedAt) as Request
        assertEquals(RESUME, next.source)

        val reconnectReachedBackTo = reconnectSend.sentAtMs - reconnectSend.requestedSec * 1000L
        val nextReachesBackTo = resumedAt - next.requestedSec * 1000L
        assertTrue(
            "the next request starts at $nextReachesBackTo, after $reconnectReachedBackTo",
            nextReachesBackTo <= reconnectReachedBackTo
        )
    }

    @Test
    fun aSuccess_clearsTheOwedWindow_andStartsTheFiveSecondsFromItsArrival() {
        openWithASuccess()
        state.onStopped(T1)
        fail(sent(connect(T1 + 60_000L)), T1 + 60_004L)

        /* A page switch asks for the owed window, and that request succeeds. */
        state.onPaused(T1 + 120_000L)
        val recovery = resume(T1 + 122_000L) as Request
        assertTrue(recovery.journalFields.any { (key, _) -> key == "unrecoveredSec" })
        val arrivedAt = T1 + 124_000L
        succeed(sent(recovery), arrivedAt)

        /* 4.999 s after the arrival - but 6.999 s after the send - it still suppresses. */
        state.onPaused(arrivedAt + 1_000L)
        val tooSoon = resume(arrivedAt + 4_999L) as Skip
        assertEquals(RECENT_BACKFILL, tooSoon.reason)
        assertEquals(listOf("awaySec" to 3, "sinceLastBackfillMs" to 4_999L), tooSoon.details)

        /* Later, a page switch asks for its own two seconds only: nothing is owed. */
        state.onPaused(arrivedAt + 8_000L)
        val ordinary = resume(arrivedAt + 10_000L) as Request
        assertEquals(30, ordinary.requestedSec)
        assertFalse(ordinary.journalFields.any { (key, _) -> key == "unrecoveredSec" })
    }

    @Test
    fun aRequestInFlight_suppressesTheNextTrigger_untilItsResultArrives() {
        val opening = connect(T0) as Request
        assertEquals(FIRST_CONNECT, opening.source)
        val openingSend = sent(opening)

        /* The first resume, 50 ms later, finds the request still in flight. */
        val second = resume(T0 + 50L) as Skip
        assertEquals(BACKFILL_IN_FLIGHT, second.reason)

        val arrivedAt = T0 + 1_000L
        succeed(openingSend, arrivedAt)

        /* The marker is gone: half a second later only the success's five seconds hold. */
        assertEquals(RECENT_BACKFILL, (resume(arrivedAt + 500L) as Skip).reason)

        /* And once they pass, a trigger asks again. */
        assertEquals(RESUME_NO_PAUSE, (resume(arrivedAt + 5_000L) as Request).source)
    }

    @Test
    fun aFailure_clearsTheInFlightMarkerAtOnce_soARetryNeedNotWaitFiveSeconds() {
        val opening = sent(connect(T0))
        assertNotNull(fail(opening, T0 + 4L))

        /* One millisecond after the failure, a resume asks again. */
        val retry = resume(T0 + 5L)
        assertTrue("retry was $retry", retry is Request)
    }

    @Test
    fun aResumeWithNoReference_asksForTheWindowSinceTheLastSuccessfulBackfill() {
        val arrivedAt = openWithASuccess()

        /* No pause, nothing rendered, never stopped: only the backfill is a reference. */
        val resumedAt = arrivedAt + 39_000L
        val decision = resume(resumedAt) as Request
        assertEquals(RESUME_NO_PAUSE, decision.source)
        assertEquals(
            "backfill.triggered source=resume_no_pause sinceLastBackfillSec=39 requestedSec=49",
            render(decision)
        )
        assertTrue(resumedAt - decision.requestedSec * 1000L <= arrivedAt)
    }

    @Test
    fun aSuccessThatDidNotReachTheOwedStart_leavesItOwed() {
        openWithASuccess()

        /* Ten minutes away; the reconnect asks for them. */
        state.onStopped(T1)
        val reconnectAt = T1 + 600_000L
        val reconnect = sent(connect(reconnectAt))
        assertEquals(610, reconnect.requestedSec)

        /* A second later the user taps refresh: 120 s, asked for beside it. */
        val refresh = sent(refresh(reconnectAt + 1_000L))
        assertEquals(120, refresh.requestedSec)

        /* The reconnect fails; the refresh, which did not reach that far back, succeeds. */
        assertNotNull(fail(reconnect, reconnectAt + 2_000L))
        succeed(refresh, reconnectAt + 3_000L)

        /* The ten minutes are still owed: the next request reaches back to them. */
        state.onPaused(reconnectAt + 10_000L)
        val resumedAt = reconnectAt + 12_000L
        val next = resume(resumedAt) as Request
        assertTrue(
            "next reaches back to ${resumedAt - next.requestedSec * 1000L}",
            resumedAt - next.requestedSec * 1000L <= reconnect.sentAtMs - reconnect.requestedSec * 1000L
        )
    }

    @Test
    fun aSkipBehindANarrowerRequest_leavesTheRestOwed() {
        openWithASuccess()

        /* Ten minutes away. Just before the reconnect, a 120 s refresh is sent. */
        state.onStopped(T1)
        val refresh = sent(refresh(T1 + 599_500L))

        /* The reconnect skips behind it, consumes the offline reference, and says what is left. */
        val skip = connect(T1 + 600_000L) as Skip
        assertEquals(BACKFILL_IN_FLIGHT, skip.reason)
        assertEquals("uncoveredSec" to 480, skip.journalFields.last())

        /* The refresh succeeds. It never reached ten minutes back, so they stay owed. */
        succeed(refresh, T1 + 601_000L)
        state.onPaused(T1 + 610_000L)
        val resumedAt = T1 + 612_000L
        val next = resume(resumedAt) as Request
        assertTrue(
            "next reaches back to ${resumedAt - next.requestedSec * 1000L}, not to $T1",
            resumedAt - next.requestedSec * 1000L <= T1
        )
    }

    @Test
    fun aSkipBehindASuccessThatReachedBackToThePause_owesNothing() {
        /* The page is left while the opening hour-long request is in flight. */
        val opening = sent(connect(T0))
        state.onPaused(T0 + 200L)
        val arrivedAt = T0 + 1_000L
        succeed(opening, arrivedAt)

        /*
         * Two seconds after it arrives the page comes back. The pause is older than the
         * arrival but well inside the hour the request reached back over: nothing owed.
         */
        val skip = resume(arrivedAt + 2_000L) as Skip
        assertEquals(RECENT_BACKFILL, skip.reason)
        assertEquals(listOf("awaySec" to 2, "sinceLastBackfillMs" to 2_000L), skip.details)
    }

    @Test
    fun aDecisionIsTakenInsideTheMonitorThatResultsAlsoTake() {
        /*
         * The one part of the locking a unit test can state without simulating a race:
         * the policy runs while this thread holds the state's own monitor, the monitor
         * onResult also takes. A result therefore cannot land between the inputs being
         * captured and the decision being applied. The interleavings that excludes are
         * named in HistoryBackfillState's documentation; provoking them would need two
         * threads and timing, which a unit test should not depend on.
         */
        var heldWhileDeciding = false
        state.decide(T0, ircClientPresent = false) { inputs ->
            heldWhileDeciding = Thread.holdsLock(state)
            HistoryBackfillPolicy.onConnect(inputs)
        }
        assertTrue(heldWhileDeciding)
    }

    // ---------------------------------------------------------------------------
    // Channel changes. Each runs once per path - a join from the channel field and a
    // start on another channel - since both must do exactly the same thing.
    // ---------------------------------------------------------------------------

    @Test
    fun aJoin_discardsASuccessSentBeforeIt() =
        discardsASuccessSentBeforeTheChange { state.onChannelJoined("beta") }

    @Test
    fun aStartOnAnotherChannel_discardsASuccessSentBeforeIt() =
        discardsASuccessSentBeforeTheChange { assertTrue(state.onStarted("beta")) }

    @Test
    fun aJoin_discardsAFailureSentBeforeIt_andLeavesTheNewRequestsMarker() =
        discardsAFailureSentBeforeTheChange { state.onChannelJoined("beta") }

    @Test
    fun aStartOnAnotherChannel_discardsAFailureSentBeforeIt_andLeavesTheNewRequestsMarker() =
        discardsAFailureSentBeforeTheChange { assertTrue(state.onStarted("beta")) }

    @Test
    fun aJoin_forgetsEveryReferenceTakenOnThePreviousChannel() =
        forgetsEveryReferenceTakenBeforeTheChange { state.onChannelJoined("beta") }

    @Test
    fun aStartOnAnotherChannel_forgetsEveryReferenceTakenOnThePreviousChannel() =
        forgetsEveryReferenceTakenBeforeTheChange { assertTrue(state.onStarted("beta")) }

    @Test
    fun aStartOnTheSameChannel_forgetsNothing() {
        assertFalse("the first start only binds the channel", state.onStarted("alpha"))
        val opening = connectSending(T0)
        state.onStopped(T0 + 100L)

        /* Case aside, the same channel: not a change. */
        assertFalse(state.onStarted("Alpha"))

        /* The opening request is still this channel's: its result is applied. */
        succeed(opening, T0 + 1_000L)
        assertEquals(RECENT_BACKFILL, (resume(T0 + 1_500L) as Skip).reason)
    }

    @Test
    fun aStartAfterAJoin_isOnTheJoinedChannel() {
        state.onStarted("alpha")
        state.onChannelJoined("beta")
        val opening = connectSending(T0)
        state.onStopped(T0 + 100L)

        assertFalse(state.onStarted("beta"))
        succeed(opening, T0 + 1_000L)
    }

    /** Alpha's opening hour arrives after [change], before beta has asked for anything. */
    private fun discardsASuccessSentBeforeTheChange(change: () -> Unit) {
        state.onStarted("alpha")
        val alphaOpening = connectSending(T0)
        change()

        val outcome = state.onResult(BackendHistoryResult.Success(listOf(ROW)), alphaOpening, T0 + 1_000L)
        assertEquals(
            "backfill.discarded result=success requestedSec=3600 messageCount=1 " +
                    "requestChannelGeneration=0 currentChannelGeneration=1",
            render(outcome)
        )

        /* Nothing arrived for beta: no five seconds hold, and there is nothing to measure from. */
        assertEquals(
            "backfill.skipped reason=nothing_to_measure_from historyLoaded=false",
            render(resume(T0 + 1_500L))
        )
    }

    /**
     * Beta's first connect leaves in the same millisecond as alpha's, which the send
     * instant cannot tell apart; then alpha's fails.
     */
    private fun discardsAFailureSentBeforeTheChange(change: () -> Unit) {
        state.onStarted("alpha")
        val alphaOpening = connectSending(T0)
        change()
        val betaOpening = connectSending(T0)

        assertEquals(
            "backfill.discarded result=request_failed requestedSec=3600 " +
                    "requestChannelGeneration=0 currentChannelGeneration=1",
            render(state.onResult(BackendHistoryResult.Failed, alphaOpening, T0 + 4L))
        )

        /* Beta's request still holds the marker. */
        assertEquals(BACKFILL_IN_FLIGHT, (resume(T0 + 5L) as Skip).reason)

        /* Beta's hour arrives. Nobody owes alpha's hour, and beta stays loaded. */
        succeed(betaOpening, T0 + 1_000L)
        assertEquals(
            "backfill.triggered source=resume_no_pause sinceLastBackfillSec=9 requestedSec=30",
            render(resume(T0 + 10_000L))
        )
        assertEquals(
            "backfill.skipped reason=backfill_in_flight inFlightMs=1000",
            render(connect(T0 + 11_000L))
        )
    }

    /**
     * Sets every reference on alpha, changes channel with [change], and asks the probe
     * decisions of the page and of one that has just opened on beta: they must agree.
     */
    private fun forgetsEveryReferenceTakenBeforeTheChange(change: () -> Unit) {
        state.onStarted("alpha")

        /* The loaded mark, and a success with how far back it reached. */
        openWithASuccess()
        /* The render watermark, a pause, a stop and the offline reference it arms. */
        state.onMessageRendered((T1 - 30_000L) / 1000.0)
        state.onPaused(T1 - 20_000L)
        state.onStopped(T1 - 10_000L)
        /* A window owed by a failure, and a request in flight with its reach. */
        assertNotNull(fail(sent(refresh(T1 - 5_000L)), T1 - 4_000L))
        sent(refresh(T1 - 1_000L))

        change()

        val opened = HistoryBackfillState().apply { onStarted("beta") }
        assertEquals(probe(opened), probe(state))
    }

    /**
     * Three decisions that between them read every reference: a resume reads the pause,
     * the marker, the last success, the watermark, the stop, the owed window and the
     * loaded mark; a connect reads the loaded mark; a reconnect the offline reference.
     */
    private fun probe(target: HistoryBackfillState): List<HistoryBackfillDecision> = listOf(
        target.decide(T1 + 2_000L, ircClientPresent = true, policy = HistoryBackfillPolicy::onResume),
        target.decide(T1 + 3_000L, ircClientPresent = false, policy = HistoryBackfillPolicy::onConnect),
        target.decide(T1 + 4_000L, ircClientPresent = true, policy = HistoryBackfillPolicy::onConnect)
    ).map { applied -> applied.decision }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private companion object {
        /** 2026-09-21T20:26:40Z, the page opens. */
        const val T0 = 1_790_000_000_000L

        /** Ten minutes later. */
        const val T1 = T0 + 600_000L

        /** One history row; the state only counts it. */
        val ROW = BackendHistoryMessage(
            user = "viewer",
            text = "hello",
            emotesRaw = null,
            messageId = "m-1",
            timestampSec = (T0 - 60_000L) / 1000.0
        )
    }

    /** First connection, answered with an empty success one second later; returns the arrival. */
    private fun openWithASuccess(): Long {
        val opening = sent(connect(T0))
        val arrivedAt = T0 + 1_000L
        succeed(opening, arrivedAt)
        return arrivedAt
    }

    private fun connect(nowMs: Long): HistoryBackfillDecision =
        record(state.decide(nowMs, ircClientPresent = false, policy = HistoryBackfillPolicy::onConnect))

    private fun resume(nowMs: Long): HistoryBackfillDecision =
        record(state.decide(nowMs, ircClientPresent = true, policy = HistoryBackfillPolicy::onResume))

    private fun refresh(nowMs: Long): HistoryBackfillDecision =
        record(state.decide(nowMs, ircClientPresent = true, policy = HistoryBackfillPolicy::onManualRefresh))

    /** Remembers what a decision, taken as ChatFragment takes it, asked to send. */
    private fun record(applied: HistoryBackfillApplied): HistoryBackfillDecision {
        assertEquals(applied.decision is Request, applied.send != null)
        applied.send?.let { send -> sends[applied.decision] = send }
        return applied.decision
    }

    private val sends = HashMap<HistoryBackfillDecision, HistoryBackfillSend>()

    private fun sent(decision: HistoryBackfillDecision): HistoryBackfillSend =
        checkNotNull(sends[decision]) { "$decision sent nothing" }

    private fun succeed(send: HistoryBackfillSend, atMs: Long) {
        assertNull(applied(state.onResult(BackendHistoryResult.Success(emptyList()), send, atMs)))
    }

    private fun fail(send: HistoryBackfillSend, atMs: Long): HistoryBackfillFailureEffects? =
        applied(state.onResult(BackendHistoryResult.Failed, send, atMs))

    /** The failure effects of a result the state applied; fails the test if it discarded it. */
    private fun applied(outcome: HistoryBackfillResultOutcome): HistoryBackfillFailureEffects? {
        assertTrue("the result was discarded: $outcome", outcome is Applied)
        return (outcome as Applied).failure
    }

    /** Sends a connect's request, read from the decision's own result rather than [sends]. */
    private fun connectSending(nowMs: Long): HistoryBackfillSend =
        checkNotNull(state.decide(nowMs, ircClientPresent = false, policy = HistoryBackfillPolicy::onConnect).send)

    private fun render(decision: HistoryBackfillDecision): String =
        line(decision.journalEvent, decision.journalFields)

    private fun render(outcome: HistoryBackfillResultOutcome): String {
        assertTrue("the result was applied: $outcome", outcome is Discarded)
        return line((outcome as Discarded).journalEvent, outcome.journalFields)
    }

    /** One journal line as the journal writes it, null fields omitted. */
    private fun line(event: String, journalFields: List<Pair<String, Any?>>): String {
        val fields = journalFields
            .filter { (_, value) -> value != null }
            .joinToString(separator = " ") { (key, value) -> "$key=$value" }
        return "$event $fields"
    }
}
