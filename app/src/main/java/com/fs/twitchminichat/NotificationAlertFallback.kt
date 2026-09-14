package com.fs.twitchminichat

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import android.util.Log

/**
 * Watches for the system's own alert player and, when it never arrives, plays
 * the chime once in its place.
 *
 * The watching lives here rather than in the diagnostics package on purpose.
 * A product behaviour cannot depend on an instrument that is meant to be
 * removed, so this owns the capability and any later observation reads through
 * it. What is shared with the diagnostics work is the platform call underneath,
 * [AudioManager.getActivePlaybackConfigurations], which is Android's and not
 * anybody's instrument.
 */
object NotificationAlertFallback {

    private const val TAG = "FCM_FALLBACK"

    /**
     * Returns how many audio players the framework reports as active.
     *
     * Players belonging to other applications are anonymised for an ordinary
     * caller, so the count is trustworthy while the details are not. Counting
     * is all this needs.
     */
    fun activePlayerCount(audioManager: AudioManager?): Int {
        return runCatching {
            audioManager?.activePlaybackConfigurations?.size ?: 0
        }.getOrDefault(0)
    }

    /**
     * Blocks until a player rises above [playersBefore] or the grace expires.
     *
     * Returns how many milliseconds after the call the player appeared, or null
     * if none did. The elapsed value is the same quantity the diagnostics work
     * called firstRiseMs, so rows from before and after this change describe
     * the same measurement and the grace can be re-derived from live data
     * rather than from the sample it was chosen on.
     *
     * Returns as soon as a player appears, so a healthy alert costs only the
     * time it actually took to start.
     */
    fun awaitSystemPlayer(
        audioManager: AudioManager?,
        playersBefore: Int
    ): Long? {
        audioManager ?: return null

        val startedAtMs = SystemClock.elapsedRealtime()

        while (
            SystemClock.elapsedRealtime() - startedAtMs <
            NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS
        ) {
            val playerCount = activePlayerCount(audioManager)
            val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs

            if (
                NotificationAlertFallbackPolicy.systemPlayerAppeared(
                    playersBefore = playersBefore,
                    playersPeak = playerCount
                )
            ) {
                return elapsedMs
            }

            runCatching {
                Thread.sleep(NotificationAlertFallbackPolicy.POLL_INTERVAL_MS)
            }.getOrElse { return null }
        }

        return null
    }

    /**
     * Plays the alert sound exactly once, and reports whether it started.
     *
     * Plays [channelSound], the sound the notification channel is actually
     * configured with, rather than the bundled chime. The activation test
     * already asks whether the channel has a sound; playing a different one
     * would make the application answer an alert with audio the user never
     * chose, and would diverge silently the day the channel's sound changes.
     * The bundled chime is the fallback for the fallback, used only when the
     * configured sound cannot be opened - a ringtone on removed storage, a
     * revoked content permission.
     *
     * Carries the same attributes the channel uses, so the sound follows the
     * notification volume rather than the media volume. It does not vibrate:
     * the system has already done that when it posted the notification, and a
     * second buzz would be worse than the silence being repaired.
     *
     * The player releases itself on completion. Nothing retries: if both
     * sources fail the alert stays silent, which is the state it was already
     * in.
     */
    fun playOnce(
        context: Context,
        audioManager: AudioManager?,
        channelSound: Uri?
    ): Boolean {
        val applicationContext = context.applicationContext

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val sessionId = runCatching {
            audioManager?.generateAudioSessionId()
        }.getOrNull() ?: AudioManager.AUDIO_SESSION_ID_GENERATE

        val fromChannel = channelSound?.let { uri ->
            runCatching {
                MediaPlayer.create(applicationContext, uri, null, attributes, sessionId)
            }.getOrNull()
        }

        if (channelSound != null && fromChannel == null) {
            Log.w(TAG, "channel sound could not be opened, using the bundled chime")
        }

        val player = fromChannel ?: runCatching {
            MediaPlayer.create(
                applicationContext,
                R.raw.tmc_spawn_alert_chime,
                attributes,
                sessionId
            )
        }.getOrNull()

        if (player == null) {
            Log.w(TAG, "no alert sound could be prepared")
            return false
        }

        player.setOnCompletionListener { finished ->
            runCatching { finished.release() }
        }

        return runCatching {
            player.start()
            true
        }.getOrElse {
            runCatching { player.release() }
            Log.w(TAG, "alert sound could not be started")
            false
        }
    }
}
