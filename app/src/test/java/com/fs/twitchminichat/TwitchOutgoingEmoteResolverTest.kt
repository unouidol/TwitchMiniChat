package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TwitchOutgoingEmoteResolverTest {

    @Test
    fun buildIrcTag_resolvesRepeatedCaseSensitiveTokens() {
        val catalog = catalog(
            entry(id = "25", name = "Kappa")
        )

        val result = TwitchOutgoingEmoteResolver.buildIrcTag(
            message = "Kappa hello Kappa kappa",
            catalog = catalog
        )

        assertEquals("25:0-4,12-16", result)
    }

    @Test
    fun buildIrcTag_keepsSeparateEmoteIdsInMessageOrder() {
        val catalog = catalog(
            entry(id = "1", name = "HeyGuys"),
            entry(id = "2", name = "PogChamp")
        )

        val result = TwitchOutgoingEmoteResolver.buildIrcTag(
            message = "HeyGuys PogChamp",
            catalog = catalog
        )

        assertEquals("1:0-6/2:8-15", result)
    }

    @Test
    fun buildIrcTag_doesNotMatchInsideNormalText() {
        val catalog = catalog(entry(id = "25", name = "Kappa"))

        assertNull(
            TwitchOutgoingEmoteResolver.buildIrcTag(
                message = "KappaTest notKappa",
                catalog = catalog
            )
        )
    }

    @Test
    fun buildIrcTag_prefersCurrentBroadcasterForDuplicateName() {
        val catalog = TwitchEmoteCatalog(
            broadcasterId = "room-2",
            fetchedAtMs = 1L,
            entries = listOf(
                entry(id = "global", name = "SameName", ownerId = ""),
                entry(id = "channel", name = "SameName", ownerId = "room-2")
            )
        )

        assertEquals(
            "channel:0-7",
            TwitchOutgoingEmoteResolver.buildIrcTag("SameName", catalog)
        )
    }

    private fun catalog(vararg entries: TwitchEmoteCatalogEntry): TwitchEmoteCatalog {
        return TwitchEmoteCatalog(
            broadcasterId = "room",
            fetchedAtMs = 1L,
            entries = entries.toList()
        )
    }

    private fun entry(
        id: String,
        name: String,
        ownerId: String = "owner"
    ): TwitchEmoteCatalogEntry {
        return TwitchEmoteCatalogEntry(
            id = id,
            name = name,
            ownerId = ownerId,
            emoteType = "subscriptions",
            formats = setOf("static")
        )
    }
}
