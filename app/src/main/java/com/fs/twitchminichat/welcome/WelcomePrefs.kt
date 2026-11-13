package com.fs.twitchminichat.welcome

import android.content.Context
import androidx.core.content.edit

/**
 * The welcome page's one preference file, holding a [WelcomeState].
 *
 * Both resets in Data & Account Control erase it, with no line of their own:
 * `LocalDataCleaner` deletes every file in `shared_prefs` by directory listing, except the
 * keep-accounts exclusions. This file is not among the exclusions, and
 * `LocalDataCleanerKeepAccountsTest` pins that. After either reset the page shows as on a
 * first launch, and the terms gate comes back with it.
 *
 * Nothing in it identifies the user: a version code, two release tags and a time.
 */
class WelcomePrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** The stored state. A key never written reads as `null`. */
    fun read(): WelcomeState = WelcomeState(
        lastSeenVersionCode = if (prefs.contains(KEY_LAST_SEEN_VERSION_CODE)) {
            prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, 0)
        } else {
            null
        },
        latestReleaseTag = prefs.getString(KEY_LATEST_RELEASE_TAG, null),
        dismissedReleaseTag = prefs.getString(KEY_DISMISSED_RELEASE_TAG, null),
        lastCheckEpochMs = if (prefs.contains(KEY_LAST_CHECK_EPOCH_MS)) {
            prefs.getLong(KEY_LAST_CHECK_EPOCH_MS, 0L)
        } else {
            null
        }
    )

    /*
     * Each write below touches only its own keys. The check's thread and the main thread
     * can write moments apart, and an edit that rewrote every key from a copy read
     * earlier could undo the other's change.
     */

    /** Records a start's decision: the version seen, and the tag whose notice was shown. */
    fun recordStart(record: StartRecord) {
        prefs.edit {
            putInt(KEY_LAST_SEEN_VERSION_CODE, record.lastSeenVersionCode)
            // Null only when no notice was ever shown, so there is nothing to overwrite.
            record.dismissedReleaseTag?.let { putString(KEY_DISMISSED_RELEASE_TAG, it) }
        }
    }

    /** Records that a version check is starting, before its request goes out. */
    fun recordCheckAttempt(epochMs: Long) {
        prefs.edit { putLong(KEY_LAST_CHECK_EPOCH_MS, epochMs) }
    }

    /** Records the tag a successful check found. */
    fun recordLatestTag(tag: String) {
        prefs.edit { putString(KEY_LATEST_RELEASE_TAG, tag) }
    }

    companion object {
        /** The file name, which the reset test checks against the keep-accounts exclusions. */
        const val PREFERENCES_NAME = "welcome_prefs"

        private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        private const val KEY_LATEST_RELEASE_TAG = "latest_release_tag"
        private const val KEY_DISMISSED_RELEASE_TAG = "dismissed_release_tag"
        private const val KEY_LAST_CHECK_EPOCH_MS = "last_check_epoch_ms"
    }
}
