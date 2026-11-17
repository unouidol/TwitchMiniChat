package com.fs.twitchminichat

import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How one exchange with the IRC-token endpoint becomes an API result, without a network. */
class TokenForIrcResponseInterpreterTest {

    /** A complete JSON answer is a success. */
    @Test
    fun aCompleteAnswerIsASuccess() {
        val result = TokenForIrcResponseInterpreter.interpret(
            statusCode = 200,
            body = """{"profile_id":"p","username":"u","user_id":"1","access_token":"t"}""",
            retryAfterHeader = null
        )

        assertEquals(
            BackendIrcTokenApiResult.Success(
                OAuthTokenForIrcResult(profileId = "p", username = "u", userId = "1", accessToken = "t")
            ),
            result
        )
    }

    /** An empty 2xx is malformed, not an HTTP error with a 2xx code. */
    @Test
    fun anEmptySuccessIsMalformed() {
        assertEquals(
            BackendIrcTokenApiResult.MalformedResponse(BackendIrcTokenMalformedReason.EMPTY_BODY),
            TokenForIrcResponseInterpreter.interpret(statusCode = 200, body = "  \n", retryAfterHeader = null)
        )
    }

    /** A 2xx that is not the expected JSON, such as a Wi-Fi login page, is malformed. */
    @Test
    fun aNonJsonSuccessIsMalformed() {
        assertEquals(
            BackendIrcTokenApiResult.MalformedResponse(BackendIrcTokenMalformedReason.UNPARSABLE_BODY),
            TokenForIrcResponseInterpreter.interpret(
                statusCode = 200,
                body = "<html><body>Sign in to this network</body></html>",
                retryAfterHeader = null
            )
        )
    }

    /** -1 and codes outside 100-599 are not a valid status line. */
    @Test
    fun anUnreadableStatusIsMalformed() {
        for (status in listOf(-1, 0, 600)) {
            assertEquals(
                "status $status",
                BackendIrcTokenApiResult.MalformedResponse(BackendIrcTokenMalformedReason.INVALID_STATUS),
                TokenForIrcResponseInterpreter.interpret(statusCode = status, body = "", retryAfterHeader = "60")
            )
        }
    }

    /** A non-2xx is an HTTP error, with its Retry-After when valid. */
    @Test
    fun aNon2xxIsAnHttpErrorWithItsRetryAfter() {
        assertEquals(
            BackendIrcTokenApiResult.HttpError(statusCode = 429, retryAfterSeconds = 60),
            TokenForIrcResponseInterpreter.interpret(
                statusCode = 429,
                body = """{"ok":false,"error":"rate_limited"}""",
                retryAfterHeader = "60"
            )
        )
        assertEquals(
            BackendIrcTokenApiResult.HttpError(statusCode = 503, retryAfterSeconds = null),
            TokenForIrcResponseInterpreter.interpret(statusCode = 503, body = "", retryAfterHeader = null)
        )
    }

    /** Every input/output failure is a network error, the TLS handshake included. */
    @Test
    fun ioFailuresAreNetworkErrors() {
        val failures = listOf(
            UnknownHostException("no host"),
            ConnectException("refused"),
            SocketTimeoutException("timeout"),
            SocketException("Connection reset"),
            SSLHandshakeException("handshake"),
            IOException("other")
        )

        for (failure in failures) {
            assertEquals(
                failure.javaClass.simpleName,
                BackendIrcTokenApiResult.NetworkError,
                TokenForIrcResponseInterpreter.interpret(failure)
            )
        }
    }

    /** Any other exception is unexpected, named by its class only. */
    @Test
    fun otherExceptionsAreUnexpected() {
        assertEquals(
            BackendIrcTokenApiResult.UnexpectedError("IllegalStateException"),
            TokenForIrcResponseInterpreter.interpret(IllegalStateException("message with details"))
        )
        assertEquals(
            BackendIrcTokenApiResult.UnexpectedError("SecurityException"),
            TokenForIrcResponseInterpreter.interpret(SecurityException("permission"))
        )
    }

    /** Retry-After is read in seconds only. */
    @Test
    fun retryAfterIsReadInSecondsOnly() {
        assertEquals(60L, RetryAfterHeader.parseSeconds("60"))
        assertEquals(0L, RetryAfterHeader.parseSeconds("0"))
        assertEquals(120L, RetryAfterHeader.parseSeconds(" 120 "))
        assertNull(RetryAfterHeader.parseSeconds(null))
        assertNull(RetryAfterHeader.parseSeconds(""))
        assertNull(RetryAfterHeader.parseSeconds("-5"))
        assertNull(RetryAfterHeader.parseSeconds("1.5"))
        assertNull(RetryAfterHeader.parseSeconds("soon"))
        assertNull(RetryAfterHeader.parseSeconds("Wed, 21 Oct 2026 07:28:00 GMT"))
    }

    /** A very long wait is capped at one day, never overflowing. */
    @Test
    fun retryAfterIsCappedAtOneDay() {
        assertEquals(RetryAfterHeader.MAX_SECONDS, RetryAfterHeader.parseSeconds("86401"))
        assertEquals(RetryAfterHeader.MAX_SECONDS, RetryAfterHeader.parseSeconds("99999999999999999999999"))
    }
}
