package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.fs.twitchminichat.pcg.GeckoSessionManager

object RemoteDeletionChecker {

    private const val TAG = "REMOTE_DELETE"

    @Volatile
    private var checkInFlight = false

    @Volatile
    private var checkedThisProcess = false

    private val mainHandler = Handler(Looper.getMainLooper())

    fun checkOnceOnAppOpen(
        context: Context,
        onNoDeletionDetected: (() -> Unit)? = null
    ) {
        val appContext = context.applicationContext

        if (checkedThisProcess) {
            postNoDeletionDetected(onNoDeletionDetected)
            return
        }

        if (checkInFlight) {
            return
        }

        val profileIds = buildKnownProfileIds(appContext)

        if (profileIds.isEmpty()) {
            checkedThisProcess = true
            postNoDeletionDetected(onNoDeletionDetected)
            return
        }

        checkInFlight = true

        FcmRegistrationUploader.fetchProfileDeletionState(
            context = appContext,
            knownProfileIds = profileIds
        ) { result ->
            checkInFlight = false

            Log.d(
                TAG,
                "app-open check ok=${result.ok} deleted=${result.deletedProfileIds} raw=${result.rawResponse}"
            )

            if (!result.ok) {
                checkedThisProcess = true
                postNoDeletionDetected(onNoDeletionDetected)
                return@fetchProfileDeletionState
            }

            if (result.deletedProfileIds.isEmpty()) {
                checkedThisProcess = true
                postNoDeletionDetected(onNoDeletionDetected)
                return@fetchProfileDeletionState
            }

            checkedThisProcess = true

            Log.w(
                TAG,
                "remote delete detected on app open deletedProfileIds=${result.deletedProfileIds}"
            )

            GeckoSessionManager.clearAllWebData(appContext) { geckoOk: Boolean, geckoMessage: String ->
                Log.d(TAG, "gecko ok=$geckoOk message=$geckoMessage")

                try {
                    val localResult = LocalDataCleaner.clearAllLocalData(appContext)

                    Log.d(
                        TAG,
                        "local wipe deletedSharedPrefs=${localResult.deletedSharedPrefs} " +
                                "skippedSharedPrefs=${localResult.skippedSharedPrefs} " +
                                "clearedCacheDirs=${localResult.clearedCacheDirs} " +
                                "deletedPrefNames=${localResult.deletedPrefNames}"
                    )
                } catch (t: Throwable) {
                    Log.e(TAG, "local wipe failed", t)
                }

                mainHandler.post {
                    Toast.makeText(
                        appContext,
                        "This app account was deleted on another device. Local data has been reset.",
                        Toast.LENGTH_LONG
                    ).show()

                    mainHandler.postDelayed(
                        { restartApp(appContext) },
                        500L
                    )
                }
            }
        }
    }

    private fun postNoDeletionDetected(callback: (() -> Unit)?) {
        if (callback == null) return
        mainHandler.post { callback.invoke() }
    }

    private fun buildKnownProfileIds(context: Context): List<String> {
        return AccountRepository(context)
            .loadAccounts()
            .map {
                // IMPORTANTE:
                // se il tuo modello account ha già il vero profile_id server-side,
                // usa quello qui al posto di derivarlo dallo username.
                ProfileIdUtil.fromUsername(it.username)
            }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun restartApp(context: Context) {
        try {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

            if (launchIntent == null) {
                Log.e(TAG, "restart failed: launch intent is null")
                return
            }

            context.startActivity(launchIntent)
            Runtime.getRuntime().exit(0)
        } catch (t: Throwable) {
            Log.e(TAG, "restart failed", t)
        }
    }
}