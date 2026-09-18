package com.fs.twitchminichat

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.fs.twitchminichat.diagnostics.AudioPlaybackObservation
import com.fs.twitchminichat.diagnostics.AudioPlayerSample
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
             * How old the process was when this push reached it. A push that
             * arrives after the process was killed is handled by one only
             * milliseconds old, and early observations tied such alerts to
             * silence - but every one of them fell in the period when the
             * device's default notification sound was broken, which was the
             * dominant cause of silence then. Whether a young process posts silent
             * alerts on its own has never been measured. This value travels to
             * the alert_audio line, next to the outcome, so each cold alert now
             * answers that question as it happens.
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

            val ingestion = SmartCatchSpawnIngestion.ingestFcmPayload(
                context = applicationContext,
                data = data,
                messageSentAtMs = remoteMessage.sentTime
            )

            /*
             * Firebase holds undelivered pushes while a device is offline and
             * releases the whole backlog at once when it returns. Every one of
             * them used to become its own alert, so a phone coming back after a
             * night produced around seventy notifications a couple of seconds
             * apart and vibrated for the better part of a minute.
             *
             * None of them could be acted on: a spawn lasts ninety seconds, and
             * the coordinator already decides that question against the spawn's
             * own start time, discounting the reminder delay. Reusing its verdict
             * keeps one definition of "over" in the app instead of adding a second
             * threshold here.
             *
             * This suppresses alerts that arrive after the catch window has
             * closed, not alerts that are merely late. An alert delayed by
             * seconds still reaches the user, which is what the product rule
             * about lateness protects.
             */
            if (
                ingestion.outcome ==
                SmartCatchSpawnIngestionOutcome.IGNORED_EXPIRED
            ) {
                Log.d(TAG, "Spawn alert suppressed: catch window already closed")
                HistoryDiagnosticsLog.record(
                    applicationContext,
                    "fcm.suppressed",
                    "reason" to "spawn_expired",
                    "latencySec" to latencySec,
                    "reminder" to data[PcgNotificationPayloadPolicy.REMINDER_KEY]
                )
                return
            }

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
                targetProfileId = targetProfileId.orEmpty(),
                processUptimeMs = processUptimeMs
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
        targetProfileId: String,
        processUptimeMs: Long
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

        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        val channelSound = channel?.sound

        /*
         * Read before posting, both of them. The four settings describe what the
         * user asked for at the moment the alert was raised, and the player
         * count is the baseline that tells a player this alert started from
         * audio that was already running.
         */
        val suppressionReason = currentSuppressionReason(
            notificationManager = notificationManager,
            audioManager = audioManager,
            channelHasSound = channelSound != null
        )

        /*
         * Counted for every alert, not only the ones the fallback will watch: the
         * audio observation below needs the same baseline on suppressed alerts
         * too. One count, from the product's own watcher, serves both.
         */
        val playersBefore = NotificationAlertFallback.activePlayerCount(audioManager)

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

        /*
         * Only the scheme and the final segment of the channel sound are taken.
         * A user-chosen ringtone sits at a path that names their storage layout,
         * which the journal has no reason to carry.
         */
        HistoryDiagnosticsLog.record(
            applicationContext,
            "fcm.notification.posted",
            "channelId" to channelId,
            "channelImportance" to channel?.importance,
            "channelHasSound" to (channelSound != null),
            "channelSoundScheme" to channelSound?.scheme,
            "channelSoundName" to channelSound?.lastPathSegment,
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
            "btOutputConnected" to hasConnectedBluetoothOutput(audioManager),
            "btOutputRouted" to isBluetoothRoutingActive(audioManager),
            "activeNotifications" to runCatching {
                notificationManager.activeNotifications.size
            }.getOrNull(),
            "msSinceLastPost" to previousPostedAtMs
                .takeIf { it > 0L }
                ?.let { postedAtMs - it }
        )

        /*
         * A suppressed alert is watched too, with the fallback disarmed: the
         * observation row is written for every alert, and its alert_audio line
         * is written here, now, since its outcome needs no sample.
         */
        if (suppressionReason != null) {
            recordAlertAudioOutcome(
                outcome = NotificationAlertFallbackPolicy.OUTCOME_SUPPRESSED,
                processUptimeMs = processUptimeMs,
                reason = suppressionReason
            )
        }

        watchAlertAudioOffTheDispatchThread(
            audioManager = audioManager,
            playersBefore = playersBefore,
            channelSound = channelSound,
            processUptimeMs = processUptimeMs,
            fallbackArmed = suppressionReason == null,
            suppressionNow = {
                currentSuppressionReason(
                    notificationManager = notificationManager,
                    audioManager = audioManager,
                    channelHasSound = notificationManager
                        .getNotificationChannel(channelId)
                        ?.sound != null
                )
            }
        )
    }

    /**
     * Reads the four settings that can ask for silence, and returns the one
     * that does, or null.
     *
     * One reading, used twice: before the alert is posted, and again just
     * before the fallback would play, since any of them can change during the
     * grace. A state the framework refuses to report reads as a sentinel that
     * matches no real constant, so an unreadable device is never permission to
     * make a sound.
     */
    private fun currentSuppressionReason(
        notificationManager: NotificationManager,
        audioManager: AudioManager?,
        channelHasSound: Boolean
    ): String? {
        return NotificationAlertFallbackPolicy.suppressionReason(
            interruptionFilter = runCatching {
                notificationManager.currentInterruptionFilter
            }.getOrDefault(NOT_A_FILTER),
            ringerMode = audioManager?.ringerMode ?: NOT_A_RINGER_MODE,
            notificationVolume = audioManager?.getStreamVolume(
                AudioManager.STREAM_NOTIFICATION
            ) ?: 0,
            channelHasSound = channelHasSound
        )
    }

    /**
     * Watches for the alert sound the system either plays or does not.
     *
     * Every field recorded so far describes the request handed to Android, and
     * all of them have been permissive on alerts that were reported as silent.
     * Whether a sound actually came out has been established only by ear, at a
     * notification volume low enough to make that judgement unreliable. Polling
     * the active players for a moment after posting answers it directly.
     *
     * playersDelta above zero means a player started while the alert was being
     * raised. Zero throughout means the system posted the notification without
     * playing anything, which is a different defect entirely from one that plays
     * a sound too quiet to notice.
     *
     * It takes no readings of its own. The samples are the ones the fallback's
     * watch took for the same alert, so this row and the alert_audio line are
     * two descriptions of one set of readings: firstRiseMs here equals
     * systemStartMs there whenever the system played within the grace, and they
     * cannot contradict each other on the alerts that fail, which is where both
     * are read. The counter is device-wide, so when the fallback plays at the
     * end of its grace, that player appears in the later samples too - a late
     * rise is read together with the alert_audio outcome, which says whose sound
     * it was.
     */
    private fun recordAudioPlaybackObservation(
        playersBefore: Int,
        samples: List<AudioPlayerSample>
    ) {
        if (samples.isEmpty()) return

        val summary = AudioPlaybackObservation.summarize(playersBefore, samples)

        HistoryDiagnosticsLog.record(
            applicationContext,
            "fcm.notification.audio",
            "playersBefore" to playersBefore,
            "playersPeak" to summary.playersPeak,
            "playersDelta" to (summary.playersPeak - playersBefore),
            "firstRiseMs" to summary.firstRiseMs,
            "lastRiseMs" to summary.lastRiseMs,
            "elevatedSamples" to summary.elevatedSamples,
            "elevatedSpanMs" to summary.elevatedSpanMs,
            "sampleCount" to samples.size,
            "observedMs" to AUDIO_OBSERVATION_MS
        )
    }

    /**
     * Returns whether any connected audio output is a Bluetooth device.
     *
     * Only the device type is read. Product names and addresses identify the
     * user's own hardware, so they are deliberately never touched.
     */
    private fun hasConnectedBluetoothOutput(audioManager: AudioManager?): Boolean? {
        audioManager ?: return null

        return runCatching {
            audioManager
                .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .any { device -> isBluetoothOutputType(device.type) }
        }.getOrNull()
    }

    /**
     * Returns whether a notification alert would be routed to Bluetooth.
     *
     * getAudioDevicesForAttributes is the only public query that answers where
     * audio would actually go, and it was introduced in API 33. Below that this
     * stays null rather than inferring routing from what merely happens to be
     * connected, which would read as a measurement without being one.
     */
    private fun isBluetoothRoutingActive(audioManager: AudioManager?): Boolean? {
        audioManager ?: return null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        return runCatching {
            audioManager
                .getAudioDevicesForAttributes(attributes)
                .any { device -> isBluetoothOutputType(device.type) }
        }.getOrNull()
    }

    /** Returns whether [type] is one of the Bluetooth output kinds. */
    private fun isBluetoothOutputType(type: Int): Boolean {
        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        ) {
            return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            type == AudioDeviceInfo.TYPE_HEARING_AID
        ) {
            return true
        }

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (
                type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                )
    }

    /**
     * Watches the alert once, plays the sound if the system never does, and
     * writes both rows the alert produces from that single watch.
     *
     * One sampler per alert. The fallback's verdict and the observation row
     * come from the same readings, so the alert_audio line and the
     * fcm.notification.audio line cannot tell two different stories about the
     * same alert.
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
        channelSound: Uri?,
        processUptimeMs: Long,
        fallbackArmed: Boolean,
        suppressionNow: () -> String?
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
            val samples = mutableListOf<AudioPlayerSample>()

            val watch = NotificationAlertFallback.watch(
                context = this,
                audioManager = audioManager,
                playersBefore = playersBefore,
                channelSound = channelSound,
                fallbackArmed = fallbackArmed,
                windowMs = AUDIO_OBSERVATION_MS,
                suppressionNow = suppressionNow,
                onSample = { offsetMs, playerCount ->
                    samples += AudioPlayerSample(offsetMs = offsetMs, playerCount = playerCount)
                }
            )

            recordAudioPlaybackObservation(playersBefore, samples)

            /* A suppressed alert already has its alert_audio line. */
            if (!fallbackArmed) return@Thread

            if (watch.systemStartMs != null) {
                recordAlertAudioOutcome(
                    outcome = NotificationAlertFallbackPolicy.OUTCOME_PLAYED,
                    processUptimeMs = processUptimeMs,
                    playersBefore = playersBefore,
                    systemStartMs = watch.systemStartMs,
                    sampleCount = watch.sampleCount
                )
                return@Thread
            }

            /*
             * Too few readings to tell silence from a sound the watch missed.
             * Nothing was played, and the line says so rather than calling it a
             * fallback that did not start.
             */
            if (watch.unobserved) {
                Log.w(TAG, "Alert audio unobserved, sampleCount=${watch.sampleCount}")
                recordAlertAudioOutcome(
                    outcome = NotificationAlertFallbackPolicy.OUTCOME_UNOBSERVED,
                    processUptimeMs = processUptimeMs,
                    playersBefore = playersBefore,
                    sampleCount = watch.sampleCount
                )
                return@Thread
            }

            watch.lateSuppressionReason?.let { reason ->
                recordAlertAudioOutcome(
                    outcome = NotificationAlertFallbackPolicy.OUTCOME_SUPPRESSED,
                    processUptimeMs = processUptimeMs,
                    reason = reason,
                    playersBefore = playersBefore,
                    sampleCount = watch.sampleCount,
                    checkedAt = CHECKED_BEFORE_PLAYBACK
                )
                return@Thread
            }

            Log.w(
                TAG,
                "System never started an alert player, fallback played=${watch.fallbackPlayed}"
            )

            recordAlertAudioOutcome(
                outcome = NotificationAlertFallbackPolicy.OUTCOME_FALLBACK,
                processUptimeMs = processUptimeMs,
                playersBefore = playersBefore,
                fallbackPlayed = watch.fallbackPlayed,
                sampleCount = watch.sampleCount
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
        processUptimeMs: Long,
        reason: String? = null,
        playersBefore: Int? = null,
        systemStartMs: Long? = null,
        fallbackPlayed: Boolean? = null,
        sampleCount: Int? = null,
        checkedAt: String? = null
    ) {
        /*
         * processUptimeMs rides on the same line as the outcome so a cold alert
         * reads as a complete trial on its own, without joining to the
         * fcm.received line by position in the journal.
         */
        HistoryDiagnosticsLog.record(
            applicationContext,
            "fcm.notification.alert_audio",
            "outcome" to outcome,
            "processUptimeMs" to processUptimeMs,
            "reason" to reason,
            "playersBefore" to playersBefore,
            "systemStartMs" to systemStartMs,
            "fallbackPlayed" to fallbackPlayed,
            "sampleCount" to sampleCount,
            "checkedAt" to checkedAt,
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

        /** Marks a suppression found when the settings were read again before playback. */
        private const val CHECKED_BEFORE_PLAYBACK = "before_playback"

        /** One push arrived and was dropped before it could become a notification. */
        private const val MARKER_MESSAGE_HANDLING_FAILED = "fcm_message_handling_failed"

        /**
         * How long the active players are watched after an alert is posted.
         *
         * Sized from the sound itself rather than from a round number.
         * res/raw/tmc_spawn_alert_chime.wav is mono 44.1 kHz 16 bit with a data
         * chunk of 127890 bytes, which its own header makes 1450 ms. A player
         * that starts around 860 ms therefore stops around 2310 ms, past the
         * 1800 ms this used to watch for: every audible alert had its span cut
         * off at the edge of the window, so elevatedSpanMs reported roughly 940
         * ms whatever the sound actually did. Watching to 2600 ms sees the sound
         * end on its own, which turns that field from a lower bound into a
         * duration.
         */
        private const val AUDIO_OBSERVATION_MS = 2_600L

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
