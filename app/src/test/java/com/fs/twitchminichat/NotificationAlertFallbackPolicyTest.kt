package com.fs.twitchminichat

import android.app.NotificationManager
import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies when the application plays the spawn alert chime in place of the
 * system, and when it must stay quiet.
 *
 * The expensive mistake here is playing on top of a system that was working,
 * or over a silence the user asked for, so the cases that must produce no
 * sound are enumerated exhaustively rather than sampled.
 */
class NotificationAlertFallbackPolicyTest {

    /** Every condition satisfied: the user could have heard an alert. */
    @Test
    fun isSilenceADefect_trueWhenTheUserCouldHaveHeardIt() {
        assertTrue(
            NotificationAlertFallbackPolicy.isSilenceADefect(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 7,
                channelHasSound = true
            )
        )
    }

    /** Do Not Disturb, in any of its filtered shapes, means silence was asked for. */
    @Test
    fun isSilenceADefect_falseUnderEveryInterruptionFilterButAll() {
        val filtered = listOf(
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        )

        for (filter in filtered) {
            assertFalse(
                "filter $filter must suppress the fallback",
                NotificationAlertFallbackPolicy.isSilenceADefect(
                    interruptionFilter = filter,
                    ringerMode = AudioManager.RINGER_MODE_NORMAL,
                    notificationVolume = 7,
                    channelHasSound = true
                )
            )
        }
    }

    /** A silent or vibrating ringer is a decision, not a fault. */
    @Test
    fun isSilenceADefect_falseWhenTheRingerIsNotNormal() {
        for (mode in listOf(AudioManager.RINGER_MODE_SILENT, AudioManager.RINGER_MODE_VIBRATE)) {
            assertFalse(
                "ringer mode $mode must suppress the fallback",
                NotificationAlertFallbackPolicy.isSilenceADefect(
                    interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                    ringerMode = mode,
                    notificationVolume = 7,
                    channelHasSound = true
                )
            )
        }
    }

    /** The notification stream turned fully down is the same instruction. */
    @Test
    fun isSilenceADefect_falseWhenTheNotificationVolumeIsZero() {
        assertFalse(
            NotificationAlertFallbackPolicy.isSilenceADefect(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 0,
                channelHasSound = true
            )
        )
    }

    /** A channel the user configured without a sound must never be overridden. */
    @Test
    fun isSilenceADefect_falseOnASilentChannel() {
        assertFalse(
            NotificationAlertFallbackPolicy.isSilenceADefect(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 7,
                channelHasSound = false
            )
        )
    }

    /**
     * All sixteen combinations of the four conditions.
     *
     * Exactly one of them may make a sound. Enumerating them all is the only
     * way to show that no pair of settings combines into an unintended yes.
     */
    @Test
    fun isSilenceADefect_onlyOneOfSixteenCombinationsAllowsSound() {
        var allowed = 0

        for (filterOk in listOf(true, false)) {
            for (ringerOk in listOf(true, false)) {
                for (volumeOk in listOf(true, false)) {
                    for (soundOk in listOf(true, false)) {
                        val result = NotificationAlertFallbackPolicy.isSilenceADefect(
                            interruptionFilter = if (filterOk) {
                                NotificationManager.INTERRUPTION_FILTER_ALL
                            } else {
                                NotificationManager.INTERRUPTION_FILTER_PRIORITY
                            },
                            ringerMode = if (ringerOk) {
                                AudioManager.RINGER_MODE_NORMAL
                            } else {
                                AudioManager.RINGER_MODE_SILENT
                            },
                            notificationVolume = if (volumeOk) 7 else 0,
                            channelHasSound = soundOk
                        )

                        val everythingOk = filterOk && ringerOk && volumeOk && soundOk
                        assertEquals(
                            "filter=$filterOk ringer=$ringerOk volume=$volumeOk sound=$soundOk",
                            everythingOk,
                            result
                        )

                        if (result) allowed++
                    }
                }
            }
        }

        assertEquals(1, allowed)
    }

    /** An unreadable state is never mistaken for permission to make a sound. */
    @Test
    fun isSilenceADefect_falseWhenTheStateCouldNotBeRead() {
        assertFalse(
            NotificationAlertFallbackPolicy.isSilenceADefect(
                interruptionFilter = -1,
                ringerMode = -1,
                notificationVolume = 0,
                channelHasSound = true
            )
        )
    }

    /** Nothing suppresses the sound when every setting allows it. */
    @Test
    fun suppressionReason_nullWhenNothingAsksForSilence() {
        assertEquals(
            null,
            NotificationAlertFallbackPolicy.suppressionReason(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 7,
                channelHasSound = true
            )
        )
    }

    /**
     * Each setting, alone, names itself as the cause.
     *
     * A journal line saying only that nothing was played would leave the reader
     * to guess which of four settings decided, which is the difference between
     * a record and a shrug.
     */
    @Test
    fun suppressionReason_namesTheSettingThatDecided() {
        assertEquals(
            NotificationAlertFallbackPolicy.REASON_INTERRUPTION_FILTER,
            NotificationAlertFallbackPolicy.suppressionReason(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 7,
                channelHasSound = true
            )
        )
        assertEquals(
            NotificationAlertFallbackPolicy.REASON_RINGER_MODE,
            NotificationAlertFallbackPolicy.suppressionReason(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_VIBRATE,
                notificationVolume = 7,
                channelHasSound = true
            )
        )
        assertEquals(
            NotificationAlertFallbackPolicy.REASON_NOTIFICATION_VOLUME,
            NotificationAlertFallbackPolicy.suppressionReason(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 0,
                channelHasSound = true
            )
        )
        assertEquals(
            NotificationAlertFallbackPolicy.REASON_CHANNEL_SILENT,
            NotificationAlertFallbackPolicy.suppressionReason(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 7,
                channelHasSound = false
            )
        )
    }

