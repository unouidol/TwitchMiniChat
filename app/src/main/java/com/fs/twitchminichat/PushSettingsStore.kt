package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

/**
 * The legacy per-profile push setting, kept only so that removing an account still deletes it.
 *
 * Builds before the PCG notification menu stored a push mode, or an older on/off flag, here.
 * Nothing reads it any more: the alert selection lives in [PcgSpawnAlertModeStore] and the
 * stores beside it, and the chat's bell is drawn from that selection
 * ([PcgAlertBellDescription]). An installation upgraded from such a build can still hold the
 * old keys, so account removal clears them with the rest of the profile's local data.
 */
object PushSettingsStore {
    private const val PREFS_NAME = "push_settings"

    private fun normalizeProfileId(profileId: String?): String {
        return profileId?.trim()?.lowercase().orEmpty()
    }

    private fun legacyEnabledKey(profileId: String): String {
        return "push_enabled_${normalizeProfileId(profileId)}"
    }

    private fun modeKey(profileId: String): String {
        return "push_mode_${normalizeProfileId(profileId)}"
    }

    /**
     * Deletes legacy push settings for one profile.
     *
     * New PCG notification mode is stored in PcgSpawnAlertModeStore, but this legacy
     * store can still contain old keys from previous builds, so account deletion
     * clears both generations.
     */
    fun clearProfile(context: Context, profileId: String) {
        val normalized = normalizeProfileId(profileId)
        if (normalized.isEmpty()) return

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(legacyEnabledKey(normalized))
                remove(modeKey(normalized))
            }
    }
}
