package com.fs.twitchminichat

/** Logical groups shown by the Twitch emote picker. */
enum class TwitchEmotePickerSectionKind {
    RECENT,
    CHANNEL,
    OTHER,
    GLOBAL
}

/** Immutable picker section containing one ordered group of emotes. */
data class TwitchEmotePickerSection(
    val kind: TwitchEmotePickerSectionKind,
    val entries: List<TwitchEmoteCatalogEntry>
)

/**
 * Builds deterministic picker sections from one account-and-channel catalog.
 *
 * Recent emotes are repeated as shortcuts at the top while also remaining in
 * their original broadcaster, account-access, or Twitch global section.
 */
object TwitchEmotePickerSectionBuilder {

    /** Builds non-empty sections in their final visual order. */
    fun build(
        catalog: TwitchEmoteCatalog,
        recentEmoteIds: List<String>,
        query: String = ""
    ): List<TwitchEmotePickerSection> {
        val normalizedEntries = normalizeEntries(catalog)
        val normalizedQuery = query.trim()

        val filteredEntries = if (normalizedQuery.isEmpty()) {
            normalizedEntries
        } else {
            normalizedEntries.filter { entry ->
                entry.name.contains(normalizedQuery, ignoreCase = true)
            }
        }

        val entriesById = filteredEntries.associateBy { entry -> entry.id }
        val recentIdsAdded = LinkedHashSet<String>()

        val recentEntries = recentEmoteIds.mapNotNull { recentId ->
            entriesById[recentId]
                ?.takeIf { entry -> recentIdsAdded.add(entry.id) }
        }

        /*
         * Recent is an additional shortcut section. Each emote also remains in
         * its original Channel, Other, or Global section for repeated selection.
         */
        val remainingEntries = filteredEntries

        val channelEntries = remainingEntries
            .filter { entry ->
                isCurrentChannelEntry(
                    entry = entry,
                    broadcasterId = catalog.broadcasterId
                )
            }
            .sortedWith(ALPHABETICAL_ENTRY_ORDER)

        val globalEntries = remainingEntries
            .filter { entry ->
                !isCurrentChannelEntry(entry, catalog.broadcasterId) &&
                        isGlobalEntry(entry)
            }
            .sortedWith(ALPHABETICAL_ENTRY_ORDER)

        val otherEntries = remainingEntries
            .filter { entry ->
                !isCurrentChannelEntry(entry, catalog.broadcasterId) &&
                        !isGlobalEntry(entry)
            }
            .sortedWith(ALPHABETICAL_ENTRY_ORDER)

        return buildList {
            addSection(
                kind = TwitchEmotePickerSectionKind.RECENT,
                entries = recentEntries
            )
            addSection(
                kind = TwitchEmotePickerSectionKind.CHANNEL,
                entries = channelEntries
            )
            addSection(
                kind = TwitchEmotePickerSectionKind.OTHER,
                entries = otherEntries
            )
            addSection(
                kind = TwitchEmotePickerSectionKind.GLOBAL,
                entries = globalEntries
            )
        }
    }

    /** Adds one section only when it contains visible emotes. */
    private fun MutableList<TwitchEmotePickerSection>.addSection(
        kind: TwitchEmotePickerSectionKind,
        entries: List<TwitchEmoteCatalogEntry>
    ) {
        if (entries.isEmpty()) return

        add(
            TwitchEmotePickerSection(
                kind = kind,
                entries = entries
            )
        )
    }

    /** Deduplicates typed names while preferring the current channel version. */
    private fun normalizeEntries(
        catalog: TwitchEmoteCatalog
    ): List<TwitchEmoteCatalogEntry> {
        return catalog.entries
            .asSequence()
            .filter { entry ->
                entry.id.isNotBlank() && entry.name.isNotBlank()
            }
            .groupBy { entry -> entry.name }
            .mapNotNull { (_, candidates) ->
                candidates.maxByOrNull { entry ->
                    entryPriority(
                        entry = entry,
                        broadcasterId = catalog.broadcasterId
                    )
                }
            }
            .sortedWith(ALPHABETICAL_ENTRY_ORDER)
    }

    /** Returns true when Twitch attributes the emote to the active broadcaster. */
    private fun isCurrentChannelEntry(
        entry: TwitchEmoteCatalogEntry,
        broadcasterId: String?
    ): Boolean {
        return !broadcasterId.isNullOrBlank() &&
                entry.ownerId == broadcasterId
    }

    /** Returns true for Twitch global and legacy smiley emote types. */
    private fun isGlobalEntry(entry: TwitchEmoteCatalogEntry): Boolean {
        return entry.emoteType.equals("globals", ignoreCase = true) ||
                entry.emoteType.equals("smilies", ignoreCase = true)
    }

    /** Gives channel and subscription entries priority over duplicate names. */
    private fun entryPriority(
        entry: TwitchEmoteCatalogEntry,
        broadcasterId: String?
    ): Int {
        val basePriority = when {
            isCurrentChannelEntry(entry, broadcasterId) -> 100
            entry.emoteType.equals("follower", ignoreCase = true) -> 80
            entry.emoteType.equals("subscriptions", ignoreCase = true) -> 70
            isGlobalEntry(entry) -> 20
            else -> 50
        }

        val animatedBonus = if (
            entry.formats.any { format ->
                format.equals("animated", ignoreCase = true)
            }
        ) {
            1
        } else {
            0
        }

        return basePriority + animatedBonus
    }

    /** Case-insensitive alphabetical ordering used inside non-recent sections. */
    private val ALPHABETICAL_ENTRY_ORDER:
        Comparator<TwitchEmoteCatalogEntry> =
        compareBy(
            String.CASE_INSENSITIVE_ORDER
        ) { entry: TwitchEmoteCatalogEntry ->
            entry.name
        }
}