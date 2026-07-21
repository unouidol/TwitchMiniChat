package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

/** Stores the independent event-spawn notification flag for each profile. */
object PcgEventSpawnAlertStore {

    private const val PREFS_NAME = "pcg_event_spawn_alerts"
    private const val KEY_PREFIX = "event_spawn_enabled_"

    /** Returns false for new, missing, or invalid profiles. */
    fun isEnabled(context: Context, profileId: String): Boolean {
        val cleanProfileId = normalizeProfileId(profileId)
        if (cleanProfileId.isEmpty()) return false

        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREFIX + cleanProfileId, false)
    }

    /** Saves the event-spawn preference for one normalized profile. */
    fun setEnabled(
        context: Context,
        profileId: String,
        enabled: Boolean
    ) {
        val cleanProfileId = normalizeProfileId(profileId)
        if (cleanProfileId.isEmpty()) return

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_PREFIX + cleanProfileId, enabled)
            }
    }

    /** Removes the local event-spawn preference for one profile. */
    fun clearProfile(context: Context, profileId: String) {
        val cleanProfileId = normalizeProfileId(profileId)
        if (cleanProfileId.isEmpty()) return

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(KEY_PREFIX + cleanProfileId)
            }
    }

    private fun normalizeProfileId(profileId: String): String {
        return profileId.trim().lowercase()
    }
}
