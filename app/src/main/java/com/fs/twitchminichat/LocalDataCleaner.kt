package com.fs.twitchminichat

import android.content.Context
import android.os.Build
import java.io.File

object LocalDataCleaner {

    data class Result(
        val deletedSharedPrefs: Int,
        val skippedSharedPrefs: Int,
        val clearedCacheDirs: Int,
        val processedPrefNames: List<String>,
        val deletedPrefNames: List<String>,
        val skippedPrefNames: List<String>
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
            excludedSharedPrefs = accountSharedPrefs.map { it.trim() }.filter { it.isNotBlank() }.toSet()
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

        val deletedPrefNames = mutableListOf<String>()
        val skippedPrefNames = mutableListOf<String>()

        for (name in prefNames) {
            if (name in excludedSharedPrefs) {
                skippedSharedPrefs++
                skippedPrefNames += name
                continue
            }

            runCatching {
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()

                if (appContext.deleteSharedPreferences(name)) {
                    deletedSharedPrefs++
                    deletedPrefNames += name
                }
            }
        }

        var clearedCacheDirs = 0

        if (clearDirectoryChildren(appContext.cacheDir)) {
            clearedCacheDirs++
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (clearDirectoryChildren(appContext.codeCacheDir)) {
                clearedCacheDirs++
            }
        }

        return Result(
            deletedSharedPrefs = deletedSharedPrefs,
            skippedSharedPrefs = skippedSharedPrefs,
            clearedCacheDirs = clearedCacheDirs,
            processedPrefNames = prefNames,
            deletedPrefNames = deletedPrefNames,
            skippedPrefNames = skippedPrefNames
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

    private fun clearDirectoryChildren(dir: File?): Boolean {
        if (dir == null || !dir.exists() || !dir.isDirectory) return false

        val children = dir.listFiles().orEmpty()
        for (child in children) {
            runCatching {
                child.deleteRecursively()
            }
        }
        return true
    }
}