package com.fs.twitchminichat

import com.fs.twitchminichat.diagnostics.AudioPlaybackObservation
import com.fs.twitchminichat.diagnostics.AudioPlayerSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins down the sampling loop behind the fallback, not just the policy it
 * consults.
 *
 * The loop decides early but must not stop early: the observation row is
 * derived from its samples, and a loop that ended at the first rise would leave
 * elevatedSpanMs with nothing to measure. That field is what told a sound never
 * born from a sound cut short. Losing it would break no policy test, so these
 * tests drive the loop itself with a clock that only moves when it pauses.
 */
class NotificationAlertFallbackWatchTest {

    /**
     * A clock that stands still until the loop pauses, then jumps by the pause -
     * or by what [advance] says, which is how a pause served late is staged.
     */
    private class FakeClock(
        private val advance: (call: Int, requestedMs: Long) -> Long
    ) {
        var nowMs = 0L
        private var calls = 0
        fun pause(ms: Long): Boolean {
            nowMs += advance(calls++, ms)
            return true
        }
    }

    private class Run(
        val watch: NotificationAlertFallback.Watch,
        val samples: List<AudioPlayerSample>,
        val fallbackOffsets: List<Long>
    )

    /** Drives the loop with [countAt] giving the player count at each instant. */
    private fun run(
        playersBefore: Int,
        fallbackArmed: Boolean,
        suppressionNow: () -> String? = { null },
        advance: (call: Int, requestedMs: Long) -> Long = { _, ms -> ms },
        countAt: (nowMs: Long, fallbackStarted: Boolean) -> Int
    ): Run {
        val clock = FakeClock(advance)
        val samples = mutableListOf<AudioPlayerSample>()
        val fallbackOffsets = mutableListOf<Long>()

        val watch = NotificationAlertFallback.sampleAndDecide(
            playersBefore = playersBefore,
            fallbackArmed = fallbackArmed,
            windowMs = WINDOW_MS,
            now = { clock.nowMs },
            playerCount = { countAt(clock.nowMs, fallbackOffsets.isNotEmpty()) },
            pause = clock::pause,
            playFallback = {
                fallbackOffsets += clock.nowMs
                true
            },
            suppressionNow = suppressionNow,
            onSample = { offsetMs, count -> samples += AudioPlayerSample(offsetMs, count) }
        )

        return Run(watch, samples, fallbackOffsets)
    }

    /**
     * The property this class exists for.
     *
     * The system player rises early and stays up. The verdict is reached at the
     * first rise, and sampling goes on to the end of the window, so the summary
     * reports a span rather than stopping at the first elevated reading.
     */
    @Test
    fun earlyRise_decidesAtTheRiseButKeepsSamplingToTheEnd() {
        val run = run(playersBefore = 0, fallbackArmed = true) { nowMs, _ ->
            if (nowMs in 840L..2_280L) 1 else 0
        }

        assertEquals(840L, run.watch.systemStartMs)
        assertNull("the system played, so nothing was attempted", run.watch.fallbackPlayed)
        assertTrue("no fallback sound", run.fallbackOffsets.isEmpty())

        assertEquals("samples across the whole window, 0 to 2580", 44, run.samples.size)
        assertEquals(2_580L, run.samples.last().offsetMs)

        val summary = AudioPlaybackObservation.summarize(playersBefore = 0, samples = run.samples)
        assertEquals(840L, summary.firstRiseMs)
        assertEquals(2_280L, summary.lastRiseMs)
        assertEquals(1_440L, summary.elevatedSpanMs)
        assertTrue(
            "elevatedSpanMs must be measured, not truncated at the first rise",
            (summary.elevatedSpanMs ?: 0L) > 0L
        )
    }

    /**
     * The observation and the verdict describe the same readings.
     *
     * Whenever the system played within the grace, the observation's first
     * rise and the fallback's system start are the same number.
     */
    @Test
    fun earlyRise_firstRiseAndSystemStartAreTheSameReading() {
        val run = run(playersBefore = 0, fallbackArmed = true) { nowMs, _ ->
            if (nowMs >= 900L) 1 else 0
        }

        val summary = AudioPlaybackObservation.summarize(playersBefore = 0, samples = run.samples)
        assertEquals(run.watch.systemStartMs, summary.firstRiseMs)
    }

