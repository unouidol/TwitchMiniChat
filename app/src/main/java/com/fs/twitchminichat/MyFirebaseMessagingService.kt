package com.fs.twitchminichat

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.fs.twitchminichat.pcg.PcgNotificationChannelManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random
import android.app.Notification
import android.provider.Settings
import com.fs.twitchminichat.pcg.PcgNotificationAlertPrefsStore

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed")
        Log.d(TAG, "FCM token saved locally before profile registration")

        val prefs = applicationContext.getSharedPreferences(PREFS_FCM_REGISTRATION, MODE_PRIVATE)
        prefs.edit {
            putString(KEY_LATEST_FCM_TOKEN, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            val data = remoteMessage.data

            val title = data["title"]
                ?: remoteMessage.notification?.title
                ?: "Pokémon spawn"

            val body = data["body"]
                ?: remoteMessage.notification?.body
                ?: "A missing Pokémon spawned"

            val pokemon = data["pokemon"].orEmpty()

            val profiles = data["profiles"]
                ?: data["matched_profiles"]
                ?: ""

            val targetProfileId = data["target_profile_id"]
                ?: data["profile_id"]
                ?: data["profileId"]
                ?: data["matched_profile_id"]
                ?: inferSingleProfileIdFromProfiles(profiles)

            /*
             * Production-safe diagnostic log.
             *
             * Do not log the raw FCM payload, profile ids, Pokémon name, notification
             * title/body, or matched profile list. Those values can identify user activity.
             */
            Log.d(
                TAG,
                "spawn notification received " +
                        "hasPokemon=${pokemon.isNotBlank()} " +
                        "hasProfiles=${profiles.isNotBlank()} " +
                        "hasTargetProfile=${!targetProfileId.isNullOrBlank()}"
            )

            showSpawnNotification(
                title = title,
                body = body,
                pokemon = pokemon,
                profiles = profiles,
                targetProfileId = targetProfileId.orEmpty()
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Crash inside onMessageReceived", t)
        }
    }

    private fun inferSingleProfileIdFromProfiles(profiles: String): String? {
        val cleaned = profiles.trim()
        if (cleaned.isBlank()) return null

        val parts = cleaned
            .split(",", ";", "|")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

        return if (parts.size == 1) {
            parts.first()
        } else {
            null
        }
    }

    /**
     * Shows a local PCG spawn notification.
     *
     * The notification channel is selected at send time from the user's current
     * sound/vibration preferences. This lets TMC support:
     *
     * - silent alerts
     * - sound-only alerts
     * - vibration-only alerts
     * - sound + vibration alerts
     *
     * Android 8+ uses notification channels for sound/vibration behavior.
     * Android 7.1 and lower use NotificationCompat legacy defaults.
     */
    @SuppressLint("MissingPermission")
    private fun showSpawnNotification(
        title: String,
        body: String,
        pokemon: String,
        profiles: String,
        targetProfileId: String
    ) {
        /*
         * Create all PCG alert channels before choosing one.
         *
         * Android keeps channel behavior stable once a channel has been created,
         * so PcgNotificationChannelManager owns multiple channel IDs instead of
         * trying to mutate one existing channel.
         */
        PcgNotificationChannelManager.ensureChannels(this)

        val channelId = PcgNotificationChannelManager.resolveChannelId(this)
        val deliverySettings = PcgNotificationAlertPrefsStore.getSettings(this)

        Log.d(
            TAG,
            "resolved PCG notification channelId=$channelId " +
                    "soundEnabled=${deliverySettings.soundEnabled} " +
                    "vibrationEnabled=${deliverySettings.vibrationEnabled}"
        )

        val notificationId = Random.nextInt(1, Int.MAX_VALUE)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("open_from_push", true)

            if (pokemon.isNotBlank()) {
                putExtra("spawn_pokemon", pokemon)
            }

            if (profiles.isNotBlank()) {
                putExtra("spawn_profiles", profiles)
            }

            if (targetProfileId.isNotBlank()) {
                putExtra(MainActivity.EXTRA_TARGET_PROFILE_ID, targetProfileId)
                putExtra(MainActivity.EXTRA_PROFILE_ID, targetProfileId)

                /*
                 * Duplicate extras are not strictly necessary, but they make
                 * debugging and future compatibility easier if another entry
                 * point expects generic profile keys.
                 */
                putExtra("target_profile_id", targetProfileId)
                putExtra("profile_id", targetProfileId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification_bell)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setSilent(false)
            .setContentIntent(pendingIntent)

        /*
         * Android 8+ ignores most per-notification sound/vibration settings and
         * uses the channel instead. Older Android versions still need these
         * NotificationCompat settings.
         */
        PcgNotificationChannelManager.applyLegacyAlertBehavior(
            context = this,
            builder = builder
        )

        /*
         * Extra explicit alert behavior.
         *
         * Android 8+ should use the notification channel, but some ROMs are more
         * reliable when the builder also carries the requested sound/vibration intent.
         */
        if (deliverySettings.soundEnabled) {
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
        }

        if (deliverySettings.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0L, 180L, 90L, 180L))
        }

        var defaults = 0

        if (deliverySettings.soundEnabled) {
            defaults = defaults or Notification.DEFAULT_SOUND
        }

        if (deliverySettings.vibrationEnabled) {
            defaults = defaults or Notification.DEFAULT_VIBRATE
        }

        builder.setDefaults(defaults)

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS not allowed: alert not shown")
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(channelId)

            Log.d(
                TAG,
                "channelId=$channelId exists=${channel != null} " +
                        "importance=${channel?.importance} " +
                        "canBypassDnd=${channel?.canBypassDnd()} " +
                        "sound=${channel?.sound} " +
                        "shouldVibrate=${channel?.shouldVibrate()} " +
                        "vibrationPattern=${channel?.vibrationPattern?.joinToString()}"
            )
        } else {
            Log.d(TAG, "legacy notification behavior channelId=$channelId")
        }

        val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        Log.d(TAG, "notificationsEnabled=$notificationsEnabled")

        NotificationManagerCompat.from(this).notify(
            notificationId,
            notification
        )

        Log.d(
            TAG,
            "spawn notification posted " +
                    "notificationId=$notificationId " +
                    "channelId=$channelId " +
                    "hasTargetProfile=${targetProfileId.isNotBlank()}"
        )
    }

    companion object {
        private const val TAG = "FCM"

        private const val PREFS_FCM_REGISTRATION = "fcm_registration"
        private const val KEY_LATEST_FCM_TOKEN = "latest_fcm_token"
    }
}