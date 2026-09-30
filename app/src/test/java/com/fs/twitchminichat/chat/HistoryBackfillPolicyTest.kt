package com.fs.twitchminichat.chat

import com.fs.twitchminichat.chat.HistoryBackfillDecision.Request
import com.fs.twitchminichat.chat.HistoryBackfillDecision.Skip
import com.fs.twitchminichat.chat.HistoryBackfillFailure.REAUTHORIZATION_REQUIRED
import com.fs.twitchminichat.chat.HistoryBackfillFailure.REQUEST_FAILED
import com.fs.twitchminichat.chat.HistoryBackfillFailure.SESSION_MISSING
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.AWAY_BELOW_THRESHOLD
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.HISTORY_ALREADY_LOADED
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.NO_RECOVERY_REFERENCE
import com.fs.twitchminichat.chat.HistoryBackfillSkipReason.NO_RECOVERY_REFERENCE_RECONNECT
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
 * Characterizes the history backfill decision as it stood in ChatFragment at
 * `bf7dd65`, before it moved into [HistoryBackfillPolicy].
 *
 * Every expected value was derived from that code, not from the policy.
 *
 * Tests named `frozen_…` pin behaviour that is known to be wrong, and each one says
 * what is wrong with it. They are meant to fail when that behaviour is corrected, so
 * that the correction changes a test on purpose rather than by accident.
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
    fun frozen_recentBackfillIsStampedWhenTheRequestLeaves_soAFailureSuppressesTheNextFiveSeconds() {
        /*
         * PINS TODAY'S BEHAVIOUR. What is wrong: the recent-backfill time is stamped
         * when the request is sent, and no failure clears it, so for five seconds after
         * a request that delivered nothing the resume paths skip with
         * reason=recent_backfill, and the journal blames the skip on a backfill that
         * never arrived. The connect path escapes only because the failure also clears
         * historyLoaded and first_connect ignores the window (pinned separately).
         */
        val start = inputs(offlineRecoveryAtMs = NOW - 60_000L)
        val sent = HistoryBackfillPolicy.onConnect(start) as Request
        assertEquals(NOW, sent.effects.armLastBackfillAtMs)

        val failed = start
            .after(sent)
            .after(HistoryBackfillPolicy.afterFailure(REQUEST_FAILED, sent.requestedSec))
        assertEquals(NOW, failed.lastBackfillAtMs)

        assertEquals(
            Skip(
                reason = RECENT_BACKFILL,
                details = listOf("awaySec" to 34, "sinceLastBackfillMs" to 4_999L),
                effects = HistoryBackfillEffects(consumeLastPausedAt = true)
            ),
            HistoryBackfillPolicy.onResume(
                failed.copy(nowMs = NOW + 4_999L, lastPausedAtMs = NOW - 30_000L)
            )
        )
        assertEquals(
            Skip(
                reason = RECENT_BACKFILL,
                details = listOf("sinceLastBackfillMs" to 4_999L),
                effects = HistoryBackfillEffects()
            ),
            HistoryBackfillPolicy.onResume(failed.copy(nowMs = NOW + 4_999L))
        )

        assertEquals(
            Request(
                source = RESUME,
                requestedSec = 45,
                details = listOf("awaySec" to 35),
                effects = HistoryBackfillEffects(
                    consumeLastPausedAt = true,
                    armLastBackfillAtMs = NOW + 5_000L
                )
            ),
            HistoryBackfillPolicy.onResume(
                failed.copy(nowMs = NOW + 5_000L, lastPausedAtMs = NOW - 30_000L)
            )
        )
    }

    @Test
    fun frozen_afterRequestFailed_nothingRecordsTheWindowThatWasLost() {
        /*
         * PINS TODAY'S BEHAVIOUR. What is wrong: the window a failed request asked for
         * exists only in its journal line. The failure clears historyLoaded and nothing
         * else, and the offline reference that measured the window was consumed before
         * the request left. The next request is sized by whatever reference it finds
         * then - the few seconds of a page switch while IRC stays up, or the full hour
         * on the next connect - and never by the window that was lost.
         */
        val start = inputs(offlineRecoveryAtMs = NOW - 600_000L)
        val sent = HistoryBackfillPolicy.onConnect(start) as Request
        assertEquals(RECONNECT_OFFLINE, sent.source)
        assertEquals(610, sent.requestedSec)

        val failure = HistoryBackfillPolicy.afterFailure(REQUEST_FAILED, sent.requestedSec)
        assertEquals(
            HistoryBackfillFailureEffects(
                clearHistoryLoaded = true,
                journalFields = listOf("reason" to "request_failed", "requestedSec" to 610)
            ),
            failure
        )

        val failed = start.after(sent).after(failure)
        assertEquals(
            start.copy(historyLoaded = false, offlineRecoveryAtMs = 0L, lastBackfillAtMs = NOW),
            failed
        )

        assertEquals(
            Request(
                source = RESUME,
                requestedSec = 30,
                details = listOf("awaySec" to 2),
                effects = HistoryBackfillEffects(
                    consumeLastPausedAt = true,
                    armLastBackfillAtMs = NOW + 62_000L
                )
            ),
            HistoryBackfillPolicy.onResume(
                failed.copy(nowMs = NOW + 62_000L, lastPausedAtMs = NOW + 60_000L)
            )
        )

        assertEquals(
            Request(
                source = FIRST_CONNECT,
                requestedSec = 3600,
                details = emptyList(),
                effects = HistoryBackfillEffects(
                    armHistoryLoaded = true,
                    armLastBackfillAtMs = NOW + 120_000L
                )
            ),
            HistoryBackfillPolicy.onConnect(failed.copy(nowMs = NOW + 120_000L))
        )
    }

    @Test
    fun frozen_noRecoveryReference_writesAJournalLineAndDoesNothingElse() {
        /*
         * PINS TODAY'S BEHAVIOUR. What is wrong: a page resumed for the first time with
         * no rendered message, no onStop and no request in the last five seconds records
         * reason=no_recovery_reference and stops there. It requests nothing, arms
         * nothing and schedules nothing, so whatever that page missed stays missing
         * until some other path happens to ask.
         */
        val nothing = inputs(historyLoaded = true)
        val decision = HistoryBackfillPolicy.onResume(nothing)

        assertEquals(
            Skip(
                reason = NO_RECOVERY_REFERENCE,
                details = listOf("historyLoaded" to true),
                effects = HistoryBackfillEffects()
            ),
            decision
        )
        assertEquals("backfill.skipped reason=no_recovery_reference historyLoaded=true", render(decision))

        /* A request exactly five seconds old is no longer recent, and changes nothing here. */
        assertEquals(
            "backfill.skipped reason=no_recovery_reference historyLoaded=false",
            render(
                HistoryBackfillPolicy.onResume(
                    inputs(historyLoaded = false, lastBackfillAtMs = NOW - 5_000L)
                )
            )
        )

        /* A newest message stamped in the future by a skewed clock counts as none. */
        assertEquals(
            NO_RECOVERY_REFERENCE,
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
                    armLastBackfillAtMs = NOW + 10_000L
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
                    armLastBackfillAtMs = NOW + 20_000L
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
                    armLastBackfillAtMs = NOW
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
                    armLastBackfillAtMs = NOW + 600_000L
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
            HistoryBackfillPolicy.onConnect(inputs(offlineRecoveryAtMs = NOW - 500L)) to
                    "backfill.skipped reason=history_already_loaded offlineSec=0",
            HistoryBackfillPolicy.onResume(inputs(lastPausedAtMs = NOW - 500L)) to
                    "backfill.skipped reason=away_below_threshold awaySec=0",
            HistoryBackfillPolicy.onResume(inputs(historyLoaded = true)) to
                    "backfill.skipped reason=no_recovery_reference historyLoaded=true",
            HistoryBackfillPolicy.onConnect(inputs()) to
                    "backfill.skipped reason=no_recovery_reference_reconnect"
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
            render(HistoryBackfillPolicy.afterFailure(SESSION_MISSING, 3600))
        )
        assertEquals(
            "backfill.failed reason=reauthorization_required requestedSec=70",
            render(HistoryBackfillPolicy.afterFailure(REAUTHORIZATION_REQUIRED, 70))
        )
        assertEquals(
            "backfill.failed reason=request_failed requestedSec=120",
            render(HistoryBackfillPolicy.afterFailure(REQUEST_FAILED, 120))
        )
    }

    // ---------------------------------------------------------------------------
    // The rest of today's decision
    // ---------------------------------------------------------------------------

    @Test
    fun failure_onlyReauthorizationKeepsHistoryLoaded() {
        assertEquals(true, HistoryBackfillPolicy.afterFailure(SESSION_MISSING, 30).clearHistoryLoaded)
        assertEquals(false, HistoryBackfillPolicy.afterFailure(REAUTHORIZATION_REQUIRED, 30).clearHistoryLoaded)
        assertEquals(true, HistoryBackfillPolicy.afterFailure(REQUEST_FAILED, 30).clearHistoryLoaded)
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

        val resume = HistoryBackfillPolicy.onResume(start.after(connect).copy(nowMs = NOW + 40L))
        assertEquals(
            Skip(
                reason = RECENT_BACKFILL,
                details = listOf("awaySec" to 120, "sinceLastBackfillMs" to 40L),
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
                    armLastBackfillAtMs = NOW
                )
            ),
            HistoryBackfillPolicy.onConnect(inputs(offlineRecoveryAtMs = NOW - 1_000L))
        )
    }

    @Test
    fun connect_withoutAnyReferenceStillConsumesTheOfflineReference() {
        assertEquals(
            Skip(
                reason = NO_RECOVERY_REFERENCE_RECONNECT,
                details = emptyList(),
                effects = HistoryBackfillEffects(consumeOfflineRecovery = true)
            ),
            HistoryBackfillPolicy.onConnect(inputs())
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
                    armLastBackfillAtMs = NOW
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
                effects = HistoryBackfillEffects(armLastBackfillAtMs = NOW)
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
        lastPausedAtMs: Long = 0L,
        lastStoppedAtMs: Long = 0L,
        offlineRecoveryAtMs: Long = 0L,
        lastRenderedMessageTsSec: Double = 0.0,
        ircClientPresent: Boolean = true
    ) = HistoryBackfillInputs(
        nowMs = nowMs,
        historyLoaded = historyLoaded,
        lastBackfillAtMs = lastBackfillAtMs,
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
            lastBackfillAtMs = effects.armLastBackfillAtMs ?: lastBackfillAtMs,
            lastPausedAtMs = if (effects.consumeLastPausedAt) 0L else lastPausedAtMs,
            offlineRecoveryAtMs = if (effects.consumeOfflineRecovery) 0L else offlineRecoveryAtMs
        )
    }

    /** Applies a failure's effects to the stored references, as ChatFragment does. */
    private fun HistoryBackfillInputs.after(failure: HistoryBackfillFailureEffects): HistoryBackfillInputs {
        return copy(historyLoaded = historyLoaded && !failure.clearHistoryLoaded)
    }

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
