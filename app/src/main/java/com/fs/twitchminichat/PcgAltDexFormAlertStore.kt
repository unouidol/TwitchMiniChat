package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

/**
 * Stores, per profile, whether Missing Alt Dex form alerts are on for this phone
 * (`docs/design-alt-dex-alert.md`, *f*).
 *
 * Off by default, for new installations and for updates (decision 2). Like the other categories
 * it is written only after the backend accepted the selection, and it is what the bell menu shows;
 * whether the backend has confirmed it is [PcgProfileAlertAcknowledgementStore]'s to say. Cleared
 * when its profile is removed, and by every local-data reset, which deletes every preferences file
 * it does not explicitly keep.
 */
object PcgAltDexFormAlertStore {

    private const val PREFS_NAME = "pcg_alt_dex_form_alerts"
    private const val KEY_PREFIX = "alt_dex_form_alerts_enabled_"

    /** Returns false for new, missing, or invalid profiles. */
    fun isEnabled(context: Context, profileId: String): Boolean {
        val cleanProfileId = AccountProfileIdResolver.normalize(profileId)
        if (cleanProfileId.isEmpty()) return false

        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREFIX + cleanProfileId, false)
    }

    /** Saves the preference for one profile. */
    fun setEnabled(
        context: Context,
        profileId: String,
        enabled: Boolean
    ) {
        val cleanProfileId = AccountProfileIdResolver.normalize(profileId)
        if (cleanProfileId.isEmpty()) return

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_PREFIX + cleanProfileId, enabled)
            }
    }

    /** Removes the preference of one profile. */
    fun clearProfile(context: Context, profileId: String) {
        val cleanProfileId = AccountProfileIdResolver.normalize(profileId)
        if (cleanProfileId.isEmpty()) return

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(KEY_PREFIX + cleanProfileId)
            }
    }
}
