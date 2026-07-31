package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies classification of terminal IRC authentication NOTICE messages. */
class TwitchIrcNoticeClassifierTest {

    /** Recognizes the exact Twitch response observed for an expired local token. */
    @Test
    fun loginAuthenticationFailureIsTerminal() {
        val category = TwitchIrcNoticeClassifier.classify(
            msgId = null,
            message = "Login authentication failed"
        )

        assertEquals(
            TwitchIrcNoticeCategory.AUTHENTICATION_FAILED,
            category
        )
    }

    /** Authentication matching remains stable across casing and surrounding whitespace. */
    @Test
    fun authenticationFailureMatchingIsNormalized() {
        val category = TwitchIrcNoticeClassifier.classify(
            msgId = null,
            message = "  IMPROPERLY FORMATTED AUTH  "
        )

        assertEquals(
            TwitchIrcNoticeCategory.AUTHENTICATION_FAILED,
            category
        )
    }

    /** A normal send rejection stays in the existing outgoing-message flow. */
    @Test
    fun rateLimitNoticeRemainsOther() {
        val category = TwitchIrcNoticeClassifier.classify(
            msgId = "msg_rate-limit",
            message = "You are sending messages too quickly"
        )

        assertEquals(
            TwitchIrcNoticeCategory.OTHER,
            category
        )
    }
}
