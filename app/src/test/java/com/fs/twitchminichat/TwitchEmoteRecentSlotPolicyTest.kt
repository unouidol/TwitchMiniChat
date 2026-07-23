package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies stable visual slots and chronological recent-emote replacement. */
class TwitchEmoteRecentSlotPolicyTest {

    /** Selecting an existing emote must not move or refresh it. */
    @Test
    fun record_keepsExistingEmoteInItsCurrentSlot() {
        val update = TwitchEmoteRecentSlotPolicy.record(
            currentEmoteIds = listOf("A", "B", "C"),
            currentReplacementOrder = listOf("A", "B", "C"),
            selectedEmoteId = "B",
            maxSize = 3
        )

        assertEquals(
            listOf("A", "B", "C"),
            update.emoteIds
        )
        assertEquals(
            listOf("A", "B", "C"),
            update.replacementOrder
        )
    }

    /** New emotes fill available slots without moving existing ones. */
    @Test
    fun record_appendsNewEmoteWhileCapacityRemains() {
        val update = TwitchEmoteRecentSlotPolicy.record(
            currentEmoteIds = listOf("A", "B"),
            currentReplacementOrder = listOf("A", "B"),
            selectedEmoteId = "C",
            maxSize = 3
        )

        assertEquals(
            listOf("A", "B", "C"),
            update.emoteIds
        )
        assertEquals(
            listOf("A", "B", "C"),
            update.replacementOrder
        )
    }

    /** A full list replaces only the oldest emote's visual slot. */
    @Test
    fun record_replacesOnlyOldestEmoteSlot() {
        val update = TwitchEmoteRecentSlotPolicy.record(
            currentEmoteIds = listOf("A", "B", "C"),
            currentReplacementOrder = listOf("B", "A", "C"),
            selectedEmoteId = "D",
            maxSize = 3
        )

        assertEquals(
            listOf("A", "D", "C"),
            update.emoteIds
        )
        assertEquals(
            listOf("A", "C", "D"),
            update.replacementOrder
        )
    }

    /** Sequential additions replace stable slots in first-in-first-out order. */
    @Test
    fun record_rotatesThroughStableSlots() {
        val firstUpdate = TwitchEmoteRecentSlotPolicy.record(
            currentEmoteIds = listOf("A", "B", "C"),
            currentReplacementOrder = listOf("A", "B", "C"),
            selectedEmoteId = "D",
            maxSize = 3
        )

        assertEquals(
            listOf("D", "B", "C"),
            firstUpdate.emoteIds
        )
        assertEquals(
            listOf("B", "C", "D"),
            firstUpdate.replacementOrder
        )

        val secondUpdate = TwitchEmoteRecentSlotPolicy.record(
            currentEmoteIds = firstUpdate.emoteIds,
            currentReplacementOrder = firstUpdate.replacementOrder,
            selectedEmoteId = "E",
            maxSize = 3
        )

        assertEquals(
            listOf("D", "E", "C"),
            secondUpdate.emoteIds
        )
        assertEquals(
            listOf("C", "D", "E"),
            secondUpdate.replacementOrder
        )
    }

    /** Reversed legacy chronology replaces the actual oldest stored emote. */
    @Test
    fun record_preservesLegacyNewestToOldestMigration() {
        val update = TwitchEmoteRecentSlotPolicy.record(
            currentEmoteIds = listOf("C", "B", "A"),
            currentReplacementOrder = listOf("A", "B", "C"),
            selectedEmoteId = "D",
            maxSize = 3
        )

        assertEquals(
            listOf("C", "B", "D"),
            update.emoteIds
        )
        assertEquals(
            listOf("B", "C", "D"),
            update.replacementOrder
        )
    }
}