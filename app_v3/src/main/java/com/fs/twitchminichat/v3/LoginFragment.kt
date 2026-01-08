package com.fs.twitchminichat.v3

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.UUID

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var repo: AccountRepository

    private val clientId = "7tvgt6i65b58k3e8lhxxv1p0b2vrib"

    // callback su GitHub Pages
    private val redirectUri = "https://unouidol.github.io/ircminichat/callback.html"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = AccountRepository(requireContext())

        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val editChannel = view.findViewById<EditText>(R.id.editChannel)

        renderAccountsList()

        btnLogin.setOnClickListener {
            val channel = editChannel.text.toString().trim().removePrefix("#")
            if (channel.isBlank()) {
                appendInlineHint("Inserisci un canale (es. unouidol)")
                return@setOnClickListener
            }

            // 1) creo un ID temporaneo per questo login
            val pendingId = UUID.randomUUID().toString()

            // 2) salvo il canale associato a quell'ID (AuthCallback lo rilegge)
            setPendingChannel(pendingId, channel)

            // 3) avvio oauth passando pendingId nello state
            startTwitchLogin(pendingId)
        }
    }

    override fun onResume() {
        super.onResume()
        // quando torni dal browser (OAuth) ricarica la lista
        if (this::repo.isInitialized) {
            renderAccountsList()
        }
    }

    private fun renderAccountsList() {
        val root = view ?: return
        val list = root.findViewById<LinearLayout>(R.id.accountsList)
        list.removeAllViews()

        val accounts = repo.getAll()

        if (accounts.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "Nessun account collegato."
                alpha = 0.8f
                setPadding(0, 0, 0, 12)
            }
            list.addView(empty)
            return
        }

        val header = TextView(requireContext()).apply {
            text = "Account collegati"
            textSize = 16f
            setPadding(0, 0, 0, 12)
        }
        list.addView(header)

        for (acc in accounts) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12, 12, 12, 12)
            }

            val tv = TextView(requireContext()).apply {
                text = "@${acc.username}  •  #${acc.channel}"
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                textSize = 15f
            }

            val btnX = TextView(requireContext()).apply {
                text = "✕"
                textSize = 18f
                setPadding(16, 0, 16, 0)
                alpha = 0.85f
            }

            fun confirmDelete() {
                AlertDialog.Builder(requireContext())
                    .setTitle("Rimuovere account?")
                    .setMessage("@${acc.username} su #${acc.channel}")
                    .setNegativeButton("Annulla", null)
                    .setPositiveButton("Rimuovi") { _, _ ->
                        repo.deleteById(acc.id)

                        // avvisa MainActivity di ricaricare le pagine
                        requireContext().sendBroadcast(
                            Intent(ACTION_ACCOUNTS_CHANGED)
                        )

                        renderAccountsList()
                    }
                    .show()
            }

            // click su ✕
            btnX.setOnClickListener { confirmDelete() }

            // long-press sulla riga
            row.setOnLongClickListener {
                confirmDelete()
                true
            }

            row.addView(tv)
            row.addView(btnX)
            list.addView(row)
        }
    }

    private fun startTwitchLogin(pendingId: String) {
        // state: v3-<pendingId>-<timestamp>
        val state = "v3-$pendingId-${System.currentTimeMillis()}"

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

    private fun appendInlineHint(text: String) {
        val list = requireView().findViewById<LinearLayout>(R.id.accountsList)
        val tv = TextView(requireContext()).apply {
            this.text = text
            setPadding(0, 8, 0, 16)
            alpha = 0.9f
        }
        list.addView(tv, 0)
    }

    private fun setPendingChannel(pendingId: String, channel: String) {
        val prefs = requireContext().getSharedPreferences(PREF_PENDING, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("pending_channel_$pendingId", channel)
            .apply()
    }

    companion object {
        const val ACTION_ACCOUNTS_CHANGED = "com.fs.twitchminichat.v3.ACCOUNTS_CHANGED"
        private const val PREF_PENDING = "v3_pending"
    }
}
