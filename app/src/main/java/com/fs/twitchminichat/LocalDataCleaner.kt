package com.fs.twitchminichat

import android.content.Context
import java.io.File

object LocalDataCleaner {

    data class Result(
        val deletedSharedPrefs: Int,
        val skippedSharedPrefs: Int,
        val failedSharedPrefs: Int,
        val clearedCacheDirs: Int,
        val failedCacheDirs: Int,
        val processedPrefNames: List<String>,
        val deletedPrefNames: List<String>,
        val skippedPrefNames: List<String>,
        val failedPrefNames: List<String>
    )

    fun clearAllLocalData(context: Context): Result {
        return clearInternal(
            context = context,
            excludedSharedPrefs = emptySet()
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
                .toSet()
        )
    }

    fun debugListSharedPrefs(context: Context): List<String> {
        return listSharedPreferenceNames(context.applicationContext)
    }

    private fun clearInternal(
        context: Context,
        excludedSharedPrefs: Set<String>
    ): Result {
        val appContext = context.applicationContext

        val prefNames = listSharedPreferenceNames(appContext)
        var deletedSharedPrefs = 0
        var skippedSharedPrefs = 0
        var failedSharedPrefs = 0

        val deletedPrefNames = mutableListOf<String>()
        val skippedPrefNames = mutableListOf<String>()
        val failedPrefNames = mutableListOf<String>()

        for (name in prefNames) {
            if (name in excludedSharedPrefs) {
                skippedSharedPrefs++
                skippedPrefNames += name
                continue
            }

            val deleted = runCatching {
                appContext.deleteSharedPreferences(name)
            }.getOrElse {
                false
            }

            if (deleted) {
                deletedSharedPrefs++
                deletedPrefNames += name
            } else {
                failedSharedPrefs++
                failedPrefNames += name
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

        return Result(
            deletedSharedPrefs = deletedSharedPrefs,
            skippedSharedPrefs = skippedSharedPrefs,
            failedSharedPrefs = failedSharedPrefs,
            clearedCacheDirs = clearedCacheDirs,
            failedCacheDirs = failedCacheDirs,
            processedPrefNames = prefNames,
            deletedPrefNames = deletedPrefNames,
            skippedPrefNames = skippedPrefNames,
            failedPrefNames = failedPrefNames
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