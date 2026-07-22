package com.fs.twitchminichat

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Outcome of one read-only Twitch emote catalog request. */
sealed interface TwitchEmoteCatalogLoadResult {
    /** Contains the complete paginated catalog. */
    data class Success(val catalog: TwitchEmoteCatalog) : TwitchEmoteCatalogLoadResult

    /** The current access token was authorized without `user:read:emotes`. */
    data object MissingScope : TwitchEmoteCatalogLoadResult

    /** The catalog could not be loaded; no outgoing chat action is retried. */
    data class Failed(val httpStatus: Int? = null) : TwitchEmoteCatalogLoadResult
}

/** Loads an account's emotes from official Twitch read-only endpoints. */
fun interface TwitchEmoteCatalogLoader {
    /** Performs one catalog refresh for an optional current broadcaster. */
    fun load(
        accessToken: String,
        broadcasterId: String?
    ): TwitchEmoteCatalogLoadResult
}

/** Official Helix implementation used by the Android chat screen. */
object TwitchEmoteCatalogApi : TwitchEmoteCatalogLoader {

    override fun load(
        accessToken: String,
        broadcasterId: String?
    ): TwitchEmoteCatalogLoadResult {
        if (accessToken.isBlank()) return TwitchEmoteCatalogLoadResult.Failed()

        val tokenMetadata = validateToken(accessToken)
            ?: return TwitchEmoteCatalogLoadResult.Failed()

        if (EMOTE_SCOPE !in tokenMetadata.scopes) {
            return TwitchEmoteCatalogLoadResult.MissingScope
        }

        val entries = mutableListOf<TwitchEmoteCatalogEntry>()
        val visitedCursors = mutableSetOf<String>()
        var cursor: String? = null
        var pageCount = 0

        do {
            pageCount += 1
            if (pageCount > MAX_PAGES) {
                return TwitchEmoteCatalogLoadResult.Failed()
            }

            val url = buildCatalogUrl(
                userId = tokenMetadata.userId,
                broadcasterId = broadcasterId,
                cursor = cursor
            )

            val response = readJson(
                url = url,
                authorization = "Bearer $accessToken",
                clientId = tokenMetadata.clientId
            )

            if (
                response.status == null ||
                response.status !in 200..299 ||
                response.body.isBlank()
            ) {
                return TwitchEmoteCatalogLoadResult.Failed(response.status)
            }

            val json = try {
                JSONObject(response.body)
            } catch (_: Exception) {
                return TwitchEmoteCatalogLoadResult.Failed(response.status)
            }

            val data = json.optJSONArray("data")
            if (data != null) {
                for (index in 0 until data.length()) {
                    val item = data.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    val name = item.optString("name").trim()
                    if (id.isBlank() || name.isBlank()) continue

                    val formats = LinkedHashSet<String>()
                    val rawFormats = item.optJSONArray("format")
                    if (rawFormats != null) {
                        for (formatIndex in 0 until rawFormats.length()) {
                            rawFormats.optString(formatIndex)
                                .trim()
                                .takeIf { it.isNotBlank() }
                                ?.let(formats::add)
                        }
                    }

                    entries += TwitchEmoteCatalogEntry(
                        id = id,
                        name = name,
                        ownerId = item.optString("owner_id").trim(),
                        emoteType = item.optString("emote_type").trim(),
                        formats = formats
                    )
                }
            }

            cursor = json
                .optJSONObject("pagination")
                ?.optString("cursor")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (cursor != null && !visitedCursors.add(cursor)) {
                return TwitchEmoteCatalogLoadResult.Failed()
            }
        } while (cursor != null)

        return TwitchEmoteCatalogLoadResult.Success(
            TwitchEmoteCatalog(
                broadcasterId = broadcasterId?.trim()?.takeIf { it.isNotBlank() },
                fetchedAtMs = System.currentTimeMillis(),
                entries = entries.distinctBy { entry -> entry.id to entry.name }
            )
        )
    }

    /** Reads the token owner, client ID, and granted scopes without exposing the token. */
    private fun validateToken(accessToken: String): TokenMetadata? {
        val response = readJson(
            url = URL(TOKEN_VALIDATION_URL),
            authorization = "OAuth $accessToken",
            clientId = null
        )

        if (
            response.status == null ||
            response.status !in 200..299 ||
            response.body.isBlank()
        ) {
            return null
        }

        return try {
            val json = JSONObject(response.body)
            val clientId = json.optString("client_id").trim()
            val userId = json.optString("user_id").trim()
            if (clientId.isBlank() || userId.isBlank()) return null

            val scopes = LinkedHashSet<String>()
            val rawScopes = json.optJSONArray("scopes")
            if (rawScopes != null) {
                for (index in 0 until rawScopes.length()) {
                    rawScopes.optString(index)
                        .trim()
                        .takeIf { it.isNotBlank() }
                        ?.let(scopes::add)
                }
            }

            TokenMetadata(
                clientId = clientId,
                userId = userId,
                scopes = scopes
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Builds one paginated Get User Emotes URL using encoded query values. */
    private fun buildCatalogUrl(
        userId: String,
        broadcasterId: String?,
        cursor: String?
    ): URL {
        val query = mutableListOf("user_id=${encode(userId)}")
        broadcasterId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { query += "broadcaster_id=${encode(it)}" }
        cursor
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { query += "after=${encode(it)}" }

        return URL("$USER_EMOTES_URL?${query.joinToString("&")}")
    }

    /** Performs one HTTP GET and always closes the connection. */
    private fun readJson(
        url: URL,
        authorization: String,
        clientId: String?
    ): HttpResponse {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", authorization)
            if (!clientId.isNullOrBlank()) {
                setRequestProperty("Client-Id", clientId)
            }
        }

        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            HttpResponse(status = status, body = body)
        } catch (_: Exception) {
            HttpResponse(status = null, body = "")
        } finally {
            connection.disconnect()
        }
    }

    /** Encodes one query parameter without changing request semantics. */
    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private data class TokenMetadata(
        val clientId: String,
        val userId: String,
        val scopes: Set<String>
    )

    private data class HttpResponse(
        val status: Int?,
        val body: String
    )

    private const val EMOTE_SCOPE = "user:read:emotes"
    private const val TOKEN_VALIDATION_URL = "https://id.twitch.tv/oauth2/validate"
    private const val USER_EMOTES_URL = "https://api.twitch.tv/helix/chat/emotes/user"
    private const val HTTP_TIMEOUT_MS = 10_000
    private const val MAX_PAGES = 100
}
