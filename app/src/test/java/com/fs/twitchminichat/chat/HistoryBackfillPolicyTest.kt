package com.fs.twitchminichat.chat

import com.fs.twitchminichat.chat.HistoryBackfillDecision.Request
import com.fs.twitchminichat.chat.HistoryBackfillDecision.Skip
import com.fs.twitchminichat.chat.HistoryBackfillFailure.REAUTHORIZATION_REQUIRED
import com.fs.twitchminichat.chat.HistoryBackfillFailure.REQUEST_FAILED
import com.fs.twitchminichat.chat.HistoryBackfillFailure.SESSION_MISSING
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.AWAY_BELOW_THRESHOLD
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.BACKFILL_IN_FLIGHT
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.HISTORY_ALREADY_LOADED
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.NOTHING_TO_MEASURE_FROM
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.NOTHING_TO_MEASURE_FROM_RECONNECT
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.RECENT_BACKFILL
import com.fs.twitchminichat.chat.HistoryBackfillSource.FIRST_CONNECT
import com.fs.twitchminichat.chat.HistoryBackfillSource.MANUAL_REFRESH
import com.fs.twitchminichat.chat.HistoryBackfillSource.RECONNECT_NO_PAUSE_REFERENCE
import com.fs.twitchminichat.chat.HistoryBackfillSource.RECONNECT_OFFLINE
import com.fs.twitchminichat.chat.HistoryBackfillSource.RESUME
import com.fs.twitchminichat.chat.HistoryBackfillSource.RESUME_NO_PAUSE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the history backfill decision in [HistoryBackfillPolicy].
 *
 * It began as a characterization of the decision as it stood in ChatFragment at
 * `bf7dd65`, every expected value derived from that code. Each correction since has
 * replaced the `frozen_…` test that pinned what it corrects, in a commit of its own.
 *
 * Tests still named `frozen_…` pin behaviour that is known to be wrong, and each one
 * says what is wrong with it. They are meant to fail when that behaviour is
 * corrected, so that the correction changes a test on purpose rather than by accident.
 *
 * Journal lines are asserted as rendered text: the diagnostics analysis of log31 is
 * the baseline for judging later changes, and a renamed field or value would
 * invalidate it without anything else noticing.
 */
class HistoryBackfillPolicyTest {

    // ---------------------------------------------------------------------------
    // Frozen defects
    // ---------------------------------------------------------------------------

    @Test
    fun inFlight_suppressesUntilItsResultArrives_andAFailureLiftsItAtOnce() {
        /*
         * Replaces frozen_recentBackfillIsStampedWhenTheRequestLeaves_…, which pinned
         * the five-second window stamped when a request left: a request that failed in
         * four milliseconds suppressed every recovery for five seconds, and the journal
         * blamed the skips on a backfill that never arrived. Now the request in flight
         * is what suppresses - a reconnect and the resume after it ask milliseconds
         * apart, before any result exists - and its failure lifts that at once.
         */
        val start = inputs(offlineRecoveryAtMs = NOW - 60_000L)
        val sent = HistoryBackfillPolicy.onConnect(start) as Request
        assertEquals(NOW, sent.effects.armInFlightSinceMs)

        val inFlight = start.after(sent)
        assertEquals(NOW, inFlight.inFlightSinceMs)
        assertEquals(0L, inFlight.lastBackfillAtMs)

        assertEquals(
            Skip(
                reason = BACKFILL_IN_FLIGHT,
                details = listOf("awaySec" to 30, "inFlightMs" to 40L),
                effects = HistoryBackfillEffects(consumeLastPausedAt = true)
            ),
            HistoryBackfillPolicy.onResume(
                inFlight.copy(nowMs = NOW + 40L, lastPausedAtMs = NOW - 30_000L)
            )
        )
        assertEquals(
            Skip(
                reason = BACKFILL_IN_FLIGHT,
                details = listOf("inFlightMs" to 40L),
                effects = HistoryBackfillEffects()
            ),
            HistoryBackfillPolicy.onResume(inFlight.copy(nowMs = NOW + 40L))
        )

        val failed = inFlight.after(
            HistoryBackfillPolicy.afterFailure(
                failure = REQUEST_FAILED,
                requestedSec = sent.requestedSec,
                sentAtMs = NOW,
                inFlightSinceMs = inFlight.inFlightSinceMs,
                unrecoveredSinceMs = inFlight.unrecoveredSinceMs
            )
        )
        assertEquals(0L, failed.inFlightSinceMs)
        assertEquals(0L, failed.lastBackfillAtMs)

        /* Not suppressed; and it reaches back over the 70 s the failed request owed. */
        assertEquals(
            Request(
                source = RESUME,
                requestedSec = 80,
                details = listOf("awaySec" to 30, "unrecoveredSec" to 70),
                effects = HistoryBackfillEffects(
                    consumeLastPausedAt = true,
                    armInFlightSinceMs = NOW + 5L
                )
            ),
            HistoryBackfillPolicy.onResume(
                failed.copy(nowMs = NOW + 5L, lastPausedAtMs = NOW - 30_000L)
            )
        )
    }

