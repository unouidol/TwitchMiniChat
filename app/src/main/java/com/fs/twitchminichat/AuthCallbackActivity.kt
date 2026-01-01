package com.fs.twitchminichat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity che riceve il deep link dopo il login Twitch.
 *
 * Formato atteso dell'URI:
 *   ircminichat://auth#slot=1&access_token=XYZ
 *
 * Lo slot (1 o 2) indica quale account aggiornare.
 * Viene aggiornato SOLO il token; username e channel restano quelli salvati in precedenza.
 */
class AuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent?.data)
        // Chiudiamo subito: questa activity è solo "di servizio"
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent?.data)
        finish()
    }

    private fun handleDeepLink(data: Uri?) {
        if (data == null) {
            Toast.makeText(this, "Deep link senza dati", Toast.LENGTH_LONG).show()
            return
        }

        // Esempio atteso:
        // ircminichat://auth#slot=1&access_token=XYZ
        val fragment = data.fragment ?: ""
        if (fragment.isEmpty()) {
            Toast.makeText(this, "Deep link senza fragment", Toast.LENGTH_LONG).show()
            return
        }

        // Parsiamo il fragment tipo "slot=1&access_token=XYZ&foo=bar"
        val params = fragment.split("&")
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) {
                    null
                } else {
                    val key = part.substring(0, idx)
                    val value = Uri.decode(part.substring(idx + 1))
                    key to value
                }
            }
            .toMap()

        val accessToken = params["access_token"]
        val slotStr = params["slot"] ?: "1"
        val slot = slotStr.toIntOrNull() ?: 1

        if (accessToken.isNullOrEmpty()) {
            Toast.makeText(this, "Token mancante nel deep link", Toast.LENGTH_LONG).show()
            return
        }

        // Carichiamo l'account esistente per questo slot
        val existing = AccountStorage.loadAccount(this, slot)

        if (existing == null) {
            // Non creiamo account finti: prima devi configurare username/canale nell'app
            Toast.makeText(
                this,
                "Account $slot non configurato (username/canale).\nConfiguralo nell'app e poi rifai il login.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Manteniamo username e channel come già salvati, aggiorniamo solo il token
            val updatedConfig = existing.copy(accessToken = accessToken)
            AccountStorage.saveAccount(this, slot, updatedConfig)

            Toast.makeText(
                this,
                "Token aggiornato per ${existing.username} (account $slot)",
                Toast.LENGTH_LONG
            ).show()
        }

        // Torniamo alla MainActivity
        val backIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(backIntent)
    }
}
