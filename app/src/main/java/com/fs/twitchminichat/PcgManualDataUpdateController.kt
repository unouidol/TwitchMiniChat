package com.fs.twitchminichat

import android.content.Context
import android.widget.Toast

/**
 * Handles explicit user-triggered PCG data update requests.
 *
 * This controller exists so ChatFragment only wires UI buttons, while the
 * manual update behavior stays in a small, named component.
 */
class PcgManualDataUpdateController(
    private val context: Context,
    private val bridge: Bridge
) {

    /**
     * Small bridge interface implemented by the owner of the Gecko/WebExtension
     * connection.
     *
     * Keeping this as an interface avoids making this controller depend directly
     * on ChatFragment internals.
     */
    interface Bridge {
        fun requestManualPokedexUpdate(): Boolean
        fun requestManualInventoryUpdate(): Boolean
    }

    fun onRegisterPokedexClicked() {
        val requested = bridge.requestManualPokedexUpdate()

        if (requested) {
            Toast.makeText(
                context,
                context.getString(R.string.pcg_pokedex_update_requested),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.pcg_manual_update_not_ready),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun onRegisterInventoryClicked() {
        val requested = bridge.requestManualInventoryUpdate()

        if (requested) {
            Toast.makeText(
                context,
                context.getString(R.string.pcg_inventory_update_requested),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.pcg_manual_update_not_ready),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun onWrongPokedexTab() {
        Toast.makeText(
            context,
            context.getString(R.string.pcg_pokedex_wrong_tab),
            Toast.LENGTH_LONG
        ).show()
    }

    fun onWrongInventoryTab() {
        Toast.makeText(
            context,
            context.getString(R.string.pcg_inventory_wrong_tab),
            Toast.LENGTH_LONG
        ).show()
    }

    fun onManualUpdateFailed() {
        Toast.makeText(
            context,
            context.getString(R.string.pcg_manual_update_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}