    @Test
    fun afterRequestFailed_theLostWindowIsRecordedAndTheNextRequestReachesBackToIt() {
        /*
         * Replaces frozen_afterRequestFailed_nothingRecordsTheWindowThatWasLost, which
         * pinned that the window a failed request asked for existed only in its journal
         * line: the next request was sized by whatever reference it found then - the
         * few seconds of a page switch while IRC stayed up - and the lost window was
         * never asked for again. Now the failure records where that window began, and
         * every later request reaches back to it until one succeeds.
         */
        val start = inputs(offlineRecoveryAtMs = NOW - 600_000L)
        val sent = HistoryBackfillPolicy.onConnect(start) as Request
        assertEquals(RECONNECT_OFFLINE, sent.source)
        assertEquals(610, sent.requestedSec)

        val failure = HistoryBackfillPolicy.afterFailure(
            failure = REQUEST_FAILED,
            requestedSec = sent.requestedSec,
            sentAtMs = NOW,
            inFlightSinceMs = NOW,
            unrecoveredSinceMs = 0L
        )
        assertEquals(
            HistoryBackfillFailureEffects(
                inFlightSinceMs = 0L,
                unrecoveredSinceMs = NOW - 610_000L,
                clearHistoryLoaded = true,
                journalFields = listOf("reason" to "request_failed", "requestedSec" to 610)
            ),
            failure
        )

        val failed = start.after(sent).after(failure)
        assertEquals(
            start.copy(
                historyLoaded = false,
                offlineRecoveryAtMs = 0L,
                unrecoveredSinceMs = NOW - 610_000L
            ),
            failed
        )

        /* IRC stays up; a minute later the user switches pages for two seconds. */
        val pageSwitch = HistoryBackfillPolicy.onResume(
            failed.copy(nowMs = NOW + 62_000L, lastPausedAtMs = NOW + 60_000L)
        )
        assertEquals(
            Request(
                source = RESUME,
                requestedSec = 682,
                details = listOf("awaySec" to 2, "unrecoveredSec" to 672),
                effects = HistoryBackfillEffects(
                    consumeLastPausedAt = true,
                    armInFlightSinceMs = NOW + 62_000L
                )
            ),
            pageSwitch
        )
        assertEquals(
            "backfill.triggered source=resume awaySec=2 unrecoveredSec=672 requestedSec=682",
            render(pageSwitch)
        )

        /* The next connect asks for the whole hour anyway, and says nothing more. */
        assertEquals(
            "backfill.triggered source=first_connect requestedSec=3600",
            render(HistoryBackfillPolicy.onConnect(failed.copy(nowMs = NOW + 62_000L)))
        )

        /* The page switch's request succeeds: nothing is owed any more. */
        val recovered = failed
            .copy(nowMs = NOW + 62_000L, lastPausedAtMs = NOW + 60_000L)
            .after(pageSwitch)
            .after(
                HistoryBackfillPolicy.afterSuccess(
                    sentAtMs = NOW + 62_000L,
                    nowMs = NOW + 63_000L,
                    inFlightSinceMs = NOW + 62_000L
                )
            )
        assertEquals(0L, recovered.unrecoveredSinceMs)
        assertEquals(
            "backfill.triggered source=resume awaySec=2 requestedSec=30",
            render(
                HistoryBackfillPolicy.onResume(
                    recovered.copy(nowMs = NOW + 120_000L, lastPausedAtMs = NOW + 118_000L)
                )
            )
        )
    }

    @Test
    fun noPauseNoWatermarkNoStop_fallsBackToTheLastSuccessfulBackfill() {
        /*
         * Replaces frozen_noRecoveryReference_writesAJournalLineAndDoesNothingElse,
         * which pinned a dead end: a page resumed for the first time with no rendered
         * message, no onStop and no recent request wrote reason=no_recovery_reference
         * and asked for nothing, even when a backfill had arrived since. The last
         * successful backfill is a real reference now that it records an arrival, so
         * the page asks for the window since then. Only with no successful backfill
         * either does it skip, under a reason of its own.
         */
        val afterASuccess = HistoryBackfillPolicy.onResume(inputs(lastBackfillAtMs = NOW - 40_000L))
        assertEquals(
            Request(
                source = RESUME_NO_PAUSE,
                requestedSec = 50,
                details = listOf(
                    "renderedGapSec" to null,
                    "offlineSec" to null,
                    "sinceLastBackfillSec" to 40
                ),
                effects = HistoryBackfillEffects(armInFlightSinceMs = NOW)
            ),
            afterASuccess
        )
        assertEquals(
            "backfill.triggered source=resume_no_pause sinceLastBackfillSec=40 requestedSec=50",
            render(afterASuccess)
        )

        /* A success exactly five seconds old no longer suppresses, and is the reference. */
        assertEquals(
            "backfill.triggered source=resume_no_pause sinceLastBackfillSec=5 requestedSec=30",
            render(HistoryBackfillPolicy.onResume(inputs(lastBackfillAtMs = NOW - 5_000L)))
        )

        /* Nothing at all: the one case left with nothing to measure from. */
        val nothing = HistoryBackfillPolicy.onResume(inputs(historyLoaded = true))
        assertEquals(
            Skip(
                reason = NOTHING_TO_MEASURE_FROM,
                details = listOf("historyLoaded" to true),
                effects = HistoryBackfillEffects()
            ),
            nothing
        )
        assertEquals("backfill.skipped reason=nothing_to_measure_from historyLoaded=true", render(nothing))

        /* A newest message stamped in the future by a skewed clock still counts as none. */
        assertEquals(
            NOTHING_TO_MEASURE_FROM,
            (HistoryBackfillPolicy.onResume(
                inputs(lastRenderedMessageTsSec = NOW / 1000.0 + 5.0)
            ) as Skip).reason
        )
    }

