package com.fs.twitchminichat.chat

import com.fs.twitchminichat.BackendHistoryResult

/** One history request the caller is to send, as its decision armed it. */
data class HistoryBackfillSend(
    val requestedSec: Int,
    val sentAtMs: Long
)

/**
 * Holds one chat page's history backfill state and applies [HistoryBackfillPolicy]'s
 * answers to it.
 *
 * The chat page keeps what is Android: sending the request, writing the journal,
 * touching views. Everything it stores about backfill lives here, so the whole cycle -
 * decide, apply, send, result - can be driven from a unit test.
 *
 * Threading is what it was while these fields lived in the chat page:
 * - [lastPausedAtMs], [lastStoppedAtMs], [offlineRecoveryAtMs] and
 *   [lastRenderedMessageTsSec] are read and written on the main thread only;
 * - [historyLoaded], [lastBackfillAtMs], [inFlightSinceMs] and [unrecoveredSinceMs]
 *   are also written from the history request thread, so they are volatile;
 * - every update that reads one of the last three before writing it holds [lock], so a
 *   result cannot clear a newer request's marker or overwrite an older owed start.
 *
 * [inputs] takes no lock: it sees each field's latest value, not necessarily one
 * consistent group of them.
 */
class HistoryBackfillState {

    /*
     * Written from the history request thread as well as the UI thread, because a
     * request that fails has to undo the mark the caller set before sending it.
     */
    @Volatile
    var historyLoaded: Boolean = false
        private set

    /* The last onPause, consumed by the resume that follows it. Main thread only. */
    private var lastPausedAtMs: Long = 0L

    /** The last onStop, 0 before the first. Never consumed. Main thread only. */
    var lastStoppedAtMs: Long = 0L
        private set

    /*
     * Instant of the last onStop, consumed by the first reconnect that follows
     * it. Distinct from lastStoppedAtMs, which is never consumed and therefore
     * keeps growing while the app is open: reading that one on every connect
     * would make an ordinary IRC reconnect ask for an hour it already has.
     */
    private var offlineRecoveryAtMs: Long = 0L

    /* Epoch seconds of the newest message accepted for display, 0 when none. */
    private var lastRenderedMessageTsSec: Double = 0.0

    /*
     * When a history result last arrived successfully, 0 when none has. Not when a
     * request left: a request that failed must not count as a recent backfill.
     * Written from the history request thread.
     */
    @Volatile
    private var lastBackfillAtMs: Long = 0L

    /*
     * When the request still awaiting its result was sent, 0 when none is. Armed on
     * the main thread and cleared from the history request thread, under lock, so a
     * result cannot clear a newer request's marker.
     */
    @Volatile
    private var inFlightSinceMs: Long = 0L

    /*
     * The oldest moment a failed history request did not recover, 0 when nothing is
     * owed. Every later request reaches back to it until one that did succeeds.
     * Written from the history request thread, under lock.
     */
    @Volatile
    private var unrecoveredSinceMs: Long = 0L

    /* Guards the backfill state written from both the main and request threads. */
    private val lock = Any()

    /** Captures, at [nowMs], everything [HistoryBackfillPolicy] reads. */
    fun inputs(nowMs: Long, ircClientPresent: Boolean): HistoryBackfillInputs {
        return HistoryBackfillInputs(
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
    }

    /** Records an onPause. Main thread. */
    fun onPaused(nowMs: Long) {
        lastPausedAtMs = nowMs
    }

    /** Records an onStop, and arms the reference the next reconnect consumes. Main thread. */
    fun onStopped(nowMs: Long) {
        lastStoppedAtMs = nowMs
        offlineRecoveryAtMs = nowMs
    }

    /** Advances the render watermark to a newer displayed message. Main thread. */
    fun onMessageRendered(messageTimestampSec: Double) {
        if (messageTimestampSec > lastRenderedMessageTsSec) {
            lastRenderedMessageTsSec = messageTimestampSec
        }
    }

    /** Makes the next connect a first connection again. Main thread. */
    fun forgetHistoryLoaded() {
        historyLoaded = false
    }

    /** Forgets everything measured on the previous channel. Main thread. */
    fun resetForNewChannel() {
        historyLoaded = false
        lastRenderedMessageTsSec = 0.0
        synchronized(lock) {
            lastBackfillAtMs = 0L
            inFlightSinceMs = 0L
            unrecoveredSinceMs = 0L
        }
    }

    /**
     * Applies one decision: consumes and arms what it says, and for a request arms the
     * in-flight marker and returns what to send. Main thread.
     */
    fun apply(decision: HistoryBackfillDecision): HistoryBackfillSend? {
        val effects = decision.effects

        if (effects.consumeLastPausedAt) {
            lastPausedAtMs = 0L
        }
        if (effects.consumeOfflineRecovery) {
            offlineRecoveryAtMs = 0L
        }
        if (effects.armHistoryLoaded) {
            historyLoaded = true
        }

        if (decision !is HistoryBackfillDecision.Request) return null

        val sentAtMs = checkNotNull(effects.armInFlightSinceMs) {
            "Every history request arms the in-flight marker"
        }
        synchronized(lock) {
            inFlightSinceMs = sentAtMs
        }
        return HistoryBackfillSend(requestedSec = decision.requestedSec, sentAtMs = sentAtMs)
    }

    /**
     * Applies one request's result, arrived at [nowMs]. History request thread.
     *
     * Returns the failure's effects, whose journal line the caller writes, or null
     * for a success.
     */
    fun onResult(
        result: BackendHistoryResult,
        send: HistoryBackfillSend,
        nowMs: Long
    ): HistoryBackfillFailureEffects? {
        return when (result) {
            is BackendHistoryResult.Success -> {
                onSuccess(send, nowMs)
                null
            }

            BackendHistoryResult.SessionRequired ->
                onFailure(HistoryBackfillFailure.SESSION_MISSING, send)

            BackendHistoryResult.ReauthorizationRequired ->
                onFailure(HistoryBackfillFailure.REAUTHORIZATION_REQUIRED, send)

            BackendHistoryResult.Failed ->
                onFailure(HistoryBackfillFailure.REQUEST_FAILED, send)
        }
    }

    private fun onSuccess(send: HistoryBackfillSend, nowMs: Long) {
        synchronized(lock) {
            val effects = HistoryBackfillPolicy.afterSuccess(
                sentAtMs = send.sentAtMs,
                requestedSec = send.requestedSec,
                nowMs = nowMs,
                inFlightSinceMs = inFlightSinceMs,
                unrecoveredSinceMs = unrecoveredSinceMs
            )
            inFlightSinceMs = effects.inFlightSinceMs
            lastBackfillAtMs = effects.lastBackfillAtMs
            unrecoveredSinceMs = effects.unrecoveredSinceMs
        }
    }

    private fun onFailure(
        failure: HistoryBackfillFailure,
        send: HistoryBackfillSend
    ): HistoryBackfillFailureEffects {
        val effects = synchronized(lock) {
            HistoryBackfillPolicy.afterFailure(
                failure = failure,
                requestedSec = send.requestedSec,
                sentAtMs = send.sentAtMs,
                inFlightSinceMs = inFlightSinceMs,
                unrecoveredSinceMs = unrecoveredSinceMs
            ).also { decided ->
                inFlightSinceMs = decided.inFlightSinceMs
                unrecoveredSinceMs = decided.unrecoveredSinceMs
            }
        }

        if (effects.clearHistoryLoaded) {
            historyLoaded = false
        }

        return effects
    }
}
