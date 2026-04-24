package com.fs.twitchminichat

import android.Manifest
import android.app.NotificationChannel
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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        Log.d(TAG, "Token received, local save before profiles registration")

        val prefs = applicationContext.getSharedPreferences(PREFS_FCM_REGISTRATION, MODE_PRIVATE)
        prefs.edit {
            putString(KEY_LATEST_FCM_TOKEN, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            Log.d(TAG, "From: ${remoteMessage.from}")
            Log.d(TAG, "Data: ${remoteMessage.data}")

            remoteMessage.notification?.let { notification ->
                Log.d(
                    TAG,
                    "Notification title=${notification.title}, body=${notification.body}"
                )
            }

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

            Log.d(
                TAG,
                "Resolved notification targetProfileId=$targetProfileId pokemon=$pokemon profiles=$profiles"
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

    private fun showSpawnNotification(
        title: String,
        body: String,
        pokemon: String,
        profiles: String,
        targetProfileId: String
    ) {
        ensureNotificationChannel()

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

                // Extra duplicato non necessario ma comodo per debug/log o compatibilità futura.
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

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_SPAWN_ALERTS)
            .setSmallIcon(R.drawable.ic_bell_on)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS non allowed: alert not shown")
                return
            }
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val ch = nm.getNotificationChannel(CHANNEL_ID_SPAWN_ALERTS)
        Log.d(
            TAG,
            "channel exists=${ch != null} importance=${ch?.importance} canBypassDnd=${ch?.canBypassDnd()}"
        )

        val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        Log.d(TAG, "notificationsEnabled=$notificationsEnabled")

        NotificationManagerCompat.from(this).notify(
            notificationId,
            notification
        )

        Log.d(
            TAG,
            "Notification posted: title=$title body=$body pokemon=$pokemon profiles=$profiles targetProfileId=$targetProfileId notificationId=$notificationId"
        )
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID_SPAWN_ALERTS)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID_SPAWN_ALERTS,
            "Spawn alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for missing Pokémon from PCG"
        }

        manager.createNotificationChannel(channel)
        Log.d(TAG, "Created notification channel: $CHANNEL_ID_SPAWN_ALERTS")
    }

    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID_SPAWN_ALERTS = "spawn_alerts"

        private const val PREFS_FCM_REGISTRATION = "fcm_registration"
        private const val KEY_LATEST_FCM_TOKEN = "latest_fcm_token"
    }
}