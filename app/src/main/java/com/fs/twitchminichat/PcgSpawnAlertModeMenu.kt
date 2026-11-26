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
 * It exposes spawn alert modes, Most Wanted alerts, Missing Alt Dex form alerts and
 * local notification delivery preferences. It never sends chat or gameplay commands.
 */
object PcgSpawnAlertModeMenu {

    /**
     * Shows the complete informative alert menu.
     *
     * Most Wanted and Missing Alt Dex form alerts are independent from ordinary and
     * event spawn alerts and from each other. All changed values are applied only after
     * the explicit OK tap.
     *
     * @param currentSelection what this phone holds, which the menu shows.
     * @param confirmedSelection [currentSelection] as the backend has confirmed it
     *   ([PcgProfileAlertSelection.confirmedBy]). OK calls back when the selection differs
     *   from this one, so a Missing Alt Dex form category the server refused is sent again
     *   although its checkbox still shows it on.
     * @param altDexReadingStored whether this phone holds an accepted alt-dex reading for
     *   the profile. Without one, the ticked Missing Alt Dex form checkbox shows a hint on
     *   how to make one (decision 3 of `docs/design-alt-dex-alert.md`).
     */
    fun show(
        anchor: View,
        currentSelection: PcgProfileAlertSelection,
        confirmedSelection: PcgProfileAlertSelection,
        altDexReadingStored: Boolean,
        onMostWantedRequested: () -> Unit,
        onSelectionSelected: (PcgProfileAlertSelection) -> Unit
    ) {
        val context = anchor.context
        var selectedMode = currentSelection.spawnSettings.regularMode

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
                isChecked =
                    mode == currentSelection.spawnSettings.regularMode
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
            isChecked =
                currentSelection.spawnSettings.eventSpawnsEnabled
            setPadding(0, dp(context, 8), 0, dp(context, 2))
        }
        root.addView(eventSpawnCheckBox)

        val mostWantedCheckBox = CheckBox(context).apply {
            text = context.getString(R.string.pcg_most_wanted_enabled)
            isChecked = currentSelection.mostWantedEnabled
            setPadding(0, dp(context, 2), 0, dp(context, 2))
        }
        root.addView(mostWantedCheckBox)

        val altDexCheckBox = CheckBox(context).apply {
            text = context.getString(R.string.pcg_alt_dex_form_alert_label)
            isChecked = currentSelection.altDexFormAlertsEnabled
            setPadding(0, dp(context, 2), 0, dp(context, 2))
        }
        root.addView(altDexCheckBox)

        val altDexNoReadingHint = TextView(context).apply {
            text = context.getString(R.string.pcg_alt_dex_form_alert_no_reading)
            textSize = 12f
            setPadding(dp(context, 32), 0, 0, dp(context, 4))
        }
        root.addView(altDexNoReadingHint)

        fun updateAltDexNoReadingHint() {
            altDexNoReadingHint.visibility =
                if (showsNoAltDexReadingHint(altDexCheckBox.isChecked, altDexReadingStored)) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
        updateAltDexNoReadingHint()
        altDexCheckBox.setOnCheckedChangeListener { _, _ -> updateAltDexNoReadingHint() }

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

        val reminderCheckBox = CheckBox(context).apply {
            text = context.getString(
                R.string.pcg_notification_reminder_label
            )
            isChecked =
                PcgNotificationAlertPrefsStore.isReminderEnabled(context)
            setPadding(0, dp(context, 2), 0, dp(context, 2))
        }

        root.addView(soundCheckBox)
        root.addView(vibrationCheckBox)
        root.addView(reminderCheckBox)

        val scrollView = ScrollView(context).apply {
            addView(root)
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.pcg_spawn_alert_dialog_title)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val selectedSelection = PcgProfileAlertSelection(
                    spawnSettings = PcgSpawnAlertSettings(
                        regularMode = selectedMode,
                        eventSpawnsEnabled =
                            eventSpawnCheckBox.isChecked
                    ),
                    mostWantedEnabled = mostWantedCheckBox.isChecked,
                    altDexFormAlertsEnabled = altDexCheckBox.isChecked
                )

                if (selectedSelection != confirmedSelection) {
                    onSelectionSelected(selectedSelection)
                }

                PcgNotificationAlertPrefsStore.setSoundEnabled(
                    context = context,
                    enabled = soundCheckBox.isChecked
                )
                PcgNotificationAlertPrefsStore.setVibrationEnabled(
                    context = context,
                    enabled = vibrationCheckBox.isChecked
                )
                PcgNotificationAlertPrefsStore.setReminderEnabled(
                    context = context,
                    enabled = reminderCheckBox.isChecked
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

    /**
     * Whether the hint under the Missing Alt Dex form checkbox is shown: while the box is
     * ticked and this phone holds no alt-dex reading for the profile. It is the same
     * condition under which the bell menu's turn-on has nothing to upload
     * ([PcgAltDexAlertSyncOutcome.NO_READING]), whose toast repeats the hint once after OK.
     */
    internal fun showsNoAltDexReadingHint(checked: Boolean, readingStored: Boolean): Boolean =
        checked && !readingStored

    /** Converts density-independent pixels to raw pixels. */
    private fun dp(
        context: android.content.Context,
        value: Int
    ): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
