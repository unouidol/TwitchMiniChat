package com.fs.twitchminichat.v2

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class OAuthFinalizeResult(
    val profileId: String,
    val slot: Int,
    val username: String,
    val userId: String,
    val accessToken: String
)

data class OAuthTokenForIrcResult(
    val profileId: String,
    val username: String,
    val userId: String,
    val accessToken: String
)

object OAuthBackendApi {

    private const val BASE_URL = "https://api.ircminichat.party"

    fun finalizeLogin(loginToken: String): OAuthFinalizeResult? {
        return try {
            val url = URL("$BASE_URL/oauth/finalize")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val requestJson = JSONObject().apply {
                put("login_token", loginToken)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            conn.disconnect()

            if (code !in 200..299 || body.isBlank()) {
                null
            } else {
                val json = JSONObject(body)
                OAuthFinalizeResult(
                    profileId = json.optString("profile_id", ""),
                    slot = json.optInt("slot", -1),
                    username = json.optString("username", ""),
                    userId = json.optString("user_id", ""),
                    accessToken = json.optString("access_token", "")
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun tokenForIrc(profileId: String): OAuthTokenForIrcResult? {
        return try {
            val url = URL("$BASE_URL/oauth/token_for_irc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val requestJson = JSONObject().apply {
                put("profile_id", profileId)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            conn.disconnect()

            if (code !in 200..299 || body.isBlank()) {
                null
            } else {
                val json = JSONObject(body)
                OAuthTokenForIrcResult(
                    profileId = json.optString("profile_id", ""),
                    username = json.optString("username", ""),
                    userId = json.optString("user_id", ""),
                    accessToken = json.optString("access_token", "")
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}