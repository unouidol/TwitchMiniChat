package com.fs.twitchminichat.v3

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data ?: run { finish(); return }

        // ircminichatv3://auth#access_token=...&state=...
        val fragment = data.fragment ?: ""
        val fake = Uri.parse("ircminichatv3://auth?$fragment")

        val token = fake.getQueryParameter("access_token")?.trim().orEmpty()
        val state = fake.getQueryParameter("state")?.trim().orEmpty()

        val pendingId = extractPendingId(state)

        if (token.isBlank() || pendingId.isBlank()) {
            Toast.makeText(this, "Token/state mancanti", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val channel = loadPendingChannel(pendingId).ifBlank { "unouidol" }

        thread {
            val login = fetchLoginFromValidate(token) ?: "unknown"

            val repo = AccountRepository(this)

            // upsert: se @login + #channel già esiste, aggiorna token e RIUSA id
            val accountId = repo.upsert(
                username = login,
                channel = channel,
                accessToken = token
            )

            clearPendingChannel(pendingId)

            runOnUiThread {
                Toast.makeText(this, "Account: @$login (#$channel)", Toast.LENGTH_LONG).show()
            }

            // torna alla Main e chiedi di switchare sulla pagina dell'account appena aggiunto
            val i = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("new_account_id", accountId)
            }
            startActivity(i)
            finish()
        }
    }

    private fun extractPendingId(state: String): String {
        // atteso: v3-<pendingId>-<timestamp>
        val parts = state.split('-')
        return if (parts.size >= 3 && parts[0] == "v3") parts[1] else ""
    }

    private fun loadPendingChannel(pendingId: String): String {
        val prefs = getSharedPreferences("v3_pending", Context.MODE_PRIVATE)
        return prefs.getString("pending_channel_$pendingId", "") ?: ""
    }

    private fun clearPendingChannel(pendingId: String) {
        val prefs = getSharedPreferences("v3_pending", Context.MODE_PRIVATE)
        prefs.edit().remove("pending_channel_$pendingId").apply()
    }

    private fun fetchLoginFromValidate(token: String): String? {
        return try {
            val url = URL("https://id.twitch.tv/oauth2/validate")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("Authorization", "OAuth $token")
            }

            if (conn.responseCode != 200) {
                conn.disconnect()
                null
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                JSONObject(body).optString("login", null)
            }
        } catch (_: Exception) {
            null
        }
    }
}
