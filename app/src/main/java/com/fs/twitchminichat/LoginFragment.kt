package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var editChannel: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AccountsAdapter
    private lateinit var repo: AccountRepository
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = AccountRepository(requireContext())

        editChannel = view.findViewById(R.id.editChannel)
        recycler = view.findViewById(R.id.recyclerAccounts)
        val btnSafetyPrivacy = view.findViewById<Button>(R.id.btnSafetyPrivacy)
        val btnPrivacyPolicy = view.findViewById<Button>(R.id.btnPrivacyPolicy)
        val btnTermsOfUse = view.findViewById<Button>(R.id.btnTermsOfUse)

        adapter = AccountsAdapter(
            onClick = { cfg ->
                ensureTermsAccepted {
                    (activity as? MainActivity)?.openAccount(cfg.id)
                }
            },
            onDelete = { cfg ->
                deleteAccount(cfg.id)
            },

            onStartDragRequest = { holder ->
                itemTouchHelper.startDrag(holder)
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun isLongPressDragEnabled(): Boolean = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                adapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                val orderedIds = adapter.currentAccounts().map { it.id }
                repo.reorderAccounts(orderedIds)

                recyclerView.post {
                    if (!isAdded) return@post
                    requireContext().sendBroadcast(Intent(MainActivity.ACTION_ACCOUNTS_CHANGED))
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(recycler)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val channel = normalizeChannel(editChannel.text?.toString().orEmpty())
            if (channel.isBlank()) {
                editChannel.error = "Type a Twitch Channel"
                return@setOnClickListener
            }

            ensureTermsAccepted {
                startTwitchLogin(channel)
            }
        }

        btnSafetyPrivacy.setOnClickListener {
            SafetyPrivacyActivity.start(requireContext())
        }

        btnPrivacyPolicy.setOnClickListener {
            PolicyPageActivity.open(
                context = requireContext(),
                title = "Privacy Policy",
                asset = "privacy.html",
                webUrl = WebPolicies.PRIVACY_URL
            )
        }

        btnTermsOfUse.setOnClickListener {
            PolicyPageActivity.open(
                context = requireContext(),
                title = "Terms of Use",
                asset = "terms.html",
                webUrl = WebPolicies.TERMS_URL
            )
        }
        refreshList()
    }



    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        adapter.submitAccounts(repo.loadAccounts())
    }

    private fun deleteAccount(accountId: String) {
        val list = repo.loadAccounts().toMutableList()
        val removed = list.removeAll { it.id == accountId }
        if (removed) {
            repo.saveAll(list)
            refreshList()
            requireContext().sendBroadcast(Intent(MainActivity.ACTION_ACCOUNTS_CHANGED))
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

    private fun ensureTermsAccepted(onAccepted: () -> Unit) {
        if (TermsPrefs.hasAcceptedCurrentVersion(requireContext())) {
            onAccepted()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_terms_gate, null)

        val btnOpenTerms = dialogView.findViewById<Button>(R.id.btnOpenTermsFromGate)
        val btnOpenPrivacy = dialogView.findViewById<Button>(R.id.btnOpenPrivacyFromGate)
        val checkAccept = dialogView.findViewById<CheckBox>(R.id.checkAcceptTerms)

        btnOpenTerms.setOnClickListener {
            PolicyPageActivity.open(
                context = requireContext(),
                title = "Terms of Use",
                asset = "terms.html",
                webUrl = WebPolicies.TERMS_URL
            )
        }

        btnOpenPrivacy.setOnClickListener {
            PolicyPageActivity.open(
                context = requireContext(),
                title = "Privacy Policy",
                asset = "privacy.html",
                webUrl = WebPolicies.PRIVACY_URL
            )
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Before you continue")
            .setView(dialogView)
            .setNegativeButton("Not now", null)
            .setPositiveButton("Accept and continue", null)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isEnabled = checkAccept.isChecked

            checkAccept.setOnCheckedChangeListener { _, isChecked ->
                positive.isEnabled = isChecked
            }

            positive.setOnClickListener {
                if (!checkAccept.isChecked) return@setOnClickListener

                TermsPrefs.markAcceptedCurrentVersion(requireContext())
                dialog.dismiss()
                onAccepted()
            }
        }

        dialog.show()
    }
}