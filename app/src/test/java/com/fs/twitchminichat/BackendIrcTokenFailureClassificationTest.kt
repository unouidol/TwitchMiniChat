package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterises how the IRC token step reports its failures (#115, defect F1).
 *
 * Every outcome other than a usable token or an HTTP 401/403 becomes the same
 * [BackendIrcTokenResult.Failed]. A network error while the phone is offline is
 * therefore indistinguishable from a definitive refusal, and the chat page, which
 * schedules no retry after [BackendIrcTokenResult.Failed], stops reconnecting.
 */
class BackendIrcTokenFailureClassificationTest {

    /** No connectivity, DNS, timeout or reset: today the same Failed as a refusal. */
    @Test
    fun networkErrorIsFailed() {
        assertEquals(
            BackendIrcTokenResult.Failed,
            acquireWith(BackendIrcTokenApiResult.NetworkError)
        )
    }

    /** A server error is not told apart either. */
    @Test
    fun serverErrorIsFailed() {
        for (status in listOf(500, 502, 503)) {
            assertEquals(
                "status $status",
                BackendIrcTokenResult.Failed,
                acquireWith(BackendIrcTokenApiResult.HttpError(status))
            )
        }
    }

    /** A rate limit is not told apart either. */
    @Test
    fun rateLimitIsFailed() {
        assertEquals(
            BackendIrcTokenResult.Failed,
            acquireWith(BackendIrcTokenApiResult.HttpError(429))
        )
    }

    /** A definitive refusal other than 401/403. */
    @Test
    fun profileNotFoundIsFailed() {
        assertEquals(
            BackendIrcTokenResult.Failed,
            acquireWith(BackendIrcTokenApiResult.HttpError(404))
        )
    }

    /** A 2xx with an empty body is reported by the client as an HTTP error with its 2xx code. */
    @Test
    fun emptySuccessBodyIsFailed() {
        assertEquals(
            BackendIrcTokenResult.Failed,
            acquireWith(BackendIrcTokenApiResult.HttpError(200))
        )
    }

    /** A 2xx whose JSON lacks the token is refused locally. */
    @Test
    fun blankCredentialsAreFailed() {
        assertEquals(
            BackendIrcTokenResult.Failed,
            acquireWith(
                BackendIrcTokenApiResult.Success(
                    OAuthTokenForIrcResult(
                        profileId = "profile-a",
                        username = "fresh-user",
                        userId = "user-id",
                        accessToken = " "
                    )
                )
            )
        )
    }

    /** An unreadable stored session fails before any request. */
    @Test
    fun unavailableSessionIsFailedWithoutRequest() {
        var requests = 0
        val provider = BackendIrcTokenProvider(
            sessionReader = BackendSessionReader { BackendSessionLookup.Unavailable },
            tokenApi = BackendIrcTokenApi { _, _ ->
                requests += 1
                BackendIrcTokenApiResult.NetworkError
            }
        )

        val result = provider.acquire(
            profileId = "profile-a",
            localAccessToken = "local-token",
            localUsername = "local-user"
        )

        assertEquals(BackendIrcTokenResult.Failed, result)
        assertEquals(0, requests)
    }

    /** Runs one acquisition against a stored session and a fixed API answer. */
    private fun acquireWith(apiResult: BackendIrcTokenApiResult): BackendIrcTokenResult {
        val provider = BackendIrcTokenProvider(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Present("backend-session")
            },
            tokenApi = BackendIrcTokenApi { _, _ -> apiResult }
        )

        return provider.acquire(
            profileId = "profile-a",
            localAccessToken = "local-token",
            localUsername = "local-user"
        )
    }
}
