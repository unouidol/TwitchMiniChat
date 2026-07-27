package com.fs.twitchminichat

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One parsed chat row returned by the backend history endpoint. */
data class BackendHistoryMessage(
    val user: String,
    val text: String,
    val emotesRaw: String?,
    val messageId: String?,
    val timestampSec: Double
)

/** Result of one profile-scoped history request. */
sealed interface BackendHistoryResult {

    /** Parsed history rows returned by the backend. */
    data class Success(val messages: List<BackendHistoryMessage>) : BackendHistoryResult

    /** No backend session exists locally for the requested profile. */
    data object SessionRequired : BackendHistoryResult

    /** The backend rejected the stored session and requires manual reauthorization. */
    data object ReauthorizationRequired : BackendHistoryResult

    /** The request could not be completed or parsed safely. */
    data object Failed : BackendHistoryResult
}

/** Low-level result returned by the history Hypertext Transfer Protocol (HTTP) transport. */
sealed interface BackendHistoryTransportResult {

    /** Successful response body awaiting parsing. */
    data class Success(val body: String) : BackendHistoryTransportResult

    /** Completed HTTP response outside the successful status range. */
    data class HttpError(val statusCode: Int) : BackendHistoryTransportResult

    /** Transport, timeout, or response-reading failure. */
    data object NetworkError : BackendHistoryTransportResult
}

/** Narrow transport contract used to test session-only history authentication. */
fun interface BackendHistoryTransport {

    /** Performs exactly one authenticated history request. */
    fun load(
        channel: String,
        seconds: Int,
        authorizationHeader: String
    ): BackendHistoryTransportResult
}

/** Parses backend history responses without retaining their raw payload. */
object BackendHistoryResponseParser {

    /** Parses a complete JSON history array or returns null when it is malformed. */
    fun parse(body: String): List<BackendHistoryMessage>? {
        if (body.isBlank()) return null

        return runCatching {
            val array = JSONArray(body)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val text = item.optString("text", "")
                    if (text.isBlank()) continue

                    add(
                        BackendHistoryMessage(
                            user = item.optString("user", "unknown"),
                            text = text,
                            emotesRaw = item.optString("emotes", "")
                                .takeIf { value ->
                                    value.isNotBlank() && value != "null"
                                },
                            messageId = item.optString("id", "")
                                .takeIf { value ->
                                    value.isNotBlank() && value != "null"
                                },
                            timestampSec = item.optDouble("timestamp", 0.0)
                        )
                    )
                }
            }
        }.getOrNull()
    }
}

/**
 * Loads backend history only when a revocable session exists for the selected profile.
 *
 * A missing, unreadable, or rejected session never falls back to the shared legacy key.
 */
class BackendHistoryClient(
    private val sessionReader: BackendSessionReader,
    private val transport: BackendHistoryTransport = HttpBackendHistoryTransport
) {

    /** Loads and parses one bounded history window for a single profile. */
    fun load(
        profileId: String,
        channel: String,
        seconds: Int
    ): BackendHistoryResult {
        val normalizedProfileId = BackendSessionStore.normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            return BackendHistoryResult.SessionRequired
        }

        val authorizationHeader = when (
            val lookup = sessionReader.lookup(normalizedProfileId)
        ) {
            BackendSessionLookup.Missing -> {
                return BackendHistoryResult.SessionRequired
            }

            BackendSessionLookup.Unavailable -> {
                return BackendHistoryResult.Failed
            }

            is BackendSessionLookup.Present -> {
                BackendAuthHeaderProvider.bearerHeader(lookup.token)
                    ?: return BackendHistoryResult.Failed
            }
        }

        val normalizedChannel = channel.trim().removePrefix("#").lowercase()
        if (normalizedChannel.isBlank()) {
            return BackendHistoryResult.Failed
        }

        return when (
            val response = transport.load(
                channel = normalizedChannel,
                seconds = seconds.coerceIn(MIN_HISTORY_SECONDS, MAX_HISTORY_SECONDS),
                authorizationHeader = authorizationHeader
            )
        ) {
            is BackendHistoryTransportResult.Success -> {
                val messages = BackendHistoryResponseParser.parse(response.body)
                    ?: return BackendHistoryResult.Failed
                BackendHistoryResult.Success(messages)
            }

            is BackendHistoryTransportResult.HttpError -> {
                if (response.statusCode in AUTHENTICATION_ERROR_CODES) {
                    BackendHistoryResult.ReauthorizationRequired
                } else {
                    BackendHistoryResult.Failed
                }
            }

            BackendHistoryTransportResult.NetworkError -> BackendHistoryResult.Failed
        }
    }

    private companion object {

        /** Smallest accepted history window in seconds. */
        const val MIN_HISTORY_SECONDS = 30

        /** Largest accepted history window in seconds. */
        const val MAX_HISTORY_SECONDS = 3600

        /** HTTP statuses that explicitly reject a supplied Bearer session. */
        val AUTHENTICATION_ERROR_CODES = setOf(401, 403)
    }
}

/** Production HTTPS transport for the backend history endpoint. */
object HttpBackendHistoryTransport : BackendHistoryTransport {

    /** Performs one GET request with a mandatory Bearer Authorization header. */
    override fun load(
        channel: String,
        seconds: Int,
        authorizationHeader: String
    ): BackendHistoryTransportResult {
        var connection: HttpURLConnection? = null
        return try {
            val encodedChannel = URLEncoder.encode(channel, Charsets.UTF_8.name())
            val url = URL(
                "$BASE_URL/history?channel=$encodedChannel&seconds=$seconds"
            )

            val activeConnection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", authorizationHeader)
            }
            connection = activeConnection

            val statusCode = activeConnection.responseCode
            if (statusCode != HttpURLConnection.HTTP_OK) {
                BackendHistoryTransportResult.HttpError(statusCode)
            } else {
                val body = activeConnection.inputStream
                    .bufferedReader()
                    .use { reader -> reader.readText() }
                BackendHistoryTransportResult.Success(body)
            }
        } catch (_: Exception) {
            BackendHistoryTransportResult.NetworkError
        } finally {
            connection?.disconnect()
        }
    }

    /** Trusted Transport Layer Security (TLS) backend base URL. */
    private const val BASE_URL = "https://api.ircminichat.party"
}
