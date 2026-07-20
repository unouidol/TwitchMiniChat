package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies local outgoing-message reconciliation without Android or network access. */
class OutgoingChatMessageTrackerTest {

    @Test
    fun userstate_confirms_oldest_recent_write() {
        val tracker = OutgoingChatMessageTracker()
        val first = tracker.register("channel", "User", "first", 100.0)
        val second = tracker.register("channel", "User", "second", 101.0)

        val confirmed = tracker.confirmOldestFromUserState("#channel", 102.0)

        assertEquals(first.localId, confirmed?.localId)
        assertTrue(confirmed?.transportConfirmed == true)
        assertTrue(tracker.contains(second.localId))
    }

    @Test
    fun canonical_history_replaces_matching_local_echo() {
        val tracker = OutgoingChatMessageTracker()
        val pending = tracker.register("channel", "User", "hello", 100.0)

        val canonical = tracker.confirmCanonical(
            channel = "#CHANNEL",
            username = "user",
            message = "hello",
            messageTimestampSec = 101.0
        )

        assertEquals(pending.localId, canonical?.localId)
        assertFalse(tracker.contains(pending.localId))
    }

    @Test
    fun action_messages_match_their_irc_framing() {
        val tracker = OutgoingChatMessageTracker()
        val pending = tracker.register("channel", "User", "/me waves", 100.0)

        val canonical = tracker.confirmCanonical(
            channel = "channel",
            username = "User",
            message = "\u0001ACTION waves\u0001",
            messageTimestampSec = 100.5
        )

        assertEquals(pending.localId, canonical?.localId)
    }

    @Test
    fun unrelated_or_old_messages_do_not_remove_pending_echo() {
        val tracker = OutgoingChatMessageTracker(
            canonicalMatchWindowSec = 5.0
        )
        val pending = tracker.register("channel", "User", "hello", 100.0)

        assertNull(tracker.confirmCanonical("other", "User", "hello", 101.0))
        assertNull(tracker.confirmCanonical("channel", "Other", "hello", 101.0))
        assertNull(tracker.confirmCanonical("channel", "User", "different", 101.0))
        assertNull(tracker.confirmCanonical("channel", "User", "hello", 110.0))
        assertTrue(tracker.contains(pending.localId))
    }

    @Test
    fun rejection_removes_only_newest_unconfirmed_write() {
        val tracker = OutgoingChatMessageTracker()
        val confirmed = tracker.register("channel", "User", "first", 100.0)
        tracker.confirmOldestFromUserState("channel", 101.0)
        val awaiting = tracker.register("channel", "User", "second", 102.0)

        val removed = tracker.removeNewestAwaiting("channel")

        assertEquals(awaiting.localId, removed?.localId)
        assertTrue(tracker.contains(confirmed.localId))
        assertFalse(tracker.contains(awaiting.localId))
    }

    @Test
    fun capacity_is_bounded() {
        val tracker = OutgoingChatMessageTracker(maximumPendingMessages = 2)
        val first = tracker.register("channel", "User", "one", 1.0)
        val second = tracker.register("channel", "User", "two", 2.0)
        val third = tracker.register("channel", "User", "three", 3.0)

        assertFalse(tracker.contains(first.localId))
        assertTrue(tracker.contains(second.localId))
        assertTrue(tracker.contains(third.localId))
        assertNotNull(tracker.remove(third.localId))
    }
}
