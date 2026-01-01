package com.fs.twitchminichat.v2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import java.util.UUID

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var editChannel: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editChannel = view.findViewById(R.id.editChannel)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val channel = normalizeChannel(editChannel.text?.toString().orEmpty())
            startTwitchLogin(channel)
        }
    }

    private fun normalizeChannel(raw: String): String {
        return raw.trim()
            .removePrefix("#")
            .lowercase()
            .ifBlank { "unouidol" } // default
    }

    private fun savePendingChannel(pendingId: String, channel: String) {
        val prefs = requireContext().getSharedPreferences("v2_pending", Context.MODE_PRIVATE)
        prefs.edit().putString("pending_channel_$pendingId", channel).apply()
    }

    private fun startTwitchLogin(channel: String) {
        val clientId = "7tvgt6i65b58k3e8lhxxv1p0b2vrib"
        val redirectUri = "https://unouidol.github.io/ircminichat/callback.html"

        val pendingId = UUID.randomUUID().toString()
        savePendingChannel(pendingId, channel)

        val state = "v2-$pendingId-${System.currentTimeMillis()}"

        val uri = Uri.parse("https://id.twitch.tv/oauth2/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("scope", "chat:read chat:edit")
            .appendQueryParameter("state", state)
            .build()

        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
