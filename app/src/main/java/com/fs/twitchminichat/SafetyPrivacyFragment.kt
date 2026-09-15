package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
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
                    textRes = R.string.reset_local_data_full,
                    onClick = { clearAllLocalDataNow() }
                ),
                DialogAction(
                    textRes = R.string.reset_local_data_full_and_unregister,
                    onClick = { performDeviceDeleteNow() }
                ),
                DialogAction(
                    textRes = R.string.cancel,
                    onClick = { }
                )
            )
        )
    }

    /** Removes this device and its known profiles from the server, then wipes locally. */
    private fun performTotalDeleteNow() {
        performServerDeletion(
            logTag = "TOTAL_DELETE",
            geckoFailedMessageRes = R.string.server_delete_ok_gecko_failed
        ) { context, profileIds, onResult ->
            FcmRegistrationUploader.deleteServerData(
                context = context,
                candidateProfileIds = profileIds,
                onComplete = onResult
            )
        }
    }

    /**
     * Removes only this device's registration from the server, then wipes locally.
     *
     * Profile-scoped server data is left in place, so other devices signed in to the
     * same profiles keep working.
     */
    private fun performDeviceDeleteNow() {
        performServerDeletion(
            logTag = "DEVICE_DELETE",
            geckoFailedMessageRes = R.string.device_delete_ok_gecko_failed
        ) { context, profileIds, onResult ->
            FcmRegistrationUploader.deleteDeviceData(
                context = context,
                candidateProfileIds = profileIds,
                onComplete = onResult
            )
        }
    }

    /**
     * Runs one server deletion and wipes local data only after it succeeded.
     *
     * The order is shared by both scopes and matters: local data holds the device and
     * profile identifiers the server request needs, so it must never be cleared first.
     */
    private fun performServerDeletion(
        logTag: String,
        @StringRes geckoFailedMessageRes: Int,
        deletion: (
            Context,
            List<String>,
            (FcmRegistrationUploader.DeleteServerDataResult) -> Unit
        ) -> Unit
    ) {
        val ctx = requireContext()
        val profileIds = profileIdsForServerDeletionAuthorization()

        Log.d(
            logTag,
            "start profileCandidateCount=${profileIds.size}"
        )

        deletion(ctx, profileIds) serverResult@{ serverResult ->
            if (!isAdded) return@serverResult

            Log.d(
                logTag,
                "Server deletion completed ok=${serverResult.ok}"
            )

            if (!serverResult.ok) {
                Toast.makeText(
                    requireContext(),
                    serverResult.message,
                    Toast.LENGTH_SHORT
                ).show()
                return@serverResult
            }

            GeckoSessionManager.clearAllWebData(requireContext()) { geckoOk: Boolean, geckoMessage: String ->
                if (!isAdded) return@clearAllWebData

                Log.d(
                    logTag,
                    "Gecko data clear completed ok=$geckoOk"
                )

                if (!geckoOk) {
                    Toast.makeText(
                        requireContext(),
                        getString(geckoFailedMessageRes, geckoMessage),
                        Toast.LENGTH_LONG
                    ).show()
                    return@clearAllWebData
                }

                val localResult = LocalDataCleaner.clearAllLocalData(requireContext())
                TermsPrefs.clearAcceptance(requireContext())

                Log.d(
                    logTag,
                    "local deletedSharedPrefs=${localResult.deletedSharedPrefs} " +
                            "skippedSharedPrefs=${localResult.skippedSharedPrefs} " +
                            "failedSharedPrefs=${localResult.failedSharedPrefs} " +
                            "clearedCacheDirs=${localResult.clearedCacheDirs} " +
                            "failedCacheDirs=${localResult.failedCacheDirs}"
                )

                restartAppAfterLocalClear()
            }
        }
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

    private fun clearAllLocalDataNow() {
        val ctx = requireContext()

        val result = LocalDataCleaner.clearAllLocalData(ctx)
        TermsPrefs.clearAcceptance(ctx)

        Log.d(
            "LOCAL_CLEAR",
            "full result " +
                    "deletedSharedPrefs=${result.deletedSharedPrefs} " +
                    "skippedSharedPrefs=${result.skippedSharedPrefs} " +
                    "failedSharedPrefs=${result.failedSharedPrefs} " +
                    "clearedCacheDirs=${result.clearedCacheDirs} " +
                    "failedCacheDirs=${result.failedCacheDirs}"
        )

        Toast.makeText(
            ctx,
            getString(R.string.all_local_data_cleared),
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

        val dialog = AlertDialog.Builder(ctx)
            .setTitle(titleRes)
            .setView(container)
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
}
