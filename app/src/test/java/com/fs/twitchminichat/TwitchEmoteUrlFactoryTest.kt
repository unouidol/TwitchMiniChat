package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies official Twitch Content Delivery Network URL generation. */
class TwitchEmoteUrlFactoryTest {

    @Test
    fun animatedDarkUrlUsesRequestedOfficialTemplateValues() {
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/12345/animated/dark/2.0",
            TwitchEmoteUrlFactory.build(
                emoteId = "12345",
                format = TwitchEmoteFormat.ANIMATED,
                theme = TwitchEmoteTheme.DARK,
                scale = "2.0"
            )
        )
    }

    @Test
    fun unsafeEmoteIdentifierIsRejected() {
        assertNull(
            TwitchEmoteUrlFactory.build(
                emoteId = "../secret",
                format = TwitchEmoteFormat.STATIC,
                theme = TwitchEmoteTheme.LIGHT,
                scale = "1.0"
            )
        )
    }

    @Test
    fun renderSizeSelectsSmallestSuitableSourceScale() {
        assertEquals("1.0", TwitchEmoteUrlFactory.scaleForRenderSize(28))
        assertEquals("2.0", TwitchEmoteUrlFactory.scaleForRenderSize(56))
        assertEquals("3.0", TwitchEmoteUrlFactory.scaleForRenderSize(57))
    }
}
