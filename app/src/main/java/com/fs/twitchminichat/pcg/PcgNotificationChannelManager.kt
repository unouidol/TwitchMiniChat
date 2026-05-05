package com.fs.twitchminichat.pcg

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.fs.twitchminichat.R
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Owns the Android notification channels used by PCG spawn alerts.
 *
 * Android 8+ stores sound/vibration behaviour on channels. Since channel behaviour
 * cannot be freely changed after creation, TMC uses separate channel IDs for the
 * four sound/vibration combinations and chooses the right one per notification.
 */
object PcgNotificationChannelManager {

    const val CHANNEL_PCG_ALERTS_SILENT = "pcg_alerts_silent_v4"
    const val CHANNEL_PCG_ALERTS_SOUND = "pcg_alerts_sound_v4"
    const val CHANNEL_PCG_ALERTS_VIBRATE = "pcg_alerts_vibrate_v4"
    const val CHANNEL_PCG_ALERTS_SOUND_VIBRATE = "pcg_alerts_sound_vibrate_v4"

    private val vibrationPattern = longArrayOf(0L, 180L, 90L, 180L)

    /**
     * Creates all PCG alert channels.
     *
     * Safe to call repeatedly. Android treats recreating an existing channel with
     * the same configuration as a no-op.
     */
    fun ensureChannels(context: Context) {


        val appContext = context.applicationContext
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            buildChannel(
                context = appContext,
                id = CHANNEL_PCG_ALERTS_SILENT,
                nameRes = R.string.pcg_notification_channel_silent_name,
                descriptionRes = R.string.pcg_notification_channel_silent_description,
                soundEnabled = false,
                vibrationEnabled = false
            ),
            buildChannel(
                context = appContext,
                id = CHANNEL_PCG_ALERTS_SOUND,
                nameRes = R.string.pcg_notification_channel_sound_name,
                descriptionRes = R.string.pcg_notification_channel_sound_description,
                soundEnabled = true,
                vibrationEnabled = false
            ),
            buildChannel(
                context = appContext,
                id = CHANNEL_PCG_ALERTS_VIBRATE,
                nameRes = R.string.pcg_notification_channel_vibrate_name,
                descriptionRes = R.string.pcg_notification_channel_vibrate_description,
                soundEnabled = false,
                vibrationEnabled = true
            ),
            buildChannel(
                context = appContext,
                id = CHANNEL_PCG_ALERTS_SOUND_VIBRATE,
                nameRes = R.string.pcg_notification_channel_sound_vibrate_name,
                descriptionRes = R.string.pcg_notification_channel_sound_vibrate_description,
                soundEnabled = true,
                vibrationEnabled = true
            )
        )

        notificationManager.createNotificationChannels(channels)
    }

    /**
     * Returns the channel ID matching the user's current sound/vibration choices.
     */
    fun resolveChannelId(context: Context): String {
        val settings = PcgNotificationAlertPrefsStore.getSettings(context)

        return when {
            settings.soundEnabled && settings.vibrationEnabled -> CHANNEL_PCG_ALERTS_SOUND_VIBRATE
            settings.soundEnabled -> CHANNEL_PCG_ALERTS_SOUND
            settings.vibrationEnabled -> CHANNEL_PCG_ALERTS_VIBRATE
            else -> CHANNEL_PCG_ALERTS_SILENT
        }
    }

    /**
     * Applies pre-Android-8 sound/vibration behaviour to a NotificationCompat builder.
     *
     * On Android 8+ this is controlled by channels. On Android 7.1 and lower,
     * NotificationCompat defaults/vibration still matter.
     */
    fun applyLegacyAlertBehavior(
        context: Context,
        builder: NotificationCompat.Builder
    ) {


        val settings = PcgNotificationAlertPrefsStore.getSettings(context)
        var defaults = 0

        if (settings.soundEnabled) {
            defaults = defaults or NotificationCompat.DEFAULT_SOUND
        }

        if (settings.vibrationEnabled) {
            defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
            builder.setVibrate(vibrationPattern)
        } else {
            builder.setVibrate(null)
        }

        builder.setDefaults(defaults)
    }

    private fun buildChannel(
        context: Context,
        id: String,
        nameRes: Int,
        descriptionRes: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ): NotificationChannel {
        /*
         * Alerting channels should use HIGH importance so Android treats them as
         * real user-visible alerts. The silent channel can stay DEFAULT because it
         * intentionally should not interrupt with sound or vibration.
         */
        val importance = if (soundEnabled || vibrationEnabled) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }

        val channel = NotificationChannel(
            id,
            context.getString(nameRes),
            importance
        )

        channel.description = context.getString(descriptionRes)

        if (soundEnabled) {
            val soundUri = "android.resource://${context.packageName}/${R.raw.tmc_spawn_alert_chime}".toUri()
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            channel.setSound(soundUri, audioAttributes)
        } else {
            channel.setSound(null, null)
        }

        channel.enableVibration(vibrationEnabled)

        if (vibrationEnabled) {
            channel.vibrationPattern = vibrationPattern
        }

        return channel
    }
}