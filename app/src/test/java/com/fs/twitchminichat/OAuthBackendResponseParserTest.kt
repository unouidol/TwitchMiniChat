package com.fs.twitchminichat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    /** Finalization always carries the verifier required by the hardened backend. */
    @Test
    fun finalizeRequestIncludesMandatoryCodeVerifier() {
        val loginToken = "opaque-login-token"
        val codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

        val body = OAuthBackendRequestBuilder.buildFinalize(loginToken, codeVerifier)
        val json = JSONObject(body.orEmpty())

        assertEquals(2, json.length())
        assertEquals(loginToken, json.getString("login_token"))
        assertEquals(codeVerifier, json.getString("code_verifier"))
    }

    /** Missing or malformed proof cannot fall back to token-only finalization. */
    @Test
    fun finalizeRequestRejectsInvalidCodeVerifier() {
        assertNull(OAuthBackendRequestBuilder.buildFinalize("opaque-login-token", ""))
        assertNull(
            OAuthBackendRequestBuilder.buildFinalize(
                "opaque-login-token",
                "dBjftJeZ4CVP+mB92K27uhbUJU1p1r/wW1gFWFOEjXk="
            )
        )
    }
}