    /**
     * With several settings against it, one cause is reported, not a set.
     *
     * The order is fixed so the same device state always produces the same
     * line, which is what makes rows countable rather than merely readable.
     */
    @Test
    fun suppressionReason_reportsOneCauseWhenSeveralApply() {
        assertEquals(
            NotificationAlertFallbackPolicy.REASON_INTERRUPTION_FILTER,
            NotificationAlertFallbackPolicy.suppressionReason(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_NONE,
                ringerMode = AudioManager.RINGER_MODE_SILENT,
                notificationVolume = 0,
                channelHasSound = false
            )
        )
    }

    /** The reported cause and the decision can never disagree. */
    @Test
    fun suppressionReason_agreesWithTheDecisionEverywhere() {
        for (filterOk in listOf(true, false)) {
            for (ringerOk in listOf(true, false)) {
                for (volumeOk in listOf(true, false)) {
                    for (soundOk in listOf(true, false)) {
                        val filter = if (filterOk) {
                            NotificationManager.INTERRUPTION_FILTER_ALL
                        } else {
                            NotificationManager.INTERRUPTION_FILTER_PRIORITY
                        }
                        val ringer = if (ringerOk) {
                            AudioManager.RINGER_MODE_NORMAL
                        } else {
                            AudioManager.RINGER_MODE_SILENT
                        }
                        val volume = if (volumeOk) 7 else 0

                        val reason = NotificationAlertFallbackPolicy.suppressionReason(
                            filter, ringer, volume, soundOk
                        )
                        val defect = NotificationAlertFallbackPolicy.isSilenceADefect(
                            filter, ringer, volume, soundOk
                        )

                        assertEquals(
                            "filter=$filterOk ringer=$ringerOk volume=$volumeOk sound=$soundOk",
                            defect,
                            reason == null
                        )
                    }
                }
            }
        }
    }

    /** A player above the starting count is the system doing its job. */
    @Test
    fun systemPlayerAppeared_trueOnlyAboveTheStartingCount() {
        assertTrue(NotificationAlertFallbackPolicy.systemPlayerAppeared(0, 1))
        assertTrue(NotificationAlertFallbackPolicy.systemPlayerAppeared(2, 3))
        assertFalse(NotificationAlertFallbackPolicy.systemPlayerAppeared(0, 0))
        assertFalse(NotificationAlertFallbackPolicy.systemPlayerAppeared(1, 1))
    }

    /**
     * Audio already playing is not the alert.
     *
     * This is the case the residual failures concentrate in: one player is
     * already running, the alert never starts a second, and a count that
     * ignored the baseline would read the music as the alert and stay silent.
     */
    @Test
    fun systemPlayerAppeared_falseWhenOnlyPreexistingAudioIsPlaying() {
        assertFalse(NotificationAlertFallbackPolicy.systemPlayerAppeared(1, 1))
    }

    /** The fallback plays only when it could be heard and nothing was played. */
    @Test
    fun shouldPlayFallback_playsOnlyOnAnAudibleSilence() {
        assertTrue(
            NotificationAlertFallbackPolicy.shouldPlayFallback(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 7,
                channelHasSound = true,
                systemPlayerAppeared = false
            )
        )
    }

    /** A working system is never doubled, whatever the settings say. */
    @Test
    fun shouldPlayFallback_neverPlaysOnTopOfTheSystem() {
        assertFalse(
            NotificationAlertFallbackPolicy.shouldPlayFallback(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 7,
                channelHasSound = true,
                systemPlayerAppeared = true
            )
        )
    }

    /** Requested silence wins even when the system played nothing. */
    @Test
    fun shouldPlayFallback_staysQuietWhenSilenceWasRequested() {
        assertFalse(
            NotificationAlertFallbackPolicy.shouldPlayFallback(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_NONE,
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                notificationVolume = 7,
                channelHasSound = true,
                systemPlayerAppeared = false
            )
        )
    }

    /**
     * The grace clears the whole observed distribution of system starts.
     *
     * Asserted as a relationship to the measurements rather than as a literal,
     * so lowering it under the latest start ever seen fails here instead of
     * producing doubled chimes on a device.
     */
    @Test
    fun grace_sitsBeyondEveryObservedSystemStart() {
        assertTrue(
            "must clear the latest observed start",
            NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS > LATEST_OBSERVED_START_MS
        )
        assertTrue(
            "must clear the usual band with room to spare",
            NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS > 2 * TYPICAL_LATEST_START_MS
        )
    }

    /** A late alert is acceptable, but not an arbitrarily late one. */
    @Test
    fun grace_staysWithinTheProjectsToleranceForALateAlert() {
        assertTrue(
            NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS <= 3_000L
        )
    }

    /** Sampling is fine enough to notice a player that lives briefly. */
    @Test
    fun pollInterval_samplesManyTimesWithinTheGrace() {
        val samples =
            NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS /
                NotificationAlertFallbackPolicy.POLL_INTERVAL_MS

        assertTrue("expected many samples, got $samples", samples >= 30)
    }

    private companion object {
        /** Latest first-rise seen across roughly 300 observed alerts. */
        const val LATEST_OBSERVED_START_MS = 1_520L

        /** Top of the band the usual starts fall in. */
        const val TYPICAL_LATEST_START_MS = 1_047L
    }
}
