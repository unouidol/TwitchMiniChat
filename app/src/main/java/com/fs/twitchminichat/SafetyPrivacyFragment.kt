package com.fs.twitchminichat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView



class SafetyPrivacyFragment : Fragment(R.layout.fragment_safety_privacy) {

    private val accountSharedPrefsToKeepForTesting = setOf(
        "v2_accounts"
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
                "Privacy policy coming soon",
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
    private fun knownProfileIdsForServerDeletion(): List<String> {
        return AccountRepository(requireContext())
            .loadAccounts()
            .map { ProfileIdUtil.fromUsername(it.username) }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun showTotalDeleteDialog() {
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val pad = (24 * density).toInt()
        val space12 = (12 * density).toInt()
        val space16 = (16 * density).toInt()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val messageView = TextView(ctx).apply {
            text = "This will delete app data on the server for this device and known profiles, including device registrations, dex lists, and stored app OAuth/account records when found, then erase all local app data from this device. You will need to log in again."
            textSize = 15f
        }

        val btnDelete = Button(ctx).apply {
            text = "Delete app account and all data"
        }

        val btnCancel = Button(ctx).apply {
            text = "Cancel"
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
        container.addView(btnDelete, buttonLp)
        container.addView(btnCancel, buttonLp)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Delete app account and all data")
            .setView(container)
            .create()

        btnDelete.setOnClickListener {
            dialog.dismiss()
            performTotalDeleteNow()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performTotalDeleteNow() {
        val ctx = requireContext()
        val profileIds = knownProfileIdsForServerDeletion()
        val prefsBefore = LocalDataCleaner.debugListSharedPrefs(ctx)
        val hiddenBefore = HiddenUsersStore.getAll(ctx)

        Log.d(
            "TOTAL_DELETE",
            "start profileIds=$profileIds prefsBefore=$prefsBefore hiddenBefore=$hiddenBefore"
        )

        FcmRegistrationUploader.deleteServerData(
            context = ctx,
            knownProfileIds = profileIds
        ) { serverResult ->
            if (!isAdded) return@deleteServerData

            Log.d(
                "TOTAL_DELETE",
                "server ok=${serverResult.ok} " +
                        "message=${serverResult.message} " +
                        "removedDevice=${serverResult.removedDevice} " +
                        "deletedDexProfiles=${serverResult.deletedDexProfiles} " +
                        "oauthDeletedRows=${serverResult.oauthDeletedRows} " +
                        "oauthDeletedTables=${serverResult.oauthDeletedTables} " +
                        "requestId=${serverResult.requestId} " +
                        "auditLogPath=${serverResult.auditLogPath} " +
                        "raw=${serverResult.rawResponse}"
            )

            if (!serverResult.ok) {
                Toast.makeText(
                    requireContext(),
                    serverResult.message,
                    Toast.LENGTH_SHORT
                ).show()
                return@deleteServerData
            }

            val localResult = LocalDataCleaner.clearAllLocalData(requireContext())
            val prefsAfter = LocalDataCleaner.debugListSharedPrefs(requireContext())
            val hiddenAfter = HiddenUsersStore.getAll(requireContext())

            Log.d(
                "TOTAL_DELETE",
                "local deletedSharedPrefs=${localResult.deletedSharedPrefs} " +
                        "skippedSharedPrefs=${localResult.skippedSharedPrefs} " +
                        "clearedCacheDirs=${localResult.clearedCacheDirs} " +
                        "processedPrefNames=${localResult.processedPrefNames} " +
                        "deletedPrefNames=${localResult.deletedPrefNames} " +
                        "skippedPrefNames=${localResult.skippedPrefNames}"
            )

            Log.d(
                "TOTAL_DELETE",
                "after prefsAfter=$prefsAfter hiddenAfter=$hiddenAfter"
            )

            restartAppAfterLocalClear()
        }
    }
    private fun showClearLocalDataDialog() {
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val pad = (24 * density).toInt()
        val space12 = (12 * density).toInt()
        val space16 = (16 * density).toInt()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val messageView = TextView(ctx).apply {
            text = "Choose what local data to reset on this device."
            textSize = 15f
        }

        val btnKeepAccounts = Button(ctx).apply {
            text = "Reset local data, keep accounts"
        }

        val btnFullClear = Button(ctx).apply {
            text = "Erase everything on this device"
        }

        val btnCancel = Button(ctx).apply {
            text = "Cancel"
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
        container.addView(btnKeepAccounts, buttonLp)
        container.addView(btnFullClear, buttonLp)
        container.addView(btnCancel, buttonLp)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Reset local data")
            .setView(container)
            .create()

        btnKeepAccounts.setOnClickListener {
            dialog.dismiss()
            clearLocalDataKeepingAccounts()
        }

        btnFullClear.setOnClickListener {
            dialog.dismiss()
            clearAllLocalDataNow()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearLocalDataKeepingAccounts() {
        val ctx = requireContext()

        Log.d("LOCAL_CLEAR", "prefs before=${LocalDataCleaner.debugListSharedPrefs(ctx)}")
        Log.d("LOCAL_CLEAR", "hidden before=${HiddenUsersStore.getAll(ctx)}")
        Log.d("LOCAL_CLEAR", "keeping account prefs=$accountSharedPrefsToKeepForTesting")

        val result = LocalDataCleaner.clearNonAccountLocalData(
            context = ctx,
            accountSharedPrefs = accountSharedPrefsToKeepForTesting
        )

        Log.d(
            "LOCAL_CLEAR",
            "keep-accounts result deletedSharedPrefs=${result.deletedSharedPrefs} skippedSharedPrefs=${result.skippedSharedPrefs} clearedCacheDirs=${result.clearedCacheDirs}"
        )

        Log.d("LOCAL_CLEAR", "prefs after=${LocalDataCleaner.debugListSharedPrefs(ctx)}")
        Log.d("LOCAL_CLEAR", "hidden after=${HiddenUsersStore.getAll(ctx)}")

        Toast.makeText(
            ctx,
            "Local data cleared, accounts kept",
            Toast.LENGTH_SHORT
        ).show()

        restartAppAfterLocalClear()
    }

    private fun clearAllLocalDataNow() {
        val ctx = requireContext()

        Log.d("LOCAL_CLEAR", "prefs before=${LocalDataCleaner.debugListSharedPrefs(ctx)}")
        Log.d("LOCAL_CLEAR", "hidden before=${HiddenUsersStore.getAll(ctx)}")

        val result = LocalDataCleaner.clearAllLocalData(ctx)

        Log.d(
            "LOCAL_CLEAR",
            "full result deletedSharedPrefs=${result.deletedSharedPrefs} skippedSharedPrefs=${result.skippedSharedPrefs} clearedCacheDirs=${result.clearedCacheDirs}"
        )

        Log.d("LOCAL_CLEAR", "prefs after=${LocalDataCleaner.debugListSharedPrefs(ctx)}")
        Log.d("LOCAL_CLEAR", "hidden after=${HiddenUsersStore.getAll(ctx)}")

        Toast.makeText(
            ctx,
            "All local data cleared",
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
}