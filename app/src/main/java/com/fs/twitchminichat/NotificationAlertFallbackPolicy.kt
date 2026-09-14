package com.fs.twitchminichat

import android.app.NotificationManager
import android.media.AudioManager

/**
 * Decides whether a posted spawn alert that made no sound is a defect worth
 * repairing, and how long to wait before concluding that it was.
 *
 * The alert sound is played by the system, not by this application. On around
 * 279 alerts observed in an audible environment it was sometimes never played
 * at all. The dominant cause was external and has been fixed, taking the rate
 * from 14.5% to 3.7%. What remains concentrates where audio was already
 * playing - 3 of 20 against 2 of 114 - and in those cases the player count
 * never rises, meaning the system player is never born. It cannot be repaired
 * from here. It can only stop being depended upon when it fails.
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
     */
    const val SYSTEM_PLAYER_GRACE_MS: Long = 2_500L

    /** Gap between two reads of the active players while waiting. */
    const val POLL_INTERVAL_MS: Long = 60L

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
        if (interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return false
        if (ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
        if (notificationVolume <= 0) return false

        return channelHasSound
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
