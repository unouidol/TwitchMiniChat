package com.fs.twitchminichat

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Converts semantic PCG probe events into user-facing Android toast messages.
 *
 * Keeping this mapping outside the Activity/Fragment avoids spreading string-resource
 * decisions across the PCG bridge code. The WebExtension only reports what happened;
 * Android decides which localized string should be shown.
 */
object PcgProbeToastRouter {

    const val TYPE_INVENTORY_WRONG_TAB = "pcg_inventory_wrong_tab"
    const val TYPE_POKEDEX_WRONG_TAB = "pcg_pokedex_wrong_tab"

    /**
     * Shows a toast for PCG probe events that are directly user-facing.
     *
     * Returns true when the event was handled here, so the caller can skip any
     * generic fallback toast that would otherwise be shown for the same event.
     */
    fun showToastForProbeEvent(context: Context, type: String): Boolean {
        val messageRes = when (type) {
            TYPE_INVENTORY_WRONG_TAB -> R.string.pcg_inventory_wrong_tab
            TYPE_POKEDEX_WRONG_TAB -> R.string.pcg_pokedex_wrong_tab
            else -> return false
        }

        showLongToast(context, messageRes)
        return true
    }

    private fun showLongToast(context: Context, @StringRes messageRes: Int) {
        Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
    }
}