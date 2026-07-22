package com.fs.twitchminichat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID
import kotlin.concurrent.thread

/** Receives OAuth browser callbacks and adds or safely refreshes local accounts. */
class AuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data ?: run {
            finish()
            return
        }

        val loginToken = data.getQueryParameter("login_token")?.trim().orEmpty()
        val slot = data.getQueryParameter("slot")?.trim()?.toIntOrNull()
        val deepLinkProfileId = data.getQueryParameter("profile_id")?.trim().orEmpty()
        val pendingStore = OAuthPendingRequestStore(this)

        if (loginToken.isBlank() || slot == null) {
            if (slot != null) pendingStore.clear(slot)
            Toast.makeText(this, "Login callback invalid", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val pendingRequest = pendingStore.load(slot)
        if (pendingRequest == null) {
            Toast.makeText(this, "Missing Twitch Channel.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Toast.makeText(this, "Logging-in...", Toast.LENGTH_SHORT).show()

        thread(name = "tmc-oauth-finalize") {
            val result = OAuthBackendApi.finalizeLogin(loginToken)

            if (
                result == null ||
                result.username.isBlank() ||
                result.accessToken.isBlank()
            ) {
                pendingStore.clear(slot)
                finishWithToast(R.string.oauth_finalize_failed)
                return@thread
            }

            val finalProfileId = when {
                result.profileId.isNotBlank() -> result.profileId
                deepLinkProfileId.isNotBlank() -> deepLinkProfileId
                else -> ""
            }

            if (pendingRequest.isReauthorization) {
                finishReauthorization(
                    pendingRequest = pendingRequest,
                    result = result,
                    finalProfileId = finalProfileId,
                    pendingStore = pendingStore
                )
            } else {
                finishNewAccount(
                    pendingRequest = pendingRequest,
                    result = result,
                    finalProfileId = finalProfileId,
                    pendingStore = pendingStore
                )
            }
        }
    }

    /** Adds a new local account after a successful OAuth callback. */
    private fun finishNewAccount(
        pendingRequest: OAuthPendingRequest,
        result: OAuthFinalizeResult,
        finalProfileId: String,
        pendingStore: OAuthPendingRequestStore
    ) {
        val accountId = UUID.randomUUID().toString()
        val repo = AccountRepository(this)

        repo.addAccount(
            AccountConfig(
                id = accountId,
                username = result.username,
                channel = pendingRequest.channel,
                accessToken = result.accessToken,
                profileId = finalProfileId
            )
        )

        pendingStore.clear(pendingRequest.slot)
        openAccount(
            accountId = accountId,
            toastMessage = "Added account @${result.username} (#${pendingRequest.channel})"
        )
    }

    /**
     * Replaces credentials only when the callback identity matches the selected account.
     *
     * No profile-scoped data is deleted or migrated by this operation.
     */
    private fun finishReauthorization(
        pendingRequest: OAuthPendingRequest,
        result: OAuthFinalizeResult,
        finalProfileId: String,
        pendingStore: OAuthPendingRequestStore
    ) {
        val repo = AccountRepository(this)
        val existingAccount = repo.getById(pendingRequest.accountId)

        if (existingAccount == null) {
            pendingStore.clear(pendingRequest.slot)
            Log.w(TAG, "Reauthorization target no longer exists accountId=${pendingRequest.accountId}")
            finishWithToast(R.string.account_reauthorize_failed)
            return
        }

        val identityMatches = OAuthReauthorizationIdentityPolicy.matches(
            expectedUsername = pendingRequest.expectedUsername,
            expectedProfileId = pendingRequest.expectedProfileId,
            actualUsername = result.username,
            actualProfileId = finalProfileId
        )

        if (!identityMatches) {
            pendingStore.clear(pendingRequest.slot)
            Log.w(
                TAG,
                "Reauthorization identity mismatch accountId=${pendingRequest.accountId} " +
                        "expectedUser=${pendingRequest.expectedUsername} actualUser=${result.username}"
            )
            finishWithToast(R.string.account_reauthorize_identity_mismatch)
            return
        }

        val updated = repo.updateCredentialsInPlace(
            accountId = pendingRequest.accountId,
            username = result.username,
            accessToken = result.accessToken,
            profileId = finalProfileId
        )
        pendingStore.clear(pendingRequest.slot)

        if (!updated) {
            finishWithToast(R.string.account_reauthorize_failed)
            return
        }

        Log.i(
            TAG,
            "Reauthorization completed accountId=${pendingRequest.accountId} " +
                    "profileId=${finalProfileId.ifBlank { pendingRequest.expectedProfileId }}"
        )
        openAccount(
            accountId = pendingRequest.accountId,
            toastMessage = getString(R.string.account_reauthorize_done, result.username)
        )
    }

    /** Returns to the selected account and broadcasts the repository change. */
    private fun openAccount(accountId: String, toastMessage: String) {
        runOnUiThread {
            sendBroadcast(
                Intent(MainActivity.ACTION_ACCOUNTS_CHANGED).setPackage(packageName)
            )

            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()

            val mainIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_NEW_ACCOUNT_ID, accountId)
            }
            startActivity(mainIntent)
            finish()
        }
    }

    /** Shows a callback failure and closes this transient activity. */
    private fun finishWithToast(@StringRes messageRes: Int) {
        runOnUiThread {
            Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private companion object {
        /** Logcat tag for non-sensitive OAuth account refresh events. */
        const val TAG = "TMC_OAUTH"
    }
}