    @Test
    fun frozen_reconnectOffline_consumesItsReferenceBeforeTheSkipChecks() {
        /*
         * PINS TODAY'S BEHAVIOUR. What is wrong: offlineRecoveryAtMs is consumed before
         * the skip checks run, so a reconnect that skips - because a request left under
         * five seconds ago, or because the stop was under a second ago - throws away
         * the only record of when the page went offline. The next connect falls back to
         * the render watermark, or to nothing.
         */
        val start = inputs(
            offlineRecoveryAtMs = NOW - 60_000L,
            lastBackfillAtMs = NOW - 1_000L,
            lastRenderedMessageTsSec = (NOW - 90_000L) / 1000.0
        )
        val skipped = HistoryBackfillPolicy.onConnect(start)
        assertEquals(
            Skip(
                reason = RECENT_BACKFILL,
                details = listOf("offlineSec" to 60, "sinceLastBackfillMs" to 1_000L),
                effects = HistoryBackfillEffects(consumeOfflineRecovery = true)
            ),
            skipped
        )

        assertEquals(
            Request(
                source = RECONNECT_NO_PAUSE_REFERENCE,
                requestedSec = 110,
                details = listOf("renderedGapSec" to 100),
                effects = HistoryBackfillEffects(
                    consumeOfflineRecovery = true,
                    armInFlightSinceMs = NOW + 10_000L
                )
            ),
            HistoryBackfillPolicy.onConnect(start.after(skipped).copy(nowMs = NOW + 10_000L))
        )

        assertEquals(
            Skip(
                reason = HISTORY_ALREADY_LOADED,
                details = listOf("offlineSec" to 0),
                effects = HistoryBackfillEffects(consumeOfflineRecovery = true)
            ),
            HistoryBackfillPolicy.onConnect(inputs(offlineRecoveryAtMs = NOW - 999L))
        )
    }

    @Test
    fun frozen_resume_consumesItsReferenceBeforeItsOwnChecks() {
        /*
         * PINS TODAY'S BEHAVIOUR. What is wrong: lastPausedAtMs is consumed before the
         * resume checks run, so a resume that skips - for a recent request, or for an
         * absence under a second - discards the instant the page was paused. If the
         * request that caused the skip did not cover that absence, no later resume can
         * measure it: the next one measures from the next pause.
         */
        val start = inputs(lastPausedAtMs = NOW - 30_000L, lastBackfillAtMs = NOW - 2_000L)
        val skipped = HistoryBackfillPolicy.onResume(start)
        assertEquals(
            Skip(
                reason = RECENT_BACKFILL,
                details = listOf("awaySec" to 30, "sinceLastBackfillMs" to 2_000L),
                effects = HistoryBackfillEffects(consumeLastPausedAt = true)
            ),
            skipped
        )
        assertEquals(0L, start.after(skipped).lastPausedAtMs)

        assertEquals(
            Skip(
                reason = AWAY_BELOW_THRESHOLD,
                details = listOf("awaySec" to 0),
                effects = HistoryBackfillEffects(consumeLastPausedAt = true)
            ),
            HistoryBackfillPolicy.onResume(inputs(lastPausedAtMs = NOW - 999L))
        )

        /* Paused again ten seconds later: only those ten seconds are measured. */
        assertEquals(
            Request(
                source = RESUME,
                requestedSec = 30,
                details = listOf("awaySec" to 10),
                effects = HistoryBackfillEffects(
                    consumeLastPausedAt = true,
                    armInFlightSinceMs = NOW + 20_000L
                )
            ),
            HistoryBackfillPolicy.onResume(
                start.after(skipped).copy(nowMs = NOW + 20_000L, lastPausedAtMs = NOW + 10_000L)
            )
        )
    }

    @Test
    fun frozen_firstConnect_ignoresTheFiveSecondWindowAndLeavesOfflineRecoveryArmed() {
        /*
         * PINS TODAY'S BEHAVIOUR. What is wrong, twice over: first_connect asks for the
         * full hour even when a request left a millisecond ago, and it does not consume
         * offlineRecoveryAtMs. The stale reference survives into the next mid-session
         * reconnect, which then asks again for a window measured from an old onStop that
         * the first connection's hour already covered.
         */
        val start = inputs(
            historyLoaded = false,
            lastBackfillAtMs = NOW - 1L,
            offlineRecoveryAtMs = NOW - 60_000L
        )
        val decision = HistoryBackfillPolicy.onConnect(start)
        assertEquals(
            Request(
                source = FIRST_CONNECT,
                requestedSec = 3600,
                details = emptyList(),
                effects = HistoryBackfillEffects(
                    armHistoryLoaded = true,
                    armInFlightSinceMs = NOW
                )
            ),
            decision
        )

        val armed = start.after(decision)
        assertEquals(NOW - 60_000L, armed.offlineRecoveryAtMs)

        assertEquals(
            Request(
                source = RECONNECT_OFFLINE,
                requestedSec = 670,
                details = listOf("offlineSec" to 660),
                effects = HistoryBackfillEffects(
                    consumeOfflineRecovery = true,
                    armInFlightSinceMs = NOW + 600_000L
                )
            ),
            HistoryBackfillPolicy.onConnect(armed.copy(nowMs = NOW + 600_000L))
        )
    }

    @Test
    fun frozen_manualRefresh_ignoresTheFiveSecondWindowAndIsFixedAtTwoMinutes() {
        /*
         * PINS TODAY'S BEHAVIOUR. What is wrong: the refresh button asks for 120
         * seconds whatever the page actually missed, and it ignores the five-second
         * window, so it can run beside a request that left a millisecond earlier. Only
         * its own 1.5-second tap debounce, which stays in ChatFragment, limits it.
         */
        val decision = HistoryBackfillPolicy.onManualRefresh(
            inputs(
                lastBackfillAtMs = NOW - 1L,
                offlineRecoveryAtMs = NOW - 3_600_000L,
                lastPausedAtMs = NOW - 900_000L,
                lastStoppedAtMs = NOW - 3_600_000L,
                lastRenderedMessageTsSec = (NOW - 1_800_000L) / 1000.0
            )
        )

        assertEquals(
            Request(
                source = MANUAL_REFRESH,
                requestedSec = 120,
                details = emptyList(),
                effects = HistoryBackfillEffects(armLastBackfillAtMs = NOW)
            ),
            decision
        )
        assertEquals("backfill.triggered source=manual_refresh requestedSec=120", render(decision))
    }

