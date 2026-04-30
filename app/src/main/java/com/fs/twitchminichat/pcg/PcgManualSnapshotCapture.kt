package com.fs.twitchminichat.pcg

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Owns a short manual capture window for passive PCG probe snapshots.
 *
 * The JavaScript probe keeps emitting snapshots passively. This class does not ask
 * the iframe to do anything; it only decides whether Android is currently willing
 * to accept one of those snapshots as the result of a user-triggered action.
 *
 * The first valid candidate received during the active window is held until the
 * visual progress duration completes. This gives the user clear feedback while
 * still avoiding fragile direct iframe commands.
 */
class PcgManualSnapshotCapture<T : Any>(
    private val debugLabel: String,
    private val captureDurationMs: Long = 5_000L,
    private val timeoutMs: Long = 10_000L,
    private val progressIntervalMs: Long = 100L,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val clockMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val onStarted: () -> Unit,
    private val onProgress: (Float) -> Unit,
    private val onSnapshotReady: (T) -> Unit,
    private val onTimedOut: () -> Unit,
    private val onCancelled: () -> Unit = {}
) {
    private val lock = Any()

    private var activeToken: Int = 0
    private var active: Boolean = false
    private var startedAtMs: Long = 0L
    private var candidate: T? = null
    private var progressComplete: Boolean = false

    init {
        require(captureDurationMs > 0L) { "captureDurationMs must be > 0" }
        require(timeoutMs >= captureDurationMs) {
            "timeoutMs must be greater than or equal to captureDurationMs"
        }
        require(progressIntervalMs > 0L) { "progressIntervalMs must be > 0" }
    }

    /**
     * True while Android is accepting a passive snapshot for this manual action.
     */
    val isActive: Boolean
        get() = synchronized(lock) { active }

    /**
     * Starts or restarts the capture window.
     *
     * Restarting is intentional: if the user somehow presses the button twice before
     * the UI disables it, the newest tap becomes the active source of truth.
     */
    fun begin() {
        val token = synchronized(lock) {
            activeToken += 1
            active = true
            startedAtMs = clockMs()
            candidate = null
            progressComplete = false
            activeToken
        }

        Log.d("PCG_INV_CAPTURE", "capture_started debugLabel=$debugLabel token=$token")

        handler.post {
            onStarted()
            onProgress(0f)
            scheduleProgressTick(token)
            scheduleTimeout(token)
        }
    }

    /**
     * Offers a valid passive snapshot to the active manual capture.
     *
     * Returns true only when this snapshot became the accepted candidate. Additional
     * snapshots in the same window are ignored so the user action has one clear
     * result and cannot save multiple rapidly emitted probe reads.
     */
    fun submitCandidate(snapshot: T): Boolean {
        val token: Int
        val shouldCompleteNow: Boolean

        synchronized(lock) {
            if (!active || candidate != null) {
                return false
            }

            candidate = snapshot
            token = activeToken
            shouldCompleteNow = progressComplete
        }

        Log.d(
            "PCG_INV_CAPTURE",
            "capture_candidate_accepted debugLabel=$debugLabel token=$token " +
                    "progressComplete=$shouldCompleteNow"
        )

        if (shouldCompleteNow) {
            completeWithSnapshot(token, snapshot)
        }

        return true
    }

    /**
     * Cancels the active capture without saving the accepted candidate.
     */
    fun cancel() {
        val shouldNotify = synchronized(lock) {
            if (!active) {
                false
            } else {
                activeToken += 1
                active = false
                candidate = null
                progressComplete = false
                true
            }
        }

        if (!shouldNotify) return

        Log.d("PCG_INV_CAPTURE", "capture_cancelled debugLabel=$debugLabel")

        handler.post {
            onCancelled()
        }
    }

    private fun scheduleProgressTick(token: Int) {
        handler.postDelayed(
            {
                handleProgressTick(token)
            },
            progressIntervalMs
        )
    }

    private fun handleProgressTick(token: Int) {
        val tick = synchronized(lock) {
            if (!active || activeToken != token) {
                return
            }

            val elapsedMs = clockMs() - startedAtMs
            val progress = (elapsedMs.toFloat() / captureDurationMs.toFloat())
                .coerceIn(0f, 1f)

            if (progress >= 1f) {
                progressComplete = true

                ProgressTick(
                    progress = 1f,
                    shouldContinue = false,
                    readyCandidate = candidate
                )
            } else {
                ProgressTick(
                    progress = progress,
                    shouldContinue = true,
                    readyCandidate = null
                )
            }
        }

        onProgress(tick.progress)

        val readyCandidate = tick.readyCandidate
        if (readyCandidate != null) {
            completeWithSnapshot(token, readyCandidate)
            return
        }

        if (tick.shouldContinue) {
            scheduleProgressTick(token)
        }
    }

    private fun scheduleTimeout(token: Int) {
        handler.postDelayed(
            {
                handleTimeout(token)
            },
            timeoutMs
        )
    }

    private fun handleTimeout(token: Int) {
        val candidateAtTimeout: T?
        val shouldTimeout: Boolean

        synchronized(lock) {
            if (!active || activeToken != token) {
                return
            }

            candidateAtTimeout = candidate

            if (candidateAtTimeout != null) {
                shouldTimeout = false
            } else {
                activeToken += 1
                active = false
                candidate = null
                progressComplete = false
                shouldTimeout = true
            }
        }

        if (candidateAtTimeout != null) {
            completeWithSnapshot(token, candidateAtTimeout)
            return
        }

        if (!shouldTimeout) return

        Log.d("PCG_INV_CAPTURE", "capture_timeout debugLabel=$debugLabel token=$token")

        handler.post {
            onProgress(1f)
            onTimedOut()
        }
    }

    private fun completeWithSnapshot(token: Int, snapshot: T) {
        val shouldComplete = synchronized(lock) {
            if (!active || activeToken != token) {
                false
            } else {
                activeToken += 1
                active = false
                candidate = null
                progressComplete = false
                true
            }
        }

        if (!shouldComplete) return

        Log.d("PCG_INV_CAPTURE", "capture_completed debugLabel=$debugLabel token=$token")

        handler.post {
            onProgress(1f)
            onSnapshotReady(snapshot)
        }
    }

    private data class ProgressTick<T : Any>(
        val progress: Float,
        val shouldContinue: Boolean,
        val readyCandidate: T?
    )
}