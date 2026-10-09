package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [ChatMentionSuggestions] records every sender at once and fills the list only when it is
 * read, so a backfill of hundreds of messages fills it once instead of once per message.
 */
class ChatMentionSuggestionsTest {

    private var nowMs = 0L
    private var adapterExists = true
    private val fills = mutableListOf<List<String>>()
    private val tracker = ChatMentionUserTracker(monotonicTimeMillis = { nowMs })
    private val suggestions = ChatMentionSuggestions(
        users = tracker,
        authenticatedUsername = { ME },
        canFill = { adapterExists },
        fill = { items -> fills += items }
    )

    @Test
    fun aBatchOfMessages_fillsTheListOnce_whenItIsNextRead_notOncePerMessage() {
        tracker.reset(ME)
        repeat(800) { k -> suggestions.onMessage("user${k % 300}") }

        assertEquals("filled while nobody read it", 0, fills.size)

        suggestions.fillIfStale()
        suggestions.fillIfStale()

        assertEquals(1, fills.size)
        assertEquals(301, fills.single().size)
    }

    @Test
    fun theListFilled_isEveryActiveUserAtTheMomentOfTheRead_inTheTrackersOrder_eachAfterAnAt() {
        tracker.reset(ME)
        suggestions.onMessage("zoe")
        suggestions.onMessage("Bob")
        suggestions.onMessage("alice")

        suggestions.fillIfStale()

        assertEquals(listOf("@alice", "@Bob", "@me", "@zoe"), fills.single())
        assertEquals(tracker.activeDisplayNames(ME).map { "@$it" }, fills.single())
    }

    @Test
    fun aMessageAfterARead_makesTheNextReadFillAgain() {
        tracker.reset(ME)
        suggestions.onMessage("alice")
        suggestions.fillIfStale()

        suggestions.onMessage("bob")
        suggestions.fillIfStale()

        assertEquals(listOf(listOf("@alice", "@me"), listOf("@alice", "@bob", "@me")), fills)
    }

    @Test
    fun aBlankSender_recordsNothing_soTheNextReadHasNothingToFill() {
        tracker.reset(ME)
        suggestions.fillNow()

        suggestions.onMessage("   ")
        suggestions.fillIfStale()

        assertEquals(1, fills.size)
    }

    @Test
    fun fillNow_fillsEvenWithNothingNew_asTypingAnAtAndAChannelChangeAlwaysDid() {
        tracker.reset(ME)

        suggestions.fillNow()
        suggestions.fillNow()

        assertEquals(listOf(listOf("@me"), listOf("@me")), fills)
    }

    @Test
    fun beforeTheAdapterExists_nothingIsFilled_andTheSendersWaitForTheFirstRead() {
        tracker.reset(ME)
        adapterExists = false
        suggestions.onMessage("alice")
        suggestions.fillIfStale()
        suggestions.fillNow()

        adapterExists = true
        suggestions.fillIfStale()

        assertEquals(listOf(listOf("@alice", "@me")), fills)
    }

    private companion object {
        const val ME = "me"
    }
}
