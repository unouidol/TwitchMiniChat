package com.fs.twitchminichat

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