    // ---------------------------------------------------------------------------
    // Journal lines, byte for byte
    // ---------------------------------------------------------------------------

    @Test
    fun journal_everyDecisionKeepsItsEventFieldNamesAndValues() {
        val cases = listOf(
            HistoryBackfillPolicy.onConnect(inputs(historyLoaded = false)) to
                    "backfill.triggered source=first_connect requestedSec=3600",
            HistoryBackfillPolicy.onConnect(inputs(offlineRecoveryAtMs = NOW - 60_000L)) to
                    "backfill.triggered source=reconnect_offline offlineSec=60 requestedSec=70",
            HistoryBackfillPolicy.onConnect(
                inputs(lastRenderedMessageTsSec = (NOW - 100_000L) / 1000.0)
            ) to "backfill.triggered source=reconnect_no_pause_reference renderedGapSec=100 requestedSec=110",
            HistoryBackfillPolicy.onResume(inputs(lastPausedAtMs = NOW - 42_000L)) to
                    "backfill.triggered source=resume awaySec=42 requestedSec=52",
            HistoryBackfillPolicy.onResume(
                inputs(
                    lastRenderedMessageTsSec = (NOW - 200_000L) / 1000.0,
                    lastStoppedAtMs = NOW - 300_000L
                )
            ) to "backfill.triggered source=resume_no_pause renderedGapSec=200 offlineSec=300 requestedSec=310",
            HistoryBackfillPolicy.onResume(inputs(lastStoppedAtMs = NOW - 300_000L)) to
                    "backfill.triggered source=resume_no_pause offlineSec=300 requestedSec=310",
            HistoryBackfillPolicy.onManualRefresh(inputs()) to
                    "backfill.triggered source=manual_refresh requestedSec=120",
            HistoryBackfillPolicy.onConnect(
                inputs(offlineRecoveryAtMs = NOW - 60_000L, unrecoveredSinceMs = NOW - 600_000L)
            ) to "backfill.triggered source=reconnect_offline offlineSec=60 unrecoveredSec=600 requestedSec=610",
            HistoryBackfillPolicy.onResume(
                inputs(lastPausedAtMs = NOW - 42_000L, lastBackfillAtMs = NOW - 3_000L)
            ) to "backfill.skipped reason=recent_backfill awaySec=42 sinceLastBackfillMs=3000",
            HistoryBackfillPolicy.onResume(inputs(lastBackfillAtMs = NOW - 3_000L)) to
                    "backfill.skipped reason=recent_backfill sinceLastBackfillMs=3000",
            HistoryBackfillPolicy.onConnect(
                inputs(offlineRecoveryAtMs = NOW - 60_000L, lastBackfillAtMs = NOW - 3_000L)
            ) to "backfill.skipped reason=recent_backfill offlineSec=60 sinceLastBackfillMs=3000",
            HistoryBackfillPolicy.onConnect(inputs(lastBackfillAtMs = NOW - 3_000L)) to
                    "backfill.skipped reason=recent_backfill sinceLastBackfillMs=3000",
            HistoryBackfillPolicy.onConnect(
                inputs(offlineRecoveryAtMs = NOW - 60_000L, inFlightSinceMs = NOW - 250L)
            ) to "backfill.skipped reason=backfill_in_flight offlineSec=60 inFlightMs=250",
            HistoryBackfillPolicy.onResume(
                inputs(lastPausedAtMs = NOW - 42_000L, inFlightSinceMs = NOW - 250L)
            ) to "backfill.skipped reason=backfill_in_flight awaySec=42 inFlightMs=250",
            HistoryBackfillPolicy.onResume(inputs(inFlightSinceMs = NOW - 250L)) to
                    "backfill.skipped reason=backfill_in_flight inFlightMs=250",
            HistoryBackfillPolicy.onConnect(inputs(offlineRecoveryAtMs = NOW - 500L)) to
                    "backfill.skipped reason=history_already_loaded offlineSec=0",
            HistoryBackfillPolicy.onResume(inputs(lastPausedAtMs = NOW - 500L)) to
                    "backfill.skipped reason=away_below_threshold awaySec=0",
            HistoryBackfillPolicy.onResume(inputs(historyLoaded = true)) to
                    "backfill.skipped reason=nothing_to_measure_from historyLoaded=true",
            HistoryBackfillPolicy.onConnect(inputs()) to
                    "backfill.skipped reason=nothing_to_measure_from_reconnect",
            HistoryBackfillPolicy.onResume(inputs(lastBackfillAtMs = NOW - 40_000L)) to
                    "backfill.triggered source=resume_no_pause sinceLastBackfillSec=40 requestedSec=50",
            HistoryBackfillPolicy.onConnect(inputs(lastBackfillAtMs = NOW - 40_000L)) to
                    "backfill.triggered source=reconnect_no_pause_reference sinceLastBackfillSec=40 requestedSec=50"
        )

        cases.forEach { (decision, expected) ->
            assertEquals(expected, render(decision))
        }
    }

    @Test
    fun journal_nullFieldsStayInTheDecisionForTheJournalToOmit() {
        /* The journal drops null values; the decision still lists the field in place. */
        assertEquals(
            listOf(
                "reason" to "recent_backfill",
                "offlineSec" to null,
                "sinceLastBackfillMs" to 3_000L
            ),
            HistoryBackfillPolicy.onConnect(inputs(lastBackfillAtMs = NOW - 3_000L)).journalFields
        )
        assertEquals(
            listOf(
                "source" to "resume_no_pause",
                "renderedGapSec" to null,
                "offlineSec" to 300,
                "requestedSec" to 310
            ),
            HistoryBackfillPolicy.onResume(inputs(lastStoppedAtMs = NOW - 300_000L)).journalFields
        )
    }

