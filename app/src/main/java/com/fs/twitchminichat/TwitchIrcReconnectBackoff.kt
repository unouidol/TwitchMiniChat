package com.fs.twitchminichat

/**
 * Provides bounded reconnect delays for the Twitch IRC connection.
 *
 * The defaults give 1, 2, 4, 8 and 16 s, then 16 s for every later attempt, with no
 * limit on their number: the sequence Chatterino2 uses (`IrcConnection2`,
 * `ExponentialBackoff<5>` from 1000 ms, read at commit 24632fe, 2026-11-07).
 */
class TwitchIrcReconnectBackoff(
    private val initialDelayMs: Long = 1_000L,
    private val maximumDelayMs: Long = 16_000L
) {
    private var nextDelayMs = initialDelayMs

    init {
        require(initialDelayMs > 0L) {
            "initialDelayMs must be positive"
        }
        require(maximumDelayMs >= initialDelayMs) {
            "maximumDelayMs must be at least initialDelayMs"
        }
    }

    /** Returns the current delay and advances the following delay exponentially. */
    @Synchronized
    fun consumeDelayMs(): Long {
        val currentDelayMs = nextDelayMs
        nextDelayMs = (nextDelayMs * 2L).coerceAtMost(maximumDelayMs)
        return currentDelayMs
    }

    /** Resets reconnect timing after a successful connection or intentional close. */
    @Synchronized
    fun reset() {
        nextDelayMs = initialDelayMs
    }
}
