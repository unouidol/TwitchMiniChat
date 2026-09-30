package com.fs.twitchminichat.chat

/** Where one history request came from, spelled exactly as the diagnostics journal records it. */
enum class HistoryBackfillSource(val journalValue: String) {
    FIRST_CONNECT("first_connect"),
    RECONNECT_OFFLINE("reconnect_offline"),
    RECONNECT_NO_PAUSE_REFERENCE("reconnect_no_pause_reference"),
    RESUME("resume"),
    RESUME_NO_PAUSE("resume_no_pause"),
    MANUAL_REFRESH("manual_refresh")
}

/** Why no history request was sent, spelled exactly as the diagnostics journal records it. */
enum class HistoryBackfillSkipReason(val journalValue: String) {
    RECENT_BACKFILL("recent_backfill"),
    HISTORY_ALREADY_LOADED("history_already_loaded"),
    AWAY_BELOW_THRESHOLD("away_below_threshold"),
    NO_RECOVERY_REFERENCE("no_recovery_reference"),
    NO_RECOVERY_REFERENCE_RECONNECT("no_recovery_reference_reconnect")
}

/** How a sent history request failed, spelled exactly as the diagnostics journal records it. */
enum class HistoryBackfillFailure(val journalValue: String) {
    SESSION_MISSING("session_missing"),
    REAUTHORIZATION_REQUIRED("reauthorization_required"),
    REQUEST_FAILED("request_failed")
}

/**
 * Everything the backfill decision reads, captured by the caller at one instant.
 *
 * Instants are epoch milliseconds and use 0 for "never", as the chat page's own
 * fields do. [lastRenderedMessageTsSec] is epoch seconds, 0.0 when nothing with a
 * timestamp has been displayed.
 */
data class HistoryBackfillInputs(
    val nowMs: Long,
    val historyLoaded: Boolean,
    val lastBackfillAtMs: Long,
    val lastPausedAtMs: Long,
    val lastStoppedAtMs: Long,
    val offlineRecoveryAtMs: Long,
    val lastRenderedMessageTsSec: Double,
    val ircClientPresent: Boolean
)

/**
 * Stored references the caller must change when it applies a decision.
 *
 * The consumptions and [armHistoryLoaded] are applied before the journal line is
 * written, and [armLastBackfillAtMs] just before the request leaves: the order
 * these side effects had while the decision lived in the chat page.
 */
data class HistoryBackfillEffects(
    val consumeLastPausedAt: Boolean = false,
    val consumeOfflineRecovery: Boolean = false,
    val armHistoryLoaded: Boolean = false,
    val armLastBackfillAtMs: Long? = null
)

/** One backfill decision: a request to send or a skip, with its journal line and side effects. */
sealed interface HistoryBackfillDecision {

    /** References the caller consumes or arms. */
    val effects: HistoryBackfillEffects

    /** Journal event name. */
    val journalEvent: String

    /** Journal fields in the order they are written; null values are omitted by the journal. */
    val journalFields: List<Pair<String, Any?>>

    /** Send one history request for [requestedSec], already clamped. */
    data class Request(
        val source: HistoryBackfillSource,
        val requestedSec: Int,
        val details: List<Pair<String, Any?>>,
        override val effects: HistoryBackfillEffects
    ) : HistoryBackfillDecision {
        override val journalEvent: String
            get() = HistoryBackfillPolicy.TRIGGERED_EVENT

        override val journalFields: List<Pair<String, Any?>>
            get() = listOf("source" to source.journalValue) +
                    details +
                    ("requestedSec" to requestedSec)
    }

    /** Send nothing. */
    data class Skip(
        val reason: HistoryBackfillSkipReason,
        val details: List<Pair<String, Any?>>,
        override val effects: HistoryBackfillEffects
    ) : HistoryBackfillDecision {
        override val journalEvent: String
            get() = HistoryBackfillPolicy.SKIPPED_EVENT

        override val journalFields: List<Pair<String, Any?>>
            get() = listOf("reason" to reason.journalValue) + details
    }
}

/** What the caller does after one sent request failed. */
data class HistoryBackfillFailureEffects(
    val clearHistoryLoaded: Boolean,
    val journalFields: List<Pair<String, Any?>>
) {
    /** Journal event name. */
    val journalEvent: String
        get() = HistoryBackfillPolicy.FAILED_EVENT
}

