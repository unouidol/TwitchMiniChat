package com.fs.twitchminichat

import android.app.NotificationManager
import android.media.AudioManager

/**
 * Decides whether a posted spawn alert that made no sound is a defect worth
 * repairing, and how long to wait before concluding that it was.
 *
 * The alert sound is played by the system, not by this application, and it is
 * sometimes never played at all. Measured on three journal exports,
 * deduplicated: once the device's own broken default sound was fixed on
 * 2026-09-11, the system player never started for 3 of 135 alerts from a warm
 * process. On log27, 16 to 18 September, 1 of the 73 alerts observed
 * regularly was really silent, 1.4%. It cannot be repaired from here. It can
 * only stop being depended upon when it fails.
 */
object NotificationAlertFallbackPolicy {

    /**
     * How long the system is given to start its own player before giving up.
     *
     * Taken from the measured distribution rather than rounded to something
     * comfortable. Across roughly 300 observed alerts the system player first
     * appears between 787 and 1047 milliseconds after the post, and the latest
     * start seen at all was near 1520. This waits 2500: about two thirds again
     * beyond the latest start ever observed, and more than twice the top of the
     * usual band.
     *
     * The margin is deliberately generous because the two ways of being wrong
     * do not cost the same. Waiting too long costs a late alert - the chime
     * begins about two and a half seconds after the notification and, at 1450
     * milliseconds long, finishes near four - which is within what this project
     * accepts. Concluding too early costs a chime on top of the system's own,
     * and a doubled alert is not acceptable at any frequency. Between a late
     * sound and two sounds, the tail of the distribution gets the benefit of
     * the doubt.
     *
     * Measured again in the field on log27: the system player started between
     * 726 and 1039 milliseconds across more than seventy alerts. The margin
     * stands.
     */
    const val SYSTEM_PLAYER_GRACE_MS: Long = 2_500L

    /** Gap between two reads of the active players while waiting. */
    const val POLL_INTERVAL_MS: Long = 60L

    /** The system played the alert itself, as it is supposed to. */
    const val OUTCOME_PLAYED = "played"

    /** The system played nothing, so this application played the chime. */
    const val OUTCOME_FALLBACK = "fallback"

    /**
     * Silence was asked for, so nothing was played.
     *
     * Written before the alert is posted when a setting already asked for it,
     * or just before the fallback would have played when one changed during
     * the grace; the second carries checkedAt=before_playback.
     */
    const val OUTCOME_SUPPRESSED = "suppressed"

    /**
     * The watch read the players too rarely to tell silence from a sound it
     * missed, so nothing was played. The line carries the sampleCount it had.
     */
    const val OUTCOME_UNOBSERVED = "unobserved"

    /**
     * Fewest readings on which "no system player appeared" may be concluded.
     *
     * Half the grace divided by the polling interval, derived rather than
     * written so that changing either constant moves it. Below it the readings
     * are too sparse to see a system sound that starts and ends between two of
     * them, and concluding silence would be concluding it in the dark.
     *
     * Why it exists, measured on log27, 85 armed alerts from 16 to 18 September.
     * The sampler went blind 12 times, 14%: a single pause of 60 milliseconds
     * served after 2.7 seconds ends the loop after one reading, before any
     * reading reaches the grace, and the undecided watch used to play. The
     * fallback played 13 times, once on 40 readings - a real repair - and twelve
     * times blind, and those were heard as doubled alerts. With 1 really silent
     * alert in 73 observed regularly, 12 blind windows hide about 0.17 silent
     * ones: playing in the dark bought a sixth of a spawn and cost twelve
     * doubled chimes. An unobserved alert now stays unrepaired, and says so.
     */
    const val MIN_SAMPLES_FOR_SILENCE: Int =
        (SYSTEM_PLAYER_GRACE_MS / 2 / POLL_INTERVAL_MS).toInt()

    /** Whether [sampleCount] readings are enough to conclude the system stayed silent. */
    fun samplingAdequate(sampleCount: Int): Boolean {
        return sampleCount >= MIN_SAMPLES_FOR_SILENCE
    }

    /** Do Not Disturb, or any filter narrower than everything. */
    const val REASON_INTERRUPTION_FILTER = "interruption_filter"

    /** The ringer is silent or set to vibrate. */
    const val REASON_RINGER_MODE = "ringer_mode"

    /** The notification stream is turned down to nothing. */
    const val REASON_NOTIFICATION_VOLUME = "notification_volume"

    /** The channel itself was configured without a sound. */
    const val REASON_CHANNEL_SILENT = "channel_silent"

    /**
     * Returns which setting asked for silence, or null when none did.
     *
     * The conditions are checked in a fixed order and the first that decides is
     * the one reported, so a journal line names a single cause rather than a
     * set. It is the same test as [isSilenceADefect], phrased so the answer can
     * be written down: an entry saying only that nothing was played leaves the
     * reader to guess which of four settings was responsible.
     */
    fun suppressionReason(
        interruptionFilter: Int,
        ringerMode: Int,
        notificationVolume: Int,
        channelHasSound: Boolean
    ): String? {
        if (interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
            return REASON_INTERRUPTION_FILTER
        }
        if (ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return REASON_RINGER_MODE
        }
        if (notificationVolume <= 0) {
            return REASON_NOTIFICATION_VOLUME
        }
        if (!channelHasSound) {
            return REASON_CHANNEL_SILENT
        }

        return null
    }

    /**
     * Returns whether silence after this alert would be a fault rather than a
     * setting.
     *
     * Every one of these four says the user asked to be able to hear alerts. If
     * any is false the silence was requested, and nothing may override it: Do
     * Not Disturb or a filtered mode, a silent or vibrate ringer, the
     * notification stream turned down to nothing, or a channel the user
     * configured without a sound.
     */
    fun isSilenceADefect(
        interruptionFilter: Int,
        ringerMode: Int,
        notificationVolume: Int,
        channelHasSound: Boolean
    ): Boolean {
        return suppressionReason(
            interruptionFilter = interruptionFilter,
            ringerMode = ringerMode,
            notificationVolume = notificationVolume,
            channelHasSound = channelHasSound
        ) == null
    }

    /**
     * Returns whether the system started a player of its own.
     *
     * Compared against the count taken before posting, so audio that was
     * already running - the case where the failure concentrates - is not
     * mistaken for the alert being played.
     */
    fun systemPlayerAppeared(playersBefore: Int, playersPeak: Int): Boolean {
        return playersPeak > playersBefore
    }

    /**
     * Returns whether this application should play the chime itself.
     *
     * True only when the user could have heard an alert and the system did not
     * produce one. It is asked once per alert and never reconsidered: a dropped
     * alert is not retried, because a queued or repeated sound would be a
     * second failure rather than a recovery.
     */
    fun shouldPlayFallback(
        interruptionFilter: Int,
        ringerMode: Int,
        notificationVolume: Int,
        channelHasSound: Boolean,
        systemPlayerAppeared: Boolean
    ): Boolean {
        if (systemPlayerAppeared) return false

        return isSilenceADefect(
            interruptionFilter = interruptionFilter,
            ringerMode = ringerMode,
            notificationVolume = notificationVolume,
            channelHasSound = channelHasSound
        )
    }
}
