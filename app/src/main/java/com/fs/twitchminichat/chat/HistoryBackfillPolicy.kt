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
    BACKFILL_IN_FLIGHT("backfill_in_flight"),
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
 *
 * [lastBackfillAtMs] is when a request's result last arrived **successfully**, not
 * when a request left. [inFlightSinceMs] is when the request still awaiting its
 * result was sent, 0 when none is. [unrecoveredSinceMs] is the oldest moment a
 * failed request did not recover, 0 when nothing is owed.
 */
data class HistoryBackfillInputs(
    val nowMs: Long,
    val historyLoaded: Boolean,
    val lastBackfillAtMs: Long,
    val inFlightSinceMs: Long,
    val unrecoveredSinceMs: Long,
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
 * written, and [armInFlightSinceMs] just before the request leaves. Only a request
 * arms the in-flight marker, and every request does, with the decision's instant.
 */
data class HistoryBackfillEffects(
    val consumeLastPausedAt: Boolean = false,
    val consumeOfflineRecovery: Boolean = false,
    val armHistoryLoaded: Boolean = false,
    val armInFlightSinceMs: Long? = null
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

/**
 * What the caller stores after one sent request failed.
 *
 * [inFlightSinceMs] is the in-flight marker's new value: cleared when it still
 * belongs to the failed request, left alone when a later request has replaced it.
 * [unrecoveredSinceMs] is the start of the window still owed, after this failure.
 */
data class HistoryBackfillFailureEffects(
    val inFlightSinceMs: Long,
    val unrecoveredSinceMs: Long,
    val clearHistoryLoaded: Boolean,
    val journalFields: List<Pair<String, Any?>>
) {
    /** Journal event name. */
    val journalEvent: String
        get() = HistoryBackfillPolicy.FAILED_EVENT
}

/**
 * What the caller stores after one sent request's result arrived successfully.
 *
 * [inFlightSinceMs] follows the same rule as on failure. [lastBackfillAtMs] is the
 * instant the result arrived, which is what the five-second rule counts from.
 * [unrecoveredSinceMs] is 0: a window that arrived owes nothing.
 */
data class HistoryBackfillSuccessEffects(
    val inFlightSinceMs: Long,
    val lastBackfillAtMs: Long,
    val unrecoveredSinceMs: Long
)

/**
 * Decides whether a chat page asks the backend for history, for which window,
 * and what it records in the diagnostics journal.
 *
 * Pure: no Android types and no clock of its own. The caller passes the current
 * time and every stored reference in [HistoryBackfillInputs] and applies the
 * returned [HistoryBackfillDecision].
 *
 * It was moved out of the chat page without change, and is corrected here one
 * behaviour at a time. What is still known to be wrong is pinned by the `frozen_`
 * tests in HistoryBackfillPolicyTest, so correcting it has to change a test on
 * purpose.
 */
object HistoryBackfillPolicy {

    /** Longest window the backend serves, and the window a first connection asks for. */
    const val HISTORY_SECONDS = 3600

    /**
     * Suppresses a recovery request within this long after a successful one.
     *
     * Counted from a result that arrived, not from a request that left: a request
     * failing in four milliseconds used to block every retry for five seconds.
     */
    const val RECENT_BACKFILL_WINDOW_MS = 5_000L

    /**
     * Age past which an in-flight marker no longer suppresses anything.
     *
     * The history transport has no overall deadline. It sets a 5 s connect timeout
     * and a 5 s timeout per read, so a request that cannot reach the backend ends
     * in about five seconds, and one that reaches it has its first byte within ten.
     * Name resolution happens before the connect timeout applies, and a body that
     * keeps arriving within five seconds per read is not bounded at all. Thirty
     * seconds is three times the ten that bound covers, for those two. A marker
     * older than that is treated as lost: the worst a live request past it can
     * cause is one duplicate request, which is what the five-second window used
     * to allow after five seconds, while a marker that was never cleared would
     * otherwise suppress every recovery for good.
     */
    const val IN_FLIGHT_CEILING_MS = 30_000L

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
             * the failure branches of loadHistoryFromBot do, as afterFailure decides.
             */
            return HistoryBackfillDecision.Request(
                source = HistoryBackfillSource.FIRST_CONNECT,
                requestedSec = HISTORY_SECONDS,
                details = emptyList(),
                effects = HistoryBackfillEffects(
                    armHistoryLoaded = true,
                    armInFlightSinceMs = inputs.nowMs
                )
            )
        }

        /*
         * Reconnecting an already-initialized fragment. onResume used to be
         * the only place that recovered this window, which covers every page
         * the user actually looks at — but a page that never becomes the
         * current one never reaches RESUMED, so nothing recovered it at all.
         *
         * Seen on 2026-09-08: the streaming account's tab lost 09:16:46 to
         * 09:32:17, one whole spawn cycle, because the app was open for nine
         * seconds and that page was never visible. Its two siblings, which
         * did resume, recovered the same window correctly.
         *
         * The reference is consumed here so a later IRC reconnect within the
         * same session does not ask again for a window it already holds.
         */
        /* Consumed before the checks below, so a skip loses it too. */
        val consumed = HistoryBackfillEffects(consumeOfflineRecovery = true)
        val offlineSec = inputs.offlineRecoveryAtMs
            .takeIf { it > 0L }
            ?.let { ((inputs.nowMs - it) / 1000).toInt() }

        if (offlineSec != null && offlineSec < 1) {
            return HistoryBackfillDecision.Skip(
                reason = HistoryBackfillSkipReason.HISTORY_ALREADY_LOADED,
                details = listOf("offlineSec" to offlineSec),
                effects = consumed
            )
        }

        suppression(
            inputs = inputs,
            leadingDetails = listOf("offlineSec" to offlineSec),
            effects = consumed
        )?.let { skip -> return skip }

        return when {
            offlineSec != null -> request(
                source = HistoryBackfillSource.RECONNECT_OFFLINE,
                window = window(inputs, historyWindowSeconds(offlineSec)),
                details = listOf("offlineSec" to offlineSec),
                effects = consumed.copy(armInFlightSinceMs = inputs.nowMs)
            )

            else -> {
                /*
                 * No onStop preceded this reconnect, so offlineRecoveryAtMs was
                 * never armed. That is not "nothing to recover": it is a mid-
                 * session IRC drop, the case an idle soTimeout is about to make
                 * real. Fall back to the render watermark, the same reference
                 * withoutPauseReference uses for its own no-onStop case.
                 */
                val renderedGapSec = secondsSinceLastRenderedMessage(
                    nowMs = inputs.nowMs,
                    lastRenderedMessageTsSec = inputs.lastRenderedMessageTsSec
                )

                /* A window a failed request still owes is a reference of its own. */
                val requestWindow = windowOrOwed(inputs, renderedGapSec?.let(::historyWindowSeconds))

                if (requestWindow == null) {
                    HistoryBackfillDecision.Skip(
                        reason = HistoryBackfillSkipReason.NO_RECOVERY_REFERENCE_RECONNECT,
                        details = emptyList(),
                        effects = consumed
                    )
                } else {
                    request(
                        source = HistoryBackfillSource.RECONNECT_NO_PAUSE_REFERENCE,
                        window = requestWindow,
                        details = listOf("renderedGapSec" to renderedGapSec),
                        effects = consumed.copy(armInFlightSinceMs = inputs.nowMs)
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
            suppression(
                inputs = inputs,
                leadingDetails = listOf("awaySec" to awaySec),
                effects = consumed
            )?.let { skip -> return skip }

            return request(
                source = HistoryBackfillSource.RESUME,
                window = window(inputs, historyWindowSeconds(awaySec)),
                details = listOf("awaySec" to awaySec),
                effects = consumed.copy(armInFlightSinceMs = inputs.nowMs)
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
        return request(
            source = HistoryBackfillSource.MANUAL_REFRESH,
            window = window(inputs, MANUAL_REFRESH_SECONDS),
            details = emptyList(),
            effects = HistoryBackfillEffects(armInFlightSinceMs = inputs.nowMs)
        )
    }

    /**
     * Decides what a failed request changes, applied from the request's own thread.
     *
     * [sentAtMs] is the instant the failed request armed the in-flight marker with;
     * [inFlightSinceMs] and [unrecoveredSinceMs] are the stored values as they stand
     * now.
     *
     * Every failure records the start of the window it asked for, the reauthorization
     * one included: that changes nothing while the user is blocked, and covers the
     * window once they unblock it. The start is the send instant minus the window,
     * not the moment the failure arrived, which would start it later by the time the
     * request took to fail. If a start is already recorded the older one is kept:
     * the oldest moment not recovered is what the next request has to reach.
     */
    fun afterFailure(
        failure: HistoryBackfillFailure,
        requestedSec: Int,
        sentAtMs: Long,
        inFlightSinceMs: Long,
        unrecoveredSinceMs: Long
    ): HistoryBackfillFailureEffects {
        val clearHistoryLoaded = when (failure) {
            /*
             * A session can be established later, so the hour is still
             * worth asking for. Without clearing the mark this fragment
             * would take the history_already_loaded branch forever and
             * the window would be lost with nothing visible to the user.
             */
            HistoryBackfillFailure.SESSION_MISSING -> true

            /*
             * Deliberately left marked. Only the user can unblock this,
             * so retrying on every connect would ask the backend for an
             * hour it will keep refusing.
             */
            HistoryBackfillFailure.REAUTHORIZATION_REQUIRED -> false

            /*
             * Observed failing four milliseconds after the request left,
             * which is a device with no network rather than a backend
             * that said no. The next connect should ask again.
             */
            HistoryBackfillFailure.REQUEST_FAILED -> true
        }

        val windowStartMs = sentAtMs - requestedSec * 1000L

        return HistoryBackfillFailureEffects(
            inFlightSinceMs = inFlightAfterResult(sentAtMs, inFlightSinceMs),
            unrecoveredSinceMs = if (unrecoveredSinceMs == 0L) {
                windowStartMs
            } else {
                minOf(unrecoveredSinceMs, windowStartMs)
            },
            clearHistoryLoaded = clearHistoryLoaded,
            journalFields = listOf(
                "reason" to failure.journalValue,
                "requestedSec" to requestedSec
            )
        )
    }

    /**
     * Decides what a successful result changes, applied from the request's own thread.
     *
     * [nowMs] is the instant the result arrived.
     */
    fun afterSuccess(
        sentAtMs: Long,
        nowMs: Long,
        inFlightSinceMs: Long
    ): HistoryBackfillSuccessEffects {
        return HistoryBackfillSuccessEffects(
            inFlightSinceMs = inFlightAfterResult(sentAtMs, inFlightSinceMs),
            lastBackfillAtMs = nowMs,
            unrecoveredSinceMs = 0L
        )
    }

    /**
     * Returns the age of the request still awaiting its result, while it counts.
     *
     * Null when no request is in flight, or when the marker is older than
     * [IN_FLIGHT_CEILING_MS] and is treated as lost.
     */
    fun msInFlight(nowMs: Long, inFlightSinceMs: Long): Long? {
        if (inFlightSinceMs == 0L) return null

        return (nowMs - inFlightSinceMs)
            .takeIf { age -> age < IN_FLIGHT_CEILING_MS }
    }

    /**
     * Returns the age of the last successful result when it arrived moments ago.
     *
     * Two paths can ask for the same window within milliseconds — the reconnect
     * and the resume that may follow it — so both consult this rather than each
     * carrying its own copy of the rule. That is also why a request in flight
     * suppresses them: the second path usually asks before any result exists.
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

    /**
     * Returns the seconds elapsed since the oldest moment a failed request did not
     * recover.
     *
     * Null means nothing is owed.
     */
    fun secondsSinceUnrecovered(nowMs: Long, unrecoveredSinceMs: Long): Int? {
        if (unrecoveredSinceMs == 0L) return null

        return ((nowMs - unrecoveredSinceMs) / 1000).toInt()
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
        /* A first connection or an earlier resume is covering, or has just covered, this window. */
        suppression(
            inputs = inputs,
            leadingDetails = emptyList(),
            effects = HistoryBackfillEffects()
        )?.let { skip -> return skip }

        val renderedGapSec = secondsSinceLastRenderedMessage(
            nowMs = inputs.nowMs,
            lastRenderedMessageTsSec = inputs.lastRenderedMessageTsSec
        )
        val offlineSec = offlineSecondsSinceStop(
            nowMs = inputs.nowMs,
            lastStoppedAtMs = inputs.lastStoppedAtMs
        )
        val elapsedSec = listOfNotNull(renderedGapSec, offlineSec).maxOrNull()

        /* A window a failed request still owes is a reference of its own. */
        val requestWindow = windowOrOwed(inputs, elapsedSec?.let(::historyWindowSeconds))
            ?: return HistoryBackfillDecision.Skip(
                reason = HistoryBackfillSkipReason.NO_RECOVERY_REFERENCE,
                details = listOf("historyLoaded" to inputs.historyLoaded),
                effects = HistoryBackfillEffects()
            )

        return request(
            source = HistoryBackfillSource.RESUME_NO_PAUSE,
            window = requestWindow,
            details = listOf(
                "renderedGapSec" to renderedGapSec,
                "offlineSec" to offlineSec
            ),
            effects = HistoryBackfillEffects(armInFlightSinceMs = inputs.nowMs)
        )
    }

    /**
     * One request's window, and how many seconds back the owed window reached when
     * that is what made it wider.
     */
    private data class RequestWindow(
        val requestedSec: Int,
        val unrecoveredSec: Int?
    )

    /**
     * Sizes a request: its own window, or the window back to the oldest moment a
     * failed request did not recover, whichever is larger.
     *
     * Both pass through the same clamp, so a request is only reported as widened
     * when the owed window actually asks for more.
     */
    private fun window(inputs: HistoryBackfillInputs, ownWindowSec: Int): RequestWindow {
        val owed = owedWindow(inputs)

        return if (owed != null && owed.requestedSec > ownWindowSec) {
            owed
        } else {
            RequestWindow(ownWindowSec, null)
        }
    }

    /**
     * Sizes a request that may have no window of its own, which the owed window then
     * stands in for. Null when neither exists.
     */
    private fun windowOrOwed(inputs: HistoryBackfillInputs, ownWindowSec: Int?): RequestWindow? {
        return if (ownWindowSec != null) window(inputs, ownWindowSec) else owedWindow(inputs)
    }

    /** The window back to the oldest moment not recovered, null when nothing is owed. */
    private fun owedWindow(inputs: HistoryBackfillInputs): RequestWindow? {
        val unrecoveredSec = secondsSinceUnrecovered(
            nowMs = inputs.nowMs,
            unrecoveredSinceMs = inputs.unrecoveredSinceMs
        ) ?: return null

        return RequestWindow(historyWindowSeconds(unrecoveredSec), unrecoveredSec)
    }

    /**
     * Builds a request, adding `unrecoveredSec` to its journal line when the owed
     * window is what widened it.
     */
    private fun request(
        source: HistoryBackfillSource,
        window: RequestWindow,
        details: List<Pair<String, Any?>>,
        effects: HistoryBackfillEffects
    ): HistoryBackfillDecision.Request {
        return HistoryBackfillDecision.Request(
            source = source,
            requestedSec = window.requestedSec,
            details = details + listOfNotNull(
                window.unrecoveredSec?.let { seconds -> "unrecoveredSec" to seconds }
            ),
            effects = effects
        )
    }

    /**
     * Returns the skip for a window another request is covering or has just covered.
     *
     * A request in flight is checked first: it is the newer fact, and it is the
     * case the rule exists for, a reconnect and a resume asking milliseconds apart.
     */
    private fun suppression(
        inputs: HistoryBackfillInputs,
        leadingDetails: List<Pair<String, Any?>>,
        effects: HistoryBackfillEffects
    ): HistoryBackfillDecision.Skip? {
        msInFlight(
            nowMs = inputs.nowMs,
            inFlightSinceMs = inputs.inFlightSinceMs
        )?.let { inFlightMs ->
            return HistoryBackfillDecision.Skip(
                reason = HistoryBackfillSkipReason.BACKFILL_IN_FLIGHT,
                details = leadingDetails + ("inFlightMs" to inFlightMs),
                effects = effects
            )
        }

        msSinceRecentBackfill(
            nowMs = inputs.nowMs,
            lastBackfillAtMs = inputs.lastBackfillAtMs
        )?.let { sinceLastBackfillMs ->
            return HistoryBackfillDecision.Skip(
                reason = HistoryBackfillSkipReason.RECENT_BACKFILL,
                details = leadingDetails + ("sinceLastBackfillMs" to sinceLastBackfillMs),
                effects = effects
            )
        }

        return null
    }

    /** Clears the in-flight marker only when it still belongs to the request whose result arrived. */
    private fun inFlightAfterResult(sentAtMs: Long, inFlightSinceMs: Long): Long {
        return if (inFlightSinceMs == sentAtMs) 0L else inFlightSinceMs
    }
}