/**
 * Decides whether a chat page asks the backend for history, for which window,
 * and what it records in the diagnostics journal.
 *
 * Pure: no Android types and no clock of its own. The caller passes the current
 * time and every stored reference in [HistoryBackfillInputs] and applies the
 * returned [HistoryBackfillDecision].
 *
 * This is the decision as it stood inside the chat page, moved without change.
 * Several of its behaviours are known to be wrong and are pinned deliberately by
 * HistoryBackfillPolicyTest, so that correcting one has to change a test on purpose.
 */
object HistoryBackfillPolicy {

    /** Longest window the backend serves, and the window a first connection asks for. */
    const val HISTORY_SECONDS = 3600

    /** Suppresses a recovery request that would duplicate one just issued. */
    const val RECENT_BACKFILL_WINDOW_MS = 5_000L

    /** Fixed window of the refresh button. */
    const val MANUAL_REFRESH_SECONDS = 120

    /** Journal event of a sent request. */
    const val TRIGGERED_EVENT = "backfill.triggered"

    /** Journal event of a request not sent. */
    const val SKIPPED_EVENT = "backfill.skipped"

    /** Journal event of a sent request that did not deliver. */
    const val FAILED_EVENT = "backfill.failed"

    /** Decides the history request that accompanies an IRC connection attempt. */
    fun onConnect(inputs: HistoryBackfillInputs): HistoryBackfillDecision {
        if (!inputs.historyLoaded) {
            /*
             * Marked before the answer arrives so a second connect cannot start a
             * duplicate hour-long request. A failure has to clear it again, which
             * afterFailure does.
             */
            return HistoryBackfillDecision.Request(
                source = HistoryBackfillSource.FIRST_CONNECT,
                requestedSec = HISTORY_SECONDS,
                details = emptyList(),
                effects = HistoryBackfillEffects(
                    armHistoryLoaded = true,
                    armLastBackfillAtMs = inputs.nowMs
                )
            )
        }

        /*
         * Reconnecting an already-initialized page. onResume used to be the only
         * place that recovered this window, which covers every page the user
         * actually looks at — but a page that never becomes the current one never
         * reaches RESUMED, so nothing recovered it at all.
         *
         * Seen on 2026-09-08: the streaming account's tab lost 09:16:46 to
         * 09:32:17, one whole spawn cycle, because the app was open for nine
         * seconds and that page was never visible. Its two siblings, which did
         * resume, recovered the same window correctly.
         *
         * The reference is consumed here so a later IRC reconnect within the same
         * session does not ask again for a window it already holds. It is consumed
         * before the checks below, so a skip loses it too.
         */
        val consumed = HistoryBackfillEffects(consumeOfflineRecovery = true)
        val offlineSec = inputs.offlineRecoveryAtMs
            .takeIf { it > 0L }
            ?.let { ((inputs.nowMs - it) / 1000).toInt() }
        val sinceLastBackfillMs = msSinceRecentBackfill(
            nowMs = inputs.nowMs,
            lastBackfillAtMs = inputs.lastBackfillAtMs
        )

        return when {
            offlineSec != null && offlineSec < 1 -> HistoryBackfillDecision.Skip(
                reason = HistoryBackfillSkipReason.HISTORY_ALREADY_LOADED,
                details = listOf("offlineSec" to offlineSec),
                effects = consumed
            )

            sinceLastBackfillMs != null -> HistoryBackfillDecision.Skip(
                reason = HistoryBackfillSkipReason.RECENT_BACKFILL,
                details = listOf(
                    "offlineSec" to offlineSec,
                    "sinceLastBackfillMs" to sinceLastBackfillMs
                ),
                effects = consumed
            )

            offlineSec != null -> HistoryBackfillDecision.Request(
                source = HistoryBackfillSource.RECONNECT_OFFLINE,
                requestedSec = historyWindowSeconds(offlineSec),
                details = listOf("offlineSec" to offlineSec),
                effects = consumed.copy(armLastBackfillAtMs = inputs.nowMs)
            )

            else -> {
                /*
                 * No onStop preceded this reconnect, so the offline reference was
                 * never armed. That is not "nothing to recover": it is a mid-session
                 * IRC drop. Fall back to the render watermark, the same reference the
                 * no-pause resume uses for its own no-onStop case.
                 */
                val renderedGapSec = secondsSinceLastRenderedMessage(
                    nowMs = inputs.nowMs,
                    lastRenderedMessageTsSec = inputs.lastRenderedMessageTsSec
                )

                if (renderedGapSec == null) {
                    HistoryBackfillDecision.Skip(
                        reason = HistoryBackfillSkipReason.NO_RECOVERY_REFERENCE_RECONNECT,
                        details = emptyList(),
                        effects = consumed
                    )
                } else {
                    HistoryBackfillDecision.Request(
                        source = HistoryBackfillSource.RECONNECT_NO_PAUSE_REFERENCE,
                        requestedSec = historyWindowSeconds(renderedGapSec),
                        details = listOf("renderedGapSec" to renderedGapSec),
                        effects = consumed.copy(armLastBackfillAtMs = inputs.nowMs)
                    )
                }
            }
        }
    }

