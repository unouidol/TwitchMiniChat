package com.fs.twitchminichat

/** Carries one partial update of authenticated Twitch IRC session metadata. */
data class TwitchIrcSessionMetadataUpdate(
    val userId: String? = null,
    val channel: String? = null,
    val roomId: String? = null,
    val emoteSetIds: Set<String>? = null
)

/** Stores the current authenticated user, channel IDs, and available emote sets. */
data class TwitchIrcSessionMetadataSnapshot(
    val userId: String? = null,
    val roomIdsByChannel: Map<String, String> = emptyMap(),
    val emoteSetIds: Set<String> = emptySet()
) {
    /** Returns the numeric Twitch room ID for one normalized channel name. */
    fun roomIdFor(channel: String): String? {
        val normalizedChannel = channel.trim().removePrefix("#").lowercase()
        if (normalizedChannel.isBlank()) return null
        return roomIdsByChannel[normalizedChannel]
    }
}

/**
 * Keeps non-secret Twitch IRC identity metadata available across fragment switches.
 *
 * OAuth tokens are deliberately excluded. This store only retains public numeric
 * identifiers and emote-set IDs needed by the future emote catalog provider.
 */
object TwitchIrcSessionMetadataStore {
    private val lock = Any()
    private val snapshotsByAccountId = mutableMapOf<String, TwitchIrcSessionMetadataSnapshot>()

    /** Merges one partial IRC metadata update into the selected account snapshot. */
    fun merge(
        accountId: String,
        update: TwitchIrcSessionMetadataUpdate
    ): TwitchIrcSessionMetadataSnapshot {
        val normalizedAccountId = accountId.trim()
        require(normalizedAccountId.isNotBlank()) {
            "accountId must not be blank"
        }

        return synchronized(lock) {
            val current = snapshotsByAccountId[normalizedAccountId]
                ?: TwitchIrcSessionMetadataSnapshot()

            val nextRooms = LinkedHashMap(current.roomIdsByChannel)
            val normalizedChannel = update.channel
                ?.trim()
                ?.removePrefix("#")
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
            val normalizedRoomId = update.roomId
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (normalizedChannel != null && normalizedRoomId != null) {
                nextRooms[normalizedChannel] = normalizedRoomId
            }

            val next = TwitchIrcSessionMetadataSnapshot(
                userId = update.userId
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: current.userId,
                roomIdsByChannel = nextRooms.toMap(),
                emoteSetIds = update.emoteSetIds
                    ?.asSequence()
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toCollection(LinkedHashSet())
                    ?: current.emoteSetIds
            )

            snapshotsByAccountId[normalizedAccountId] = next
            next
        }
    }

    /** Returns the latest immutable snapshot for one account. */
    fun get(accountId: String): TwitchIrcSessionMetadataSnapshot? {
        val normalizedAccountId = accountId.trim()
        if (normalizedAccountId.isBlank()) return null

        return synchronized(lock) {
            snapshotsByAccountId[normalizedAccountId]
        }
    }

    /** Removes metadata belonging to one account. */
    fun remove(accountId: String) {
        val normalizedAccountId = accountId.trim()
        if (normalizedAccountId.isBlank()) return

        synchronized(lock) {
            snapshotsByAccountId.remove(normalizedAccountId)
        }
    }
}
