package com.fs.twitchminichat

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Dialog used by the bell button to select Pokémon Community Game spawn
 * notification behavior for the current Twitch profile.
 *
 * Kept outside ChatFragment so the chat screen only has to open the selector
 * and react to the selected mode.
 */
object PcgSpawnAlertModeDialog {

    /**
     * Shows a mutually exclusive selector for the available spawn alert modes.
     */
    fun show(
        context: Context,
        currentMode: PcgSpawnAlertMode,
        onModeSelected: (PcgSpawnAlertMode) -> Unit
    ) {
        val modes = PcgSpawnAlertMode.values()

        val labels = modes.map { mode ->
            val title = context.getString(mode.titleRes)
            val description = context.getString(mode.descriptionRes)
            "$title\n$description"
        }.toTypedArray()

        val checkedIndex = modes.indexOf(currentMode).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle(R.string.spawn_alert_mode_dialog_title)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                onModeSelected(modes[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}