    /** Decides the history request a page makes when it becomes the visible one. */
    fun onResume(inputs: HistoryBackfillInputs): HistoryBackfillDecision {
        val pausedAt = inputs.lastPausedAtMs
        if (pausedAt == 0L) {
            return withoutPauseReference(inputs)
        }

        /* Consumed before the checks below, so a skip loses it too. */
        val consumed = HistoryBackfillEffects(consumeLastPausedAt = true)
        val awaySec = ((inputs.nowMs - pausedAt) / 1000).toInt()

        if (awaySec >= 1 || !inputs.ircClientPresent) {
            /*
             * The reconnect that precedes this resume may have just asked for the
             * same window. Without this the visible page would fetch it twice.
             */
            val sinceLastBackfillMs = msSinceRecentBackfill(
                nowMs = inputs.nowMs,
                lastBackfillAtMs = inputs.lastBackfillAtMs
            )
            if (sinceLastBackfillMs != null) {
                return HistoryBackfillDecision.Skip(
                    reason = HistoryBackfillSkipReason.RECENT_BACKFILL,
                    details = listOf(
                        "awaySec" to awaySec,
                        "sinceLastBackfillMs" to sinceLastBackfillMs
                    ),
                    effects = consumed
                )
            }

            return HistoryBackfillDecision.Request(
                source = HistoryBackfillSource.RESUME,
                requestedSec = historyWindowSeconds(awaySec),
                details = listOf("awaySec" to awaySec),
                effects = consumed.copy(armLastBackfillAtMs = inputs.nowMs)
            )
        }

        return HistoryBackfillDecision.Skip(
            reason = HistoryBackfillSkipReason.AWAY_BELOW_THRESHOLD,
            details = listOf("awaySec" to awaySec),
            effects = consumed
        )
    }

    /** Decides the request behind the refresh button. */
    fun onManualRefresh(inputs: HistoryBackfillInputs): HistoryBackfillDecision {
        return HistoryBackfillDecision.Request(
            source = HistoryBackfillSource.MANUAL_REFRESH,
            requestedSec = MANUAL_REFRESH_SECONDS,
            details = emptyList(),
            effects = HistoryBackfillEffects(armLastBackfillAtMs = inputs.nowMs)
        )
    }

    /** Decides what a failed request changes, applied from the request's own thread. */
    fun afterFailure(
        failure: HistoryBackfillFailure,
        requestedSec: Int
    ): HistoryBackfillFailureEffects {
        val clearHistoryLoaded = when (failure) {
            /*
             * A session can be established later, so the hour is still worth
             * asking for. Without clearing the mark the page would never ask for
             * it again and the window would be lost with nothing visible.
             */
            HistoryBackfillFailure.SESSION_MISSING -> true

            /*
             * Deliberately left marked. Only the user can unblock this, so retrying
             * on every connect would ask the backend for an hour it will keep
             * refusing.
             */
            HistoryBackfillFailure.REAUTHORIZATION_REQUIRED -> false

            /*
             * Observed failing four milliseconds after the request left, which is a
             * device with no network rather than a backend that said no. The next
             * connect should ask again.
             */
            HistoryBackfillFailure.REQUEST_FAILED -> true
        }

        return HistoryBackfillFailureEffects(
            clearHistoryLoaded = clearHistoryLoaded,
            journalFields = listOf(
                "reason" to failure.journalValue,
                "requestedSec" to requestedSec
            )
        )
    }

