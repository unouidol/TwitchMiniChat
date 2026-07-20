package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Verifies bounded reconnect timing without network access. */
class TwitchIrcReconnectBackoffTest {

    @Test
    fun delays_grow_exponentially_and_stop_at_maximum() {
        val backoff = TwitchIrcReconnectBackoff(
            initialDelayMs = 1_000L,
            maximumDelayMs = 15_000L
        )

        assertEquals(1_000L, backoff.consumeDelayMs())
        assertEquals(2_000L, backoff.consumeDelayMs())
        assertEquals(4_000L, backoff.consumeDelayMs())
        assertEquals(8_000L, backoff.consumeDelayMs())
        assertEquals(15_000L, backoff.consumeDelayMs())
        assertEquals(15_000L, backoff.consumeDelayMs())
    }

    @Test
    fun successful_connection_resets_delay() {
        val backoff = TwitchIrcReconnectBackoff()

        backoff.consumeDelayMs()
        backoff.consumeDelayMs()
        backoff.reset()

        assertEquals(1_000L, backoff.consumeDelayMs())
    }

    @Test
    fun invalid_configuration_is_rejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TwitchIrcReconnectBackoff(
                initialDelayMs = 0L,
                maximumDelayMs = 15_000L
            )
        }
    }
}
