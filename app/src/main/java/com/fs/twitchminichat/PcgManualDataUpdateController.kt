package com.fs.twitchminichat

import android.content.Context

/**
 * Handles the two manual PCG data update buttons shown above the PCG panel.
 *
 * This controller intentionally does not show an "update requested" toast.
 * The user should only see a toast when Android has a real outcome:
 *
 * - wrong tab: GeckoSessionManager shows the tab-specific instruction
 * - correct tab: GeckoSessionManager shows the update success message
 *
 * This keeps the UI less noisy and avoids telling the user that an update
 * started when the current PCG tab may actually be wrong.
 */
class PcgManualDataUpdateController(
    @Suppress("UNUSED_PARAMETER")
    context: Context,
    private val bridge: Bridge
) {

    /**
     * Small bridge used by the Activity so this controller does not need to know
     * about GeckoSessionManager, account IDs, or Activity state directly.
     */
    interface Bridge {

        /**
         * Requests a manual Pokédex snapshot registration.
         *
         * Returns false when the request cannot even be armed, for example
         * because the Activity has no account ID yet or the PCG session is not ready.
         */
        fun requestManualPokedexUpdate(): Boolean

        /**
         * Requests a manual Inventory snapshot registration.
         *
         * Returns false when the request cannot even be armed, for example
         * because the Activity has no account ID yet or the PCG session is not ready.
         */
        fun requestManualInventoryUpdate(): Boolean
    }

    /**
     * Called when the user presses Register Pokédex.
     *
     * No immediate toast is shown here. GeckoSessionManager will show either
     * "Pokédex updated" after a valid snapshot, or the wrong-tab instruction.
     */
    fun onRegisterPokedexClicked() {
        bridge.requestManualPokedexUpdate()
    }

    /**
     * Called when the user presses Register inventory.
     *
     * No immediate toast is shown here. GeckoSessionManager will show either
     * "Inventory updated" after a valid snapshot, or the wrong-tab instruction.
     */
    fun onRegisterInventoryClicked() {
        bridge.requestManualInventoryUpdate()
    }
}