package com.fs.twitchminichat.v2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var editChannel: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AccountsAdapter
    private lateinit var repo: AccountRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = AccountRepository(requireContext())

        editChannel = view.findViewById(R.id.editChannel)
        recycler = view.findViewById(R.id.recyclerAccounts)

        adapter = AccountsAdapter(
            onClick = { cfg ->
                // vai al tab chat dell’account (MainActivity gestisce)
                (activity as? MainActivity)?.openAccount(cfg.id)
            },
            onDelete = { cfg ->
                deleteAccount(cfg.id)
            },
            onLongPressDelete = { cfg ->
                deleteAccount(cfg.id)
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val channel = normalizeChannel(editChannel.text?.toString().orEmpty())
            if (channel.isBlank()) {
                editChannel.error = "Inserisci un canale Twitch"
                return@setOnClickListener
            }
            startTwitchLogin(channel)
        }


        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        adapter.submitList(repo.loadAccounts())
    }

    private fun deleteAccount(accountId: String) {
        val list = repo.loadAccounts().toMutableList()
        val removed = list.removeAll { it.id == accountId }
        if (removed) {
            repo.saveAll(list)
            refreshList()
            // avvisa MainActivity di ricaricare i tab
            requireContext().sendBroadcast(Intent(MainActivity.ACTION_ACCOUNTS_CHANGED))
        }
    }

    private fun normalizeChannel(raw: String): String {
        return raw.trim()
            .removePrefix("#")
            .lowercase()
            .ifBlank { "" }
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

        // ✅ IMPORTANT: qui tu userai v2g- nella gecko build (lascia così se è v2_gecko)
        val state = "v2g-$pendingId-${System.currentTimeMillis()}"

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
