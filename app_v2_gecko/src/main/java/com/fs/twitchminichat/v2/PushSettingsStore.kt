package com.fs.twitchminichat.v2

import android.content.Context
import androidx.core.content.edit

object PushSettingsStore {
    private const val PREFS_NAME = "push_settings"
    private fun normalizeProfileId(profileId: String?): String {
        return profileId?.trim()?.lowercase().orEmpty()
    }

    private fun key(profileId: String): String = "push_enabled_${normalizeProfileId(profileId)}"

    fun isPushEnabled(context: Context, profileId: String): Boolean {
        val normalized = normalizeProfileId(profileId)
        if (normalized.isEmpty()) return true

        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key(normalized), true)
    }

    fun setPushEnabled(context: Context, profileId: String, enabled: Boolean) {
        val normalized = normalizeProfileId(profileId)
        if (normalized.isEmpty()) return

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(key(normalized), enabled)
            }
    }
    fun syncProfilesForDevice(
        context: Context,
        allProfileIds: Collection<String>,
        enabledProfileIds: Collection<String>
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val normalizedEnabled = enabledProfileIds
            .map { normalizeProfileId(it) }
            .filter { it.isNotEmpty() }
            .toSet()

        val normalizedAll = linkedSetOf<String>()
        for (profileId in allProfileIds) {
            val normalized = normalizeProfileId(profileId)
            if (normalized.isNotEmpty()) {
                normalizedAll.add(normalized)
            }
        }

        prefs.edit {
            for (profileId in normalizedAll) {
                putBoolean(key(profileId), profileId in normalizedEnabled)
            }
        }
    }


}