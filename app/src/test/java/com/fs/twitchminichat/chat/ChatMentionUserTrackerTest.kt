package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies mention candidates without Android UI or real-time dependencies. */
class ChatMentionUserTrackerTest {

    @Test
    fun recordIgnoresBlankUsersAndUpdatesExistingDisplayCasing() {
        var nowMs = 0L
        val tracker = ChatMentionUserTracker(
            monotonicTimeMillis = { nowMs }
        )

        assertFalse(tracker.record("   ", authenticatedUsername = null))
        assertTrue(tracker.record(" Alice ", authenticatedUsername = null))

        nowMs = 1L
        assertTrue(tracker.record("ALICE", authenticatedUsername = null))

        assertEquals(
            listOf("ALICE"),
            tracker.activeDisplayNames(authenticatedUsername = null)
        )
    }

    @Test
    fun suggestionsUseExistingCaseInsensitiveOrder() {
        val tracker = ChatMentionUserTracker(
            monotonicTimeMillis = { 0L }
        )

        tracker.record("zoe", authenticatedUsername = null)
        tracker.record("Alice", authenticatedUsername = null)
        tracker.record("bob", authenticatedUsername = null)

        assertEquals(
            listOf("Alice", "bob", "zoe"),
            tracker.activeDisplayNames(authenticatedUsername = null)
        )
    }

    @Test
    fun inactiveUsersExpireOnlyAfterBoundaryWhileCurrentUserRemains() {
        var nowMs = 0L
        val tracker = ChatMentionUserTracker(
            monotonicTimeMillis = { nowMs }
        )

        tracker.record("Alice", authenticatedUsername = "Alice")
        tracker.record("Bob", authenticatedUsername = "Alice")

        nowMs = 10 * 60 * 1_000L
        assertEquals(
            listOf("Alice", "Bob"),
            tracker.activeDisplayNames(authenticatedUsername = "ALICE")
        )

        nowMs += 1L
        assertEquals(
            listOf("Alice"),
            tracker.activeDisplayNames(authenticatedUsername = "ALICE")
        )
    }

    @Test
    fun observingExistingUserRenewsInactivityWindow() {
        var nowMs = 0L
        val tracker = ChatMentionUserTracker(
            inactivityTimeoutMs = 1_000L,
            monotonicTimeMillis = { nowMs }
        )

        tracker.record("Bob", authenticatedUsername = null)

        nowMs = 900L
        tracker.record("bob", authenticatedUsername = null)

        nowMs = 1_500L
        assertEquals(
            listOf("bob"),
            tracker.activeDisplayNames(authenticatedUsername = null)
        )

        nowMs = 1_901L
        assertEquals(
            emptyList<String>(),
            tracker.activeDisplayNames(authenticatedUsername = null)
        )
    }

    @Test
    fun resetDropsPreviousChannelUsersAndSeedsCurrentUser() {
        var nowMs = 0L
        val tracker = ChatMentionUserTracker(
            monotonicTimeMillis = { nowMs }
        )

        tracker.record("Alice", authenticatedUsername = "Alice")
        tracker.record("Bob", authenticatedUsername = "Alice")

        nowMs = 100L
        tracker.reset(authenticatedUsername = " Alice ")

        assertEquals(
            listOf("Alice"),
            tracker.activeDisplayNames(authenticatedUsername = "Alice")
        )

        tracker.reset(authenticatedUsername = null)
        assertEquals(
            emptyList<String>(),
            tracker.activeDisplayNames(authenticatedUsername = null)
        )
    }

    @Test
    fun inactivityTimeoutMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            ChatMentionUserTracker(
                inactivityTimeoutMs = 0L,
                monotonicTimeMillis = { 0L }
            )
        }
    }
}
