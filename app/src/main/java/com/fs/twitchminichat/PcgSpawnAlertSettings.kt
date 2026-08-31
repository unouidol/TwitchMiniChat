package com.fs.twitchminichat

/**
 * Complete per-profile PCG spawn notification selection.
 *
 * Ordinary spawn filtering remains mutually exclusive through [regularMode].
 * Event spawns are independent and may remain enabled when the ordinary mode
 * is [PcgSpawnAlertMode.NONE].
 */
data class PcgSpawnAlertSettings(
    val regularMode: PcgSpawnAlertMode,
    val eventSpawnsEnabled: Boolean
) {
    /** Whether ordinary or event spawn alerts are enabled for this profile. */
    val hasOrdinaryOrEventAlerts: Boolean
        get() = regularMode.isPushEnabledForCompatibility || eventSpawnsEnabled

    companion object {
        /** Fully disables ordinary and configured-event spawn notifications. */
        val DISABLED = PcgSpawnAlertSettings(
            regularMode = PcgSpawnAlertMode.NONE,
            eventSpawnsEnabled = false
        )
    }
}
