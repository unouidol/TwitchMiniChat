package com.fs.twitchminichat.chat

import com.fs.twitchminichat.BackendHistoryResult

/** One history request the caller is to send, as its decision armed it. */
data class HistoryBackfillSend(
    val requestedSec: Int,
    val sentAtMs: Long
)

/** A decision taken by [HistoryBackfillState.decide], and the request it armed, if any. */
data class HistoryBackfillApplied(
    val decision: HistoryBackfillDecision,
    val send: HistoryBackfillSend?
)

/**
 * Holds one chat page's history backfill state and applies [HistoryBackfillPolicy]'s
 * answers to it.
 *
 * The chat page keeps what is Android: sending the request, writing the journal,
 * touching views. Everything it stores about backfill lives here, so the whole cycle -
 * decide, apply, send, result - can be driven from a unit test.
 *
 * **Threading: one monitor, the instance itself, for every read and every write.** The
 * main thread decides and records lifecycle events; the history request thread applies
 * results. Every public member is `@Synchronized`, so no field is read or written
 * outside the monitor, and none needs `@Volatile`: the monitor already makes each write
 * visible to the next thread that takes it.
 *
 * A decision is one locked step, [decide]: capture the inputs, run the pure policy, apply
 * its effects. Two interleavings that were possible while the fields were read one by one
 * are now impossible:
 *
 * - **A result landing in the middle of the capture.** The capture read
 *   `inFlightSinceMs` while the request was still in flight, then the request thread
 *   cleared the marker together with `inFlightCoversFromMs`, then the capture read the
 *   coverage as 0. The decision saw a marker whose request covered nothing before itself
 *   and recorded a debt that did not exist. The reverse order hid one. The marker and
 *   how far back its request reached are a pair, meaningful only read together.
 * - **A result landing between the decision and its application.** The decision was
 *   taken on a snapshot, and its consumptions and its owed start were then applied over
 *   state a result had changed in between.
 *
 * Nothing that blocks runs inside the monitor: the policy is pure, and the request and
 * the journal stay with the caller, outside it.
 */
class HistoryBackfillState {

    /*
     * Cleared by a failed request on the request thread, set by a first connection on the
     * main thread.
     */
    var historyLoaded: Boolean = false
        @Synchronized get
        private set

    /* The last onPause, consumed by the resume that follows it. */
    private var lastPausedAtMs: Long = 0L

    /** The last onStop, 0 before the first. Never consumed. */
    var lastStoppedAtMs: Long = 0L
        @Synchronized get
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
     */
    private var lastBackfillAtMs: Long = 0L

    /* How far back the request behind lastBackfillAtMs reached. */
    private var lastBackfillCoversFromMs: Long = 0L

    /*
     * When the request still awaiting its result was sent, 0 when none is. A result
     * clears it only if it still holds that result's own send instant, so it cannot
     * clear a newer request's marker.
     */
    private var inFlightSinceMs: Long = 0L

    /* How far back the request behind inFlightSinceMs reaches. */
    private var inFlightCoversFromMs: Long = 0L

    /*
     * The oldest moment a failed history request did not recover, 0 when nothing is
     * owed. Every later request reaches back to it until one that did succeeds.
     */
    private var unrecoveredSinceMs: Long = 0L

    /**
     * Takes one decision at [nowMs] in one locked step: captures the inputs, asks
     * [policy], applies what it says, and returns it with the request to send, if any.
     * Main thread.
     *
     * [policy] runs inside the monitor, so it must be pure: one of
     * [HistoryBackfillPolicy]'s decisions.
     */
    @Synchronized
    fun decide(
        nowMs: Long,
        ircClientPresent: Boolean,
        policy: (HistoryBackfillInputs) -> HistoryBackfillDecision
    ): HistoryBackfillApplied {
        val decision = policy(inputs(nowMs, ircClientPresent))
        return HistoryBackfillApplied(decision = decision, send = apply(decision))
    }

    /** Records an onPause. Main thread. */
    @Synchronized
    fun onPaused(nowMs: Long) {
        lastPausedAtMs = nowMs
    }

    /** Records an onStop, and arms the reference the next reconnect consumes. Main thread. */
    @Synchronized
    fun onStopped(nowMs: Long) {
        lastStoppedAtMs = nowMs
        offlineRecoveryAtMs = nowMs
    }

