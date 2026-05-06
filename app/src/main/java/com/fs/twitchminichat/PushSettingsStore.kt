package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

object PushSettingsStore {
    private const val PREFS_NAME = "push_settings"

    const val MODE_DEX_AND_A_TIER = 0
    const val MODE_ALL_SPAWNS = 1
    const val MODE_OFF = 2

    private fun normalizeProfileId(profileId: String?): String {
        return profileId?.trim()?.lowercase().orEmpty()
    }

    private fun legacyEnabledKey(profileId: String): String {
        return "push_enabled_${normalizeProfileId(profileId)}"
    }

    private fun modeKey(profileId: String): String {
        return "push_mode_${normalizeProfileId(profileId)}"
    }

    private fun coerceMode(mode: Int): Int {
        return when (mode) {
            MODE_DEX_AND_A_TIER,
            MODE_ALL_SPAWNS,
            MODE_OFF -> mode

            else -> MODE_DEX_AND_A_TIER
        }
    }

    fun getPushMode(context: Context, profileId: String): Int {
        val normalized = normalizeProfileId(profileId)
        if (normalized.isEmpty()) return MODE_DEX_AND_A_TIER

        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val modeKey = modeKey(normalized)
        if (prefs.contains(modeKey)) {
            return coerceMode(
                prefs.getInt(modeKey, MODE_DEX_AND_A_TIER)
            )
        }

        // Compatibility with old boolean system:
        // true  -> Dex + A-tier
        // false -> Off
        val legacyKey = legacyEnabledKey(normalized)
        if (prefs.contains(legacyKey)) {
            val legacyEnabled = prefs.getBoolean(legacyKey, true)
            return if (legacyEnabled) {
                MODE_DEX_AND_A_TIER
            } else {
                MODE_OFF
            }
        }

        return MODE_DEX_AND_A_TIER
    }

    fun isPushEnabled(context: Context, profileId: String): Boolean {
        return getPushMode(context, profileId) != MODE_OFF
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