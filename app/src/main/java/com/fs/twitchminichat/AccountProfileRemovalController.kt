package com.fs.twitchminichat

import android.content.Context
import android.util.Log

/**
 * Handles removal of one saved login account and the local data tied to its PCG profile.
 *
 * This controller intentionally lives outside LoginFragment so the login screen does
 * not become responsible for knowing every profile-scoped store and backend cleanup
 * detail.
 */
object AccountProfileRemovalController {

    private const val TAG = "ACCOUNT_REMOVE"

    data class Result(
        val removedAccount: Boolean,
        val profileId: String
    )

    /**
     * Removes one account from this device and deletes local data for the same profile.
     *
     * Local deletion is performed first and completes immediately from the user's point
     * of view. Server notification cleanup is then started as a best-effort request so
     * a slow network call cannot make the X button feel broken.
     */
    fun removeAccountFromDevice(
        context: Context,
        account: AccountConfig,
        onComplete: (Result) -> Unit
    ) {
        val appContext = context.applicationContext
        val profileId = resolveProfileId(account)

        Log.d(
            TAG,
            "removeAccountFromDevice requested accountId=${account.id}"
        )

        if (profileId.isNotBlank()) {
            /*
             * First disable the local notification state. This guarantees that if any
             * screen reads the setting after removal, it does not still see the profile
             * as notification-enabled.
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

        /*
         * Remove the account immediately. This is the main user-visible action, so it
         * must not wait for network calls.
         */
        val removedAccount = AccountRepository(appContext).removeById(account.id) != null
        TwitchEmoteCatalogStore(appContext).clearAccount(account.id)
        TwitchEmoteRecentStore(appContext).clearAccount(account.id)
        TwitchIrcSessionMetadataStore.remove(account.id)
        val backendSessionRemoved = BackendSessionStore(appContext).removeProfile(profileId)

        Log.d(
            TAG,
            "local removal finished removedAccount=$removedAccount " +
                    "backendSessionRemoved=$backendSessionRemoved"
        )

        onComplete(
            Result(
                removedAccount = removedAccount,
                profileId = profileId
            )
        )

        /*
         * Backend notification cleanup is intentionally best-effort.
         *
         * We do it after local deletion so the login page updates immediately. If this
         * request fails, the future Privacy & Safety "remove device and server data"
         * flow can still clean server-side data explicitly.
         */
        if (profileId.isNotBlank()) {
            FcmRegistrationUploader.setProfileSpawnAlertMode(
                context = appContext,
                profileId = profileId,
                settings = PcgSpawnAlertSettings.DISABLED
            ) { backendOk ->
                Log.d(
                    TAG,
                    "backend notification disable completed ok=$backendOk"
                )
            }
        }
    }

    /**
     * Clears local stores that we have already inspected and confirmed are profile-scoped.
     *
     * More stores can be added here after inspecting their implementations, for example
     * catch preset storage, current spawn state, local Dex storage, and sound/vibration
     * notification preferences.
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
    }

    /**
     * Resolves the profile id used by local PCG stores and notification backend calls.
     *
     * New OAuth accounts should already have AccountConfig.profileId. The username
     * fallback keeps account removal useful for older saved accounts created before
     * profileId was persisted.
     */
    private fun resolveProfileId(account: AccountConfig): String {
        val explicitProfileId = account.profileId.trim().lowercase()
        if (explicitProfileId.isNotBlank()) return explicitProfileId

        return account.username.trim().lowercase()
    }
}
