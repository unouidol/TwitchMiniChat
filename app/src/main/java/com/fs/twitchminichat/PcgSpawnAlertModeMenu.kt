package com.fs.twitchminichat

import android.view.View
import androidx.appcompat.widget.PopupMenu

/**
 * Popup menu anchored to the bell button.
 *
 * It exposes the four mutually exclusive Pokémon Community Game spawn alert
 * modes while keeping ChatFragment free from menu-building details.
 */
object PcgSpawnAlertModeMenu {
    private const val MENU_GROUP_ALERT_MODES = 100
    private const val MENU_ORDER_BASE = 0

    /**
     * Shows the spawn alert mode menu.
     *
     * Only one mode can be checked at a time. The caller remains responsible for
     * saving the selected mode locally and syncing it to the backend.
     */
    fun show(
        anchor: View,
        currentMode: PcgSpawnAlertMode,
        onModeSelected: (PcgSpawnAlertMode) -> Unit
    ) {
        val context = anchor.context
        val popup = PopupMenu(context, anchor)

        PcgSpawnAlertMode.values().forEachIndexed { index, mode ->
            popup.menu.add(
                MENU_GROUP_ALERT_MODES,
                mode.id,
                MENU_ORDER_BASE + index,
                context.getString(mode.titleRes)
            ).apply {
                isCheckable = true
                isChecked = mode == currentMode
            }
        }

        /*
         * Treat the four menu items as a radio group, so only one mode can be
         * checked at a time.
         */
        popup.menu.setGroupCheckable(
            MENU_GROUP_ALERT_MODES,
            true,
            true
        )

        popup.setOnMenuItemClickListener { item ->
            val selectedMode = PcgSpawnAlertMode.fromId(item.itemId)

            if (selectedMode != currentMode) {
                onModeSelected(selectedMode)
            }

            true
        }

        popup.show()
    }
}