    @Test
    fun journal_everyFailureKeepsItsEventFieldNamesAndValues() {
        assertEquals(
            "backfill.failed reason=session_missing requestedSec=3600",
            render(failure(SESSION_MISSING, 3600))
        )
        assertEquals(
            "backfill.failed reason=reauthorization_required requestedSec=70",
            render(failure(REAUTHORIZATION_REQUIRED, 70))
        )
        assertEquals(
            "backfill.failed reason=request_failed requestedSec=120",
            render(failure(REQUEST_FAILED, 120))
        )
    }

    // ---------------------------------------------------------------------------
    // The rest of today's decision
    // ---------------------------------------------------------------------------

    @Test
    fun failure_onlyReauthorizationKeepsHistoryLoaded() {
        assertEquals(true, failure(SESSION_MISSING, 30).clearHistoryLoaded)
        assertEquals(false, failure(REAUTHORIZATION_REQUIRED, 30).clearHistoryLoaded)
        assertEquals(true, failure(REQUEST_FAILED, 30).clearHistoryLoaded)
    }

    @Test
    fun stopThenStart_theReconnectAsksForTheOfflineWindowAndTheResumeAfterItSkips() {
        val start = inputs(
            lastPausedAtMs = NOW - 120_500L,
            lastStoppedAtMs = NOW - 120_000L,
            offlineRecoveryAtMs = NOW - 120_000L
        )
        val connect = HistoryBackfillPolicy.onConnect(start)
        assertEquals("backfill.triggered source=reconnect_offline offlineSec=120 requestedSec=130", render(connect))

        /* Forty milliseconds later the reconnect's request has no result yet: it is in flight. */
        val resume = HistoryBackfillPolicy.onResume(start.after(connect).copy(nowMs = NOW + 40L))
        assertEquals(
            Skip(
                reason = BACKFILL_IN_FLIGHT,
                details = listOf("awaySec" to 120, "inFlightMs" to 40L),
                effects = HistoryBackfillEffects(consumeLastPausedAt = true)
            ),
            resume
        )
    }

    @Test
    fun connect_anOfflineReferenceUnderOneSecondWinsOverARecentRequest() {
        assertEquals(
            HISTORY_ALREADY_LOADED,
            (HistoryBackfillPolicy.onConnect(
                inputs(offlineRecoveryAtMs = NOW - 500L, lastBackfillAtMs = NOW - 100L)
            ) as Skip).reason
        )
    }

    @Test
    fun connect_anOfflineReferenceOfOneSecondRequestsTheMinimumWindow() {
        assertEquals(
            Request(
                source = RECONNECT_OFFLINE,
                requestedSec = 30,
                details = listOf("offlineSec" to 1),
                effects = HistoryBackfillEffects(
                    consumeOfflineRecovery = true,
                    armInFlightSinceMs = NOW
                )
            ),
            HistoryBackfillPolicy.onConnect(inputs(offlineRecoveryAtMs = NOW - 1_000L))
        )
    }

    @Test
    fun connect_withoutAnyReferenceStillConsumesTheOfflineReference() {
        assertEquals(
            Skip(
                reason = NOTHING_TO_MEASURE_FROM_RECONNECT,
                details = emptyList(),
                effects = HistoryBackfillEffects(consumeOfflineRecovery = true)
            ),
            HistoryBackfillPolicy.onConnect(inputs())
        )
    }

    @Test
    fun resume_oneSecondAwayRequestsEvenWithAnIrcClient() {
        assertEquals(
            Request(
                source = RESUME,
                requestedSec = 30,
                details = listOf("awaySec" to 1),
                effects = HistoryBackfillEffects(
                    consumeLastPausedAt = true,
                    armInFlightSinceMs = NOW
                )
            ),
            HistoryBackfillPolicy.onResume(inputs(lastPausedAtMs = NOW - 1_000L))
        )
    }

    @Test
    fun resume_underOneSecondStillRequestsWhenNoIrcClientExists() {
        assertEquals(
            Request(
                source = RESUME,
                requestedSec = 30,
                details = listOf("awaySec" to 0),
                effects = HistoryBackfillEffects(
                    consumeLastPausedAt = true,
                    armInFlightSinceMs = NOW
                )
            ),
            HistoryBackfillPolicy.onResume(
                inputs(lastPausedAtMs = NOW - 500L, ircClientPresent = false)
            )
        )
    }

    @Test
    fun resumeWithoutPause_takesTheLargerOfTheTwoReferences() {
        assertEquals(
            Request(
                source = RESUME_NO_PAUSE,
                requestedSec = 610,
                details = listOf("renderedGapSec" to 600, "offlineSec" to 45),
                effects = HistoryBackfillEffects(armInFlightSinceMs = NOW)
            ),
            HistoryBackfillPolicy.onResume(
                inputs(
                    lastRenderedMessageTsSec = (NOW - 600_000L) / 1000.0,
                    lastStoppedAtMs = NOW - 45_000L
                )
            )
        )
    }

    @Test
    fun window_isElapsedPlusTenClampedToThirtySecondsAndOneHour() {
        assertEquals(30, HistoryBackfillPolicy.historyWindowSeconds(0))
        assertEquals(30, HistoryBackfillPolicy.historyWindowSeconds(20))
        assertEquals(31, HistoryBackfillPolicy.historyWindowSeconds(21))
        assertEquals(3600, HistoryBackfillPolicy.historyWindowSeconds(3590))
        assertEquals(3600, HistoryBackfillPolicy.historyWindowSeconds(86_400))
    }

