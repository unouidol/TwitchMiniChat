package com.fs.twitchminichat

import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Decides how long a silent Twitch IRC connection may stay silent, and what a
 * read timeout means once one happens.
 *
 * A half-open socket - a network that disappears without a FIN, a Network
 * Address Translation (NAT) entry that expires - leaves the blocking read
 * waiting forever. Nothing closes, so nothing reconnects, and the chat simply
 * stops updating with no signal to the user. A read timeout is what turns that
 * silence into an ordinary end of connection.
 */
object TwitchIrcLivenessPolicy {

    /**
     * Read timeout applied to the Twitch IRC socket, in milliseconds.
     *
     * Chosen from the protocol rather than from a round number. On a channel
     * where nobody is talking, the only inbound traffic Twitch guarantees is its
     * own PING, sent roughly every 300 seconds and answered here with a PONG.
     * Chat messages guarantee nothing: a quiet channel can legitimately send no
     * line for hours. 420 seconds sits above one PING interval and below two, so
     * it expires only when a PING has unequivocally failed to arrive, never
     * during an ordinary lull.
     *
     * The two ways of being wrong do not cost the same. Noticing too late costs
     * display latency only: the backfill that follows the reconnect asks the
     * backend for the window that was missed, and the backend still holds it
     * within KEEP_SECONDS, so the messages arrive late rather than never.
     * Expiring too early costs a needless reconnect on a healthy link, which
     * drops and rejoins the channel and spends a backfill request to recover a
     * window that was never lost. The asymmetry argues for the generous side of
     * the range.
     */
    const val READ_TIMEOUT_MS: Int = 420_000

    /**
     * Arms the read timeout on [socket].
     *
     * Kept here rather than inline at the call site so the value that reaches
     * the socket is the one this policy documents, and so it can be asserted
     * without opening a connection.
     */
    fun applyReadTimeout(socket: Socket) {
        socket.soTimeout = READ_TIMEOUT_MS
    }

    /** Returns whether [cause] is the read timeout this policy arms. */
    fun isReadTimeout(cause: Throwable?): Boolean {
        return cause is SocketTimeoutException
    }

    /**
     * Returns whether the session should be re-established after [cause].
     *
     * The cause deliberately does not enter the decision: a read timeout is a
     * dead connection exactly like a stream that ended, so it takes the same
     * path through the existing disconnect callback and earns the same backoff
     * and history recovery. Only a disconnect the user asked for stops it.
     */
    fun shouldReconnectAfter(cause: Throwable?, disconnectRequested: Boolean): Boolean {
        return !disconnectRequested
    }

    /**
     * Returns whether [cause] deserves the on-screen read-error status.
     *
     * A read timeout is a reconnection already under way, not a failure the user
     * can act on, and surfacing it would flash an error for the second before
     * the session comes back. Every other cause keeps reporting as before.
     */
    fun shouldReportToUser(cause: Throwable?): Boolean {
        return !isReadTimeout(cause)
    }
}
