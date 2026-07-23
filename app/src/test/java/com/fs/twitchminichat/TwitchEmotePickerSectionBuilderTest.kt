package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchEmotePickerSectionBuilderTest {

    @Test
    fun build_keepsRecentShortcutInsideItsOriginalSection() {
        val catalog = TwitchEmoteCatalog(
            broadcasterId = "channel-owner",
            fetchedAtMs = 1L,
            entries = listOf(
                entry(
                    id = "channel-z",
                    name = "Zulu",
                    ownerId = "channel-owner",
                    emoteType = "subscriptions"
                ),
                entry(
                    id = "global",
                    name = "Kappa",
                    ownerId = "twitch",
                    emoteType = "globals"
                ),
                entry(
                    id = "recent",
                    name = "RecentOne",
                    ownerId = "other-owner",
                    emoteType = "subscriptions"
                ),
                entry(
                    id = "channel-a",
                    name = "Alpha",
                    ownerId = "channel-owner",
                    emoteType = "follower"
                ),
                entry(
                    id = "other",
                    name = "Beta",
                    ownerId = "other-owner",
                    emoteType = "subscriptions"
                )
            )
        )

        val sections = TwitchEmotePickerSectionBuilder.build(
            catalog = catalog,
            recentEmoteIds = listOf("recent", "missing")
        )

        assertEquals(
            listOf(
                TwitchEmotePickerSectionKind.RECENT,
                TwitchEmotePickerSectionKind.CHANNEL,
                TwitchEmotePickerSectionKind.OTHER,
                TwitchEmotePickerSectionKind.GLOBAL
            ),
            sections.map { section -> section.kind }
        )
        assertEquals(
            listOf("RecentOne"),
            sections[0].entries.map { entry -> entry.name }
        )
        assertEquals(
            listOf("Alpha", "Zulu"),
            sections[1].entries.map { entry -> entry.name }
        )
        assertEquals(
            listOf("Beta", "RecentOne"),
            sections[2].entries.map { entry -> entry.name }
        )
        assertEquals(
            listOf("Kappa"),
            sections[3].entries.map { entry -> entry.name }
        )

        val visibleIds = sections.flatMap { section ->
            section.entries.map { entry -> entry.id }
        }

        assertEquals(
            2,
            visibleIds.count { emoteId -> emoteId == "recent" }
        )
        assertFalse("missing" in visibleIds)
    }

    @Test
    fun build_prefersCurrentChannelWhenTypedNamesAreDuplicated() {
        val catalog = TwitchEmoteCatalog(
            broadcasterId = "channel-owner",
            fetchedAtMs = 1L,
            entries = listOf(
                entry(
                    id = "global-same",
                    name = "SameName",
                    ownerId = "twitch",
                    emoteType = "globals"
                ),
                entry(
                    id = "channel-same",
                    name = "SameName",
                    ownerId = "channel-owner",
                    emoteType = "subscriptions",
                    animated = true
                )
            )
        )

        val sections = TwitchEmotePickerSectionBuilder.build(
            catalog = catalog,
            recentEmoteIds = emptyList(),
            query = "same"
        )

        assertEquals(1, sections.size)
        assertEquals(
            TwitchEmotePickerSectionKind.CHANNEL,
            sections.single().kind
        )
        assertEquals(
            "channel-same",
            sections.single().entries.single().id
        )
        assertTrue(
            "animated" in sections.single().entries.single().formats
        )
    }

    /** Creates one concise catalog fixture. */
    private fun entry(
        id: String,
        name: String,
        ownerId: String,
        emoteType: String,
        animated: Boolean = false
    ): TwitchEmoteCatalogEntry {
        return TwitchEmoteCatalogEntry(
            id = id,
            name = name,
            ownerId = ownerId,
            emoteType = emoteType,
            formats = if (animated) {
                setOf("static", "animated")
            } else {
                setOf("static")
            }
        )
    }
}