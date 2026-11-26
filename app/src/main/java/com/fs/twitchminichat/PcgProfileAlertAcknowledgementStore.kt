package com.fs.twitchminichat

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Remembers, per profile, the alert selection the backend last acknowledged.
 *
 * Until this existed, the application knew what the user had chosen locally but not
 * what the server had been told. The two drift apart whenever a request fails, and
 * nothing could tell the difference between "the server already knows" and "the server
 * was never told", so nothing could be retried and nothing could be skipped.
 *
 * A selection is recorded here only after the backend answered 2xx for it, which is
 * what makes "acknowledged" mean something. A profile with no entry has had nothing
 * acknowledged, which is not the same as having been acknowledged as disabled - see
 * [PcgProfileAlertPushPolicy], where the difference decides whether a disable is owed.
 *
 * Kept by the keep-accounts reset (see [LocalDataCleaner.keepAccountsExclusions]): the
 * reset clears what the user chose on this phone, and the server's copy did not reset
 * with it. Forgetting the server's copy here would silently cancel work already owed.
 */
object PcgProfileAlertAcknowledgementStore {

    /**
     * Shared preferences file holding the acknowledged selections.
     *
     * Public so [LocalDataCleaner] can name it among the files a keep-accounts reset
     * leaves in place.
     */
    const val PREFERENCES_NAME = "pcg_alert_acknowledgement"

    private const val KEY_PREFIX_REGULAR_MODE = "regular_mode_"
    private const val KEY_PREFIX_EVENT_SPAWNS = "event_spawns_"
    private const val KEY_PREFIX_MOST_WANTED = "most_wanted_"
    private const val KEY_PREFIX_ALT_DEX_FORM_ALERTS = "alt_dex_form_alerts_"

    /**
     * Records that the backend has accepted [selection] for [profileId].
     *
     * Written synchronously: the caller is a background request thread that may be
     * followed immediately by a session removal, and an acknowledgement that lost the
     * race would leave the profile looking as if it still owed a disable.
     *
     * **The alt-dex category is never raised here.** A selection with it on keeps whatever was
     * acknowledged before, and one with it off records it off. The alert needs the list on the
     * server as well as the category, and only the bell menu's upload step confirms that, through
     * [setAltDexFormAlertsConfirmed]. So after a 409 `alt_dex_alerts_off` the category stays
     * unconfirmed until the upload is stored, not merely until a delivery request succeeds (the
     * owner's decision of 2026-11-25).
     */
    fun record(
        context: Context,
        profileId: String,
        selection: PcgProfileAlertSelection
    ) {
        record(preferences(context), profileId, selection)
    }

    /** [record] over the store's file, which a local test can supply (#117, F3). */
    internal fun record(
        preferences: SharedPreferences,
        profileId: String,
        selection: PcgProfileAlertSelection
    ) {
        val normalizedProfileId = AccountProfileIdResolver.normalize(profileId)
        if (normalizedProfileId.isBlank()) return

        val altDexKey = KEY_PREFIX_ALT_DEX_FORM_ALERTS + normalizedProfileId
        val altDexConfirmed =
            selection.altDexFormAlertsEnabled && preferences.getBoolean(altDexKey, false)

        preferences.edit(commit = true) {
            putInt(
                KEY_PREFIX_REGULAR_MODE + normalizedProfileId,
                selection.spawnSettings.regularMode.id
            )
            putBoolean(
                KEY_PREFIX_EVENT_SPAWNS + normalizedProfileId,
                selection.spawnSettings.eventSpawnsEnabled
            )
            putBoolean(
                KEY_PREFIX_MOST_WANTED + normalizedProfileId,
                selection.mostWantedEnabled
            )
            putBoolean(altDexKey, altDexConfirmed)
        }
    }

