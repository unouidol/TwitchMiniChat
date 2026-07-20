package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies deterministic chat ordering without Android or network access. */
class ChatTimelineOrdererTest {

    /** Older history must be inserted before already rendered live data. */
    @Test
    fun olderHistoryMessageIsInsertedBeforeNewerLiveMessage() {
        val existing = listOf(
            ChatTimelinePosition(timestampMillis = 2_000L, sequence = 1L)
        )

        val insertionIndex = ChatTimelineOrderer.insertionIndex(
            existingPositions = existing,
            candidate = ChatTimelinePosition(
                timestampMillis = 1_000L,
                sequence = 2L
            )
        )

        assertEquals(0, insertionIndex)
    }

    /** New live data must remain after an older history snapshot. */
    @Test
    fun newerLiveMessageIsAppendedAfterHistory() {
        val existing = listOf(
            ChatTimelinePosition(timestampMillis = 1_000L, sequence = 1L),
            ChatTimelinePosition(timestampMillis = 2_000L, sequence = 2L)
        )

        val insertionIndex = ChatTimelineOrderer.insertionIndex(
            existingPositions = existing,
            candidate = ChatTimelinePosition(
                timestampMillis = 3_000L,
                sequence = 3L
            )
        )

        assertEquals(2, insertionIndex)
    }

    /** Equal Twitch timestamps must keep their stable source sequence. */
    @Test
    fun equalTimestampsPreserveOriginalSequence() {
        val existing = listOf(
            ChatTimelinePosition(timestampMillis = 1_000L, sequence = 1L),
            ChatTimelinePosition(timestampMillis = 1_000L, sequence = 3L)
        )

        val insertionIndex = ChatTimelineOrderer.insertionIndex(
            existingPositions = existing,
            candidate = ChatTimelinePosition(
                timestampMillis = 1_000L,
                sequence = 2L
            )
        )

        assertEquals(1, insertionIndex)
    }

    /** Canonical replacement must remain before a causally later bot response. */
    @Test
    fun preservedOutgoingSequenceStaysBeforeBotResponseWithSameTimestamp() {
        val canonicalPosition = ChatTimelinePosition(
            timestampMillis = 2_000L,
            sequence = 1L
        )
        val botResponsePosition = ChatTimelinePosition(
            timestampMillis = 2_000L,
            sequence = 2L
        )

        val insertionIndex = ChatTimelineOrderer.insertionIndex(
            existingPositions = listOf(botResponsePosition),
            candidate = canonicalPosition
        )

        assertEquals(0, insertionIndex)
    }
}
