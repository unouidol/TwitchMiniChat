package com.fs.twitchminichat

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

    private lateinit var btnPrivacyPolicy: Button
    private lateinit var btnBlockedUsers: Button
    private lateinit var btnClearLocalData: Button
    private lateinit var btnDeleteServerData: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnPrivacyPolicy = view.findViewById(R.id.btnPrivacyPolicy)
        btnBlockedUsers = view.findViewById(R.id.btnBlockedUsers)
        btnClearLocalData = view.findViewById(R.id.btnClearLocalData)
        btnDeleteServerData = view.findViewById(R.id.btnDeleteServerData)

        btnPrivacyPolicy.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.privacy_policy_coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }

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
                    textRes = R.string.cancel,
                    onClick = { }
                )
            )
        )
    }

    private fun performTotalDeleteNow() {
        val ctx = requireContext()
        val profileIds = profileIdsForServerDeletionAuthorization()

        Log.d(
            "TOTAL_DELETE",
            "start profileCandidateCount=${profileIds.size}"
        )

        FcmRegistrationUploader.deleteServerData(
            context = ctx,
            candidateProfileIds = profileIds
        ) { serverResult: FcmRegistrationUploader.DeleteServerDataResult ->
            if (!isAdded) return@deleteServerData

            Log.d(
                "TOTAL_DELETE",
                "Server deletion completed ok=${serverResult.ok}"
            )

            if (!serverResult.ok) {
                Toast.makeText(
                    requireContext(),
                    serverResult.message,
                    Toast.LENGTH_SHORT
                ).show()
                return@deleteServerData
            }

            GeckoSessionManager.clearAllWebData(requireContext()) { geckoOk: Boolean, geckoMessage: String ->
                if (!isAdded) return@clearAllWebData

                Log.d(
                    "TOTAL_DELETE",
                    "Gecko data clear completed ok=$geckoOk"
                )

                if (!geckoOk) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.server_delete_ok_gecko_failed, geckoMessage),
                        Toast.LENGTH_LONG
                    ).show()
                    return@clearAllWebData
                }

                val localResult = LocalDataCleaner.clearAllLocalData(requireContext())
                TermsPrefs.clearAcceptance(requireContext())

                Log.d(
                    "TOTAL_DELETE",
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
