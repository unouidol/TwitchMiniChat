package com.fs.twitchminichat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.fs.twitchminichat.pcg.GeckoSessionManager

class SafetyPrivacyFragment : Fragment(R.layout.fragment_safety_privacy) {

    /*
     * Accounts are stored encrypted outside shared_prefs, and that store is never
     * touched by the keep-accounts cleanup. Only the legacy plain-text file still has
     * to be excluded explicitly, for installations that have not completed the upgrade.
     */
    private val accountSharedPrefsToKeepForTesting = setOf(
        AccountRepository.LEGACY_PREFERENCES_NAME
    )

    private lateinit var btnBlockedUsers: Button
    private lateinit var btnClearLocalData: Button
    private lateinit var btnDeleteServerData: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBlockedUsers = view.findViewById(R.id.btnBlockedUsers)
        btnClearLocalData = view.findViewById(R.id.btnClearLocalData)
        btnDeleteServerData = view.findViewById(R.id.btnDeleteServerData)

        btnBlockedUsers.setOnClickListener {
            BlockedUsersActivity.start(requireContext())
        }

        btnClearLocalData.setOnClickListener {
            showClearLocalDataDialog()
        }

        btnDeleteServerData.setOnClickListener {
            showTotalDeleteDialog()
        }
    }

    /**
     * Returns local candidates used only to select a deletion Bearer session.
     */
    private fun profileIdsForServerDeletionAuthorization(): List<String> {
        return AccountRepository(requireContext())
            .loadAccounts()
            .map { account ->
                account.profileId
                    .trim()
                    .ifBlank {
                        ProfileIdUtil.fromUsername(account.username)
                    }
            }
            .map { profileId ->
                profileId.trim().lowercase()
            }
            .filter(String::isNotBlank)
            .distinct()
    }

    private fun showTotalDeleteDialog() {
        showStackedActionDialog(
            titleRes = R.string.delete_account_all_data_title,
            messageRes = R.string.delete_account_all_data_message,
            actions = listOf(
                DialogAction(
                    textRes = R.string.delete_account_all_data_confirm,
                    onClick = { performTotalDeleteNow() }
                ),
                DialogAction(
                    textRes = R.string.cancel,
                    onClick = { }
                )
            )
        )
    }

    private fun showClearLocalDataDialog() {
        showStackedActionDialog(
            titleRes = R.string.reset_local_data_title,
            messageRes = R.string.reset_local_data_message,
            actions = listOf(
                DialogAction(
                    textRes = R.string.reset_local_data_keep_accounts,
                    onClick = { clearLocalDataKeepingAccounts() }
                ),
                DialogAction(
                    textRes = R.string.reset_local_data_full_and_unregister,
                    onClick = { performDeviceEraseNow() }
                ),
                DialogAction(
                    textRes = R.string.cancel,
                    onClick = { }
                )
            )
        )
    }

    /**
     * Deletes this device and its known profiles from the server, then erases the phone.
     *
     * Unlike the single erase below, this one stops when the server cannot be reached,
     * and that difference is deliberate. It is the only action that deletes
     * profile-scoped data, which the user's other devices share, so wiping this phone
     * after a failed call would leave the user believing their server-side data is gone
     * when it is not - and with nothing left on the phone to try again with. Erasing
     * locally for nothing is recoverable by signing in again; a deletion the user
     * thinks happened is not.
     */
    private fun performTotalDeleteNow() {
        val profileIds = profileIdsForServerDeletionAuthorization()

        Log.d(TAG_TOTAL_DELETE, "start profileCandidateCount=${profileIds.size}")

        FcmRegistrationUploader.deleteServerData(
            context = requireContext(),
            candidateProfileIds = profileIds
        ) serverResult@{ serverResult ->
            if (!isAdded) return@serverResult

            Log.d(TAG_TOTAL_DELETE, "Server deletion completed ok=${serverResult.ok}")

            if (!serverResult.ok) {
                Toast.makeText(
                    requireContext(),
                    serverResult.message,
                    Toast.LENGTH_SHORT
                ).show()
                return@serverResult
            }

            GeckoSessionManager.clearAllWebData(requireContext()) webData@{ webDataOk, webDataMessage ->
                if (!isAdded) return@webData

                Log.d(TAG_TOTAL_DELETE, "Gecko data clear completed ok=$webDataOk")

                if (!webDataOk) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.server_delete_ok_gecko_failed, webDataMessage),
                        Toast.LENGTH_LONG
                    ).show()
                    return@webData
                }

                /*
                 * This path erases the phone too, so it must not leave a live push
                 * token behind either. It runs last of the three server-facing steps
                 * and immediately before the wipe, so the abort above - the only way
                 * out of this function after the server call - cannot strand a phone
                 * that still holds its accounts without a token to receive alerts on.
                 */
                FcmRegistrationUploader.deleteFirebaseToken(requireContext()) tokenResult@{ tokenOk ->
                    if (!isAdded) return@tokenResult

                    Log.d(TAG_TOTAL_DELETE, "Firebase token deletion ok=$tokenOk")

                    wipeLocalData(TAG_TOTAL_DELETE)
                    recordOwedTokenDeletionIfNeeded(TAG_TOTAL_DELETE, tokenOk)

                    restartAppAfterLocalClear()
                }
            }
        }
    }

    /**
     * Removes this device from the server, deletes the push token, and erases the phone.
     *
     * The phone is erased whether or not the first two steps went through, and that is
     * the whole point of this action: erasing one's own data must not depend on a
     * server being reachable. Deleting the token is what makes it safe to carry on
     * regardless - a phone without a token receives nothing, and the backend drops the
     * registration the next time it tries to send to it. Whatever did not go through is
     * recorded as owed and finished later by [OwedServerOperationWorker].
     */
    private fun performDeviceEraseNow() {
        val profileIds = profileIdsForServerDeletionAuthorization()

        Log.d(TAG_DEVICE_ERASE, "start profileCandidateCount=${profileIds.size}")

        FcmRegistrationUploader.deleteDeviceData(
            context = requireContext(),
            candidateProfileIds = profileIds
        ) serverResult@{ serverResult ->
            if (!isAdded) return@serverResult

            Log.d(
                TAG_DEVICE_ERASE,
                "Server device removal completed ok=${serverResult.ok}"
            )

            FcmRegistrationUploader.deleteFirebaseToken(requireContext()) tokenResult@{ tokenOk ->
                if (!isAdded) return@tokenResult

                Log.d(TAG_DEVICE_ERASE, "Firebase token deletion ok=$tokenOk")

                GeckoSessionManager.clearAllWebData(requireContext()) webData@{ webDataOk, webDataMessage ->
                    if (!isAdded) return@webData

                    Log.d(TAG_DEVICE_ERASE, "Gecko data clear completed ok=$webDataOk")

                    /*
                     * No early return on a failed browser clear, which reverses the
                     * decision taken in #40. That one was right for the action it was
                     * written for: "Erase everything on this device" touched nothing
                     * outside the phone, so aborting left the phone exactly as it was
                     * and the user could try again.
                     *
                     * This action is not that one. It has already asked the server to
                     * remove this device and already deleted the push token, so aborting
                     * here would leave the worse of the two half-states: a phone
                     * deregistered from the server with all of its data still on it, and
                     * no way to undo either step.
                     *
                     * The second reason is what kind of failure this is. A browser clear
                     * fails locally, not for want of a network, so nothing about it says
                     * the erase would go better later. Refusing the erase the user came
                     * for because of it denies them the thing they asked for. The failure
                     * is reported in the same message instead, and the erase completes.
                     *
                     * What #40 fixed stands: the browser data is still cleared here, and
                     * before the wipe. Only its all-or-nothing rule is gone.
                     */
                    wipeLocalData(TAG_DEVICE_ERASE)
                    recordOwedTokenDeletionIfNeeded(TAG_DEVICE_ERASE, tokenOk)

                    val outcome = DeviceEraseOutcomePolicy.decide(
                        serverRemovalOk = serverResult.ok,
                        tokenDeletionOk = tokenOk
                    )

                    Log.d(TAG_DEVICE_ERASE, "erase outcome=$outcome")

                    Toast.makeText(
                        requireContext(),
                        eraseMessage(outcome, webDataOk, webDataMessage),
                        Toast.LENGTH_LONG
                    ).show()

                    restartAppAfterLocalClear()
                }
            }
        }
    }

    /**
     * Erases every local store and the terms acceptance, logging counts only.
     *
     * Local data holds the device and profile identifiers the server requests need, so
     * this must never run before them.
     */
    private fun wipeLocalData(logTag: String) {
        val ctx = requireContext()

        val result = LocalDataCleaner.clearAllLocalData(ctx)
        TermsPrefs.clearAcceptance(ctx)

        Log.d(
            logTag,
            "local deletedSharedPrefs=${result.deletedSharedPrefs} " +
                    "skippedSharedPrefs=${result.skippedSharedPrefs} " +
                    "failedSharedPrefs=${result.failedSharedPrefs} " +
                    "clearedCacheDirs=${result.clearedCacheDirs} " +
                    "failedCacheDirs=${result.failedCacheDirs}"
        )
    }

    /**
     * Records a token deletion that did not go through, and queues the work that ends it.
     *
     * Called after [wipeLocalData] and never before: the wipe deletes every shared
     * preferences file, so a record written earlier would be erased by the very
     * operation it exists to outlive.
     */
    private fun recordOwedTokenDeletionIfNeeded(logTag: String, tokenDeletionOk: Boolean) {
        if (!DeviceEraseOutcomePolicy.tokenDeletionOwed(tokenDeletionOk)) return

        val ctx = requireContext()
        OwedServerOperationStore.recordFirebaseTokenDeletion(ctx)
        OwedServerOperationWorker.enqueue(ctx)

        Log.d(logTag, "Firebase token deletion recorded as owed")
    }

    /**
     * Builds the single message the erase shows, adding the browser clause when it applies.
     */
    private fun eraseMessage(
        outcome: DeviceEraseOutcome,
        webDataOk: Boolean,
        webDataMessage: String
    ): String {
        val outcomeText = getString(
            when (outcome) {
                DeviceEraseOutcome.REMOVED_FROM_SERVER -> R.string.erase_device_removed
                DeviceEraseOutcome.ALERTS_STOPPED -> R.string.erase_device_alerts_stopped
                DeviceEraseOutcome.NOTHING_REACHED -> R.string.erase_device_nothing_reached
            }
        )

        if (webDataOk) return outcomeText

        return outcomeText + " " +
                getString(R.string.erase_device_web_data_failed, webDataMessage)
    }

    private fun clearLocalDataKeepingAccounts() {
        val ctx = requireContext()

        Log.d(
            "LOCAL_CLEAR",
            "Keeping account preferences count=${accountSharedPrefsToKeepForTesting.size}"
        )

        val result = LocalDataCleaner.clearNonAccountLocalData(
            context = ctx,
            accountSharedPrefs = accountSharedPrefsToKeepForTesting
        )

        TermsPrefs.clearAcceptance(ctx)

        Log.d(
            "LOCAL_CLEAR",
            "keep-accounts result " +
                    "deletedSharedPrefs=${result.deletedSharedPrefs} " +
                    "skippedSharedPrefs=${result.skippedSharedPrefs} " +
                    "failedSharedPrefs=${result.failedSharedPrefs} " +
                    "clearedCacheDirs=${result.clearedCacheDirs} " +
                    "failedCacheDirs=${result.failedCacheDirs}"
        )

        Toast.makeText(
            ctx,
            getString(R.string.local_data_cleared_accounts_kept),
            Toast.LENGTH_SHORT
        ).show()

        restartAppAfterLocalClear()
    }

    private fun restartAppAfterLocalClear() {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    private fun showStackedActionDialog(
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        actions: List<DialogAction>
    ) {
        val ctx = requireContext()
        val pad = dpToPx(24)
        val space12 = dpToPx(12)
        val space16 = dpToPx(16)

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val messageView = TextView(ctx).apply {
            setText(messageRes)
            textSize = 15f
        }

        val messageLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = space16
        }

        val buttonLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = space12
        }

        container.addView(messageView, messageLp)

        /*
         * AlertDialog does not scroll a custom view. The reset message is four
         * paragraphs, so on a short screen the stacked buttons below it would be
         * pushed off the dialog with no way to reach them.
         */
        val scroller = ScrollView(ctx).apply {
            addView(
                container,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = AlertDialog.Builder(ctx)
            .setTitle(titleRes)
            .setView(scroller)
            .create()

        actions.forEach { action ->
            val button = Button(ctx).apply {
                setText(action.textRes)
                setOnClickListener {
                    dialog.dismiss()
                    action.onClick()
                }
            }
            container.addView(button, buttonLp)
        }

        dialog.show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private data class DialogAction(
        @field:StringRes val textRes: Int,
        val onClick: () -> Unit
    )

    private companion object {

        /**
         * Logcat tags, unchanged from before the two erase actions became one, so
         * filters already in use on the test device keep matching.
         */
        const val TAG_TOTAL_DELETE = "TOTAL_DELETE"
        const val TAG_DEVICE_ERASE = "DEVICE_DELETE"
    }
}
