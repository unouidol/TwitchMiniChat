package com.fs.twitchminichat

/**
 * Maintains Twitch recent-emote ordering from newest to oldest.
 *
 * The first identifier maps to the top-left picker cell. Subsequent identifiers
 * are displayed from left to right before continuing on the next grid row.
 */
object TwitchEmoteRecentOrderPolicy {

    /**
     * Moves one manually selected emote to the beginning of the recent list.
     *
     * Duplicate and blank identifiers are removed. When capacity is exceeded,
     * the final and oldest identifier is discarded.
     */
    fun record(
        currentEmoteIds: List<String>,
        selectedEmoteId: String,
        maxSize: Int
    ): List<String> {
        require(maxSize > 0) {
            "Recent-emote capacity must be greater than zero."
        }

        val normalizedSelectedEmoteId = selectedEmoteId.trim()

        if (normalizedSelectedEmoteId.isBlank()) {
            return normalize(
                emoteIds = currentEmoteIds,
                maxSize = maxSize
            )
        }

        return buildList {
            add(normalizedSelectedEmoteId)

            currentEmoteIds
                .asSequence()
                .map { emoteId -> emoteId.trim() }
                .filter { emoteId -> emoteId.isNotBlank() }
                .filterNot { emoteId ->
                    emoteId == normalizedSelectedEmoteId
                }
                .distinct()
                .take(maxSize - 1)
                .forEach(::add)
        }
    }

    /**
     * Converts previous stable visual slots into newest-to-oldest ordering.
     *
     * The legacy replacement queue stored identifiers from oldest to newest.
     * Reversing the repaired queue restores chronological recent ordering.
     */
    fun migrateFromStableSlots(
        stableEmoteIds: List<String>,
        oldestToNewestOrder: List<String>,
        maxSize: Int
    ): List<String> {
        require(maxSize > 0) {
            "Recent-emote capacity must be greater than zero."
        }

        val normalizedSlots = normalize(
            emoteIds = stableEmoteIds,
            maxSize = maxSize
        )
        val knownSlotIds = normalizedSlots.toSet()
        val addedIds = LinkedHashSet<String>()

        val repairedOldestToNewest = buildList {
            oldestToNewestOrder
                .asSequence()
                .map { emoteId -> emoteId.trim() }
                .filter { emoteId -> emoteId in knownSlotIds }
                .forEach { emoteId ->
                    if (addedIds.add(emoteId)) {
                        add(emoteId)
                    }
                }

            normalizedSlots.forEach { emoteId ->
                if (addedIds.add(emoteId)) {
                    add(emoteId)
                }
            }
        }

        return repairedOldestToNewest
            .asReversed()
            .take(maxSize)
    }

    /** Normalizes one persisted list without changing its current order. */
    private fun normalize(
        emoteIds: List<String>,
        maxSize: Int
    ): List<String> {
        return emoteIds
            .asSequence()
            .map { emoteId -> emoteId.trim() }
            .filter { emoteId -> emoteId.isNotBlank() }
            .distinct()
            .take(maxSize)
            .toList()
    }
}