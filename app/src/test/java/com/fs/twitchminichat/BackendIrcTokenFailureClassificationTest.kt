package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the IRC token step classifies its failures (#115, defect F1).
 *
 * Until #115 every outcome other than a usable token or an HTTP 401/403 was the same
 * `BackendIrcTokenResult.Failed`, and the chat page scheduled no retry after it: a
 * network error while the phone was offline ended the reconnect cycle. These cases
 * were first written to pin that (commit `e56af0f`). Each now asserts the failure's
 * own cause and whether it is transient, as the owner classified them on 2026-11-17:
 * network, 5xx, 429 and malformed answers stay in the reconnect cycle; a missing or
 * unusable session, incomplete credentials, other 4xx and non-I/O exceptions do not.
 */
class BackendIrcTokenFailureClassificationTest {

    /** Was Failed. No connectivity, DNS, timeout or reset: transient. */
    @Test
    fun networkErrorIsTransient() {
        val failure = failureOf(BackendIrcTokenApiResult.NetworkError)

        assertEquals(BackendIrcTokenFailure.Network, failure)
        assertTrue(failure.isTransient)
        assertNull(failure.minimumRetryDelayMs)
    }

    /** Was Failed. Any 5xx, including Cloudflare's 52x: transient. */
    @Test
    fun serverErrorIsTransient() {
        for (status in listOf(500, 502, 503, 504, 520, 599)) {
            val failure = failureOf(BackendIrcTokenApiResult.HttpError(status))

            assertEquals("status $status", BackendIrcTokenFailure.ServerError(status), failure)
            assertTrue("status $status", failure.isTransient)
        }
    }

    /** Was Failed. A rate limit is transient, and its Retry-After becomes the minimum wait. */
    @Test
    fun rateLimitIsTransientAndCarriesRetryAfter() {
        val failure = failureOf(BackendIrcTokenApiResult.HttpError(429, retryAfterSeconds = 60))

        assertEquals(BackendIrcTokenFailure.RateLimited(60), failure)
        assertTrue(failure.isTransient)
        assertEquals(60_000L, failure.minimumRetryDelayMs)
    }

    /** A rate limit without a valid Retry-After leaves the normal backoff in charge. */
    @Test
    fun rateLimitWithoutRetryAfterHasNoMinimumWait() {
        val failure = failureOf(BackendIrcTokenApiResult.HttpError(429))

        assertTrue(failure.isTransient)
        assertNull(failure.minimumRetryDelayMs)
    }

    /** Was Failed, unchanged in kind. Definitive answers other than 401/403/429/5xx stop. */
    @Test
    fun otherDefinitiveAnswersAreNotTransient() {
        for (status in listOf(100, 301, 302, 400, 404, 405, 408, 409, 410, 426, 451, 499)) {
            val failure = failureOf(BackendIrcTokenApiResult.HttpError(status))

            assertEquals("status $status", BackendIrcTokenFailure.Rejected(status), failure)
            assertFalse("status $status", failure.isTransient)
        }
    }

    /** 401/403 stay a rejected session, never a failure the page would retry. */
    @Test
    fun rejectedSessionIsStillReauthorization() {
        for (status in listOf(401, 403)) {
            assertEquals(
                "status $status",
                BackendIrcTokenResult.ReauthorizationRequired,
                acquireWith(BackendIrcTokenApiResult.HttpError(status, retryAfterSeconds = 5))
            )
        }
    }

    /**
     * Was Failed (an empty 2xx used to arrive as `HttpError(200)`). Empty, unparsable and
     * status-less answers are malformed responses, and transient.
     */
    @Test
    fun malformedResponsesAreTransient() {
        for (reason in BackendIrcTokenMalformedReason.entries) {
            val failure = failureOf(BackendIrcTokenApiResult.MalformedResponse(reason))

            assertEquals(BackendIrcTokenFailure.MalformedResponse(reason), failure)
            assertTrue(reason.name, failure.isTransient)
        }
    }

    /** A status code no valid status line can carry is a malformed response too. */
    @Test
    fun anImpossibleStatusIsMalformed() {
        for (status in listOf(-1, 0, 99, 600, 999)) {
            assertEquals(
                "status $status",
                BackendIrcTokenFailure.MalformedResponse(BackendIrcTokenMalformedReason.INVALID_STATUS),
                failureOf(BackendIrcTokenApiResult.HttpError(status))
            )
        }
    }

    /** Was Failed, unchanged in kind. A 2xx whose JSON lacks the token stops. */
    @Test
    fun blankCredentialsAreNotTransient() {
        val failure = failureOf(
            BackendIrcTokenApiResult.Success(
                OAuthTokenForIrcResult(
                    profileId = "profile-a",
                    username = "fresh-user",
                    userId = "user-id",
                    accessToken = " "
                )
            )
        )

        assertEquals(BackendIrcTokenFailure.IncompleteCredentials, failure)
        assertFalse(failure.isTransient)
    }

    /** Was counted as a network error. A non-I/O exception has its own cause, and stops. */
    @Test
    fun unexpectedExceptionIsNotTransient() {
        val failure = failureOf(BackendIrcTokenApiResult.UnexpectedError("IllegalStateException"))

        assertEquals(BackendIrcTokenFailure.UnexpectedException("IllegalStateException"), failure)
        assertFalse(failure.isTransient)
    }

    /** Was Failed, unchanged in kind. An unreadable stored session stops, before any request. */
    @Test
    fun unavailableSessionIsNotTransientAndSendsNoRequest() {
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

        assertEquals(BackendIrcTokenResult.Failed(BackendIrcTokenFailure.SessionUnavailable), result)
        assertFalse(BackendIrcTokenFailure.SessionUnavailable.isTransient)
        assertEquals(0, requests)
    }

    /** No session and no complete local credentials stops, without a request. */
    @Test
    fun missingLocalCredentialsAreNotTransient() {
        val provider = BackendIrcTokenProvider(
            sessionReader = BackendSessionReader { BackendSessionLookup.Missing },
            tokenApi = BackendIrcTokenApi { _, _ -> error("no request expected") }
        )

        val result = provider.acquire(
            profileId = "profile-a",
            localAccessToken = " ",
            localUsername = "local-user"
        )

        assertEquals(BackendIrcTokenResult.Failed(BackendIrcTokenFailure.LocalCredentialsMissing), result)
        assertFalse(BackendIrcTokenFailure.LocalCredentialsMissing.isTransient)
    }

    /** Log fields carry the result class and codes, and nothing from the request. */
    @Test
    fun logFieldsCarryNoTokenAccountOrChannel() {
        val success = BackendIrcTokenResult.Success(accessToken = "secret-token", username = "some-user")

        assertEquals("result=success", success.logFields)
        assertEquals(
            "result=transient cause=rate_limited retryAfterSec=60",
            BackendIrcTokenResult.Failed(BackendIrcTokenFailure.RateLimited(60)).logFields
        )
        assertEquals(
            "result=transient cause=network",
            BackendIrcTokenResult.Failed(BackendIrcTokenFailure.Network).logFields
        )
        assertEquals(
            "result=not_transient cause=rejected status=404",
            BackendIrcTokenResult.Failed(BackendIrcTokenFailure.Rejected(404)).logFields
        )
        assertEquals(
            "result=not_transient cause=unexpected_exception exception=IllegalStateException",
            BackendIrcTokenResult.Failed(
                BackendIrcTokenFailure.UnexpectedException("IllegalStateException")
            ).logFields
        )
        assertEquals("result=reauthorization_required", BackendIrcTokenResult.ReauthorizationRequired.logFields)
    }

    /** Returns the failure one acquisition produced; fails the test if it did not fail. */
    private fun failureOf(apiResult: BackendIrcTokenApiResult): BackendIrcTokenFailure {
        val result = acquireWith(apiResult)
        return (result as? BackendIrcTokenResult.Failed)?.failure
            ?: throw AssertionError("expected a failure, got $result")
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
