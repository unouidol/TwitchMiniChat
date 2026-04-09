package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.edit
import androidx.core.net.toUri


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
                editChannel.error = "Type a Twitch Channel"
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
            //requireContext().sendBroadcast(Intent(MainActivity.ACTION_ACCOUNTS_CHANGED))
        }
    }

    private fun normalizeChannel(raw: String): String {
        return raw.trim()
            .removePrefix("#")
            .lowercase()
            .ifBlank { "" }
    }

    private fun savePendingChannelForSlot(slot: Int, channel: String) {
        val prefs = requireContext().getSharedPreferences("oauth_pending", Context.MODE_PRIVATE)
        prefs.edit {
            putString("pending_channel_slot_$slot", channel)
        }
    }

    private fun allocateLoginSlot(): Int {
        val prefs = requireContext().getSharedPreferences("oauth_pending", Context.MODE_PRIVATE)

        for (slot in 0..99) {
            if (!prefs.contains("pending_channel_slot_$slot")) {
                return slot
            }
        }

        return ((System.currentTimeMillis() / 1000L) % 100000L).toInt()
    }

    private fun startTwitchLogin(channel: String) {
        val slot = allocateLoginSlot()
        savePendingChannelForSlot(slot, channel)

        val authUrl = "https://api.ircminichat.party/oauth/start".toUri()
            .buildUpon()
            .appendQueryParameter("slot", slot.toString())
            .appendQueryParameter("return_scheme", "${BuildConfig.AUTH_SCHEME}://auth")
            .build()

        startActivity(Intent(Intent.ACTION_VIEW, authUrl))
    }
}