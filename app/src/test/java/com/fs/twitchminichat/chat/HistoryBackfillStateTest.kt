package com.fs.twitchminichat.chat

import com.fs.twitchminichat.BackendHistoryResult
import com.fs.twitchminichat.chat.HistoryBackfillDecision.Request
import com.fs.twitchminichat.chat.HistoryBackfillDecision.Skip
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

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private companion object {
        /** 2026-09-21T20:26:40Z, the page opens. */
        const val T0 = 1_790_000_000_000L

        /** Ten minutes later. */
        const val T1 = T0 + 600_000L
    }

    /** First connection, answered with an empty success one second later; returns the arrival. */
    private fun openWithASuccess(): Long {
        val opening = sent(connect(T0))
        val arrivedAt = T0 + 1_000L
        succeed(opening, arrivedAt)
        return arrivedAt
    }

    private fun connect(nowMs: Long): HistoryBackfillDecision =
        decide(HistoryBackfillPolicy.onConnect(state.inputs(nowMs, ircClientPresent = false)))

    private fun resume(nowMs: Long): HistoryBackfillDecision =
        decide(HistoryBackfillPolicy.onResume(state.inputs(nowMs, ircClientPresent = true)))

    private fun refresh(nowMs: Long): HistoryBackfillDecision =
        decide(HistoryBackfillPolicy.onManualRefresh(state.inputs(nowMs, ircClientPresent = true)))

    /** Applies the decision, as ChatFragment does, and remembers what it asked to send. */
    private fun decide(decision: HistoryBackfillDecision): HistoryBackfillDecision {
        val send = state.apply(decision)
        assertEquals(decision is Request, send != null)
        if (send != null) sends[decision] = send
        return decision
    }

    private val sends = HashMap<HistoryBackfillDecision, HistoryBackfillSend>()

    private fun sent(decision: HistoryBackfillDecision): HistoryBackfillSend =
        checkNotNull(sends[decision]) { "$decision sent nothing" }

    private fun succeed(send: HistoryBackfillSend, atMs: Long) {
        assertNull(state.onResult(BackendHistoryResult.Success(emptyList()), send, atMs))
    }

    private fun fail(send: HistoryBackfillSend, atMs: Long): HistoryBackfillFailureEffects? =
        state.onResult(BackendHistoryResult.Failed, send, atMs)

    private fun render(decision: HistoryBackfillDecision): String {
        val fields = decision.journalFields
            .filter { (_, value) -> value != null }
            .joinToString(separator = " ") { (key, value) -> "$key=$value" }
        return "${decision.journalEvent} $fields"
    }
}
