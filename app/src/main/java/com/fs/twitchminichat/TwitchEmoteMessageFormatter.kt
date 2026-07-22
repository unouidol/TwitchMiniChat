package com.fs.twitchminichat

/** Identifies one invisible marker that must be replaced by a Twitch emote drawable. */
data class TwitchEmoteMarker(
    val emoteId: String,
    val markerIndex: Int
)

/** Contains normalized message text and the emotes that must be rendered inside it. */
data class TwitchEmoteMessageLayout(
    val text: String,
    val markers: List<TwitchEmoteMarker>
)

/**
 * Converts Twitch Internet Relay Chat (IRC) emote ranges into stable inline markers.
 *
 * The formatter has no Android dependencies, so malformed server metadata can be
 * covered by local unit tests without creating views or starting Glide requests.
 */
object TwitchEmoteMessageFormatter {

    /** Invisible character replaced asynchronously by one emote image. */
    const val EMOTE_MARKER: Char = '\u2063'

    /** Parses one IRC emotes tag and prepares the corresponding message layout. */
    fun format(
        rawMessage: String,
        emotesRaw: String?
    ): TwitchEmoteMessageLayout {
        val ranges = parseRanges(emotesRaw)
        val normalizedMessage = normalizeMessage(
            rawMessage = rawMessage,
            hasEmotes = ranges.isNotEmpty()
        )

        if (ranges.isEmpty()) {
            return TwitchEmoteMessageLayout(
                text = normalizedMessage,
                markers = emptyList()
            )
        }

        val validRanges = mutableListOf<TwitchEmoteRange>()
        var previousEndInclusive = -1

        for (range in ranges) {
            val endExclusive = range.endInclusive + 1
            val validRange = range.start in normalizedMessage.indices &&
                    endExclusive in (range.start + 1)..normalizedMessage.length &&
                    range.start > previousEndInclusive

            if (!validRange) continue
            validRanges += range
            previousEndInclusive = range.endInclusive
        }

        if (validRanges.isEmpty()) {
            return TwitchEmoteMessageLayout(
                text = normalizedMessage,
                markers = emptyList()
            )
        }

        val renderedText = StringBuilder(normalizedMessage)

        /* Replace from right to left so earlier Twitch offsets remain unchanged. */
        for (range in validRanges.asReversed()) {
            val endExclusive = range.endInclusive + 1
            renderedText.replace(range.start, endExclusive, EMOTE_MARKER.toString())
        }

        var removedCharacterCount = 0
        val markers = validRanges.map { range ->
            val markerIndex = range.start - removedCharacterCount
            removedCharacterCount += range.endInclusive - range.start

            TwitchEmoteMarker(
                emoteId = range.emoteId,
                markerIndex = markerIndex
            )
        }

        return TwitchEmoteMessageLayout(
            text = renderedText.toString(),
            markers = markers
        )
    }

    /** Parses valid `emoteId:start-end` entries from one Twitch IRC tag. */
    private fun parseRanges(emotesRaw: String?): List<TwitchEmoteRange> {
        if (emotesRaw.isNullOrBlank()) return emptyList()

        val ranges = mutableListOf<TwitchEmoteRange>()

        for (specification in emotesRaw.split('/')) {
            val separatorIndex = specification.indexOf(':')
            if (separatorIndex <= 0 || separatorIndex + 1 >= specification.length) {
                continue
            }

            val emoteId = specification.substring(0, separatorIndex).trim()
            if (!isValidEmoteId(emoteId)) continue

            val rawPositions = specification.substring(separatorIndex + 1)
            for (rawPosition in rawPositions.split(',')) {
                val dashIndex = rawPosition.indexOf('-')
                if (dashIndex <= 0 || dashIndex + 1 >= rawPosition.length) continue

                val start = rawPosition.substring(0, dashIndex).toIntOrNull() ?: continue
                val endInclusive = rawPosition.substring(dashIndex + 1).toIntOrNull() ?: continue
                if (start < 0 || endInclusive < start) continue

                ranges += TwitchEmoteRange(
                    emoteId = emoteId,
                    start = start,
                    endInclusive = endInclusive
                )
            }
        }

        return ranges.sortedWith(
            compareBy<TwitchEmoteRange> { range -> range.start }
                .thenBy { range -> range.endInclusive }
        )
    }

    /** Removes Client-to-Client Protocol framing without changing normal chat text. */
    private fun normalizeMessage(
        rawMessage: String,
        hasEmotes: Boolean
    ): String {
        if (
            rawMessage.startsWith(ACTION_PREFIX) &&
            rawMessage.endsWith(ACTION_SUFFIX) &&
            rawMessage.length > ACTION_PREFIX.length
        ) {
            return rawMessage.substring(
                ACTION_PREFIX.length,
                rawMessage.length - ACTION_SUFFIX.length
            )
        }

        return if (hasEmotes) rawMessage else rawMessage.replace(ACTION_SUFFIX, "")
    }

    /** Rejects malformed identifiers before they can become Content Delivery Network URLs. */
    private fun isValidEmoteId(emoteId: String): Boolean {
        return emoteId.isNotBlank() && emoteId.all { character ->
            character.isLetterOrDigit() || character == '-' || character == '_'
        }
    }

    /** Represents one parsed range before the message text is shortened. */
    private data class TwitchEmoteRange(
        val emoteId: String,
        val start: Int,
        val endInclusive: Int
    )

    private const val ACTION_PREFIX = "\u0001ACTION "
    private const val ACTION_SUFFIX = "\u0001"
}
