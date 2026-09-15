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
     * What a single watch established about one alert.
     *
     * @param systemStartMs milliseconds after the watch began at which the
     *   system's player appeared, or null if it did not within the grace. The
     *   same quantity the diagnostics call firstRiseMs, taken from the same
     *   sample, so the two can never disagree.
     * @param fallbackPlayed whether the fallback sound started, or null when it
     *   was never attempted because the system played or the watch was not armed.
     */
    data class Watch(
        val systemStartMs: Long?,
        val fallbackPlayed: Boolean?
    )

    /**
     * Samples the active players for one alert, and plays the sound once if the
     * system has not started a player of its own by the end of the grace.
     *
     * This is the only sampler for an alert. Every reading is handed to
     * [onSample] as it is taken, so an observer builds its record from exactly
     * the samples this verdict was reached on rather than from a second loop
     * running out of step with it - two independent loops over the same device
     * counter can disagree about the very alerts that fail, which is where the
     * record is read.
     *
     * The verdict is fixed by the first reading at or after
     * [NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS]: a player above
     * [playersBefore] before then means the system played; none means the
     * fallback plays, when [fallbackArmed]. Sampling continues until [windowMs]
     * for the observer's sake, never shorter than the grace, so a fallback
     * sound started at the end of the grace shows up in the later samples too.
     */
    fun watch(
        context: Context,
        audioManager: AudioManager?,
        playersBefore: Int,
        channelSound: Uri?,
        fallbackArmed: Boolean,
        windowMs: Long,
        onSample: (offsetMs: Long, playerCount: Int) -> Unit
    ): Watch {
        if (audioManager == null) {
            val played = if (fallbackArmed) playOnce(context, null, channelSound) else null
            return Watch(systemStartMs = null, fallbackPlayed = played)
        }

        return sampleAndDecide(
            playersBefore = playersBefore,
            fallbackArmed = fallbackArmed,
            windowMs = windowMs,
            now = SystemClock::elapsedRealtime,
            playerCount = { activePlayerCount(audioManager) },
            pause = { ms -> runCatching { Thread.sleep(ms) }.isSuccess },
            playFallback = { playOnce(context, audioManager, channelSound) },
            onSample = onSample
        )
    }

    /**
     * The sampling loop of [watch], with the clock, the player count, the pause
     * and the sound passed in so the loop itself can be tested.
     *
     * Deciding early and stopping early are different things, and only the first
     * is allowed. The verdict is fixed at the first reading that settles it, but
     * the loop keeps reading until the window closes, because the observation row
     * is derived from these same samples: stopping at the first rise would leave
     * lastRiseMs and elevatedSpanMs with nothing to measure. elevatedSpanMs is the
     * field that told a sound never born from a sound cut short, so losing it
     * would erase the distinction the journal was built on - and no policy test
     * would notice, which is why this loop has tests of its own.
     *
     * [pause] returns false when the pause was interrupted, which ends sampling.
     */
    internal fun sampleAndDecide(
        playersBefore: Int,
        fallbackArmed: Boolean,
        windowMs: Long,
        now: () -> Long,
        playerCount: () -> Int,
        pause: (Long) -> Boolean,
        playFallback: () -> Boolean,
        onSample: (offsetMs: Long, playerCount: Int) -> Unit
    ): Watch {
        val graceMs = NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS
        val startedAtMs = now()
        val untilMs = maxOf(windowMs, graceMs)
        var decided = false
        var systemStartMs: Long? = null
        var fallbackPlayed: Boolean? = null

        while (now() - startedAtMs < untilMs) {
            /*
             * The offset is read after the count, as both loops this replaced
             * did, so rows written before and after the merge stay comparable.
             */
            val count = playerCount()
            val offsetMs = now() - startedAtMs

            onSample(offsetMs, count)

            if (!decided) {
                if (
                    offsetMs < graceMs &&
                    NotificationAlertFallbackPolicy.systemPlayerAppeared(
                        playersBefore = playersBefore,
                        playersPeak = count
                    )
                ) {
                    systemStartMs = offsetMs
                    decided = true
                } else if (offsetMs >= graceMs) {
                    decided = true
                    if (fallbackArmed) {
                        fallbackPlayed = playFallback()
                    }
                }
            }

            if (!pause(NotificationAlertFallbackPolicy.POLL_INTERVAL_MS)) break
        }

        if (!decided && fallbackArmed) {
            fallbackPlayed = playFallback()
        }

        return Watch(systemStartMs = systemStartMs, fallbackPlayed = fallbackPlayed)
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