    /**
     * Sets the acknowledged alt-dex category of [profileId] to [confirmed], and nothing else.
     *
     * Written only when an acknowledgement already exists for the profile: without one the
     * confirmed baseline is already off, and a lone key would claim an acknowledgement the
     * backend never gave. Callers are [PcgAltDexAlertSync]: confirmed after the bell menu's
     * upload step is stored or finds no reading, unconfirmed after a 409 `alt_dex_alerts_off`.
     *
     * @return whether anything was written.
     */
    internal fun setAltDexFormAlertsConfirmed(
        preferences: SharedPreferences,
        profileId: String,
        confirmed: Boolean
    ): Boolean {
        val normalizedProfileId = AccountProfileIdResolver.normalize(profileId)
        if (normalizedProfileId.isBlank()) return false
        if (!preferences.contains(KEY_PREFIX_REGULAR_MODE + normalizedProfileId)) return false

        preferences.edit(commit = true) {
            putBoolean(KEY_PREFIX_ALT_DEX_FORM_ALERTS + normalizedProfileId, confirmed)
        }
        return true
    }

    /**
     * Returns the last acknowledged selection for [profileId], or null if there is none.
     *
     * Null is a distinct answer, not a default: it means the backend has never confirmed
     * anything for this profile, so nothing can be deduced about what it holds.
     */
    fun read(context: Context, profileId: String): PcgProfileAlertSelection? {
        return read(preferences(context), profileId)
    }

    /** [read] over the store's file. */
    internal fun read(
        preferences: SharedPreferences,
        profileId: String
    ): PcgProfileAlertSelection? {
        val normalizedProfileId = AccountProfileIdResolver.normalize(profileId)
        if (normalizedProfileId.isBlank()) return null

        val modeKey = KEY_PREFIX_REGULAR_MODE + normalizedProfileId
        if (!preferences.contains(modeKey)) return null

        return PcgProfileAlertSelection(
            spawnSettings = PcgSpawnAlertSettings(
                regularMode = PcgSpawnAlertMode.fromId(
                    preferences.getInt(modeKey, PcgSpawnAlertMode.DEFAULT.id)
                ),
                eventSpawnsEnabled = preferences.getBoolean(
                    KEY_PREFIX_EVENT_SPAWNS + normalizedProfileId,
                    false
                )
            ),
            mostWantedEnabled = preferences.getBoolean(
                KEY_PREFIX_MOST_WANTED + normalizedProfileId,
                false
            ),
            // Absent in an entry written before the category existed: off, the server's default.
            altDexFormAlertsEnabled = preferences.getBoolean(
                KEY_PREFIX_ALT_DEX_FORM_ALERTS + normalizedProfileId,
                false
            )
        )
    }

    /**
     * Every profile with an acknowledged selection, including profiles whose account is
     * no longer on this phone.
     *
     * That is the point of it: once an account is removed it is gone from
     * [AccountRepository], so this is the only list that still names a profile whose
     * alerts the server may be delivering.
     */
    fun knownProfileIds(context: Context): Set<String> {
        return knownProfileIds(preferences(context))
    }

    /** [knownProfileIds] over the store's file. */
    internal fun knownProfileIds(preferences: SharedPreferences): Set<String> {
        return preferences.all.keys
            .asSequence()
            .filter { it.startsWith(KEY_PREFIX_REGULAR_MODE) }
            .map { it.removePrefix(KEY_PREFIX_REGULAR_MODE) }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /** Forgets one profile's acknowledged selection. */
    fun clearProfile(context: Context, profileId: String) {
        val normalizedProfileId = AccountProfileIdResolver.normalize(profileId)
        if (normalizedProfileId.isBlank()) return

        preferences(context).edit(commit = true) {
            remove(KEY_PREFIX_REGULAR_MODE + normalizedProfileId)
            remove(KEY_PREFIX_EVENT_SPAWNS + normalizedProfileId)
            remove(KEY_PREFIX_MOST_WANTED + normalizedProfileId)
            remove(KEY_PREFIX_ALT_DEX_FORM_ALERTS + normalizedProfileId)
        }
    }

    /** The store's file, [PREFERENCES_NAME]. */
    internal fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
}
