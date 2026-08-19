package com.fs.twitchminichat.pcg

import android.content.Context
import androidx.core.content.edit

/**
 * Stores how PCG alerts should notify the user.
 *
 * This store controls the local notification experience only. The selected
 * notification mode, such as Dexlist/A-Tier/all/muted, should remain in the
 * existing PCG notification mode store.
 */
object PcgNotificationAlertPrefsStore {

    private const val PREFS_NAME = "pcg_notification_alert_prefs"

    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_REMINDER_ENABLED = "reminder_enabled"

    /**
     * Default sound behavior for newly installed users.
     *
     * Keep this aligned with the current app behavior. If existing PCG alerts
     * already make sound, true preserves that behavior.
     */
    private const val DEFAULT_SOUND_ENABLED = true

    /**
     * Default vibration behavior for newly installed users.
     *
     * Keep this aligned with the current app behavior. If existing PCG alerts
     * already vibrate, true preserves that behavior.
     */
    private const val DEFAULT_VIBRATION_ENABLED = true

    /** Preserves the existing 45-second reminder behavior after an upgrade. */
    private const val DEFAULT_REMINDER_ENABLED = true

    /**
     * Returns whether PCG alert notifications should use a sound-capable channel.
     */
    fun isSoundEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
    }

    /**
     * Returns whether PCG alert notifications should use a vibration-capable channel.
     */
    fun isVibrationEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
    }

    /** Returns whether delayed 45-second spawn reminders should be displayed. */
    fun isReminderEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(
            KEY_REMINDER_ENABLED,
            DEFAULT_REMINDER_ENABLED
        )
    }

    /**
     * Stores whether future PCG alert notifications should use sound.
     */
    fun setSoundEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit {
            putBoolean(KEY_SOUND_ENABLED, enabled)
        }
    }

    /**
     * Stores whether future PCG alert notifications should use vibration.
     */
    fun setVibrationEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit {
            putBoolean(KEY_VIBRATION_ENABLED, enabled)
        }
    }

    /** Stores whether future delayed spawn reminders should be displayed. */
    fun setReminderEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit {
            putBoolean(KEY_REMINDER_ENABLED, enabled)
        }
    }

    /**
     * Returns the current alert delivery behavior in one immutable value.
     */
    fun getSettings(context: Context): PcgNotificationAlertSettings {
        return PcgNotificationAlertSettings(
            soundEnabled = isSoundEnabled(context),
            vibrationEnabled = isVibrationEnabled(context),
            reminderEnabled = isReminderEnabled(context)
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/**
 * Snapshot of the user's PCG notification delivery preferences.
 */
data class PcgNotificationAlertSettings(
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val reminderEnabled: Boolean
)
