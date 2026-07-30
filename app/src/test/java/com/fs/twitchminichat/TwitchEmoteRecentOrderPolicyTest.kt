package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies stable Twitch recent-emote visual ordering. */
class TwitchEmoteRecentOrderPolicyTest {

    /** A new emote enters the first slot and shifts older entries forward. */
    @Test
    fun record_placesNewEmoteInFirstPosition() {
        val updated = TwitchEmoteRecentOrderPolicy.record(
            currentEmoteIds = listOf("A", "B", "C"),
            selectedEmoteId = "D",
            maxSize = 4
        )

        assertEquals(
            listOf("D", "A", "B", "C"),
            updated
        )
    }

    /** Selecting an existing emote keeps it in its current visual position. */
    @Test
    fun record_keepsExistingEmoteInCurrentPosition() {
        val updated = TwitchEmoteRecentOrderPolicy.record(
            currentEmoteIds = listOf("A", "B", "C", "D"),
            selectedEmoteId = "C",
            maxSize = 4
        )

        assertEquals(
            listOf("A", "B", "C", "D"),
            updated
        )
    }

    /** The nineteenth emote removes the eighteenth and oldest entry. */
    @Test
    fun record_retainsAtMostEighteenEmotes() {
        val updated = TwitchEmoteRecentOrderPolicy.record(
            currentEmoteIds = listOf(
                "A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L",
                "M", "N", "O", "P", "Q", "R"
            ),
            selectedEmoteId = "S",
            maxSize = 18
        )

        assertEquals(
            listOf(
                "S", "A", "B", "C", "D", "E",
                "F", "G", "H", "I", "J", "K",
                "L", "M", "N", "O", "P", "Q"
            ),
            updated
        )
    }

    /** Legacy stable slots are restored to newest-to-oldest chronology. */
    @Test
    fun migrateFromStableSlots_restoresChronologicalOrder() {
        val migrated = TwitchEmoteRecentOrderPolicy.migrateFromStableSlots(
            stableEmoteIds = listOf("C", "A", "B"),
            oldestToNewestOrder = listOf("A", "B", "C"),
            maxSize = 12
        )

        assertEquals(
            listOf("C", "B", "A"),
            migrated
        )
    }

    /** Invalid identifiers are removed while repairing legacy chronology. */
    @Test
    fun migrateFromStableSlots_ignoresUnknownAndDuplicateIds() {
        val migrated = TwitchEmoteRecentOrderPolicy.migrateFromStableSlots(
            stableEmoteIds = listOf("C", "A", "B", "B", ""),
            oldestToNewestOrder = listOf(
                "missing",
                "A",
                "A",
                "B",
                "C"
            ),
            maxSize = 12
        )

        assertEquals(
            listOf("C", "B", "A"),
            migrated
        )
    }
}