package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for one-shot Bearer versus legacy IRC token acquisition. */
class BackendIrcTokenProviderTest {

    /** A rejected Bearer session never triggers a second legacy request or local fallback. */
    @Test
    fun bearerAuthenticationErrorDoesNotFallbackToLegacy() {
        val api = RecordingTokenApi(
            result = BackendIrcTokenApiResult.HttpError(401)
        )
        val provider = BackendIrcTokenProvider(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Present("backend-session")
            },
            tokenApi = api
        )

        val result = provider.acquire(
            profileId = " Profile-A ",
            localAccessToken = "local-twitch-token",
            localUsername = "local-user"
        )

        assertEquals(BackendIrcTokenResult.ReauthorizationRequired, result)
        assertEquals(1, api.requestCount)
        assertEquals("profile-a", api.lastProfileId)
        assertEquals("Bearer backend-session", api.lastAuthorizationHeader)
    }

    /** HTTP 403 is treated as a rejected session and also remains one-shot. */
    @Test
    fun forbiddenBearerSessionRequiresReauthorizationWithoutRetry() {
        val api = RecordingTokenApi(
            result = BackendIrcTokenApiResult.HttpError(403)
        )
        val provider = BackendIrcTokenProvider(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Present("backend-session")
            },
            tokenApi = api
        )

        val result = provider.acquire(
            profileId = "profile-a",
            localAccessToken = "local-twitch-token",
            localUsername = "local-user"
        )

        assertEquals(BackendIrcTokenResult.ReauthorizationRequired, result)
        assertEquals(1, api.requestCount)
    }

    /** A missing backend session uses local Twitch credentials without a backend request. */
    @Test
    fun missingSessionUsesLocalCredentialsWithoutBackendRequest() {
        val api = RecordingTokenApi(
            result = BackendIrcTokenApiResult.Success(
                OAuthTokenForIrcResult(
                    profileId = "profile-a",
                    username = "fresh-user",
                    userId = "user-id",
                    accessToken = "fresh-token"
                )
            )
        )
        val provider = BackendIrcTokenProvider(
            sessionReader = BackendSessionReader { BackendSessionLookup.Missing },
            tokenApi = api
        )

        val result = provider.acquire(
            profileId = "profile-a",
            localAccessToken = "local-token",
            localUsername = "local-user"
        )

        assertTrue(result is BackendIrcTokenResult.Success)
        assertEquals(
            BackendIrcTokenResult.Success(
                accessToken = "local-token",
                username = "local-user"
            ),
            result
        )
        assertEquals(0, api.requestCount)
        assertEquals(null, api.lastAuthorizationHeader)
    }

    /** Records every API invocation so tests can prove that no retry occurred. */
    private class RecordingTokenApi(
        private val result: BackendIrcTokenApiResult
    ) : BackendIrcTokenApi {

        /** Number of backend requests made by the provider. */
        var requestCount: Int = 0
            private set

        /** Normalized profile identifier from the latest request. */
        var lastProfileId: String? = null
            private set

        /** Authorization header from the latest request, or null for legacy mode. */
        var lastAuthorizationHeader: String? = null
            private set

        /** Returns the configured result while recording the one request. */
        override fun tokenForIrc(
            profileId: String,
            authorizationHeader: String
        ): BackendIrcTokenApiResult {
            requestCount += 1
            lastProfileId = profileId
            lastAuthorizationHeader = authorizationHeader
            return result
        }
    }
}
