package com.fs.twitchminichat

import android.content.Context
import com.fs.twitchminichat.pcg.mostwanted.PcgMostWantedStore

/**
 * Complete profile-scoped PCG alert selection.
 *
 * Ordinary, event and Most Wanted alerts remain independently selectable.
 * Firebase delivery must stay registered while any one of them is enabled.
 */
data class PcgProfileAlertSelection(
    val spawnSettings: PcgSpawnAlertSettings,
    val mostWantedEnabled: Boolean
) {
    /** Whether this profile still needs Firebase Cloud Messaging delivery. */
    val requiresFirebaseDelivery: Boolean
        get() =
            spawnSettings.hasOrdinaryOrEventAlerts || mostWantedEnabled

    companion object {

        /**
         * Every category off: what an account removal asks the backend to store.
         *
         * Named here because three callers need the same value - the removal, the
         * owed-work queue and the policy that compares against it - and a literal
         * repeated in three places is free to stop being the same value.
         */
        val DISABLED = PcgProfileAlertSelection(
            spawnSettings = PcgSpawnAlertSettings.DISABLED,
            mostWantedEnabled = false
        )
    }
}

/** Reads the complete locally persisted alert selection for one profile. */
object PcgProfileAlertSelectionStore {

    /** Combines the three independent local category stores. */
    fun read(
        context: Context,
        profileId: String
    ): PcgProfileAlertSelection {
        val appContext = context.applicationContext
        return PcgProfileAlertSelection(
            spawnSettings = PcgSpawnAlertSettings(
                regularMode = PcgSpawnAlertModeStore.getMode(
                    appContext,
                    profileId
                ),
                eventSpawnsEnabled = PcgEventSpawnAlertStore.isEnabled(
                    appContext,
                    profileId
                )
            ),
            mostWantedEnabled = PcgMostWantedStore(appContext)
                .isEnabled(profileId)
        )
    }
}

/** One ordered operation of a profile's registration pass. */
enum class PcgProfileRegistrationSyncStep {

    /** Register this device's Firebase Cloud Messaging token for the profile. */
    REGISTER_TOKEN,

    /**
     * Send the profile's local alert selection to the backend.
     *
     * Named for the case it was written for, restoring an active selection after a
     * token registration, and it now also carries the opposite case: telling the
     * backend that nothing is active. Both are the same request with the same payload,
     * so they are the same step rather than two that could drift apart.
     */
    RESTORE_ALERT_SELECTION
}

/**
 * Decides what one profile's registration pass has to send to the backend.
 *
 * The backend token endpoint can refresh device credentials and registry data,
 * while the alert endpoint remains the source of truth for ordinary, event,
 * and Most Wanted delivery eligibility. Their order must therefore remain
 * deterministic.
 *
 * A profile with **no category active** used to get an empty plan, and that was the
 * defect: the plan was derived from whether a notification would be delivered, so
 * "nothing to deliver" was read as "nothing to say". A phone whose alerts were all
 * switched off therefore never told the server, the server went on holding an active
 * mode, and the application could show no alerts while alerts kept arriving. Such a
 * profile now gets a plan that sends the disabled selection - and nothing else, because
 * registering a token for a profile that wants no notification would only put the
 * registration back.
 *
 * What the backend does with a disabled selection: it drops the profile from that
 * device's `profile_ids`, which is what stops the alerts. It deletes nothing.
 *
 * The acknowledged selection is what keeps this from being one request per launch for
 * ever. [PcgProfileAlertPushPolicy] compares the two, so once the backend has confirmed
 * the disabled selection, later starts send nothing.
 */
object PcgProfileRegistrationSyncPlanner {

    /**
     * @param selection what this phone holds for the profile now.
     * @param acknowledged the last selection the backend confirmed, or null if it has
     *   never confirmed one. A profile with an active selection is registered either
     *   way: the token itself may have changed, which is not something the acknowledged
     *   alert selection can tell.
     */
    fun buildPlan(
        selection: PcgProfileAlertSelection,
        acknowledged: PcgProfileAlertSelection?
    ): List<PcgProfileRegistrationSyncStep> {
        if (selection.requiresFirebaseDelivery) {
            return listOf(
                PcgProfileRegistrationSyncStep.REGISTER_TOKEN,
                PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION
            )
        }

        return when (
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Present(selection),
                acknowledged = acknowledged
            )
        ) {
            PcgProfileAlertPushDecision.UpToDate -> emptyList()

            is PcgProfileAlertPushDecision.Push -> listOf(
                PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION
            )
        }
    }
}

/** One backend synchronization operation for an alert selection change. */
enum class PcgProfileAlertSyncStep {
    FIREBASE_DELIVERY,
    MOST_WANTED
}

/**
 * Produces the safe request order for one explicit alert-menu confirmation.
 *
 * Enabling Most Wanted on an inactive profile first restores Firebase delivery,
 * because the watchlist endpoint accepts only registered profiles. Disabling it
 * happens before the final delivery request so the profile can then be removed.
 */
object PcgProfileAlertSyncPlanner {

    fun buildPlan(
        current: PcgProfileAlertSelection,
        requested: PcgProfileAlertSelection
    ): List<PcgProfileAlertSyncStep> {
        if (current == requested) return emptyList()

        val spawnSettingsChanged =
            current.spawnSettings != requested.spawnSettings
        val mostWantedChanged =
            current.mostWantedEnabled != requested.mostWantedEnabled

        if (!mostWantedChanged) {
            return if (spawnSettingsChanged) {
                listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY)
            } else {
                emptyList()
            }
        }

        if (requested.mostWantedEnabled) {
            return if (!current.requiresFirebaseDelivery) {
                listOf(
                    PcgProfileAlertSyncStep.FIREBASE_DELIVERY,
                    PcgProfileAlertSyncStep.MOST_WANTED
                )
            } else {
                buildList {
                    add(PcgProfileAlertSyncStep.MOST_WANTED)
                    if (spawnSettingsChanged) {
                        add(PcgProfileAlertSyncStep.FIREBASE_DELIVERY)
                    }
                }
            }
        }

        return buildList {
            add(PcgProfileAlertSyncStep.MOST_WANTED)
            if (
                spawnSettingsChanged ||
                current.requiresFirebaseDelivery !=
                requested.requiresFirebaseDelivery
            ) {
                add(PcgProfileAlertSyncStep.FIREBASE_DELIVERY)
            }
        }
    }
}
