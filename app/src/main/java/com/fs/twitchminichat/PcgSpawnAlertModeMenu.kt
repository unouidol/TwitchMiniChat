package com.fs.twitchminichat

import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.fs.twitchminichat.pcg.PcgNotificationAlertPrefsStore
import com.fs.twitchminichat.pcg.PcgNotificationChannelManager

/**
 * Dialog anchored conceptually to the bell button.
 *
 * It exposes spawn alert modes, Most Wanted alerts and local notification
 * delivery preferences. It never sends chat or gameplay commands.
 */
object PcgSpawnAlertModeMenu {

    /**
     * Shows the complete informative alert menu.
     *
     * Most Wanted is independent from ordinary and event spawn alerts. All
     * changed values are applied only after the explicit OK tap.
     */
    fun show(
        anchor: View,
        currentSettings: PcgSpawnAlertSettings,
        currentMostWantedEnabled: Boolean,
        onMostWantedRequested: () -> Unit,
        onMostWantedEnabledSelected: (Boolean) -> Unit,
        onSettingsSelected: (PcgSpawnAlertSettings) -> Unit
    ) {
        val context = anchor.context
        var selectedMode = currentSettings.regularMode

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(context, 20),
                dp(context, 12),
                dp(context, 20),
                dp(context, 4)
            )
        }

        val modeHeader = TextView(context).apply {
            text = context.getString(R.string.pcg_spawn_alert_modes_header)
            textSize = 14f
            setPadding(0, 0, 0, dp(context, 8))
        }
        root.addView(modeHeader)

        val modeRadioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
        }

        PcgSpawnAlertMode.entries.forEach { mode ->
            val radioButton = RadioButton(context).apply {
                id = View.generateViewId()
                text = context.getString(mode.titleRes)
                isChecked = mode == currentSettings.regularMode
                setPadding(0, dp(context, 2), 0, dp(context, 2))

                /*
                 * Store the enum on the view so selection does not depend on
                 * generated menu identifiers.
                 */
                tag = mode
            }
            modeRadioGroup.addView(radioButton)
        }

        modeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val checked = group.findViewById<RadioButton>(checkedId)
            val mode = checked?.tag as? PcgSpawnAlertMode
            if (mode != null) {
                selectedMode = mode
            }
        }
        root.addView(modeRadioGroup)

        val eventSpawnCheckBox = CheckBox(context).apply {
            text = context.getString(R.string.pcg_event_spawn_alert_label)
            isChecked = currentSettings.eventSpawnsEnabled
            setPadding(0, dp(context, 8), 0, dp(context, 2))
        }
        root.addView(eventSpawnCheckBox)

        val mostWantedCheckBox = CheckBox(context).apply {
            text = context.getString(R.string.pcg_most_wanted_enabled)
            isChecked = currentMostWantedEnabled
            setPadding(0, dp(context, 2), 0, dp(context, 2))
        }
        root.addView(mostWantedCheckBox)

        val divider = View(context).apply {
            setPadding(0, dp(context, 8), 0, dp(context, 8))
        }
        root.addView(divider)

        val deliveryHeader = TextView(context).apply {
            text = context.getString(R.string.pcg_notification_delivery_header)
            textSize = 14f
            setPadding(0, dp(context, 12), 0, dp(context, 8))
        }
        root.addView(deliveryHeader)

        val soundCheckBox = CheckBox(context).apply {
            text = context.getString(R.string.pcg_notification_sound_label)
            isChecked = PcgNotificationAlertPrefsStore.isSoundEnabled(context)
            setPadding(0, dp(context, 2), 0, dp(context, 2))
        }

        val vibrationCheckBox = CheckBox(context).apply {
            text = context.getString(
                R.string.pcg_notification_vibration_label
            )
            isChecked =
                PcgNotificationAlertPrefsStore.isVibrationEnabled(context)
            setPadding(0, dp(context, 2), 0, dp(context, 2))
        }

        root.addView(soundCheckBox)
        root.addView(vibrationCheckBox)

        val scrollView = ScrollView(context).apply {
            addView(root)
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.pcg_spawn_alert_dialog_title)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val selectedSettings = PcgSpawnAlertSettings(
                    regularMode = selectedMode,
                    eventSpawnsEnabled = eventSpawnCheckBox.isChecked
                )

                if (selectedSettings != currentSettings) {
                    onSettingsSelected(selectedSettings)
                }

                if (
                    mostWantedCheckBox.isChecked !=
                    currentMostWantedEnabled
                ) {
                    onMostWantedEnabledSelected(
                        mostWantedCheckBox.isChecked
                    )
                }

                PcgNotificationAlertPrefsStore.setSoundEnabled(
                    context = context,
                    enabled = soundCheckBox.isChecked
                )
                PcgNotificationAlertPrefsStore.setVibrationEnabled(
                    context = context,
                    enabled = vibrationCheckBox.isChecked
                )

                /*
                 * Safe to call repeatedly. The next notification resolves the
                 * channel matching the updated local delivery preferences.
                 */
                PcgNotificationChannelManager.ensureChannels(context)
            }
            .setNeutralButton(R.string.pcg_most_wanted_title) { _, _ ->
                onMostWantedRequested()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Converts density-independent pixels to raw pixels. */
    private fun dp(
        context: android.content.Context,
        value: Int
    ): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}