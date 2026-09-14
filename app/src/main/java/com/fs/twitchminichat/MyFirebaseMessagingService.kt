package com.fs.twitchminichat

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
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
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            Log.d(
                TAG,
                "Firebase message received dataFieldCount=${remoteMessage.data.size} " +
                    "hasNotificationPayload=${remoteMessage.notification != null}"
            )

            val data = remoteMessage.data

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

        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        val channelSound = channel?.sound

        /*
         * Read before posting, both of them. The four settings describe what the
         * user asked for at the moment the alert was raised, and the player
         * count is the baseline that tells a player this alert started from
         * audio that was already running.
         */
        val suppressionReason = NotificationAlertFallbackPolicy.suppressionReason(
            interruptionFilter = runCatching {
                notificationManager.currentInterruptionFilter
            }.getOrDefault(NOT_A_FILTER),
            ringerMode = audioManager?.ringerMode ?: NOT_A_RINGER_MODE,
            notificationVolume = audioManager?.getStreamVolume(
                AudioManager.STREAM_NOTIFICATION
            ) ?: 0,
            channelHasSound = channelSound != null
        )

        val playersBefore = if (suppressionReason == null) {
            NotificationAlertFallback.activePlayerCount(audioManager)
        } else {
            0
        }

        NotificationManagerCompat.from(this).notify(
            notificationId,
            notification
        )

        Log.d(TAG, "Notification posted")

        if (suppressionReason != null) {
            recordAlertAudioOutcome(
                outcome = NotificationAlertFallbackPolicy.OUTCOME_SUPPRESSED,
                reason = suppressionReason
            )
            return
        }

        watchAlertAudioOffTheDispatchThread(audioManager, playersBefore, channelSound)
    }

    /**
     * Waits for the system player, and plays the sound if it never arrives.
     *
     * Runs on a thread of its own, and the reason is measured rather than
     * assumed. In firebase-messaging 25.0.1, EnhancedIntentService dispatches
     * every message through `FcmExecutors.newIntentHandleExecutor()`, which is
     * `PoolableExecutors.factory().newSingleThreadExecutor(...)`, and
     * FirebaseMessagingService does not override handleIntentOnMainThread, whose
     * base implementation returns false. Delivery is therefore serial: waiting
     * here would hold the next message behind this one for the whole grace
     * period. That is exactly the case this feature exists for - two accounts
     * matching one spawn produce two pushes moments apart - so blocking would
     * delay the second alert by two and a half seconds to repair the first.
     *
     * What is given up by detaching is tenure. Once the service finishes its
     * last message the process may be reclaimed, and this thread can be killed
     * mid-wait, so the fallback is best effort and will occasionally not play.
     * That is the right side to lose on: a repair for an alert that has already
     * failed is worth less than the timely delivery of the next one.
     */
    private fun watchAlertAudioOffTheDispatchThread(
        audioManager: AudioManager?,
        playersBefore: Int,
        channelSound: Uri?
    ) {
        /*
         * One thread per alert, and deliberately not an executor. Pooling this
         * looks like tidying and is the bug: a single-threaded executor would
         * re-serialise exactly what detaching from the dispatch thread was for,
         * and a second alert would start reading the counter two and a half
         * seconds after it was posted - by which time its own system player has
         * been born and has died - so it would conclude the system stayed
         * silent and play on top of a sound the user already heard. The
         * generous grace exists to avoid that doubled alert; a pool would
         * reintroduce it from the other side. Alerts are rare and the thread
         * lives for at most the grace, so one each is the cheap option as well
         * as the correct one.
         *
         * The player count these threads read is global to the device, not
         * scoped to this alert. Two accounts matching one spawn produce two
         * pushes moments apart, so the second thread can see the first alert's
         * player - the system's or ours - and record that the system played for
         * it. The error only ever runs towards fewer sounds, never towards two,
         * and it matches what Android does by collapsing alerts that arrive
         * together, so it is accepted rather than corrected. It is accepted
         * knowingly: no test covers it, because it lives here in the service
         * and not in the policy the tests can reach.
         */
        Thread({
            val systemStartMs = NotificationAlertFallback.awaitSystemPlayer(
                audioManager = audioManager,
                playersBefore = playersBefore
            )

            if (systemStartMs != null) {
                recordAlertAudioOutcome(
                    outcome = NotificationAlertFallbackPolicy.OUTCOME_PLAYED,
                    playersBefore = playersBefore,
                    systemStartMs = systemStartMs
                )
                return@Thread
            }

            val played = NotificationAlertFallback.playOnce(
                context = this,
                audioManager = audioManager,
                channelSound = channelSound
            )

            Log.w(TAG, "System never started an alert player, fallback played=$played")

            recordAlertAudioOutcome(
                outcome = NotificationAlertFallbackPolicy.OUTCOME_FALLBACK,
                playersBefore = playersBefore,
                fallbackPlayed = played
            )
        }, "tmc-alert-audio").start()
    }

    /**
     * Writes the one journal line every posted alert produces.
     *
     * Exactly one per alert, whatever happened, so a journal with no line for
     * an alert means the alert was never posted rather than that it was posted
     * and went unrecorded. Null fields are dropped by the journal, so a
     * suppressed row carries its reason and nothing meaningless beside it.
     */
    private fun recordAlertAudioOutcome(
        outcome: String,
        reason: String? = null,
        playersBefore: Int? = null,
        systemStartMs: Long? = null,
        fallbackPlayed: Boolean? = null
    ) {
        HistoryDiagnosticsLog.record(
            applicationContext,
            "fcm.notification.alert_audio",
            "outcome" to outcome,
            "reason" to reason,
            "playersBefore" to playersBefore,
            "systemStartMs" to systemStartMs,
            "fallbackPlayed" to fallbackPlayed,
            "graceMs" to NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS
        )
    }

    companion object {
        private const val TAG = "FCM"

        /**
         * Stand-ins for a state the framework refused to report.
         *
         * Neither matches any real constant, so a failed read can never be
         * mistaken for permission to make a sound.
         */
        private const val NOT_A_FILTER = -1
        private const val NOT_A_RINGER_MODE = -1

        /** One push arrived and was dropped before it could become a notification. */
        private const val MARKER_MESSAGE_HANDLING_FAILED = "fcm_message_handling_failed"

        private const val PREFS_FCM_REGISTRATION = "fcm_registration"
        private const val KEY_LATEST_FCM_TOKEN = "latest_fcm_token"
    }
}