    /** Advances the render watermark to a newer displayed message. Main thread. */
    @Synchronized
    fun onMessageRendered(messageTimestampSec: Double) {
        if (messageTimestampSec > lastRenderedMessageTsSec) {
            lastRenderedMessageTsSec = messageTimestampSec
        }
    }

    /** Makes the next connect a first connection again. Main thread. */
    @Synchronized
    fun forgetHistoryLoaded() {
        historyLoaded = false
    }

    /** Forgets everything measured on the previous channel. Main thread. */
    @Synchronized
    fun resetForNewChannel() {
        historyLoaded = false
        lastRenderedMessageTsSec = 0.0
        lastBackfillAtMs = 0L
        lastBackfillCoversFromMs = 0L
        inFlightSinceMs = 0L
        inFlightCoversFromMs = 0L
        unrecoveredSinceMs = 0L
    }

    /**
     * Applies one request's result, arrived at [nowMs]. History request thread.
     *
     * Returns the failure's effects, whose journal line the caller writes, or null
     * for a success.
     */
    @Synchronized
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

    /** Captures everything [HistoryBackfillPolicy] reads. Called under the monitor. */
    private fun inputs(nowMs: Long, ircClientPresent: Boolean): HistoryBackfillInputs {
        return HistoryBackfillInputs(
            nowMs = nowMs,
            historyLoaded = historyLoaded,
            lastBackfillAtMs = lastBackfillAtMs,
            lastBackfillCoversFromMs = lastBackfillCoversFromMs,
            inFlightSinceMs = inFlightSinceMs,
            inFlightCoversFromMs = inFlightCoversFromMs,
            unrecoveredSinceMs = unrecoveredSinceMs,
            lastPausedAtMs = lastPausedAtMs,
            lastStoppedAtMs = lastStoppedAtMs,
            offlineRecoveryAtMs = offlineRecoveryAtMs,
            lastRenderedMessageTsSec = lastRenderedMessageTsSec,
            ircClientPresent = ircClientPresent
        )
    }

    /**
     * Applies one decision: consumes and arms what it says, folds in what a skip left
     * owed, and for a request arms the in-flight marker and returns what to send. Called
     * under the monitor.
     */
    private fun apply(decision: HistoryBackfillDecision): HistoryBackfillSend? {
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
        effects.owedSinceMs?.let { owedSinceMs ->
            unrecoveredSinceMs = HistoryBackfillPolicy.olderOwedStart(
                currentMs = unrecoveredSinceMs,
                startMs = owedSinceMs
            )
        }

        if (decision !is HistoryBackfillDecision.Request) return null

        val sentAtMs = checkNotNull(effects.armInFlightSinceMs) {
            "Every history request arms the in-flight marker"
        }
        inFlightSinceMs = sentAtMs
        inFlightCoversFromMs = HistoryBackfillPolicy.coversFrom(sentAtMs, decision.requestedSec)
        return HistoryBackfillSend(requestedSec = decision.requestedSec, sentAtMs = sentAtMs)
    }

    /** Called under the monitor. */
    private fun onSuccess(send: HistoryBackfillSend, nowMs: Long) {
        val effects = HistoryBackfillPolicy.afterSuccess(
            sentAtMs = send.sentAtMs,
            requestedSec = send.requestedSec,
            nowMs = nowMs,
            inFlightSinceMs = inFlightSinceMs,
            unrecoveredSinceMs = unrecoveredSinceMs
        )
        inFlightSinceMs = effects.inFlightSinceMs
        if (inFlightSinceMs == 0L) inFlightCoversFromMs = 0L
        lastBackfillAtMs = effects.lastBackfillAtMs
        lastBackfillCoversFromMs = effects.lastBackfillCoversFromMs
        unrecoveredSinceMs = effects.unrecoveredSinceMs
    }

    /** Called under the monitor. */
    private fun onFailure(
        failure: HistoryBackfillFailure,
        send: HistoryBackfillSend
    ): HistoryBackfillFailureEffects {
        val effects = HistoryBackfillPolicy.afterFailure(
            failure = failure,
            requestedSec = send.requestedSec,
            sentAtMs = send.sentAtMs,
            inFlightSinceMs = inFlightSinceMs,
            unrecoveredSinceMs = unrecoveredSinceMs
        )
        inFlightSinceMs = effects.inFlightSinceMs
        if (inFlightSinceMs == 0L) inFlightCoversFromMs = 0L
        unrecoveredSinceMs = effects.unrecoveredSinceMs
        if (effects.clearHistoryLoaded) {
            historyLoaded = false
        }
        return effects
    }
}
