package com.fs.twitchminichat.v2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread

class AuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(this, "Callback (v2_gecko) ✅", Toast.LENGTH_SHORT).show()

        val data = intent?.data ?: run { finish(); return }

        // Deve arrivare come: ircminichatv2gecko://auth#access_token=...&state=v2g-...
        val scheme = data.scheme.orEmpty()
        val fragment = data.fragment.orEmpty()

        // Parse robusto dei parametri nel fragment
        val fake = Uri.parse("${scheme}://auth?$fragment")
        val token = fake.getQueryParameter("access_token")?.trim().orEmpty()
        val state = fake.getQueryParameter("state")?.trim().orEmpty()

        val pendingId = extractPendingIdV2g(state)
        if (token.isBlank() || pendingId.isBlank()) {
            Toast.makeText(this, "Token/state non validi (v2g)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val channel = loadPendingChannelV2g(pendingId).ifBlank { "unouidol" }

        thread {
            val login = fetchLoginFromValidate(token) ?: "unknown"

            val repo = AccountRepository(this)
            val accountId = UUID.randomUUID().toString()
            sendBroadcast(Intent(MainActivity.ACTION_ACCOUNTS_CHANGED))

            repo.addAccount(
                AccountConfig(
                    id = accountId,
                    username = login,
                    channel = channel,
                    accessToken = token
                )
            )

            clearPendingChannelV2g(pendingId)

            runOnUiThread {
                Toast.makeText(this, "Added (gecko): @$login (#$channel)", Toast.LENGTH_LONG).show()
            }

            val i = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("new_account_id", accountId)
            }
            startActivity(i)
            finish()
        }
    }

    private fun extractPendingIdV2g(state: String): String {
        // atteso: "v2g-<pendingId>-<timestamp>"
        val parts = state.split('-')
        return if (parts.size >= 3 && parts[0] == "v2g") parts[1] else ""
    }

    private fun loadPendingChannelV2g(pendingId: String): String {
        val prefs = getSharedPreferences("v2g_pending", Context.MODE_PRIVATE)
        return prefs.getString("pending_channel_$pendingId", "") ?: ""
    }

    private fun clearPendingChannelV2g(pendingId: String) {
        val prefs = getSharedPreferences("v2g_pending", Context.MODE_PRIVATE)
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
