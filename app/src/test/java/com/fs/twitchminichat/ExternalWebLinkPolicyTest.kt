package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies the allowed schemes for links opened from Twitch chat. */
class ExternalWebLinkPolicyTest {

    /** Regular HTTPS links are accepted and trimmed. */
    @Test
    fun normalize_acceptsHttpsLink() {
        assertEquals(
            "https://example.com/path?q=1",
            ExternalWebLinkPolicy.normalize(
                "  https://example.com/path?q=1  "
            )
        )
    }

    /** Regular HTTP links remain supported for user-posted legacy sites. */
    @Test
    fun normalize_acceptsHttpLink() {
        assertEquals(
            "http://example.com",
            ExternalWebLinkPolicy.normalize(
                "http://example.com"
            )
        )
    }

    /** Non-web schemes cannot reach an external activity from chat. */
    @Test
    fun normalize_rejectsNonWebSchemes() {
        assertNull(
            ExternalWebLinkPolicy.normalize(
                "javascript:alert(1)"
            )
        )
        assertNull(
            ExternalWebLinkPolicy.normalize(
                "file:///sdcard/example.txt"
            )
        )
    }

    /** A scheme without a web authority is rejected. */
    @Test
    fun normalize_rejectsMissingAuthority() {
        assertNull(
            ExternalWebLinkPolicy.normalize(
                "https:example.com"
            )
        )
    }
}
