package com.fs.twitchminichat

/**
 * Result of recording one manually selected Twitch emote.
 *
 * @property emoteIds identifiers in stable visual-slot order.
 * @property replacementOrder identifiers ordered from oldest to newest.
 */
data class TwitchEmoteRecentSlotUpdate(
    val emoteIds: List<String>,
    val replacementOrder: List<String>
)

/**
 * Maintains stable Twitch emote slots with chronological replacement.
 *
 * Visual position and replacement age are kept separate so replacing the oldest
 * emote never causes the remaining emotes to move.
 */
object TwitchEmoteRecentSlotPolicy {

    /**
     * Records one manually selected emote.
     *
     * Selecting an existing identifier leaves both its slot and age unchanged.
     * A new identifier is appended while capacity remains. At capacity, it
     * replaces only the oldest identifier's slot.
     */
    fun record(
        currentEmoteIds: List<String>,
        currentReplacementOrder: List<String>,
        selectedEmoteId: String,
        maxSize: Int
    ): TwitchEmoteRecentSlotUpdate {
        require(maxSize > 0) {
            "Recent-emote capacity must be greater than zero."
        }

        val emoteIds = currentEmoteIds
            .asSequence()
            .map { emoteId -> emoteId.trim() }
            .filter { emoteId -> emoteId.isNotBlank() }
            .distinct()
            .take(maxSize)
            .toMutableList()

        val replacementOrder = normalizeReplacementOrder(
            emoteIds = emoteIds,
            replacementOrder = currentReplacementOrder
        ).toMutableList()

        val normalizedSelectedEmoteId = selectedEmoteId.trim()

        if (
            normalizedSelectedEmoteId.isBlank() ||
            normalizedSelectedEmoteId in emoteIds
        ) {
            return TwitchEmoteRecentSlotUpdate(
                emoteIds = emoteIds,
                replacementOrder = replacementOrder
            )
        }

        if (emoteIds.size < maxSize) {
            emoteIds.add(normalizedSelectedEmoteId)
            replacementOrder.add(normalizedSelectedEmoteId)

            return TwitchEmoteRecentSlotUpdate(
                emoteIds = emoteIds,
                replacementOrder = replacementOrder
            )
        }

        val oldestEmoteId = replacementOrder.first()
        val replacementIndex = emoteIds.indexOf(oldestEmoteId)

        check(replacementIndex >= 0) {
            "The oldest recent emote must occupy a visual slot."
        }

        emoteIds[replacementIndex] = normalizedSelectedEmoteId

        replacementOrder.removeAt(0)
        replacementOrder.add(normalizedSelectedEmoteId)

        return TwitchEmoteRecentSlotUpdate(
            emoteIds = emoteIds,
            replacementOrder = replacementOrder
        )
    }

    /**
     * Repairs chronological data and guarantees one entry for every visual slot.
     *
     * Unknown and duplicate identifiers are discarded. Missing identifiers are
     * treated as newer entries to avoid unexpectedly replacing them first.
     */
    private fun normalizeReplacementOrder(
        emoteIds: List<String>,
        replacementOrder: List<String>
    ): List<String> {
        val knownOrder = replacementOrder
            .asSequence()
            .map { emoteId -> emoteId.trim() }
            .filter { emoteId -> emoteId in emoteIds }
            .distinct()
            .toMutableList()

        emoteIds
            .filterNot { emoteId -> emoteId in knownOrder }
            .forEach(knownOrder::add)

        return knownOrder
    }
}