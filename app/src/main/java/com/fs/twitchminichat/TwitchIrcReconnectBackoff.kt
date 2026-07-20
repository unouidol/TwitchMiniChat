package com.fs.twitchminichat

/** Provides bounded reconnect delays for unexpected Twitch IRC disconnects. */
class TwitchIrcReconnectBackoff(
    private val initialDelayMs: Long = 1_000L,
    private val maximumDelayMs: Long = 15_000L
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