    /**
     * Returns the age of the last request when one was issued moments ago.
     *
     * Two paths can ask for the same window within milliseconds — the reconnect
     * and the resume that may follow it — so both consult this rather than each
     * carrying its own copy of the rule.
     */
    fun msSinceRecentBackfill(nowMs: Long, lastBackfillAtMs: Long): Long? {
        if (lastBackfillAtMs == 0L) return null

        return (nowMs - lastBackfillAtMs)
            .takeIf { elapsed -> elapsed < RECENT_BACKFILL_WINDOW_MS }
    }

    /**
     * Returns the seconds elapsed since the newest displayed message.
     *
     * Null when no timestamped message has been displayed yet, or when the newest
     * one is not in the past.
     */
    fun secondsSinceLastRenderedMessage(nowMs: Long, lastRenderedMessageTsSec: Double): Int? {
        if (lastRenderedMessageTsSec <= 0.0) return null

        val elapsed = (nowMs / 1000.0) - lastRenderedMessageTsSec
        if (elapsed <= 0.0) return null

        return elapsed.toInt()
    }

    /**
     * Returns the seconds elapsed since the page last reached onStop.
     *
     * Null means the page has not been stopped yet in this process.
     */
    fun offlineSecondsSinceStop(nowMs: Long, lastStoppedAtMs: Long): Int? {
        if (lastStoppedAtMs == 0L) return null

        return ((nowMs - lastStoppedAtMs) / 1000).toInt()
    }

    /** Clamps one elapsed measure into a history request window. */
    fun historyWindowSeconds(elapsedSec: Int): Int {
        return (elapsedSec + 10).coerceIn(30, HISTORY_SECONDS)
    }

    /**
     * Recovers history for a page that reached RESUMED without ever being paused.
     *
     * An off-screen pager page never receives onPause, so the elapsed-time reference
     * used by the normal resume path does not exist. Leaving without a request used to
     * lose every message received while the page was started but not visible. The
     * newest message already displayed is the exact reference here, and the last
     * onStop is the fallback when nothing has been displayed with a timestamp.
     */
    private fun withoutPauseReference(inputs: HistoryBackfillInputs): HistoryBackfillDecision {
        msSinceRecentBackfill(
            nowMs = inputs.nowMs,
            lastBackfillAtMs = inputs.lastBackfillAtMs
        )?.let { sinceLastBackfillMs ->
            /* A first connection or an earlier resume has just covered this window. */
            return HistoryBackfillDecision.Skip(
                reason = HistoryBackfillSkipReason.RECENT_BACKFILL,
                details = listOf("sinceLastBackfillMs" to sinceLastBackfillMs),
                effects = HistoryBackfillEffects()
            )
        }

        val renderedGapSec = secondsSinceLastRenderedMessage(
            nowMs = inputs.nowMs,
            lastRenderedMessageTsSec = inputs.lastRenderedMessageTsSec
        )
        val offlineSec = offlineSecondsSinceStop(
            nowMs = inputs.nowMs,
            lastStoppedAtMs = inputs.lastStoppedAtMs
        )
        val elapsedSec = listOfNotNull(renderedGapSec, offlineSec).maxOrNull()
            ?: return HistoryBackfillDecision.Skip(
                reason = HistoryBackfillSkipReason.NO_RECOVERY_REFERENCE,
                details = listOf("historyLoaded" to inputs.historyLoaded),
                effects = HistoryBackfillEffects()
            )

        return HistoryBackfillDecision.Request(
            source = HistoryBackfillSource.RESUME_NO_PAUSE,
            requestedSec = historyWindowSeconds(elapsedSec),
            details = listOf(
                "renderedGapSec" to renderedGapSec,
                "offlineSec" to offlineSec
            ),
            effects = HistoryBackfillEffects(armLastBackfillAtMs = inputs.nowMs)
        )
    }
}
