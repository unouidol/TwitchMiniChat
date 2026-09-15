package com.fs.twitchminichat

import android.content.Context
import com.fs.twitchminichat.diagnostics.HistoryDiagnosticsLog
import java.io.File

object LocalDataCleaner {

    /** Aggregate cleanup outcome without preference names or stored values. */
    data class Result(
        val deletedSharedPrefs: Int,
        val skippedSharedPrefs: Int,
        val failedSharedPrefs: Int,
        val clearedCacheDirs: Int,
        val failedCacheDirs: Int,
        val backendSessionClearAttempted: Boolean,
        val backendSessionClearSucceeded: Boolean,
        val accountStoreClearAttempted: Boolean = false,
        val accountStoreClearSucceeded: Boolean = false
    )

    fun clearAllLocalData(context: Context): Result {
        return clearInternal(
            context = context,
            excludedSharedPrefs = emptySet(),
            clearBackendSessions = true,
            clearAccountStore = true
        )
    }

    fun clearNonAccountLocalData(
        context: Context,
        accountSharedPrefs: Set<String>
    ): Result {
        return clearInternal(
            context = context,
            excludedSharedPrefs = accountSharedPrefs
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet(),
            clearBackendSessions = false,
            clearAccountStore = false
        )
    }

    private fun clearInternal(
        context: Context,
        excludedSharedPrefs: Set<String>,
        clearBackendSessions: Boolean,
        clearAccountStore: Boolean
    ): Result {
        /*
         * Invariant: every persistent store this application creates is erased
         * here. Each one needs its own line, because nothing below sweeps
         * filesDir: the two directory calls clear cacheDir and codeCacheDir only,
         * and shared_prefs is reached by name. A store written anywhere else -
         * the diagnostics journal in filesDir/diagnostics, the encrypted
         * account store - survives a reset unless it is named in this function,
         * and a reset the user was told erases everything then quietly does not.
         * Adding a store means adding it here.
         *
         * filesDir is deliberately not cleared wholesale. GeckoRuntime runs with
         * default settings and its profile may live under it; wiping that fits
         * "erase everything" but not "reset local data, keep accounts", and
         * where it actually lives has not been measured.
         */
        val appContext = context.applicationContext

        val prefNames = listSharedPreferenceNames(appContext)
        var deletedSharedPrefs = 0
        var skippedSharedPrefs = 0
        var failedSharedPrefs = 0

        for (name in prefNames) {
            if (name in excludedSharedPrefs) {
                skippedSharedPrefs++
                continue
            }

            val deleted = runCatching {
                appContext.deleteSharedPreferences(name)
            }.getOrElse {
                false
            }

            if (deleted) {
                deletedSharedPrefs++
            } else {
                failedSharedPrefs++
            }
        }

        var clearedCacheDirs = 0
        var failedCacheDirs = 0

        when (clearDirectoryChildren(appContext.cacheDir)) {
            DirectoryClearResult.CLEARED -> clearedCacheDirs++
            DirectoryClearResult.FAILED -> failedCacheDirs++
            DirectoryClearResult.SKIPPED -> Unit
        }

        when (clearDirectoryChildren(appContext.codeCacheDir)) {
            DirectoryClearResult.CLEARED -> clearedCacheDirs++
            DirectoryClearResult.FAILED -> failedCacheDirs++
            DirectoryClearResult.SKIPPED -> Unit
        }

        val backendSessionClearSucceeded = if (clearBackendSessions) {
            BackendSessionStore(appContext).clearAll()
        } else {
            false
        }

        /*
         * Accounts live in encrypted storage outside shared_prefs, so the loop above
         * cannot reach them. A full wipe must remove that store explicitly, otherwise
         * "delete all local data" would silently stop deleting credentials.
         */
        val accountStoreClearSucceeded = if (clearAccountStore) {
            EncryptedAccountStore(appContext).clear()
        } else {
            false
        }

        /*
         * Cleared on every reset, including the one that keeps accounts: it holds
         * the Twitch account name and the channels watched with their times, and
         * it is diagnostics, not account configuration.
         */
        HistoryDiagnosticsLog.clear(appContext)

        return Result(
            deletedSharedPrefs = deletedSharedPrefs,
            skippedSharedPrefs = skippedSharedPrefs,
            failedSharedPrefs = failedSharedPrefs,
            clearedCacheDirs = clearedCacheDirs,
            failedCacheDirs = failedCacheDirs,
            backendSessionClearAttempted = clearBackendSessions,
            backendSessionClearSucceeded = backendSessionClearSucceeded,
            accountStoreClearAttempted = clearAccountStore,
            accountStoreClearSucceeded = accountStoreClearSucceeded
        )
    }

    private fun listSharedPreferenceNames(context: Context): List<String> {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val files = prefsDir.listFiles().orEmpty()

        return files.mapNotNull { file ->
            val name = file.name
            if (file.isFile && name.endsWith(".xml")) {
                name.removeSuffix(".xml")
            } else {
                null
            }
        }.distinct().sorted()
    }

    private enum class DirectoryClearResult {
        CLEARED,
        FAILED,
        SKIPPED
    }

    private fun clearDirectoryChildren(dir: File?): DirectoryClearResult {
        if (dir == null || !dir.exists() || !dir.isDirectory) {
            return DirectoryClearResult.SKIPPED
        }

        val children = dir.listFiles().orEmpty()
        var allOk = true

        for (child in children) {
            val ok = runCatching {
                child.deleteRecursively()
            }.getOrElse {
                false
            }

            if (!ok) {
                allOk = false
            }
        }

        return if (allOk) {
            DirectoryClearResult.CLEARED
        } else {
            DirectoryClearResult.FAILED
        }
    }
}
