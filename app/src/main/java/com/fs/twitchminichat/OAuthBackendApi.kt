package com.fs.twitchminichat

import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Account credentials returned by a successful OAuth finalization. */
data class OAuthFinalizeResult(
    val profileId: String,
    val slot: Int,
    val username: String,
    val userId: String,
    val accessToken: String,
    val desktopSessionToken: String
)

/** Twitch credentials returned for one Internet Relay Chat connection. */
data class OAuthTokenForIrcResult(
    val profileId: String,
    val username: String,
    val userId: String,
    val accessToken: String
)

/** Result returned by the backend IRC-token endpoint. */
sealed interface BackendIrcTokenApiResult {

    /** A successful response containing parsed IRC credentials. */
    data class Success(val value: OAuthTokenForIrcResult) : BackendIrcTokenApiResult

    /**
     * A completed HTTP response outside the successful status range.
     *
     * [retryAfterSeconds] is its Retry-After header, when that header held a valid
     * number of seconds ([RetryAfterHeader]); null otherwise.
     */
    data class HttpError(
        val statusCode: Int,
        val retryAfterSeconds: Long? = null
    ) : BackendIrcTokenApiResult

    /**
     * An input/output failure: no connectivity, a name that does not resolve, a
     * timeout, a reset connection, or a failed Transport Layer Security (TLS) handshake.
     */
    data object NetworkError : BackendIrcTokenApiResult

    /** An answer that is not a usable response; [reason] says how. */
    data class MalformedResponse(
        val reason: BackendIrcTokenMalformedReason
    ) : BackendIrcTokenApiResult

    /**
     * An exception from the request that is not an input/output failure, such as a
     * programming error. [exceptionClass] is its class name, which carries no data.
     */
    data class UnexpectedError(val exceptionClass: String) : BackendIrcTokenApiResult
}

/** How an answer from the IRC-token endpoint failed to be a usable response. */
enum class BackendIrcTokenMalformedReason(
    /** Value written in log lines. */
    val logValue: String
) {

    /** A 2xx response with nothing in its body, for example a response cut short. */
    EMPTY_BODY("empty_body"),

    /** A 2xx response whose body is not the expected JSON, such as a Wi-Fi login page. */
    UNPARSABLE_BODY("unparsable_body"),

    /** No valid HTTP status could be read: the connection reported -1, or a code outside 100-599. */
    INVALID_STATUS("invalid_status")
}

/**
 * Reads an HTTP Retry-After header in its delta-seconds form.
 *
 * The HTTP-date form is not read: this backend sends seconds, and a date would need
 * a clock. A header in that form, or missing, or malformed, reads as null, and the
 * caller falls back to its normal backoff.
 */
object RetryAfterHeader {

    /**
     * Longest wait honoured, in seconds: one day. A larger value is read as this one,
     * which keeps the delay inside what a main-thread handler can post without
     * overflowing its uptime arithmetic.
     */
    const val MAX_SECONDS: Long = 86_400L

    /** Returns the header's non-negative number of seconds, capped at [MAX_SECONDS], or null. */
    fun parseSeconds(value: String?): Long? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty() || !text.all { it in '0'..'9' }) return null

        // All digits, but too many for a Long: a valid and very long wait.
        val seconds = text.toLongOrNull() ?: return MAX_SECONDS
        return seconds.coerceAtMost(MAX_SECONDS)
    }
}

/**
 * Turns what the IRC-token request produced into a [BackendIrcTokenApiResult],
 * without any network access, so that every case can be tested.
 */
internal object TokenForIrcResponseInterpreter {

    /**
     * Interprets one completed exchange: [statusCode] as the connection reported it,
     * [body] read from the input or error stream, and the raw [retryAfterHeader].
     */
    fun interpret(
        statusCode: Int,
        body: String,
        retryAfterHeader: String?
    ): BackendIrcTokenApiResult {
        if (statusCode !in 100..599) {
            return BackendIrcTokenApiResult.MalformedResponse(
                BackendIrcTokenMalformedReason.INVALID_STATUS
            )
        }

        if (statusCode !in 200..299) {
            return BackendIrcTokenApiResult.HttpError(
                statusCode = statusCode,
                retryAfterSeconds = RetryAfterHeader.parseSeconds(retryAfterHeader)
            )
        }

        if (body.isBlank()) {
            return BackendIrcTokenApiResult.MalformedResponse(
                BackendIrcTokenMalformedReason.EMPTY_BODY
            )
        }

        val parsed = OAuthBackendResponseParser.parseTokenForIrc(body)
            ?: return BackendIrcTokenApiResult.MalformedResponse(
                BackendIrcTokenMalformedReason.UNPARSABLE_BODY
            )

        return BackendIrcTokenApiResult.Success(parsed)
    }

