package com.fs.twitchminichat

import android.content.Context
import android.util.Log
import com.fs.twitchminichat.pcg.mostwanted.PcgMostWantedStore

/**
 * Handles removal of one saved login account and the local data tied to its PCG profile.
 *
 * This controller intentionally lives outside LoginFragment so the login screen does
 * not become responsible for knowing every profile-scoped store and backend cleanup
 * detail.
 */
object AccountProfileRemovalController {

    /** Logcat tag for account-removal diagnostics without sensitive credentials. */
    private const val TAG = "ACCOUNT_REMOVE"

    /** Result delivered after the local account has been removed. */
    data class Result(
        val removedAccount: Boolean,
        val profileId: String
    )

    /**
     * Removes one account from this device and deletes local data for the same profile.
     *
     * Local deletion remains immediate from the user's point of view. The backend
     * session is retained only until the best-effort notification-disable request has
     * finished, so a migrated account can authenticate that request with its Bearer
     * session. Backend failure never prevents local account removal.
     */
    fun removeAccountFromDevice(
        context: Context,
        account: AccountConfig,
        onComplete: (Result) -> Unit
    ) {
        val appContext = context.applicationContext
        val profileId = AccountProfileIdResolver.resolve(account)

        Log.d(
            TAG,
            "removeAccountFromDevice requested accountId=${account.id}"
        )

        if (profileId.isNotBlank()) {
            /*
             * Disable and clear every known local profile preference before the
             * account disappears from the visible list.
             */
            PcgSpawnAlertModeStore.setMode(
                context = appContext,
                profileId = profileId,
                mode = PcgSpawnAlertMode.NONE
            )
            PcgEventSpawnAlertStore.setEnabled(
                context = appContext,
                profileId = profileId,
                enabled = false
            )

            clearKnownLocalProfileData(
                context = appContext,
                profileId = profileId
            )
        } else {
            Log.w(TAG, "Profile id is blank; removing account without profile-scoped cleanup")
        }

        val removedAccount = AccountRepository(appContext).removeById(account.id) != null
        TwitchEmoteCatalogStore(appContext).clearAccount(account.id)
        TwitchEmoteRecentStore(appContext).clearAccount(account.id)
        TwitchIrcSessionMetadataStore.remove(account.id)

        Log.d(
            TAG,
            "local removal finished removedAccount=$removedAccount"
        )

        onComplete(
            Result(
                removedAccount = removedAccount,
                profileId = profileId
            )
        )

        if (profileId.isBlank()) {
            BackendSessionStore(appContext).removeProfile(profileId)
            return
        }

        /*
         * Keep the backend session until this request has selected and used its
         * authentication mode. Removing it earlier would force a migrated account
         * into the temporary legacy-key branch.
         */
        FcmRegistrationUploader.setProfileSpawnAlertMode(
            context = appContext,
            profileId = profileId,
            settings = PcgSpawnAlertSettings.DISABLED
        ) { backendOk ->
            val backendSessionRemoved =
                BackendSessionStore(appContext).removeProfile(profileId)

            Log.d(
                TAG,
                "backend notification disable completed ok=$backendOk " +
                    "backendSessionRemoved=$backendSessionRemoved"
            )
        }
    }

    /**
     * Clears local stores that are confirmed to be profile-scoped.
     */
    private fun clearKnownLocalProfileData(
        context: Context,
        profileId: String
    ) {
        InventoryBallStore.clearProfile(context, profileId)
        BuddyInfoStore.clearProfile(context, profileId)
        PcgSpawnAlertModeStore.clearProfile(context, profileId)
        PcgEventSpawnAlertStore.clearProfile(context, profileId)
        PushSettingsStore.clearProfile(context, profileId)
        PcgMostWantedStore(context).clearProfile(profileId)
    }
}