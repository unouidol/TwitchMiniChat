package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Freezes what the mention suggestion list holds, as the chat page reads it, for a sequence
 * of chat messages: which users, and in what order.
 *
 * The page records every message's sender, then reads the list with
 * [ChatMentionUserTracker.activeDisplayNames] and shows each name after an "@", filtered by
 * what the user has typed after their own "@". These tests pin the list at the moment it is
 * read, whenever that moment is, so a change to when the page reads it cannot change what it
 * shows without one of them failing.
 *
 * Written against the code at 859ae70, before the suggestion refresh was changed, and meant to
 * pass unchanged after it.
 */
class ChatMentionSuggestionsFrozenBehaviourTest {

    private var nowMs = 0L
    private val tracker = ChatMentionUserTracker(monotonicTimeMillis = { nowMs })

    @Test
    fun frozen_theListIsEveryActiveSender_inCaseInsensitiveAlphabeticalOrder_notInTheOrderTheyWrote() {
        tracker.reset(authenticatedUsername = ME)
        chat("zoe", "Bob", "alice", "Carol")

        assertEquals(listOf("alice", "Bob", "Carol", "me", "zoe"), shown())
    }

    @Test
    fun frozen_aRepeatFromAKnownSender_changesNeitherTheMembersNorTheOrder() {
        tracker.reset(authenticatedUsername = ME)
        chat("zoe", "Bob", "alice")
        val before = shown()

        chat("zoe", "zoe", "alice", "zoe")

        assertEquals(before, shown())
    }

    @Test
    fun frozen_aSenderWrittenWithOtherCasing_isTheSameUser_shownWithTheCasingOfTheirLatestMessage_inTheSamePlace() {
        tracker.reset(authenticatedUsername = ME)
        chat("Alice", "bob", "zoe")

        chat("ALICE")
        assertEquals(listOf("ALICE", "bob", "me", "zoe"), shown())

        chat("alice")
        assertEquals(listOf("alice", "bob", "me", "zoe"), shown())
    }

    @Test
    fun frozen_aBlankSender_isNeverListed_andSurroundingSpacesAreDropped() {
        tracker.reset(authenticatedUsername = ME)
        chat("", "   ", "\t", " bob ")

        assertEquals(listOf("bob", "me"), shown())
    }

    /*
     * Hiding a user is the chat's business, not the list's: the page records every sender
     * before it checks whether the sender is hidden, so a hidden user stays suggestible. The
     * tracker is given no hidden users and has no way to leave one out.
     */
    @Test
    fun frozen_aSenderTheChatHides_isListedLikeAnyOther() {
        tracker.reset(authenticatedUsername = ME)
        chat("alice", "hiddenTroll", "bob")

        assertEquals(listOf("alice", "bob", "hiddenTroll", "me"), shown())
    }

    @Test
    fun frozen_aSenderSilentForMoreThanTenMinutesLeaves_andEachMessageRestartsTheirTenMinutes() {
        tracker.reset(authenticatedUsername = ME)
        chat("alice", "bob")

        nowMs = 6 * MINUTE
        chat("alice")

        nowMs = 10 * MINUTE
        assertEquals(listOf("alice", "bob", "me"), shown())

        nowMs = 10 * MINUTE + 1
        assertEquals(listOf("alice", "me"), shown())

        nowMs = 16 * MINUTE + 1
        assertEquals(listOf("me"), shown())
    }

    @Test
    fun frozen_theSignedInUserIsListedFromTheStart_andNeverLeaves() {
        tracker.reset(authenticatedUsername = ME)
        assertEquals(listOf("me"), shown())

        nowMs = 60 * MINUTE
        assertEquals(listOf("me"), shown())
    }

    @Test
    fun frozen_aChannelChange_leavesOnlyTheSignedInUser() {
        tracker.reset(authenticatedUsername = ME)
        chat("alice", "bob")

        tracker.reset(authenticatedUsername = ME)

        assertEquals(listOf("me"), shown())
    }

    /* A scripted stretch of chat, read after every message: the list each time, as it stands. */
    @Test
    fun frozen_theListAfterEveryMessageOfAScriptedChat() {
        tracker.reset(authenticatedUsername = ME)
        val script = listOf(
            0L to "Zed" to listOf("me", "Zed"),
            1_000L to "amy" to listOf("amy", "me", "Zed"),
            2_000L to "  " to listOf("amy", "me", "Zed"),
            3_000L to "ZED" to listOf("amy", "me", "ZED"),
            4_000L to "Bea" to listOf("amy", "Bea", "me", "ZED"),
            5_000L to "me" to listOf("amy", "Bea", "me", "ZED"),
            10 * MINUTE + 1_001L to "carl" to listOf("Bea", "carl", "me", "ZED"),
            10 * MINUTE + 4_001L to "amy" to listOf("amy", "carl", "me"),
        )

        for ((step, expected) in script) {
            val (atMs, sender) = step
            nowMs = atMs
            chat(sender)
            assertEquals("after \"$sender\" at $atMs ms", expected, shown())
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /* Each message's sender, recorded as the page records it. */
    private fun chat(vararg senders: String) {
        for (sender in senders) tracker.record(sender, authenticatedUsername = ME)
    }

    /* The list as the page reads it to fill the suggestions. */
    private fun shown(): List<String> = tracker.activeDisplayNames(authenticatedUsername = ME)

    private companion object {
        const val ME = "me"
        const val MINUTE = 60_000L
    }
}
