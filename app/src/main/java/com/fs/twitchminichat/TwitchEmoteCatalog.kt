package com.fs.twitchminichat

/** Describes one Twitch emote that the authenticated account may type. */
data class TwitchEmoteCatalogEntry(
    val id: String,
    val name: String,
    val ownerId: String,
    val emoteType: String,
    val formats: Set<String>
)

/** Immutable account-and-channel snapshot returned by the Twitch emote API. */
data class TwitchEmoteCatalog(
    val broadcasterId: String?,
    val fetchedAtMs: Long,
    val entries: List<TwitchEmoteCatalogEntry>
) {
    /** Returns true when this snapshot contains at least one usable emote. */
    val isEmpty: Boolean
        get() = entries.isEmpty()

    companion object {
        /** Empty catalog used before disk cache or network data is available. */
        val EMPTY = TwitchEmoteCatalog(
            broadcasterId = null,
            fetchedAtMs = 0L,
            entries = emptyList()
        )
    }
}

/** Resolves a sent message against the account's already-loaded emote catalog. */
object TwitchOutgoingEmoteResolver {

    /**
     * Builds the same `emoteId:start-end` metadata consumed by the message formatter.
     *
     * Twitch emote names are case-sensitive. Only complete non-whitespace tokens are
     * matched, which prevents a catalog entry such as `Kappa` from changing normal
     * words that merely contain the same characters.
     */
    fun buildIrcTag(
        message: String,
        catalog: TwitchEmoteCatalog
    ): String? {
        if (message.isBlank() || catalog.isEmpty) return null

        val entriesByName = catalog.entries
            .asSequence()
            .filter { entry -> entry.id.isNotBlank() && entry.name.isNotBlank() }
            .groupBy { entry -> entry.name }
            .mapNotNull { (name, candidates) ->
                candidates
                    .maxByOrNull { entry -> priority(entry, catalog.broadcasterId) }
                    ?.let { entry -> name to entry }
            }
            .toMap()

        val positionsByEmoteId = LinkedHashMap<String, MutableList<String>>()

        NON_WHITESPACE_TOKEN.findAll(message).forEach { match ->
            val entry = entriesByName[match.value] ?: return@forEach
            val start = match.range.first
            val endInclusive = match.range.last

            positionsByEmoteId
                .getOrPut(entry.id) { mutableListOf() }
                .add("$start-$endInclusive")
        }

        if (positionsByEmoteId.isEmpty()) return null

        return positionsByEmoteId.entries.joinToString("/") { (emoteId, positions) ->
            "$emoteId:${positions.joinToString(",")}"
        }
    }

    /** Prefers a current-channel emote when Twitch returns duplicate typed names. */
    private fun priority(
        entry: TwitchEmoteCatalogEntry,
        broadcasterId: String?
    ): Int {
        if (!broadcasterId.isNullOrBlank() && entry.ownerId == broadcasterId) return 100

        return when (entry.emoteType) {
            "follower" -> 80
            "subscriptions" -> 70
            "globals", "smilies" -> 20
            else -> 50
        }
    }

    private val NON_WHITESPACE_TOKEN = Regex("\\S+")
}
