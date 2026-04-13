package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.fs.twitchminichat.pcg.GeckoSessionManager

object RemoteDeletionChecker {

    @Volatile
    private var checkInFlight = false

    @Volatile
    private var checkedThisProcess = false

    fun checkOnceOnAppOpen(
        context: Context,
        onNoDeletionDetected: (() -> Unit)? = null
    ) {
        val appContext = context.applicationContext

        if (checkedThisProcess) {
            onNoDeletionDetected?.invoke()
            return
        }

        if (checkInFlight) {
            return
        }

        val profileIds = AccountRepository(appContext)
            .loadAccounts()
            .map { ProfileIdUtil.fromUsername(it.username) }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

        if (profileIds.isEmpty()) {
            checkedThisProcess = true
            onNoDeletionDetected?.invoke()
            return
        }

        checkInFlight = true

        FcmRegistrationUploader.fetchProfileDeletionState(
            context = appContext,
            knownProfileIds = profileIds
        ) { result ->
            checkInFlight = false

            Log.d(
                "REMOTE_DELETE",
                "app-open check ok=${result.ok} deleted=${result.deletedProfileIds} raw=${result.rawResponse}"
            )

            if (!result.ok) {
                checkedThisProcess = true
                onNoDeletionDetected?.invoke()
                return@fetchProfileDeletionState
            }

            if (result.deletedProfileIds.isEmpty()) {
                checkedThisProcess = true
                onNoDeletionDetected?.invoke()
                return@fetchProfileDeletionState
            }

            Log.w(
                "REMOTE_DELETE",
                "remote delete detected on app open deletedProfileIds=${result.deletedProfileIds}"
            )

            GeckoSessionManager.clearAllWebData(appContext) { geckoOk: Boolean, geckoMessage: String ->
                Log.d("REMOTE_DELETE", "gecko ok=$geckoOk message=$geckoMessage")

                val localResult = LocalDataCleaner.clearAllLocalData(appContext)

                Log.d(
                    "REMOTE_DELETE",
                    "local wipe deletedSharedPrefs=${localResult.deletedSharedPrefs} " +
                            "skippedSharedPrefs=${localResult.skippedSharedPrefs} " +
                            "clearedCacheDirs=${localResult.clearedCacheDirs} " +
                            "deletedPrefNames=${localResult.deletedPrefNames}"
                )

                Toast.makeText(
                    appContext,
                    "This app account was deleted on another device. Local data has been reset.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(appContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                appContext.startActivity(intent)
            }
        }
    }

    fun resetProcessFlagForTesting() {
        checkedThisProcess = false
    }
}