package com.fs.twitchminichat

/** Result of inserting one Twitch emote name into the chat composer. */
data class TwitchEmoteTextInsertion(
    val text: String,
    val cursorPosition: Int
)

/** Inserts user-selected Twitch emote names without sending a chat message. */
object TwitchEmoteTextInserter {

    /**
     * Inserts one emote at the selected range and preserves valid token spacing.
     *
     * This method only edits local composer text. Sending remains a separate,
     * explicit user action through the Send button.
     */
    fun insert(
        currentText: String,
        selectionStart: Int,
        selectionEnd: Int,
        emoteName: String
    ): TwitchEmoteTextInsertion {
        val normalizedName = emoteName.trim()
        val fallbackCursor = selectionEnd.coerceIn(0, currentText.length)

        if (
            normalizedName.isEmpty() ||
            normalizedName.any { character -> character.isWhitespace() }
        ) {
            return TwitchEmoteTextInsertion(
                text = currentText,
                cursorPosition = fallbackCursor
            )
        }

        val normalizedStart = minOf(selectionStart, selectionEnd)
            .coerceIn(0, currentText.length)
        val normalizedEnd = maxOf(selectionStart, selectionEnd)
            .coerceIn(normalizedStart, currentText.length)

        val needsLeadingSpace =
            normalizedStart > 0 &&
                    !currentText[normalizedStart - 1].isWhitespace()

        val needsTrailingSpace =
            normalizedEnd < currentText.length &&
                    !currentText[normalizedEnd].isWhitespace()

        val replacement = buildString {
            if (needsLeadingSpace) {
                append(' ')
            }

            append(normalizedName)

            if (needsTrailingSpace) {
                append(' ')
            }
        }

        val updatedText = currentText.replaceRange(
            startIndex = normalizedStart,
            endIndex = normalizedEnd,
            replacement = replacement
        )

        return TwitchEmoteTextInsertion(
            text = updatedText,
            cursorPosition = normalizedStart + replacement.length
        )
    }
}