    /** Interprets an exception thrown before an exchange completed. */
    fun interpret(error: Exception): BackendIrcTokenApiResult {
        if (error is IOException) {
            return BackendIrcTokenApiResult.NetworkError
        }

        val exceptionClass = error.javaClass.simpleName.ifBlank { error.javaClass.name }
        return BackendIrcTokenApiResult.UnexpectedError(exceptionClass)
    }
}

/** Narrow backend contract used by [BackendIrcTokenProvider]. */
fun interface BackendIrcTokenApi {

    /** Performs exactly one request with the authentication decision made by the caller. */
    fun tokenForIrc(
        profileId: String,
        authorizationHeader: String
    ): BackendIrcTokenApiResult
}

/** Parses OAuth backend responses without retaining raw sensitive payloads. */
object OAuthBackendResponseParser {

    /** Parses a successful `/oauth/finalize` response. */
    fun parseFinalize(body: String): OAuthFinalizeResult? {
        if (body.isBlank()) return null

        return runCatching {
            val json = JSONObject(body)
            OAuthFinalizeResult(
                profileId = json.optString("profile_id", "").trim(),
                slot = json.optInt("slot", -1),
                username = json.optString("username", "").trim(),
                userId = json.optString("user_id", "").trim(),
                accessToken = json.optString("access_token", "").trim(),
                desktopSessionToken = json.optString("desktop_session_token", "").trim()
            )
        }.getOrNull()
    }

    /** Parses a successful `/oauth/token_for_irc` response. */
    fun parseTokenForIrc(body: String): OAuthTokenForIrcResult? {
        if (body.isBlank()) return null

        return runCatching {
            val json = JSONObject(body)
            OAuthTokenForIrcResult(
                profileId = json.optString("profile_id", "").trim(),
                username = json.optString("username", "").trim(),
                userId = json.optString("user_id", "").trim(),
                accessToken = json.optString("access_token", "").trim()
            )
        }.getOrNull()
    }
}

/** Builds validated OAuth request bodies without logging their secrets. */
internal object OAuthBackendRequestBuilder {

    /** Builds the proof-bound `/oauth/finalize` body, or null for invalid input. */
    fun buildFinalize(loginToken: String, codeVerifier: String): String? {
        if (!OAuthFlowSecurityPolicy.isValidLoginToken(loginToken)) return null
        if (!OAuthProofKeyPolicy.isValidCodeVerifier(codeVerifier)) return null

        return JSONObject().apply {
            put("login_token", loginToken)
            put("code_verifier", codeVerifier)
        }.toString()
    }
}

/** HTTP client for the TMC OAuth backend. */
object OAuthBackendApi : BackendIrcTokenApi {

    /** Trusted Transport Layer Security (TLS) endpoint for backend OAuth requests. */
    private const val BASE_URL = "https://api.ircminichat.party"

    /** Exchanges a one-time login token and its mandatory verifier for credentials. */
    fun finalizeLogin(loginToken: String, codeVerifier: String): OAuthFinalizeResult? {
        val requestBody = OAuthBackendRequestBuilder.buildFinalize(
            loginToken = loginToken,
            codeVerifier = codeVerifier
        ) ?: return null

        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$BASE_URL/oauth/finalize")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
            conn = connection

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val code = connection.responseCode
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (code !in 200..299 || body.isBlank()) {
                null
            } else {
                OAuthBackendResponseParser.parseFinalize(body)
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** Acquires fresh IRC credentials with a mandatory preselected Bearer header. */
    override fun tokenForIrc(
        profileId: String,
        authorizationHeader: String
    ): BackendIrcTokenApiResult {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$BASE_URL/oauth/token_for_irc")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", authorizationHeader)
            }
            conn = connection

            val requestJson = JSONObject().apply {
                put("profile_id", profileId)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val code = connection.responseCode
            val body = when (code) {
                in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
                in 100..599 -> connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                else -> ""
            }

            TokenForIrcResponseInterpreter.interpret(
                statusCode = code,
                body = body,
                retryAfterHeader = connection.getHeaderField("Retry-After")
            )
        } catch (error: Exception) {
            TokenForIrcResponseInterpreter.interpret(error)
        } finally {
            conn?.disconnect()
        }
    }
}
