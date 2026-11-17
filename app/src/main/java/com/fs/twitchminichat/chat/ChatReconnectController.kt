package com.fs.twitchminichat.chat

import com.fs.twitchminichat.TwitchIrcReconnectBackoff

/** Posts and cancels delayed work on the thread the [ChatReconnectController] runs on. */
interface ChatReconnectScheduler {

    /** Runs [task] once after [delayMs] milliseconds, unless it is cancelled first. */
    fun postDelayed(task: Runnable, delayMs: Long)

    /** Cancels [task] if it has not run yet. */
    fun cancel(task: Runnable)
}

/** Writes one reconnect line; the chat page sends it to Logcat under its IRC tag. */
fun interface ChatReconnectLog {

    /** Writes [message] at [level]. The message carries no token, account or channel. */
    fun write(level: Level, message: String)

    /** Severity of a reconnect line. */
    enum class Level { DEBUG, WARN }
}

/**
 * When one chat page attempts its IRC connection again, and what stops two attempts
 * from overlapping.
 *
 * The page owns the connection itself: the token step, the socket, and which
 * connection is current. This controller owns the decision around it: the one
 * pending retry, the backoff that times it, and the in-progress guard every
 * connection attempt passes through, whether a retry, a network callback, a start
 * or a channel join asked for it.
 *
 * Every member runs on the main thread.
 *
 * Reconnecting is not one of the automatic actions the product rules forbid. "Nothing
 * is queued or retried automatically" protects gameplay commands and alerts: a chat
 * command, a catch, a notification. Reopening the chat connection of a page the user
 * has open and in front sends nothing in the user's name and takes no action on their
 * behalf; it restores the view they already chose, and stops when the page stops.
 */
