package com.fs.twitchminichat.v2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID
import kotlin.concurrent.thread
import androidx.core.content.edit

class AuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data ?: run {
            finish()
            return
        }

        // Nuovo formato atteso:
        // ircminichatv2gecko://auth?login_token=...&slot=...&profile_id=...
        val loginToken = data.getQueryParameter("login_token")?.trim().orEmpty()
        val slot = data.getQueryParameter("slot")?.trim()?.toIntOrNull()
        val deepLinkProfileId = data.getQueryParameter("profile_id")?.trim().orEmpty()

        if (loginToken.isBlank() || slot == null) {
            Toast.makeText(this, "Login callback non valido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val channel = loadPendingChannelForSlot(slot)
            .trim()
            .removePrefix("#")
            .lowercase()

        if (channel.isBlank()) {
            Toast.makeText(
                this,
                "Canale mancante per questo login. Torna indietro e reinserisci il canale Twitch.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        Toast.makeText(this, "Login in corso...", Toast.LENGTH_SHORT).show()

        thread {
            val result = OAuthBackendApi.finalizeLogin(loginToken)

            if (
                result == null ||
                result.username.isBlank() ||
                result.accessToken.isBlank()
            ) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Finalize login fallito",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                return@thread
            }

            val finalProfileId = when {
                result.profileId.isNotBlank() -> result.profileId
                deepLinkProfileId.isNotBlank() -> deepLinkProfileId
                else -> ""
            }

            val repo = AccountRepository(this)
            val accountId = UUID.randomUUID().toString()

            repo.addAccount(
                AccountConfig(
                    id = accountId,
                    username = result.username,
                    channel = channel,
                    accessToken = result.accessToken,
                    profileId = finalProfileId
                )
            )

            clearPendingChannelForSlot(slot)

            runOnUiThread {
                sendBroadcast(Intent(MainActivity.ACTION_ACCOUNTS_CHANGED))

                Toast.makeText(
                    this,
                    "Added (gecko): @${result.username} (#$channel)",
                    Toast.LENGTH_LONG
                ).show()

                val i = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("new_account_id", accountId)
                }
                startActivity(i)
                finish()
            }
        }
    }

    private fun loadPendingChannelForSlot(slot: Int): String {
        val prefs = getSharedPreferences("v2_pending", MODE_PRIVATE)
        return prefs.getString("pending_channel_slot_$slot", "") ?: ""
    }

    private fun clearPendingChannelForSlot(slot: Int) {
        val prefs = getSharedPreferences("v2_pending", MODE_PRIVATE)
        prefs.edit {
            remove("pending_channel_slot_$slot")
        }
    }
}