package com.fs.twitchminichat.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies chat deduplication without Android, network, or wall-clock dependencies. */
class ChatMessageDeduplicatorTest {

    @Test
    fun consecutiveFallbackKeyIsSuppressedOnlyInsideRecentWindow() {
        var nowMs = 0L
        val deduplicator = ChatMessageDeduplicator(
            currentTimeMillis = { nowMs }
        )

        assertFalse(deduplicator.shouldSuppress("live:1000:user:1"))

        nowMs = 1_499L
        assertTrue(deduplicator.shouldSuppress("live:1000:user:1"))

        nowMs = 1_500L
        assertFalse(deduplicator.shouldSuppress("live:1000:user:1"))
    }

    @Test
    fun stableMessageIdRemainsSuppressedAfterWindowAndInterveningKeys() {
        var nowMs = 0L
        val deduplicator = ChatMessageDeduplicator(
            currentTimeMillis = { nowMs }
        )

        assertFalse(deduplicator.shouldSuppress("id:message-1"))

        nowMs = 2_000L
        assertFalse(deduplicator.shouldSuppress("live:2000:user:2"))

        nowMs = 4_000L
        assertTrue(deduplicator.shouldSuppress("id:message-1"))
    }

    @Test
    fun oldestStableMessageIdIsAcceptedAgainAfterCapacityEviction() {
        var nowMs = 0L
        val deduplicator = ChatMessageDeduplicator(
            maximumStableKeys = 2,
            currentTimeMillis = { nowMs }
        )

        assertFalse(deduplicator.shouldSuppress("id:message-1"))

        nowMs = 2_000L
        assertFalse(deduplicator.shouldSuppress("id:message-2"))

        nowMs = 4_000L
        assertFalse(deduplicator.shouldSuppress("id:message-3"))

        nowMs = 6_000L
        assertFalse(deduplicator.shouldSuppress("id:message-1"))
    }

    @Test
    fun clearForgetsRecentAndStableKeys() {
        var nowMs = 0L
        val deduplicator = ChatMessageDeduplicator(
            currentTimeMillis = { nowMs }
        )

        assertFalse(deduplicator.shouldSuppress("id:message-1"))

        nowMs = 2_000L
        assertTrue(deduplicator.shouldSuppress("id:message-1"))

        deduplicator.clear()
        assertFalse(deduplicator.shouldSuppress("id:message-1"))
    }
}
