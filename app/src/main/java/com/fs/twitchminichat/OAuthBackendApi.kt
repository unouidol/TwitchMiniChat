package com.fs.twitchminichat

import org.json.JSONObject
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

    /** A completed HTTP response outside the successful status range. */
    data class HttpError(val statusCode: Int) : BackendIrcTokenApiResult

    /** A transport, timeout, or response-parsing failure. */
    data object NetworkError : BackendIrcTokenApiResult
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

/** HTTP client for the TMC OAuth backend. */
object OAuthBackendApi : BackendIrcTokenApi {

    /** Trusted Transport Layer Security (TLS) endpoint for backend OAuth requests. */
    private const val BASE_URL = "https://api.ircminichat.party"

    /** Exchanges a one-time login token for account and backend-session credentials. */
    fun finalizeLogin(loginToken: String): OAuthFinalizeResult? {
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

            val requestJson = JSONObject().apply {
                put("login_token", loginToken)
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestJson.toString())
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
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (code !in 200..299 || body.isBlank()) {
                BackendIrcTokenApiResult.HttpError(code)
            } else {
                val parsed = OAuthBackendResponseParser.parseTokenForIrc(body)
                    ?: return BackendIrcTokenApiResult.NetworkError
                BackendIrcTokenApiResult.Success(parsed)
            }
        } catch (_: Exception) {
            BackendIrcTokenApiResult.NetworkError
        } finally {
            conn?.disconnect()
        }
    }
}
