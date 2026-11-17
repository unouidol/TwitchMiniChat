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
 * connection attempt passes through.
 *
 * Every member runs on the main thread.
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

    /* The one pending retry; null when none is scheduled. */
    private var pendingRetry: Runnable? = null

    /** True while a retry is scheduled and has not run or been cancelled. */
    val retryPending: Boolean
        get() = pendingRetry != null

    /**
     * The in-progress guard: a connection may start only when the page has no client
     * and no token attempt is running.
     */
    fun mayConnect(clientPresent: Boolean): Boolean = !clientPresent && !attemptInProgress

    /** Marks the start of the asynchronous token step. */
    fun beginTokenAttempt() {
        attemptInProgress = true
    }

    /** Marks the end of the token step, whatever its result. */
    fun finishTokenAttempt() {
        attemptInProgress = false
    }

    /** Schedules the next attempt after the page's live session ended unexpectedly. */
    fun scheduleAfterSessionEnded() {
        schedule(reason = REASON_SESSION)
    }

    /** The connection was confirmed: the next drop starts again from the first delay. */
    fun onSessionReady() {
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

        if (resetBackoff) {
            backoff.reset()
        }
    }

    /** Cancels the pending retry, if any. */
    fun cancelPendingRetry() {
        val pending = pendingRetry ?: return
        scheduler.cancel(pending)
        pendingRetry = null
    }

    /* Schedules one bounded retry while the page can connect. */
    private fun schedule(reason: String) {
        if (!canConnect()) return
        if (pendingRetry != null || attemptInProgress) return

        val delayMs = backoff.consumeDelayMs()
        val task = object : Runnable {
            override fun run() {
                if (pendingRetry !== this) return
                pendingRetry = null

                if (!canConnect()) return

                log.write(ChatReconnectLog.Level.DEBUG, "reconnect start reason=$reason")
                connect()
            }
        }

        pendingRetry = task
        scheduler.postDelayed(task, delayMs)

        log.write(
            ChatReconnectLog.Level.WARN,
            "reconnect scheduled delayMs=$delayMs reason=$reason"
        )
    }

    private companion object {

        /** A live IRC session ended without being asked to. */
        const val REASON_SESSION = "session"
    }
}
