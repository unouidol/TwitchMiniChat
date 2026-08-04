package com.fs.twitchminichat

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import android.util.Log

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var editChannel: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AccountsAdapter
    private lateinit var repo: AccountRepository
    private lateinit var pendingRequestStore: OAuthPendingRequestStore
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repo = AccountRepository(requireContext())
        pendingRequestStore = OAuthPendingRequestStore(requireContext())

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
            showReauthorize = BuildConfig.REQUEST_EMOTE_SCOPE,
            onReauthorize = { cfg ->
                ensureTermsAccepted {
                    startTwitchReauthorization(cfg)
                }
            },
            onDelete = { cfg ->
                showRemoveAccountDialog(cfg)
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

                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                    return false
                }

                return adapter.moveItem(from, to)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                if (!isAdded) return

                val orderedIds = adapter.currentAccounts().map { it.id }
                if (orderedIds.isEmpty()) return

                repo.reorderAccounts(orderedIds)

                recyclerView.post {
                    if (!isAdded) return@post

                    requireContext().sendBroadcast(
                        Intent(MainActivity.ACTION_ACCOUNTS_CHANGED)
                            .setPackage(requireContext().packageName)
                    )
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

    private fun showRemoveAccountDialog(account: AccountConfig) {
        Log.d(
            "LOGIN_REMOVE",
            "Account removal dialog opened"
        )
        val label = account.username
            .trim()
            .ifBlank { account.channel.trim() }
            .ifBlank { account.id }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.account_remove_dialog_title)
            .setMessage(getString(R.string.account_remove_dialog_message, label))
            .setNegativeButton(R.string.account_remove_dialog_cancel, null)
            .setPositiveButton(R.string.account_remove_dialog_positive) { _, _ ->
                removeAccount(account)
            }
            .show()
    }

    private fun removeAccount(account: AccountConfig) {
        Log.d(
            "LOGIN_REMOVE",
            "Account removal confirmed"
        )
        AccountProfileRemovalController.removeAccountFromDevice(
            context = requireContext(),
            account = account
        ) { result ->
            if (!isAdded) return@removeAccountFromDevice

            refreshList()

            requireContext().sendBroadcast(
                Intent(MainActivity.ACTION_ACCOUNTS_CHANGED)
                    .setPackage(requireContext().packageName)
            )

            val messageRes = if (result.removedAccount) {
                R.string.account_remove_done
            } else {
                R.string.account_remove_failed
            }

            Toast.makeText(
                requireContext(),
                getString(messageRes),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun normalizeChannel(raw: String): String {
        return raw.trim()
            .removePrefix("#")
            .lowercase()
            .ifBlank { "" }
    }

    /** Starts OAuth for a new local account. */
    private fun startTwitchLogin(channel: String) {
        val pendingRequest = pendingRequestStore.createNewAccount(channel)
        if (pendingRequest == null) {
            Toast.makeText(
                requireContext(),
                R.string.oauth_start_failed,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        openTwitchAuthorization(pendingRequest = pendingRequest, profileId = "")
    }

    /**
     * Starts an in-place OAuth refresh for an existing local account.
     *
     * The backend profile identifier keeps server data attached to the same profile,
     * while callback metadata keeps the same local account id and channel.
     */
    private fun startTwitchReauthorization(account: AccountConfig) {
        val profileId = AccountProfileIdResolver.resolve(account)

        val pendingRequest = pendingRequestStore.createReauthorization(account, profileId)
        if (pendingRequest == null) {
            Toast.makeText(
                requireContext(),
                R.string.oauth_start_failed,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        openTwitchAuthorization(pendingRequest = pendingRequest, profileId = profileId)
    }

    /** Opens Twitch OAuth with an S256 proof and the scopes enabled for this flavor. */
    private fun openTwitchAuthorization(
        pendingRequest: OAuthPendingRequest,
        profileId: String
    ) {
        val codeChallenge = OAuthProofKeyPolicy.deriveS256CodeChallenge(
            pendingRequest.codeVerifier
        )
        if (codeChallenge == null) {
            pendingRequestStore.clear(pendingRequest.slot)
            Toast.makeText(
                requireContext(),
                R.string.oauth_start_failed,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val authUrlBuilder = "https://api.ircminichat.party/oauth/start".toUri()
            .buildUpon()
            .appendQueryParameter("slot", pendingRequest.slot.toString())
            .appendQueryParameter("return_scheme", "${BuildConfig.AUTH_SCHEME}://auth")
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter(
                "code_challenge_method",
                OAuthProofKeyPolicy.CODE_CHALLENGE_METHOD
            )

        if (profileId.isNotBlank()) {
            authUrlBuilder.appendQueryParameter("profile_id", profileId)
        }

        TwitchOAuthRequestFeatures.queryParameters(
            requestEmoteScope = BuildConfig.REQUEST_EMOTE_SCOPE
        ).forEach { (name, value) ->
            authUrlBuilder.appendQueryParameter(name, value)
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, authUrlBuilder.build()))
        } catch (_: ActivityNotFoundException) {
            pendingRequestStore.clear(pendingRequest.slot)
            Toast.makeText(
                requireContext(),
                R.string.oauth_browser_unavailable,
                Toast.LENGTH_LONG
            ).show()
        }
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
