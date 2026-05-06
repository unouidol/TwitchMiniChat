package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

/**
 * Local source of truth for the selected spawn notification mode per Twitch profile.
 *
 * The server remains the final source of truth for actual notification delivery,
 * but storing this locally keeps the UI responsive and preserves the user's choice
 * between app launches.
 */
object PcgSpawnAlertModeStore {
    private const val PREFS_NAME = "pcg_spawn_alert_modes"
    private const val KEY_PREFIX = "profile_mode_"

    /**
     * Returns the locally saved spawn alert mode for a profile.
     */
    fun getMode(context: Context, profileId: String): PcgSpawnAlertMode {
        val cleanProfileId = profileId.trim().lowercase()
        if (cleanProfileId.isEmpty()) return PcgSpawnAlertMode.DEFAULT

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getInt(KEY_PREFIX + cleanProfileId, PcgSpawnAlertMode.DEFAULT.id)
        return PcgSpawnAlertMode.fromId(savedId)
    }


    /**
     * Saves the selected spawn alert mode for a profile.
     */
    fun setMode(
        context: Context,
        profileId: String,
        mode: PcgSpawnAlertMode
    ) {
        val cleanProfileId = profileId.trim().lowercase()
        if (cleanProfileId.isEmpty()) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(KEY_PREFIX + cleanProfileId, mode.id)
            }
    }

    /**
     * Deletes the locally remembered spawn alert mode for one profile.
     *
     * This only clears the local preference. Server-side delivery is disabled
     * separately through FcmRegistrationUploader.setProfileSpawnAlertMode(..., NONE).
     */
    fun clearProfile(context: Context, profileId: String) {
        val cleanProfileId = profileId.trim().lowercase()
        if (cleanProfileId.isEmpty()) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(KEY_PREFIX + cleanProfileId)
            }
    }
}