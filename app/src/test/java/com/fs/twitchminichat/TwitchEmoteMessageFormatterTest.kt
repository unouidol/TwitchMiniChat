package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies Twitch emote range parsing without Android framework dependencies. */
class TwitchEmoteMessageFormatterTest {

    @Test
    fun multipleEmotesBecomeOrderedMarkers() {
        val result = TwitchEmoteMessageFormatter.format(
            rawMessage = "Kappa hello PogChamp",
            emotesRaw = "25:0-4/305954156:12-19"
        )

        assertEquals("\u2063 hello \u2063", result.text)
        assertEquals(
            listOf(
                TwitchEmoteMarker(emoteId = "25", markerIndex = 0),
                TwitchEmoteMarker(emoteId = "305954156", markerIndex = 8)
            ),
            result.markers
        )
    }

    @Test
    fun repeatedEmoteRangesPreserveTheirOrder() {
        val result = TwitchEmoteMessageFormatter.format(
            rawMessage = "Kappa Kappa",
            emotesRaw = "25:0-4,6-10"
        )

        assertEquals("\u2063 \u2063", result.text)
        assertEquals(
            listOf(
                TwitchEmoteMarker(emoteId = "25", markerIndex = 0),
                TwitchEmoteMarker(emoteId = "25", markerIndex = 2)
            ),
            result.markers
        )
    }

    @Test
    fun malformedAndOutOfBoundsRangesAreIgnored() {
        val result = TwitchEmoteMessageFormatter.format(
            rawMessage = "hello",
            emotesRaw = "bad/path:wrong/25:20-30/evil.id:0-1"
        )

        assertEquals("hello", result.text)
        assertTrue(result.markers.isEmpty())
    }

    @Test
    fun actionFramingIsRemovedFromPlainMessages() {
        val result = TwitchEmoteMessageFormatter.format(
            rawMessage = "\u0001ACTION waves\u0001",
            emotesRaw = null
        )

        assertEquals("waves", result.text)
        assertTrue(result.markers.isEmpty())
    }

    @Test
    fun userSuppliedInvisibleMarkerDoesNotShiftEmoteMetadata() {
        val result = TwitchEmoteMessageFormatter.format(
            rawMessage = "\u2063 Kappa",
            emotesRaw = "25:2-6"
        )

        assertEquals("\u2063 \u2063", result.text)
        assertEquals(
            listOf(TwitchEmoteMarker(emoteId = "25", markerIndex = 2)),
            result.markers
        )
    }
}