    /**
     * A silent system gets the fallback exactly once, at the first reading at or
     * after the grace, and sampling carries on after it so the fallback's own
     * player shows up in the observation.
     */
    @Test
    fun silentSystem_playsOnceAtTheGraceAndKeepsSampling() {
        val run = run(playersBefore = 0, fallbackArmed = true) { _, fallbackStarted ->
            if (fallbackStarted) 1 else 0
        }

        assertNull(run.watch.systemStartMs)
        assertEquals(true, run.watch.fallbackPlayed)
        assertEquals("played once, never retried", listOf(2_520L), run.fallbackOffsets)
        assertEquals(44, run.samples.size)

        val summary = AudioPlaybackObservation.summarize(playersBefore = 0, samples = run.samples)
        assertTrue(
            "the late rise is the fallback's own player",
            (summary.firstRiseMs ?: 0L) > NotificationAlertFallbackPolicy.SYSTEM_PLAYER_GRACE_MS
        )
    }

    /** A suppressed alert is still observed, and nothing is ever played for it. */
    @Test
    fun disarmed_samplesTheWindowButNeverPlays() {
        val run = run(playersBefore = 0, fallbackArmed = false) { _, _ -> 0 }

        assertTrue(run.fallbackOffsets.isEmpty())
        assertNull(run.watch.fallbackPlayed)
        assertEquals(44, run.samples.size)
    }

    /**
     * Audio already playing is not the alert.
     *
     * One player runs throughout and the system never adds its own, which is
     * where the residual failures concentrate; the fallback must still fire.
     */
    @Test
    fun preexistingAudio_isNotMistakenForTheAlert() {
        val run = run(playersBefore = 1, fallbackArmed = true) { _, fallbackStarted ->
            if (fallbackStarted) 2 else 1
        }

        assertNull(run.watch.systemStartMs)
        assertEquals(listOf(2_520L), run.fallbackOffsets)
    }

    private companion object {
        /** The observation window the notification service passes in. */
        const val WINDOW_MS = 2_600L
    }
    /**
     * The defect measured on log27: a single pause served 2.7 seconds late ends
     * the loop after one reading, before any reading reaches the grace. The
     * watch saw nothing and must not conclude silence from it.
     */
    @Test
    fun blindWindow_playsNothingAndIsReportedUnobserved() {
        val run = run(
            playersBefore = 0,
            fallbackArmed = true,
            advance = { call, ms -> if (call == 0) 2_700L else ms },
            countAt = { _, _ -> 0 }
        )

        assertEquals(0, run.fallbackOffsets.size)
        assertEquals(1, run.watch.sampleCount)
        assertTrue(run.watch.unobserved)
        assertNull(run.watch.fallbackPlayed)
    }

    /**
     * The repair the guard must not remove. Readings dense enough to see a
     * sound, then one late pause that closes the window before the grace: the
     * silence was observed, so the fallback still plays, once.
     */
    @Test
    fun denseReadingsThenLatePause_stillRepairsOnce() {
        val run = run(
            playersBefore = 0,
            fallbackArmed = true,
            advance = { call, ms -> if (call == 39) 400L else ms },
            countAt = { _, _ -> 0 }
        )

        assertEquals(40, run.watch.sampleCount)
        assertEquals(1, run.fallbackOffsets.size)
        assertFalse(run.watch.unobserved)
    }

    /**
     * Do Not Disturb switched on during the grace. The settings were fine when
     * the alert was posted, and are read again just before playing.
     */
    @Test
    fun silenceRequestedDuringTheGrace_playsNothing() {
        val run = run(
            playersBefore = 0,
            fallbackArmed = true,
            suppressionNow = { NotificationAlertFallbackPolicy.REASON_INTERRUPTION_FILTER },
            countAt = { _, _ -> 0 }
        )

        assertEquals(0, run.fallbackOffsets.size)
        assertEquals(
            NotificationAlertFallbackPolicy.REASON_INTERRUPTION_FILTER,
            run.watch.lateSuppressionReason
        )
        assertNull(run.watch.fallbackPlayed)
    }

}
