package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

object TermsPrefs {
    private const val PREFS_NAME = "tmc_terms_prefs"
    private const val KEY_ACCEPTED_VERSION = "accepted_terms_version"

    // Aumentare questo numero quando cambiano in modo sostanziale i Terms/Privacy
    // 2: introduzione del crash reporting, dichiarato nel gate di accettazione
    // 3: GitHub named as a recipient, for the stream player page and the version check.
    //    A new recipient is a material change by the criterion of 2026-09-26, so every
    //    user who accepted version 2 sees the gate once more, on first opening 5.6.0.
    //    Cloudflare is named in the same version. "Hosting or infrastructure providers"
    //    already covered it, so naming it is a correction and would not raise the gate.
    //    The Pokemon Community Game statement is in the same version too: a clarification,
    //    with no new recipient and nothing newly collected.
    //    So is section 2's naming of the alert settings, the Most Wanted watchlists and the
    //    phone's model in each registration: v5.4.1 already sent all three, before version 2
    //    existed, so naming them corrects an omission and collects nothing new.
    /** The terms version a stored acceptance must reach; see [isAccepted]. */
    internal const val CURRENT_TERMS_VERSION = 3

    /**
     * Whether an acceptance stored as [acceptedVersion] covers the current terms.
     *
     * Raising [CURRENT_TERMS_VERSION] is what re-shows the gate: an acceptance of an
     * earlier version no longer counts, and `MainActivity.onResume` raises the gate.
     */
    internal fun isAccepted(acceptedVersion: Int): Boolean =
        acceptedVersion >= CURRENT_TERMS_VERSION

    fun hasAcceptedCurrentVersion(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return isAccepted(prefs.getInt(KEY_ACCEPTED_VERSION, 0))
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
