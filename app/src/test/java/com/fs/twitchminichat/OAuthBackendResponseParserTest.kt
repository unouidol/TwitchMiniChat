package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Unit tests for OAuth backend response parsing. */
class OAuthBackendResponseParserTest {

    /** The revocable backend session is retained from `/oauth/finalize`. */
    @Test
    fun finalizeResponseParsesDesktopSessionToken() {
        val result = OAuthBackendResponseParser.parseFinalize(
            """
            {
              "profile_id": "profile-a",
              "slot": 2,
              "username": "example-user",
              "user_id": "12345",
              "access_token": "twitch-token",
              "desktop_session_token": "backend-session"
            }
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals("profile-a", result?.profileId)
        assertEquals(2, result?.slot)
        assertEquals("backend-session", result?.desktopSessionToken)
    }

    /** Backend string fields are normalized before security policy validation. */
    @Test
    fun finalizeResponseTrimsCanonicalFields() {
        val result = OAuthBackendResponseParser.parseFinalize(
            """
            {
              "profile_id": " profile-a ",
              "slot": 42,
              "username": " example-user ",
              "user_id": " 12345 ",
              "access_token": " twitch-token ",
              "desktop_session_token": " backend-session "
            }
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals("profile-a", result?.profileId)
        assertEquals("example-user", result?.username)
        assertEquals("12345", result?.userId)
        assertEquals("twitch-token", result?.accessToken)
        assertEquals("backend-session", result?.desktopSessionToken)
    }
}
