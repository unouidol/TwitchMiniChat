package com.fs.twitchminichat

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
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
     * Returns true as soon as one appears, so a healthy alert costs only the
     * time it actually took to start. Called after the notification has been
     * posted, so nothing here delays delivery; it delays only the end of the
     * message callback that has already done its work.
     */
    fun awaitSystemPlayer(
        audioManager: AudioManager?,
        playersBefore: Int
    ): Boolean {
        audioManager ?: return false

        val startedAtMs = SystemClock.elapsedRealtime()

        while (
            SystemClock.elapsedRealtime() - startedAtMs <
            NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS
        ) {
            if (
                NotificationAlertFallbackPolicy.systemPlayerAppeared(
                    playersBefore = playersBefore,
                    playersPeak = activePlayerCount(audioManager)
                )
            ) {
                return true
            }

            runCatching {
                Thread.sleep(NotificationAlertFallbackPolicy.POLL_INTERVAL_MS)
            }.getOrElse { return false }
        }

        return false
    }

    /**
     * Plays the alert chime exactly once, and reports whether it started.
     *
     * Carries the same attributes the notification channel uses, so the sound
     * follows the notification volume the user set rather than the media
     * volume. It does not vibrate: the system has already done that when it
     * posted the notification, and a second buzz would be worse than the
     * silence being repaired.
     *
     * The player releases itself on completion. Nothing retries: if this fails
     * the alert stays silent, which is the state it was already in.
     */
    fun playOnce(context: Context, audioManager: AudioManager?): Boolean {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val sessionId = runCatching {
            audioManager?.generateAudioSessionId()
        }.getOrNull() ?: AudioManager.AUDIO_SESSION_ID_GENERATE

        val player = runCatching {
            MediaPlayer.create(
                context.applicationContext,
                R.raw.tmc_spawn_alert_chime,
                attributes,
                sessionId
            )
        }.getOrNull()

        if (player == null) {
            Log.w(TAG, "fallback chime could not be prepared")
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
            Log.w(TAG, "fallback chime could not be started")
            false
        }
    }
}
