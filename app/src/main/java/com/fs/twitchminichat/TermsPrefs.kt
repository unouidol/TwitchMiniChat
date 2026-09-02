package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

object TermsPrefs {
    private const val PREFS_NAME = "tmc_terms_prefs"
    private const val KEY_ACCEPTED_VERSION = "accepted_terms_version"

    // Aumentare questo numero quando cambiano in modo sostanziale i Terms/Privacy
    // 2: introduzione del crash reporting, dichiarato nel gate di accettazione
    private const val CURRENT_TERMS_VERSION = 2

    fun hasAcceptedCurrentVersion(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACCEPTED_VERSION, 0) >= CURRENT_TERMS_VERSION
    }

    fun markAcceptedCurrentVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(KEY_ACCEPTED_VERSION, CURRENT_TERMS_VERSION)
        }
    }

    fun clearAcceptance(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_ACCEPTED_VERSION)
        }
    }
}