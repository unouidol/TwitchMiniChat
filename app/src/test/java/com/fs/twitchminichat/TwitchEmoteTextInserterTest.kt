package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies cursor and spacing behavior used by the Twitch emote picker. */
class TwitchEmoteTextInserterTest {

    /** Inserts an emote into an empty composer. */
    @Test
    fun insertsIntoEmptyText() {
        val result = TwitchEmoteTextInserter.insert(
            currentText = "",
            selectionStart = 0,
            selectionEnd = 0,
            emoteName = "Kappa"
        )

        assertEquals("Kappa", result.text)
        assertEquals(5, result.cursorPosition)
    }

    /** Adds a separator when inserting after an existing word. */
    @Test
    fun addsLeadingSpaceAfterExistingWord() {
        val result = TwitchEmoteTextInserter.insert(
            currentText = "hello",
            selectionStart = 5,
            selectionEnd = 5,
            emoteName = "Kappa"
        )

        assertEquals("hello Kappa", result.text)
        assertEquals(11, result.cursorPosition)
    }

    /** Preserves existing whitespace between surrounding words. */
    @Test
    fun insertsBetweenWordsWithoutDuplicatingSpaces() {
        val result = TwitchEmoteTextInserter.insert(
            currentText = "hello world",
            selectionStart = 6,
            selectionEnd = 6,
            emoteName = "Kappa"
        )

        assertEquals("hello Kappa world", result.text)
        assertEquals(12, result.cursorPosition)
    }

    /** Replaces the selected text and restores token separation. */
    @Test
    fun replacesSelectedText() {
        val result = TwitchEmoteTextInserter.insert(
            currentText = "hello old world",
            selectionStart = 6,
            selectionEnd = 9,
            emoteName = "Kappa"
        )

        assertEquals("hello Kappa world", result.text)
        assertEquals(11, result.cursorPosition)
    }

    /** Supports reversed selection coordinates reported by the input view. */
    @Test
    fun normalizesReversedSelection() {
        val result = TwitchEmoteTextInserter.insert(
            currentText = "hello old",
            selectionStart = 9,
            selectionEnd = 6,
            emoteName = "Kappa"
        )

        assertEquals("hello Kappa", result.text)
        assertEquals(11, result.cursorPosition)
    }

    /** Rejects invalid multi-word catalog values without changing the composer. */
    @Test
    fun rejectsMultiWordName() {
        val result = TwitchEmoteTextInserter.insert(
            currentText = "hello",
            selectionStart = 5,
            selectionEnd = 5,
            emoteName = "Not An Emote"
        )

        assertEquals("hello", result.text)
        assertEquals(5, result.cursorPosition)
    }
}