    @Test
    fun recentWindow_isStrictlyUnderFiveSeconds() {
        assertNull(HistoryBackfillPolicy.msSinceRecentBackfill(NOW, 0L))
        assertEquals(4_999L, HistoryBackfillPolicy.msSinceRecentBackfill(NOW, NOW - 4_999L))
        assertNull(HistoryBackfillPolicy.msSinceRecentBackfill(NOW, NOW - 5_000L))

        /* Today a stamp in the future, after the clock moved back, counts as recent. */
        assertEquals(-1_000L, HistoryBackfillPolicy.msSinceRecentBackfill(NOW, NOW + 1_000L))
    }

    @Test
    fun renderedGap_truncatesAndIgnoresMissingOrFutureWatermarks() {
        assertNull(HistoryBackfillPolicy.secondsSinceLastRenderedMessage(NOW, 0.0))
        assertNull(HistoryBackfillPolicy.secondsSinceLastRenderedMessage(NOW, NOW / 1000.0))
        assertNull(HistoryBackfillPolicy.secondsSinceLastRenderedMessage(NOW, NOW / 1000.0 + 1.0))
        assertEquals(
            12,
            HistoryBackfillPolicy.secondsSinceLastRenderedMessage(NOW, (NOW - 12_900L) / 1000.0)
        )
    }

    @Test
    fun offlineSinceStop_isNullOnlyBeforeTheFirstStop() {
        assertNull(HistoryBackfillPolicy.offlineSecondsSinceStop(NOW, 0L))
        assertEquals(0, HistoryBackfillPolicy.offlineSecondsSinceStop(NOW, NOW - 999L))
        assertEquals(61, HistoryBackfillPolicy.offlineSecondsSinceStop(NOW, NOW - 61_500L))
    }

    @Test
    fun success_startsTheFiveSecondWindowWhenTheResultArrives() {
        val sent = HistoryBackfillPolicy.onConnect(inputs(offlineRecoveryAtMs = NOW - 60_000L))
        val arrived = inputs(offlineRecoveryAtMs = NOW - 60_000L)
            .after(sent)
            .after(
                HistoryBackfillPolicy.afterSuccess(
                    sentAtMs = NOW,
                    nowMs = NOW + 2_000L,
                    inFlightSinceMs = NOW
                )
            )
        assertEquals(0L, arrived.inFlightSinceMs)
        assertEquals(NOW + 2_000L, arrived.lastBackfillAtMs)

        assertEquals(
            Skip(
                reason = RECENT_BACKFILL,
                details = listOf("sinceLastBackfillMs" to 4_999L),
                effects = HistoryBackfillEffects()
            ),
            HistoryBackfillPolicy.onResume(arrived.copy(nowMs = NOW + 6_999L))
        )
        assertEquals(
            RESUME_NO_PAUSE,
            (HistoryBackfillPolicy.onResume(
                arrived.copy(nowMs = NOW + 7_000L, lastStoppedAtMs = NOW - 60_000L)
            ) as Request).source
        )
    }

    @Test
    fun aResultClearsOnlyTheInFlightMarkerItSet() {
        /* A later request has replaced the marker; an earlier one's result must not clear it. */
        assertEquals(
            NOW + 1_000L,
            HistoryBackfillPolicy.afterFailure(
                failure = REQUEST_FAILED,
                requestedSec = 70,
                sentAtMs = NOW,
                inFlightSinceMs = NOW + 1_000L,
                unrecoveredSinceMs = 0L
            ).inFlightSinceMs
        )
        assertEquals(
            NOW + 1_000L,
            HistoryBackfillPolicy.afterSuccess(
                sentAtMs = NOW,
                nowMs = NOW + 2_000L,
                inFlightSinceMs = NOW + 1_000L
            ).inFlightSinceMs
        )
        assertEquals(
            0L,
            HistoryBackfillPolicy.afterSuccess(
                sentAtMs = NOW,
                nowMs = NOW + 2_000L,
                inFlightSinceMs = NOW
            ).inFlightSinceMs
        )
    }

    @Test
    fun inFlight_stopsSuppressingAtTheCeiling() {
        assertEquals(30_000L, HistoryBackfillPolicy.IN_FLIGHT_CEILING_MS)
        assertNull(HistoryBackfillPolicy.msInFlight(NOW, 0L))
        assertEquals(29_999L, HistoryBackfillPolicy.msInFlight(NOW, NOW - 29_999L))
        assertNull(HistoryBackfillPolicy.msInFlight(NOW, NOW - 30_000L))

        assertEquals(
            BACKFILL_IN_FLIGHT,
            (HistoryBackfillPolicy.onConnect(
                inputs(offlineRecoveryAtMs = NOW - 60_000L, inFlightSinceMs = NOW - 29_999L)
            ) as Skip).reason
        )
        assertEquals(
            RECONNECT_OFFLINE,
            (HistoryBackfillPolicy.onConnect(
                inputs(offlineRecoveryAtMs = NOW - 60_000L, inFlightSinceMs = NOW - 30_000L)
            ) as Request).source
        )
    }

    @Test
    fun inFlight_isReportedBeforeARecentSuccess() {
        assertEquals(
            Skip(
                reason = BACKFILL_IN_FLIGHT,
                details = listOf("offlineSec" to 60, "inFlightMs" to 100L),
                effects = HistoryBackfillEffects(consumeOfflineRecovery = true)
            ),
            HistoryBackfillPolicy.onConnect(
                inputs(
                    offlineRecoveryAtMs = NOW - 60_000L,
                    inFlightSinceMs = NOW - 100L,
                    lastBackfillAtMs = NOW - 1_000L
                )
            )
        )
    }

