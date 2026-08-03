package com.fs.twitchminichat

import android.content.Intent
import android.net.Uri
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

        val callbackPayload = readCallbackPayload(intent?.data)
        val pendingStore = OAuthPendingRequestStore(this)

        if (callbackPayload == null) {
            Toast.makeText(this, R.string.oauth_callback_invalid, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val pendingRequest = pendingStore.consume(callbackPayload.slot)
        if (pendingRequest == null) {
            Toast.makeText(
                this,
                R.string.oauth_request_missing_or_expired,
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        Toast.makeText(this, R.string.oauth_login_completing, Toast.LENGTH_SHORT).show()

        thread(name = "tmc-oauth-finalize") {
            val result = OAuthBackendApi.finalizeLogin(callbackPayload.loginToken)

            if (
                result == null ||
                !OAuthFlowSecurityPolicy.isValidFinalizeResult(
                    pendingRequest = pendingRequest,
                    result = result
                )
            ) {
                Log.w(TAG, "OAuth finalization response rejected")
                finishWithToast(R.string.oauth_finalize_failed)
                return@thread
            }

            val finalProfileId = result.profileId

            if (pendingRequest.isReauthorization) {
                finishReauthorization(
                    pendingRequest = pendingRequest,
                    result = result,
                    finalProfileId = finalProfileId
                )
            } else {
                finishNewAccount(
                    pendingRequest = pendingRequest,
                    result = result,
                    finalProfileId = finalProfileId
                )
            }
        }
    }

    /** Adds a new local account after a successful OAuth callback. */
    private fun finishNewAccount(
        pendingRequest: OAuthPendingRequest,
        result: OAuthFinalizeResult,
        finalProfileId: String
    ) {
        val accountId = UUID.randomUUID().toString()
        val repo = AccountRepository(this)

        val account = AccountConfig(
            id = accountId,
            username = result.username,
            channel = pendingRequest.channel,
            accessToken = result.accessToken,
            profileId = finalProfileId
        )
        repo.addAccount(account)

        if (!persistBackendSession(result, finalProfileId)) {
            repo.removeById(accountId)
            finishWithToast(R.string.backend_session_persist_failed)
            return
        }

        openAccount(
            accountId = accountId,
            toastMessage = getString(
                R.string.oauth_account_added,
                result.username,
                pendingRequest.channel
            )
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
        finalProfileId: String
    ) {
        val repo = AccountRepository(this)
        val existingAccount = repo.getById(pendingRequest.accountId)

        if (existingAccount == null) {
            Log.w(TAG, "Reauthorization target no longer exists")
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
            Log.w(TAG, "Reauthorization identity mismatch")
            finishWithToast(R.string.account_reauthorize_identity_mismatch)
            return
        }

        val updated = repo.updateCredentialsInPlace(
            accountId = pendingRequest.accountId,
            username = result.username,
            accessToken = result.accessToken,
            profileId = finalProfileId
        )

        if (!updated) {
            finishWithToast(R.string.account_reauthorize_failed)
            return
        }

        if (!persistBackendSession(result, finalProfileId)) {
            restoreAccountSnapshot(repo, existingAccount)
            finishWithToast(R.string.backend_session_persist_failed)
            return
        }

        Log.i(TAG, "Reauthorization completed")
        openAccount(
            accountId = pendingRequest.accountId,
            toastMessage = getString(R.string.account_reauthorize_done, result.username)
        )
    }

    /**
     * Stores a returned backend session for the finalized profile.
     *
     * The current app accepts only finalization responses containing both a canonical
     * profile identifier and a revocable backend session.
     */
    private fun persistBackendSession(
        result: OAuthFinalizeResult,
        finalProfileId: String
    ): Boolean {
        val backendSessionToken = result.desktopSessionToken.trim()
        if (backendSessionToken.isBlank() || finalProfileId.isBlank()) return false

        return BackendSessionStore(applicationContext).putSession(
            profileId = finalProfileId,
            sessionToken = backendSessionToken
        )
    }

    /** Restores the exact account snapshot when backend-session persistence fails. */
    private fun restoreAccountSnapshot(
        repo: AccountRepository,
        existingAccount: AccountConfig
    ) {
        val accounts = repo.loadAccounts()
        if (accounts.none { account -> account.id == existingAccount.id }) return

        repo.saveAll(
            accounts.map { account ->
                if (account.id == existingAccount.id) existingAccount else account
            }
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

    /** Converts one untrusted deep link into a validated callback payload. */
    private fun readCallbackPayload(data: Uri?): OAuthCallbackPayload? {
        if (data == null) return null

        val loginTokens = if (data.isHierarchical) {
            readQueryParameters(data, LOGIN_TOKEN_PARAMETER)
        } else {
            emptyList()
        }
        val slots = if (data.isHierarchical) {
            readQueryParameters(data, SLOT_PARAMETER)
        } else {
            emptyList()
        }

        return OAuthFlowSecurityPolicy.validateCallback(
            input = OAuthCallbackInput(
                isHierarchical = data.isHierarchical,
                scheme = data.scheme,
                host = data.host,
                port = data.port,
                path = data.path,
                userInfo = data.userInfo,
                fragment = data.fragment,
                loginTokens = loginTokens,
                slots = slots
            ),
            expectedScheme = BuildConfig.AUTH_SCHEME
        )
    }

    /** Reads one repeated query parameter without trusting malformed URI implementations. */
    private fun readQueryParameters(data: Uri, name: String): List<String> {
        return try {
            data.getQueryParameters(name)
        } catch (_: UnsupportedOperationException) {
            emptyList()
        }
    }

    private companion object {
        /** Logcat tag for non-sensitive OAuth account refresh events. */
        const val TAG = "TMC_OAUTH"

        /** Critical one-time token parameter returned by the backend callback. */
        const val LOGIN_TOKEN_PARAMETER = "login_token"

        /** Integer correlation parameter returned by the backend callback. */
        const val SLOT_PARAMETER = "slot"
    }
}