class ChatReconnectController(
    private val scheduler: ChatReconnectScheduler,
    /** True while the page is added, has a view and is at least started. */
    private val canConnect: () -> Boolean,
    /** The page's connection entry point; it asks [mayConnect] before doing anything. */
    private val connect: () -> Unit,
    private val log: ChatReconnectLog,
    private val backoff: TwitchIrcReconnectBackoff = TwitchIrcReconnectBackoff()
) {

    /** True from [beginTokenAttempt] until [finishTokenAttempt] or [onConnectionClosed]. */
    var attemptInProgress: Boolean = false
        private set

    /**
     * True from a transient token failure until the next attempt begins or the page
     * closes its connection: the state in which a network that becomes available
     * triggers an attempt at once ([onNetworkAvailable]).
     */
    var awaitingTransientRetry: Boolean = false
        private set

    /* The one pending retry; null when none is scheduled. */
    private var pendingRetry: Runnable? = null

    /*
     * True while the pending retry carries the server's Retry-After. A network that
     * becomes available does not bring that retry forward.
     */
    private var pendingRetryHonoursRetryAfter = false

    /** True while a retry is scheduled and has not run or been cancelled. */
    val retryPending: Boolean
        get() = pendingRetry != null

    /**
     * The in-progress guard: a connection may start only when the page has no client
     * and no token attempt is running.
     */
    fun mayConnect(clientPresent: Boolean): Boolean = !clientPresent && !attemptInProgress

    /**
     * Marks the start of the asynchronous token step. A retry still pending is
     * cancelled: this attempt replaces it.
     */
    fun beginTokenAttempt() {
        cancelPendingRetry()
        awaitingTransientRetry = false
        attemptInProgress = true
    }

    /** Marks the end of the token step, whatever its result. */
    fun finishTokenAttempt() {
        attemptInProgress = false
    }

    /**
     * The token step failed in a way a later attempt can clear (#115): stays in the
     * reconnect cycle. [minimumDelayMs] is the server's Retry-After, when it gave one;
     * the next attempt then waits at least that long, and the backoff advances as usual.
     */
    fun scheduleAfterTransientTokenFailure(minimumDelayMs: Long?) {
        awaitingTransientRetry = true
        schedule(reason = REASON_TOKEN_TRANSIENT, minimumDelayMs = minimumDelayMs)
    }

    /**
     * The token step ended with a definitive answer or a local state a retry would not
     * change: no retry. The status line tells the user; a start or a channel join tries
     * again, as before.
     */
    fun recordTokenFailureWithoutRetry() {
        awaitingTransientRetry = false
        log.write(ChatReconnectLog.Level.WARN, "reconnect not scheduled reason=token_not_transient")
    }

    /** Schedules the next attempt after the page's live session ended unexpectedly. */
    fun scheduleAfterSessionEnded() {
        schedule(reason = REASON_SESSION, minimumDelayMs = null)
    }

    /**
     * The phone's default network became available, or moved to another network.
     *
     * The backoff starts again from its first delay, in every state: the delays it grew
     * to measured the path that just went away. Seen on the device for #115: the attempt
     * this callback started opened its socket on the first network to come back, the
     * default moved to a second network 1.4 s later, the socket ended 0.3 s after that,
     * and its retry waited the 16 s the outage had built up. A retry already pending keeps its delay, so a
     * Retry-After hold stays whole; only the next delay is timed from the start.
     *
     * After a transient token failure, also attempts at once instead of waiting for the
     * pending retry; otherwise starts nothing.
     *
     * The attempt goes through [connect], so it passes the same guard as every other:
     * it starts nothing while an attempt runs or a client exists, and the attempt it
     * starts cancels the retry it replaces ([beginTokenAttempt]).
     */
    fun onNetworkAvailable() {
        backoff.reset()

        log.write(
            ChatReconnectLog.Level.DEBUG,
            "network available awaitingRetry=$awaitingTransientRetry"
        )

        if (!awaitingTransientRetry) return
        if (!canConnect()) return
        if (attemptInProgress) return

        if (pendingRetryHonoursRetryAfter) {
            log.write(ChatReconnectLog.Level.DEBUG, "network available, retry held by Retry-After")
            return
        }

        log.write(ChatReconnectLog.Level.DEBUG, "reconnect start reason=$REASON_NETWORK")
        connect()
    }

    /** The connection was confirmed: the next drop starts again from the first delay. */
    fun onSessionReady() {
        awaitingTransientRetry = false
        backoff.reset()
    }

    /** A client is about to open: nothing scheduled earlier may start another. */
    fun onClientOpening() {
        cancelPendingRetry()
    }

    /**
     * The page closed its connection, or is stopping. Cancels the pending retry and
     * clears the guard; [resetBackoff] after an intentional close.
     */
    fun onConnectionClosed(resetBackoff: Boolean) {
        cancelPendingRetry()
        attemptInProgress = false
        awaitingTransientRetry = false

        if (resetBackoff) {
            backoff.reset()
        }
    }

    /** Cancels the pending retry, if any. */
    fun cancelPendingRetry() {
        val pending = pendingRetry ?: return
        scheduler.cancel(pending)
        pendingRetry = null
        pendingRetryHonoursRetryAfter = false
    }

    /*
     * Schedules one retry while the page can connect. The backoff bounds the delay
     * (1, 2, 4, 8, 16 s, then 16 s) and sets no limit on the number of attempts; only
     * the page stopping, or a connection confirmed, ends the cycle.
     */
    private fun schedule(reason: String, minimumDelayMs: Long?) {
        if (!canConnect()) return
        if (pendingRetry != null || attemptInProgress) return

        val backoffDelayMs = backoff.consumeDelayMs()
        val delayMs = if (minimumDelayMs != null) {
            maxOf(backoffDelayMs, minimumDelayMs)
        } else {
            backoffDelayMs
        }

        val task = object : Runnable {
            override fun run() {
                if (pendingRetry !== this) return
                pendingRetry = null
                pendingRetryHonoursRetryAfter = false

                if (!canConnect()) return

                log.write(ChatReconnectLog.Level.DEBUG, "reconnect start reason=$reason")
                connect()
            }
        }

        pendingRetry = task
        pendingRetryHonoursRetryAfter = minimumDelayMs != null
        scheduler.postDelayed(task, delayMs)

        val retryAfterField = if (minimumDelayMs != null) " retryAfterMs=$minimumDelayMs" else ""
        log.write(
            ChatReconnectLog.Level.WARN,
            "reconnect scheduled delayMs=$delayMs reason=$reason$retryAfterField"
        )
    }

    private companion object {

        /** A live IRC session ended without being asked to. */
        const val REASON_SESSION = "session"

        /** The token step failed in a way a later attempt can clear. */
        const val REASON_TOKEN_TRANSIENT = "token_transient"

        /** The default network became available while a transient failure waited. */
        const val REASON_NETWORK = "network"
    }
}
