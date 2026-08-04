package com.fs.twitchminichat

import android.content.Context
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
        val backendSessionClearSucceeded: Boolean
    )

    fun clearAllLocalData(context: Context): Result {
        return clearInternal(
            context = context,
            excludedSharedPrefs = emptySet(),
            clearBackendSessions = true
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
            clearBackendSessions = false
        )
    }

    private fun clearInternal(
        context: Context,
        excludedSharedPrefs: Set<String>,
        clearBackendSessions: Boolean
    ): Result {
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

        return Result(
            deletedSharedPrefs = deletedSharedPrefs,
            skippedSharedPrefs = skippedSharedPrefs,
            failedSharedPrefs = failedSharedPrefs,
            clearedCacheDirs = clearedCacheDirs,
            failedCacheDirs = failedCacheDirs,
            backendSessionClearAttempted = clearBackendSessions,
            backendSessionClearSucceeded = backendSessionClearSucceeded
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
