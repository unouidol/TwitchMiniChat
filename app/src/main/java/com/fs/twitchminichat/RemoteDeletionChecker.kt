package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.fs.twitchminichat.pcg.GeckoSessionManager

/**
 * Checks whether a locally configured account was deleted remotely.
 */
object RemoteDeletionChecker {

    private const val TAG = "REMOTE_DELETE"
    private const val RESTART_DELAY_MS = 500L

    @Volatile
    private var checkInFlight = false

    @Volatile
    private var checkedThisProcess = false

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Performs at most one remote deletion check during the current process.
     */
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

        /*
         * A fresh installation has no local account requiring a deletion check.
         * Profile identifiers are not included in the backend request.
         */
        val hasLocalAccounts = runCatching {
            AccountRepository(appContext)
                .loadAccounts()
                .isNotEmpty()
        }.getOrDefault(false)

        if (!hasLocalAccounts) {
            checkedThisProcess = true
            postNoDeletionDetected(onNoDeletionDetected)
            return
        }

        checkInFlight = true

        ProfileDeletionStateClient(appContext).fetch { result ->
            checkInFlight = false
            checkedThisProcess = true

            Log.d(
                TAG,
                "app-open check ok=${result.ok} " +
                    "responseCode=${result.responseCode} " +
                    "deletedCount=${result.deletedProfileIds.size}"
            )

            if (!result.ok || result.deletedProfileIds.isEmpty()) {
                postNoDeletionDetected(onNoDeletionDetected)
                return@fetch
            }

            Log.w(
                TAG,
                "remote deletion detected deletedCount=" +
                    result.deletedProfileIds.size
            )

            GeckoSessionManager.clearAllWebData(appContext) {
                    geckoOk: Boolean,
                    _: String ->

                Log.d(
                    TAG,
                    "Gecko data clear completed ok=$geckoOk"
                )

                try {
                    val localResult =
                        LocalDataCleaner.clearAllLocalData(appContext)

                    Log.d(
                        TAG,
                        "local wipe deletedSharedPrefs=" +
                            localResult.deletedSharedPrefs +
                            " skippedSharedPrefs=" +
                            localResult.skippedSharedPrefs +
                            " clearedCacheDirs=" +
                            localResult.clearedCacheDirs
                    )
                } catch (error: Throwable) {
                    Log.e(
                        TAG,
                        "Local wipe failed errorType=${DiagnosticError.typeOf(error)}"
                    )
                }

                mainHandler.post {
                    Toast.makeText(
                        appContext,
                        appContext.getString(
                            R.string.remote_account_deleted_message
                        ),
                        Toast.LENGTH_LONG
                    ).show()

                    mainHandler.postDelayed(
                        { restartApp(appContext) },
                        RESTART_DELAY_MS
                    )
                }
            }
        }
    }

    /** Posts the normal app-start continuation on the main thread. */
    private fun postNoDeletionDetected(
        callback: (() -> Unit)?
    ) {
        if (callback == null) {
            return
        }

        mainHandler.post {
            callback.invoke()
        }
    }

    /** Restarts the application after all local state has been cleared. */
    private fun restartApp(context: Context) {
        try {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                }

            if (launchIntent == null) {
                Log.e(TAG, "restart failed: launch intent is null")
                return
            }

            context.startActivity(launchIntent)
            Runtime.getRuntime().exit(0)
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Restart failed errorType=${DiagnosticError.typeOf(error)}"
            )
        }
    }
}
