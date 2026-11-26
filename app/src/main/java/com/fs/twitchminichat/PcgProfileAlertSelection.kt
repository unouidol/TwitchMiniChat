package com.fs.twitchminichat

import android.content.Context
import com.fs.twitchminichat.pcg.mostwanted.PcgMostWantedStore

/**
 * Complete profile-scoped PCG alert selection.
 *
 * Ordinary, event, Most Wanted and Missing Alt Dex form alerts remain independently
 * selectable. Firebase delivery must stay registered while any one of them is enabled.
 *
 * @property altDexFormAlertsEnabled Missing Alt Dex form alerts (`docs/design-alt-dex-alert.md`),
 *   off by default.
 */
data class PcgProfileAlertSelection(
    val spawnSettings: PcgSpawnAlertSettings,
    val mostWantedEnabled: Boolean,
    val altDexFormAlertsEnabled: Boolean
) {
    /** Whether this profile still needs Firebase Cloud Messaging delivery. */
    val requiresFirebaseDelivery: Boolean
        get() =
            spawnSettings.hasOrdinaryOrEventAlerts ||
                mostWantedEnabled ||
                altDexFormAlertsEnabled

    /**
     * This selection as far as the backend has confirmed it: the alt-dex category counts as on
     * only when it is on here *and* [acknowledged] has it on.
     *
     * The bell menu compares against this, not against the local selection, so that a category
     * the server refused (409 `alt_dex_alerts_off`) is sent again by the next OK, delivery first
     * and then the upload, while the checkbox keeps showing what the user chose. Only the alt-dex
     * category is masked: the owner's decision of 2026-11-25 covers that category alone.
     */
    fun confirmedBy(acknowledged: PcgProfileAlertSelection?): PcgProfileAlertSelection {
        return copy(
            altDexFormAlertsEnabled =
                altDexFormAlertsEnabled && acknowledged?.altDexFormAlertsEnabled == true
        )
    }

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
            mostWantedEnabled = false,
            altDexFormAlertsEnabled = false
        )
    }
}

/** Reads the complete locally persisted alert selection for one profile. */
object PcgProfileAlertSelectionStore {

    /** Combines the four independent local category stores. */
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
                .isEnabled(profileId),
            altDexFormAlertsEnabled = PcgAltDexFormAlertStore.isEnabled(
                appContext,
                profileId
            )
        )
    }

    /**
     * The local selection of [profileId] as the backend has confirmed it, the bell menu's
     * baseline: see [PcgProfileAlertSelection.confirmedBy].
     */
    fun readConfirmedBaseline(
        context: Context,
        profileId: String
    ): PcgProfileAlertSelection {
        return read(context, profileId).confirmedBy(
            PcgProfileAlertAcknowledgementStore.read(context, profileId)
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
    MOST_WANTED,

    /**
     * Upload the profile's stored alt-dex reading to `/upload_alt_dex`, if there is one.
     *
     * Always after [FIREBASE_DELIVERY]: the server refuses the upload while the category is off
     * there, so the selection has to be acknowledged first (decision 15).
     */
    ALT_DEX_UPLOAD
}

/**
 * Produces the safe request order for one explicit alert-menu confirmation.
 *
 * Enabling Most Wanted on an inactive profile first restores Firebase delivery,
 * because the watchlist endpoint accepts only registered profiles. Disabling it
 * happens before the final delivery request so the profile can then be removed.
 *
 * Turning Missing Alt Dex form alerts on adds [PcgProfileAlertSyncStep.ALT_DEX_UPLOAD] as the
 * last step, after the delivery step that tells the server the category is on. `current` is
 * the confirmed baseline ([PcgProfileAlertSelection.confirmedBy]), so a category the server
 * refused counts as off and is turned on again.
 */
object PcgProfileAlertSyncPlanner {

    fun buildPlan(
        current: PcgProfileAlertSelection,
        requested: PcgProfileAlertSelection
    ): List<PcgProfileAlertSyncStep> {
        if (current == requested) return emptyList()

        val steps = orderDeliveryAndWatchlist(current, requested)
        return if (
            !current.altDexFormAlertsEnabled &&
            requested.altDexFormAlertsEnabled
        ) {
            steps + PcgProfileAlertSyncStep.ALT_DEX_UPLOAD
        } else {
            steps
        }
    }

    /** The delivery and Most Wanted steps, in the order their endpoints need. */
    private fun orderDeliveryAndWatchlist(
        current: PcgProfileAlertSelection,
        requested: PcgProfileAlertSelection
    ): List<PcgProfileAlertSyncStep> {
        // The alt-dex category travels in the delivery request, like the ordinary and event ones.
        val spawnSettingsChanged =
            current.spawnSettings != requested.spawnSettings ||
                current.altDexFormAlertsEnabled != requested.altDexFormAlertsEnabled
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

/** Runs one alert-menu confirmation's plan. */
object PcgProfileAlertSyncRunner {

    /**
     * Runs [plan] in order and stops at the first step that fails, so that no step runs after
     * a failed one: an upload is never sent after a delivery step that failed (decision 15).
     *
     * Inline so that [runStep] may suspend in the bell menu's coroutine.
     *
     * @return whether every step succeeded.
     */
    inline fun runInOrder(
        plan: List<PcgProfileAlertSyncStep>,
        runStep: (PcgProfileAlertSyncStep) -> Boolean
    ): Boolean {
        for (step in plan) {
            if (!runStep(step)) return false
        }
        return true
    }
}
