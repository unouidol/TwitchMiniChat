package com.fs.twitchminichat

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.fs.twitchminichat.diagnostics.HistoryDiagnosticsLog
import com.fs.twitchminichat.pcg.PcgNotificationChannelManager
import com.fs.twitchminichat.pcg.PcgNotificationPayloadPolicy
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random
import android.app.Notification
import android.provider.Settings
import com.fs.twitchminichat.pcg.PcgNotificationAlertPrefsStore

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed registration token received")
        Log.d(TAG, "Token received, local save before profiles registration")

        val prefs = applicationContext.getSharedPreferences(PREFS_FCM_REGISTRATION, MODE_PRIVATE)
        prefs.edit {
            putString(KEY_LATEST_FCM_TOKEN, token)
        }

        /*
         * A token rotation that never reaches the backend silences every push, and
         * from the device the result looks identical to a spawn that was never sent.
         */
        HistoryDiagnosticsLog.record(
            applicationContext,
            "fcm.token_refreshed"
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            Log.d(
                TAG,
                "Firebase message received dataFieldCount=${remoteMessage.data.size} " +
                    "hasNotificationPayload=${remoteMessage.notification != null}"
            )

            val data = remoteMessage.data

            /*
             * sentTime comes from the Firebase servers, so the difference against the
             * local clock measures the end-to-end delay. priority against
             * originalPriority shows whether Firebase demoted the message, which is
             * what standby buckets and battery restrictions do to a background app.
             */
            val sentAtMs = remoteMessage.sentTime
            val latencySec = if (sentAtMs > 0L) {
                (System.currentTimeMillis() - sentAtMs) / 1000
            } else {
                null
            }

            /*
             * A push that arrives after the process was killed is handled by a
             * process only milliseconds old, and the alert it posts is the one
             * reported as silent. The reminder that follows 45 seconds later can
             * never be in that state, because the first push has just started the
             * process it runs in — so "first silent, second heard" and "cold
             * process silent" describe the same events from two sides.
             *
             * Measured here rather than inferred from a missing msSinceLastPost,
             * which is also absent for the genuinely first alert of a session.
             */
            val processUptimeMs = SystemClock.elapsedRealtime() -
                android.os.Process.getStartElapsedRealtime()

            HistoryDiagnosticsLog.record(
                applicationContext,
                "fcm.received",
                "processUptimeMs" to processUptimeMs,
                "latencySec" to latencySec,
                "reminder" to data[PcgNotificationPayloadPolicy.REMINDER_KEY],
                "priority" to remoteMessage.priority,
                "originalPriority" to remoteMessage.originalPriority,
                "dataFieldCount" to data.size
            )

            SmartCatchSpawnIngestion.ingestFcmPayload(
                context = applicationContext,
                data = data,
                messageSentAtMs = remoteMessage.sentTime
            )

            val reminderEnabled =
                PcgNotificationAlertPrefsStore.isReminderEnabled(this)
            if (
                !PcgNotificationPayloadPolicy.shouldDisplay(
                    data = data,
                    reminderEnabled = reminderEnabled
                )
            ) {
                Log.d(TAG, "Delayed spawn reminder suppressed by local preference")
                HistoryDiagnosticsLog.record(
                    applicationContext,
                    "fcm.suppressed",
                    "reason" to "reminder_disabled"
                )
                return
            }

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
                "Notification routing resolved " +
                    "hasTargetProfile=${!targetProfileId.isNullOrBlank()} " +
                    "hasPokemon=${pokemon.isNotBlank()} " +
                    "hasProfileSummary=${profiles.isNotBlank()}"
            )

            showSpawnNotification(
                title = title,
                body = body,
                pokemon = pokemon,
                profiles = profiles,
                targetProfileId = targetProfileId.orEmpty()
            )
        } catch (t: Throwable) {
            Log.e(
                TAG,
                "Firebase message handling failed errorType=${DiagnosticError.typeOf(t)}"
            )

            HistoryDiagnosticsLog.record(
                applicationContext,
                "fcm.failed",
                "errorType" to DiagnosticError.typeOf(t)
            )

            /*
             * The push is lost here and the user simply never sees the alert, which
             * looks identical to a spawn that was never sent.
             */
            CrashReporting.recordFailure(MARKER_MESSAGE_HANDLING_FAILED, t)
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
            "Resolved Pokémon Community Game notification delivery " +
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
                HistoryDiagnosticsLog.record(
                    applicationContext,
                    "fcm.notification.blocked",
                    "reason" to "post_notifications_denied"
                )
                return
            }
        }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(channelId)

        Log.d(
            TAG,
            "Notification channel state exists=${channel != null} " +
                "importance=${channel?.importance} " +
                "canBypassDnd=${channel?.canBypassDnd()} " +
                "hasSound=${channel?.sound != null} " +
                "shouldVibrate=${channel?.shouldVibrate()} " +
                "vibrationPatternSize=${channel?.vibrationPattern?.size ?: 0}"
        )

        val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        Log.d(TAG, "notificationsEnabled=$notificationsEnabled")

        NotificationManagerCompat.from(this).notify(
            notificationId,
            notification
        )

        Log.d(TAG, "Notification posted")

        /*
         * Everything above proves the alert was handed to Android correctly, which
         * the journal already showed for alerts that never made a sound. What was
         * missing is the state Android itself was in when it decided whether to
         * play one, so these fields describe that decision rather than the request.
         *
         * msSinceLastPost measures how close together two alerts from this app
         * landed: the backend sends one push per matching profile and the profiles
         * share one device, so two notifications can arrive milliseconds apart and
         * only one of them is heard.
         */
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager

        val notificationsPaused =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { notificationManager.areNotificationsPaused() }.getOrNull()
            } else {
                null
            }

        val groupBlocked =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                channel?.group?.let { groupId ->
                    runCatching {
                        notificationManager
                            .getNotificationChannelGroup(groupId)
                            ?.isBlocked
                    }.getOrNull()
                }
            } else {
                null
            }

        val postedAtMs = System.currentTimeMillis()
        val previousPostedAtMs = lastNotificationPostedAtMs
        lastNotificationPostedAtMs = postedAtMs

        HistoryDiagnosticsLog.record(
            applicationContext,
            "fcm.notification.posted",
            "channelId" to channelId,
            "channelImportance" to channel?.importance,
            "channelHasSound" to (channel?.sound != null),
            "channelVibrates" to channel?.shouldVibrate(),
            "notificationsEnabled" to notificationsEnabled,
            "channelBypassesDnd" to channel?.canBypassDnd(),
            "groupBlocked" to groupBlocked,
            "interruptionFilter" to runCatching {
                notificationManager.currentInterruptionFilter
            }.getOrNull(),
            "notificationsPaused" to notificationsPaused,
            "ringerMode" to audioManager?.ringerMode,
            "notificationVolume" to audioManager?.getStreamVolume(
                AudioManager.STREAM_NOTIFICATION
            ),
            "notificationVolumeMax" to audioManager?.getStreamMaxVolume(
                AudioManager.STREAM_NOTIFICATION
            ),
            "activeNotifications" to runCatching {
                notificationManager.activeNotifications.size
            }.getOrNull(),
            "msSinceLastPost" to previousPostedAtMs
                .takeIf { it > 0L }
                ?.let { postedAtMs - it }
        )
    }

    companion object {
        private const val TAG = "FCM"

        /** One push arrived and was dropped before it could become a notification. */
        private const val MARKER_MESSAGE_HANDLING_FAILED = "fcm_message_handling_failed"

        private const val PREFS_FCM_REGISTRATION = "fcm_registration"
        private const val KEY_LATEST_FCM_TOKEN = "latest_fcm_token"

        /**
         * Wall clock of the previous alert this process posted, 0 when none.
         *
         * Held in the companion because each push may be handled by a new service
         * instance while the process survives between them.
         */
        @Volatile
        private var lastNotificationPostedAtMs: Long = 0L
    }
}