    @Test
    fun unrecovered_keepsTheOlderOfTwoStarts() {
        /* The new failure's window starts at NOW - 70 s. */
        assertEquals(
            NOW - 900_000L,
            HistoryBackfillPolicy.afterFailure(
                failure = REQUEST_FAILED,
                requestedSec = 70,
                sentAtMs = NOW,
                inFlightSinceMs = NOW,
                unrecoveredSinceMs = NOW - 900_000L
            ).unrecoveredSinceMs
        )
        assertEquals(
            NOW - 70_000L,
            HistoryBackfillPolicy.afterFailure(
                failure = REQUEST_FAILED,
                requestedSec = 70,
                sentAtMs = NOW,
                inFlightSinceMs = NOW,
                unrecoveredSinceMs = NOW - 10_000L
            ).unrecoveredSinceMs
        )
    }

    @Test
    fun unrecovered_isRecordedForEveryFailureReason_fromWhenTheRequestLeft() {
        HistoryBackfillFailure.values().forEach { reason ->
            assertEquals(
                reason.name,
                NOW - 120_000L,
                HistoryBackfillPolicy.afterFailure(
                    failure = reason,
                    requestedSec = 120,
                    sentAtMs = NOW,
                    inFlightSinceMs = NOW,
                    unrecoveredSinceMs = 0L
                ).unrecoveredSinceMs
            )
        }
    }

    @Test
    fun success_clearsTheOwedWindow() {
        assertEquals(
            HistoryBackfillSuccessEffects(
                inFlightSinceMs = 0L,
                lastBackfillAtMs = NOW + 1_000L,
                unrecoveredSinceMs = 0L
            ),
            HistoryBackfillPolicy.afterSuccess(
                sentAtMs = NOW,
                nowMs = NOW + 1_000L,
                inFlightSinceMs = NOW
            )
        )
    }

    @Test
    fun window_isTheLargerOfTheRequestsOwnAndTheOwedOne() {
        /* Its own is larger: no field, its own window. */
        assertEquals(
            "backfill.triggered source=resume awaySec=600 requestedSec=610",
            render(
                HistoryBackfillPolicy.onResume(
                    inputs(lastPausedAtMs = NOW - 600_000L, unrecoveredSinceMs = NOW - 100_000L)
                )
            )
        )
        /* The owed one is larger: widened, and the line says by what. */
        assertEquals(
            "backfill.triggered source=resume awaySec=100 unrecoveredSec=600 requestedSec=610",
            render(
                HistoryBackfillPolicy.onResume(
                    inputs(lastPausedAtMs = NOW - 100_000L, unrecoveredSinceMs = NOW - 600_000L)
                )
            )
        )
        /* Both clamp to the same 30 s: nothing was widened, so nothing is said. */
        assertEquals(
            "backfill.triggered source=resume awaySec=5 requestedSec=30",
            render(
                HistoryBackfillPolicy.onResume(
                    inputs(lastPausedAtMs = NOW - 5_000L, unrecoveredSinceMs = NOW - 15_000L)
                )
            )
        )
    }

    @Test
    fun window_owedForMoreThanAnHourStillAsksForTheHour() {
        assertEquals(
            "backfill.triggered source=reconnect_offline offlineSec=60 unrecoveredSec=7200 requestedSec=3600",
            render(
                HistoryBackfillPolicy.onConnect(
                    inputs(offlineRecoveryAtMs = NOW - 60_000L, unrecoveredSinceMs = NOW - 7_200_000L)
                )
            )
        )
    }

    @Test
    fun manualRefresh_reachesBackOnlyWhenTheOwedWindowIsLonger() {
        assertEquals(
            "backfill.triggered source=manual_refresh requestedSec=120",
            render(HistoryBackfillPolicy.onManualRefresh(inputs(unrecoveredSinceMs = NOW - 50_000L)))
        )
        assertEquals(
            "backfill.triggered source=manual_refresh unrecoveredSec=300 requestedSec=310",
            render(HistoryBackfillPolicy.onManualRefresh(inputs(unrecoveredSinceMs = NOW - 300_000L)))
        )
    }

    @Test
    fun firstConnect_alreadyAsksForTheHourAndIsNeverWidened() {
        assertEquals(
            "backfill.triggered source=first_connect requestedSec=3600",
            render(
                HistoryBackfillPolicy.onConnect(
                    inputs(historyLoaded = false, unrecoveredSinceMs = NOW - 7_200_000L)
                )
            )
        )
    }

    @Test
    fun anOwedWindowIsAReferenceOfItsOwn() {
        assertEquals(
            "backfill.triggered source=reconnect_no_pause_reference unrecoveredSec=400 requestedSec=410",
            render(HistoryBackfillPolicy.onConnect(inputs(unrecoveredSinceMs = NOW - 400_000L)))
        )
        assertEquals(
            "backfill.triggered source=resume_no_pause unrecoveredSec=400 requestedSec=410",
            render(HistoryBackfillPolicy.onResume(inputs(unrecoveredSinceMs = NOW - 400_000L)))
        )
    }

    @Test
    fun reconnectWithNoReference_fallsBackToTheLastSuccessfulBackfill() {
        assertEquals(
            "backfill.triggered source=reconnect_no_pause_reference sinceLastBackfillSec=40 requestedSec=50",
            render(HistoryBackfillPolicy.onConnect(inputs(lastBackfillAtMs = NOW - 40_000L)))
        )
        assertEquals(
            "backfill.skipped reason=nothing_to_measure_from_reconnect",
            render(HistoryBackfillPolicy.onConnect(inputs()))
        )
    }

