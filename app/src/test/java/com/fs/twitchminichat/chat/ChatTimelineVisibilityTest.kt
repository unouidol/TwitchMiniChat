package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterizes [ChatTimelineVisibility] against what ChatFragment did at 692a7dc, where
 * removeMessagesOfHiddenUser and refreshHiddenUserVisibilityInChat set View.GONE on every
 * view whose ChatViewMeta named a hidden user, and on no other view.
 */
class ChatTimelineVisibilityTest {

    private var nextSequence = 0L

    @Test
    fun withNoHiddenUsers_everyRowIsShown() {
        val rows = listOf(message("alice"), systemLine("joined"), echo(), message("bob"))

        assertEquals(rows, ChatTimelineVisibility.shownRows(rows, emptySet()))
    }

    @Test
    fun hidingAUser_leavesOutOnlyThatUsersMessages_wherever_theyAre_inTimelineOrder() {
        val first = message("bob")
        val second = message("alice")
        val third = message("bob")
        val fourth = message("carol")
        val fifth = message("bob")

        val shown = ChatTimelineVisibility.shownRows(listOf(first, second, third, fourth, fifth), setOf("bob"))

        assertEquals(listOf(second, fourth), shown)
    }

    @Test
    fun unHidingAUser_bringsBackTheSameRowsInTheirPlaces() {
        val rows = listOf(message("alice"), message("bob"), message("carol"), message("bob"))

        val hidden = ChatTimelineVisibility.shownRows(rows, setOf("bob"))
        val unHidden = ChatTimelineVisibility.shownRows(rows, emptySet())

        assertEquals(listOf(rows[0], rows[2]), hidden)
        assertEquals(rows, unHidden)
    }

    @Test
    fun hidingAUserWithNoRows_leavesOutNothing() {
        val rows = listOf(message("alice"), message("carol"))

        assertEquals(rows, ChatTimelineVisibility.shownRows(rows, setOf("bob")))
    }

    @Test
    fun hidingSeveralUsers_leavesOutEachOfThem() {
        val rows = listOf(message("alice"), message("bob"), message("carol"))

        assertEquals(listOf(rows[2]), ChatTimelineVisibility.shownRows(rows, setOf("alice", "bob")))
    }

    @Test
    fun aMessageIsHiddenByItsLowercasedName_notByTheNameAsItWasShown() {
        val row = message(user = "Alice", usernameLower = "alice")

        assertFalse(ChatTimelineVisibility.isShown(row, setOf("alice")))
        assertTrue(ChatTimelineVisibility.isShown(row, setOf("Alice")))
    }

    @Test
    fun aSystemLine_isNeverHidden_whateverUsersAreHidden() {
        val row = systemLine("bob")

        assertTrue(ChatTimelineVisibility.isShown(row, setOf("bob", "alice", "")))
    }

    @Test
    fun thePendingOutgoingEcho_isNeverHidden_whateverUsersAreHidden() {
        assertTrue(ChatTimelineVisibility.isShown(echo(), setOf("me", "bob", "")))
    }

    @Test
    fun systemLinesAndTheEcho_stayInTheirPlaces_betweenHiddenMessages() {
        val rows = listOf(message("bob"), systemLine("switched"), message("bob"), echo(), message("bob"))

        assertEquals(listOf(rows[1], rows[3]), ChatTimelineVisibility.shownRows(rows, setOf("bob")))
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun position() = ChatTimelinePosition(timestampMillis = 1_000L, sequence = nextSequence++)

    private fun message(user: String, usernameLower: String = user): ChatMessageRow {
        return ChatMessageRow(
            position = position(),
            user = user,
            usernameLower = usernameLower,
            messageId = null,
            messageText = "text",
            messageTimestampSec = 1.0,
            emotesRaw = null,
            replyParentUserLogin = null
        )
    }

    private fun systemLine(text: String) = SystemLineRow(position(), text)

    private fun echo() = ViewOnlyRow(position())
}
