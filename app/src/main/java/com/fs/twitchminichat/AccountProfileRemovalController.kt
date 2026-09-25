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
     * Local deletion remains immediate from the user's point of view, and backend failure
     * never prevents it.
     *
     * The backend session is now retained until the notification-disable request has been
     * **acknowledged**, not merely attempted. Removing it right after the attempt - which
     * is what this did - left `BackendAuthHeaderProvider` resolving `Missing`, so a failed
     * disable could never be retried by anything: the profile stayed in that device's
     * `profile_ids` on the server for good, still receiving alerts that nothing on this
     * phone could switch off. The device credential is untouched by an account removal, so
     * it is already there; the session is the part that had to stop being thrown away.
     *
     * A failure leaves the disable owed, hands it to [OwedServerOperationWorker] and calls
     * [onAlertDisableOwed] so the user is told, rather than being left to discover it from
     * alerts that keep arriving.
     */
    fun removeAccountFromDevice(
        context: Context,
        account: AccountConfig,
        onAlertDisableOwed: () -> Unit = {},
        onComplete: (Result) -> Unit
    ) {
        val appContext = context.applicationContext
        val profileId = AccountProfileIdResolver.resolve(account)

        Log.d(
            TAG,
            "removeAccountFromDevice requested"
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
            selection = PcgProfileAlertSelection.DISABLED
        ) { backendOk ->
            if (backendOk) {
                val backendSessionRemoved =
                    BackendSessionStore(appContext).removeProfile(profileId)

                Log.d(
                    TAG,
                    "backend notification disable completed ok=true " +
                        "backendSessionRemoved=$backendSessionRemoved"
                )
                return@setProfileSpawnAlertMode
            }

            /*
             * The session stays. It is what the retry authenticates with, and there is
             * nothing else on this phone that could: the account is already gone from
             * the visible list, so no later start-up pass would iterate it.
             *
             * This is a deliberate exception to the rule that nothing is queued or
             * retried automatically. That rule keeps gameplay and alerts from acting
             * without the user. Here the opposite is at stake: data left active on the
             * server after the user asked for it to stop. Finishing that is not acting
             * on the user's behalf, and nothing in this path can send a gameplay command
             * or raise a notification.
             */
            OwedServerOperationStore.recordOwedProfileDisable(appContext, profileId)
            OwedServerOperationWorker.enqueue(appContext)

            Log.d(
                TAG,
                "backend notification disable completed ok=false; " +
                    "recorded as owed, backend session kept"
            )

            onAlertDisableOwed()
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
        PcgPokedexSnapshotStore.clearProfile(context, profileId)
        CatchPresetStore.clearProfile(context, profileId)
        BuddyInfoStore.clearProfile(context, profileId)
        PcgSpawnAlertModeStore.clearProfile(context, profileId)
        PcgEventSpawnAlertStore.clearProfile(context, profileId)
        PushSettingsStore.clearProfile(context, profileId)
        PcgMostWantedStore(context).clearProfile(profileId)
    }
}
