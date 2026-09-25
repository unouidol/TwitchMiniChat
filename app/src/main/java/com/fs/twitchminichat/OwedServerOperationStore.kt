package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

/**
 * Records the server-facing work this device has been asked to do and has not done.
 *
 * An erase must not depend on a reachable server: the phone is wiped even when the
 * request that removes this device from the backend fails. What the wipe takes with it
 * is the ability to finish that request later, because it erases both the backend
 * session and the device credential that `/delete_device_data` authenticates with.
 *
 * Exactly one server-facing operation survives a wipe, because it needs neither:
 * deleting the Firebase Cloud Messaging token. Without a token the phone receives
 * nothing at all, and the backend drops the registration the next time it tries to
 * send to it.
 *
 * The second kind of owed work, an account removal's alert disable, is not constrained
 * that way: an account removal does not wipe the phone, so the backend session and the
 * device credential it authenticates with are still there, and are deliberately kept
 * until the backend acknowledges the disable. See [AccountProfileRemovalController].
 *
 * Deliberately *not* carried across the wipe: the backend session and the device
 * credential. Keeping them would let the device removal itself be retried, at the
 * price of leaving a usable session on a phone the user has been told is erased,
 * which contradicts both the erase and the published data deletion page. That was
 * considered and refused; it is not an omission.
 *
 * The file is written *after* the wipe - the wipe deletes every shared preferences
 * file, so a record written before it would be erased by it - and it is kept by the
 * keep-accounts reset (see [LocalDataCleaner.keepAccountsExclusions]) so a later reset
 * cannot silently drop work the user already asked for.
 */
object OwedServerOperationStore {

    /**
     * Shared preferences file holding what is still owed.
     *
     * Public so [LocalDataCleaner] can name it among the files the keep-accounts reset
     * leaves in place.
     */
    const val PREFERENCES_NAME = "owed_server_operations"

    /** True while this device still owes a Firebase Cloud Messaging token deletion. */
    private const val KEY_FIREBASE_TOKEN_DELETION = "firebase_token_deletion_owed"

    /** Profile identifiers whose alert disable was attempted and did not go through. */
    private const val KEY_OWED_PROFILE_DISABLES = "profile_alert_disables_owed"

    /**
     * Records that the Firebase Cloud Messaging token still has to be deleted.
     *
     * Written synchronously: the erase restarts the application immediately after, and
     * a record that lost the race would leave the phone receiving alerts with nothing
     * anywhere saying so.
     */
    fun recordFirebaseTokenDeletion(context: Context) {
        preferences(context).edit(commit = true) {
            putBoolean(KEY_FIREBASE_TOKEN_DELETION, true)
        }
    }

    /** Whether a Firebase Cloud Messaging token deletion is still owed. */
    fun isFirebaseTokenDeletionOwed(context: Context): Boolean {
        return preferences(context).getBoolean(KEY_FIREBASE_TOKEN_DELETION, false)
    }

    /**
     * Drops the owed Firebase Cloud Messaging token deletion.
     *
     * Called when it has been done, and when signing in has made it void: see
     * [OwedTokenDeletionPolicy] for why a new account voids it rather than delaying it.
     */
    fun clearFirebaseTokenDeletion(context: Context) {
        preferences(context).edit(commit = true) {
            remove(KEY_FIREBASE_TOKEN_DELETION)
        }
    }

    /**
     * Records that [profileId] still owes the backend an alert disable.
     *
     * Written only after an attempt has failed. It exists because the
     * acknowledged-selection record cannot answer every case on its own: an installation
     * upgrading into this version has acknowledged nothing yet, so an account removed
     * before any successful push would otherwise look as if it owed nothing. See
     * [OwedProfileDisablePolicy].
     */
    fun recordOwedProfileDisable(context: Context, profileId: String) {
        val normalizedProfileId = AccountProfileIdResolver.normalize(profileId)
        if (normalizedProfileId.isBlank()) return

        val updated = owedProfileDisables(context) + normalizedProfileId
        preferences(context).edit(commit = true) {
            putStringSet(KEY_OWED_PROFILE_DISABLES, updated)
        }
    }

    /** Whether [profileId] is on record as owing an alert disable. */
    fun isProfileDisableOwed(context: Context, profileId: String): Boolean {
        val normalizedProfileId = AccountProfileIdResolver.normalize(profileId)
        return normalizedProfileId.isNotBlank() &&
            normalizedProfileId in owedProfileDisables(context)
    }

    /**
     * Every profile on record as owing an alert disable.
     *
     * Read together with [PcgProfileAlertAcknowledgementStore.knownProfileIds]: either
     * list can name a profile the other does not.
     */
    fun owedProfileDisables(context: Context): Set<String> {
        return preferences(context)
            .getStringSet(KEY_OWED_PROFILE_DISABLES, emptySet())
            .orEmpty()
    }

    /**
     * Drops the owed disable for [profileId].
     *
     * Called when the backend has acknowledged it, and when signing the account in again
     * has made it void: see [OwedProfileDisablePolicy] for why that cancels it rather
     * than delaying it.
     */
    fun clearOwedProfileDisable(context: Context, profileId: String) {
        val normalizedProfileId = AccountProfileIdResolver.normalize(profileId)
        if (normalizedProfileId.isBlank()) return

        val updated = owedProfileDisables(context) - normalizedProfileId
        preferences(context).edit(commit = true) {
            putStringSet(KEY_OWED_PROFILE_DISABLES, updated)
        }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
}