    @Test
    fun theLastSuccessfulBackfillIsUsedOnlyWhenNothingCloserExists() {
        assertEquals(
            "backfill.triggered source=resume_no_pause renderedGapSec=100 requestedSec=110",
            render(
                HistoryBackfillPolicy.onResume(
                    inputs(
                        lastBackfillAtMs = NOW - 40_000L,
                        lastRenderedMessageTsSec = (NOW - 100_000L) / 1000.0
                    )
                )
            )
        )
        assertEquals(
            "backfill.triggered source=resume_no_pause offlineSec=20 requestedSec=30",
            render(
                HistoryBackfillPolicy.onResume(
                    inputs(lastBackfillAtMs = NOW - 400_000L, lastStoppedAtMs = NOW - 20_000L)
                )
            )
        )
        assertEquals(
            "backfill.triggered source=reconnect_no_pause_reference renderedGapSec=100 requestedSec=110",
            render(
                HistoryBackfillPolicy.onConnect(
                    inputs(
                        lastBackfillAtMs = NOW - 400_000L,
                        lastRenderedMessageTsSec = (NOW - 100_000L) / 1000.0
                    )
                )
            )
        )
    }

    @Test
    fun theLastSuccessfulBackfillIsStillWidenedByAnOwedWindow() {
        assertEquals(
            "backfill.triggered source=resume_no_pause sinceLastBackfillSec=40 unrecoveredSec=300 requestedSec=310",
            render(
                HistoryBackfillPolicy.onResume(
                    inputs(lastBackfillAtMs = NOW - 40_000L, unrecoveredSinceMs = NOW - 300_000L)
                )
            )
        )
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private companion object {
        /** 2026-09-21T20:26:40Z, a fixed instant; the policy has no clock of its own. */
        const val NOW = 1_790_000_000_000L
    }

    /** Builds decision inputs; every reference defaults to "never". */
    private fun inputs(
        nowMs: Long = NOW,
        historyLoaded: Boolean = true,
        lastBackfillAtMs: Long = 0L,
        inFlightSinceMs: Long = 0L,
        unrecoveredSinceMs: Long = 0L,
        lastPausedAtMs: Long = 0L,
        lastStoppedAtMs: Long = 0L,
        offlineRecoveryAtMs: Long = 0L,
        lastRenderedMessageTsSec: Double = 0.0,
        ircClientPresent: Boolean = true
    ) = HistoryBackfillInputs(
        nowMs = nowMs,
        historyLoaded = historyLoaded,
        lastBackfillAtMs = lastBackfillAtMs,
        inFlightSinceMs = inFlightSinceMs,
        unrecoveredSinceMs = unrecoveredSinceMs,
        lastPausedAtMs = lastPausedAtMs,
        lastStoppedAtMs = lastStoppedAtMs,
        offlineRecoveryAtMs = offlineRecoveryAtMs,
        lastRenderedMessageTsSec = lastRenderedMessageTsSec,
        ircClientPresent = ircClientPresent
    )

    /** Applies a decision's effects to the stored references, as ChatFragment does. */
    private fun HistoryBackfillInputs.after(decision: HistoryBackfillDecision): HistoryBackfillInputs {
        val effects = decision.effects
        return copy(
            historyLoaded = historyLoaded || effects.armHistoryLoaded,
            inFlightSinceMs = effects.armInFlightSinceMs ?: inFlightSinceMs,
            lastPausedAtMs = if (effects.consumeLastPausedAt) 0L else lastPausedAtMs,
            offlineRecoveryAtMs = if (effects.consumeOfflineRecovery) 0L else offlineRecoveryAtMs
        )
    }

    /** Applies a failure's effects to the stored references, as ChatFragment does. */
    private fun HistoryBackfillInputs.after(failure: HistoryBackfillFailureEffects): HistoryBackfillInputs {
        return copy(
            historyLoaded = historyLoaded && !failure.clearHistoryLoaded,
            inFlightSinceMs = failure.inFlightSinceMs,
            unrecoveredSinceMs = failure.unrecoveredSinceMs
        )
    }

    /** Applies a success's effects to the stored references, as ChatFragment does. */
    private fun HistoryBackfillInputs.after(success: HistoryBackfillSuccessEffects): HistoryBackfillInputs {
        return copy(
            inFlightSinceMs = success.inFlightSinceMs,
            lastBackfillAtMs = success.lastBackfillAtMs,
            unrecoveredSinceMs = success.unrecoveredSinceMs
        )
    }

    /** One failure of a request sent at [NOW], with nothing else in flight or owed. */
    private fun failure(failure: HistoryBackfillFailure, requestedSec: Int) =
        HistoryBackfillPolicy.afterFailure(
            failure = failure,
            requestedSec = requestedSec,
            sentAtMs = NOW,
            inFlightSinceMs = NOW,
            unrecoveredSinceMs = 0L
        )

    /**
     * Renders an event and its fields the way HistoryDiagnosticsLog.record does after
     * the account, channel and resumed fields: nulls dropped, `key=value`, one space.
     */
    private fun render(decision: HistoryBackfillDecision): String =
        render(decision.journalEvent, decision.journalFields)

    private fun render(failure: HistoryBackfillFailureEffects): String =
        render(failure.journalEvent, failure.journalFields)

    private fun render(event: String, fields: List<Pair<String, Any?>>): String {
        val rendered = fields
            .filter { (_, value) -> value != null }
            .joinToString(separator = " ") { (key, value) -> "$key=$value" }
        return if (rendered.isEmpty()) event else "$event $rendered"
    }
}

/**
 * TEMPORARY. Lets the four frozen tests that still spell the request's in-flight
 * effect by its old name compile and run unchanged until each is renamed in a
 * commit of its own; the last of those commits deletes this function.
 */
@Suppress("FunctionName")
private fun HistoryBackfillEffects(
    consumeLastPausedAt: Boolean = false,
    consumeOfflineRecovery: Boolean = false,
    armHistoryLoaded: Boolean = false,
    armLastBackfillAtMs: Long
): HistoryBackfillEffects = HistoryBackfillEffects(
    consumeLastPausedAt = consumeLastPausedAt,
    consumeOfflineRecovery = consumeOfflineRecovery,
    armHistoryLoaded = armHistoryLoaded,
    armInFlightSinceMs = armLastBackfillAtMs
)
