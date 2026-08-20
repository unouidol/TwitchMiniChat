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

/** One ordered operation used whenever an FCM token is registered. */
enum class PcgProfileRegistrationSyncStep {
    REGISTER_TOKEN,
    RESTORE_ALERT_SELECTION
}

/**
 * Preserves active alert categories across Firebase token registration.
 *
 * The backend token endpoint can refresh device credentials and registry data,
 * while the alert endpoint remains the source of truth for ordinary, event,
 * and Most Wanted delivery eligibility. Their order must therefore remain
 * deterministic.
 */
object PcgProfileRegistrationSyncPlanner {

    /** Returns the only safe order for an active profile registration. */
    fun buildPlan(
        selection: PcgProfileAlertSelection
    ): List<PcgProfileRegistrationSyncStep> {
        if (!selection.requiresFirebaseDelivery) return emptyList()

        return listOf(
            PcgProfileRegistrationSyncStep.REGISTER_TOKEN,
            PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION
        )
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
