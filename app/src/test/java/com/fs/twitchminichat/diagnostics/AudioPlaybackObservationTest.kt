package com.fs.twitchminichat.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies how player-count readings become an observed alert duration.
 *
 * The sequences below stand in for the 60 ms polling the notification service
 * performs after posting an alert, so the arithmetic can be checked without a
 * device and without the audio framework.
 */
class AudioPlaybackObservationTest {

    /** Builds readings 60 ms apart from consecutive player counts. */
    private fun samples(vararg counts: Int): List<AudioPlayerSample> {
        return counts.mapIndexed { index, count ->
            AudioPlayerSample(
                offsetMs = index * POLL_INTERVAL_MS,
                playerCount = count
            )
        }
    }

    /** A player present from the first reading onwards sounds for the whole window. */
    @Test
    fun summarize_reportsSpanForImmediateStart() {
        val summary = AudioPlaybackObservation.summarize(
            playersBefore = 0,
            samples = samples(1, 1, 1, 1, 1)
        )

        assertEquals(1, summary.playersPeak)
        assertEquals(5, summary.elevatedSamples)
        assertEquals(0L, summary.firstRiseMs)
        assertEquals(240L, summary.lastRiseMs)
        assertEquals(240L, summary.elevatedSpanMs)
    }

    /** A player that appears late still reports the span it was actually present for. */
    @Test
    fun summarize_reportsSpanForLateStart() {
        val summary = AudioPlaybackObservation.summarize(
            playersBefore = 0,
            samples = samples(0, 0, 0, 1, 1, 1)
        )

        assertEquals(1, summary.playersPeak)
        assertEquals(3, summary.elevatedSamples)
        assertEquals(180L, summary.firstRiseMs)
        assertEquals(300L, summary.lastRiseMs)
        assertEquals(120L, summary.elevatedSpanMs)
    }

    /**
     * The case this field was added for.
     *
     * A player seen in exactly one reading and gone by the next produces the
     * same firstRiseMs as a chime that sounds for a second. The zero span next
     * to a single elevated sample is what separates them.
     */
    @Test
    fun summarize_reportsZeroSpanForPlayerThatDiesImmediately() {
        val summary = AudioPlaybackObservation.summarize(
            playersBefore = 0,
            samples = samples(0, 0, 1, 0, 0, 0, 0)
        )

        assertEquals(1, summary.playersPeak)
        assertEquals(1, summary.elevatedSamples)
        assertEquals(120L, summary.firstRiseMs)
        assertEquals(120L, summary.lastRiseMs)
        assertEquals(0L, summary.elevatedSpanMs)
    }

    /** Nothing ever rises above the starting count, so there is no span to report. */
    @Test
    fun summarize_reportsNothingWhenNoPlayerEverStarts() {
        val summary = AudioPlaybackObservation.summarize(
            playersBefore = 0,
            samples = samples(0, 0, 0, 0)
        )

        assertEquals(0, summary.playersPeak)
        assertEquals(0, summary.elevatedSamples)
        assertNull(summary.firstRiseMs)
        assertNull(summary.lastRiseMs)
        assertNull(summary.elevatedSpanMs)
    }

    /**
     * Audio already playing when the alert was posted is not an alert sound.
     *
     * Music through headphones keeps a player active throughout, and counting it
     * would report every silent alert as having sounded.
     */
    @Test
    fun summarize_ignoresAudioAlreadyPlayingBeforeTheAlert() {
        val summary = AudioPlaybackObservation.summarize(
            playersBefore = 1,
            samples = samples(1, 1, 1, 1)
        )

        assertEquals(1, summary.playersPeak)
        assertEquals(0, summary.elevatedSamples)
        assertNull(summary.firstRiseMs)
        assertNull(summary.elevatedSpanMs)
    }

    /**
     * A player that stops and starts again spans both bursts.
     *
     * The span is deliberately measured from first to last elevated reading, so
     * a gap in the middle widens it rather than splitting the observation.
     */
    @Test
    fun summarize_spansBothBurstsWhenPlaybackRestarts() {
        val summary = AudioPlaybackObservation.summarize(
            playersBefore = 0,
            samples = samples(0, 1, 0, 0, 1, 0)
        )

        assertEquals(2, summary.elevatedSamples)
        assertEquals(60L, summary.firstRiseMs)
        assertEquals(240L, summary.lastRiseMs)
        assertEquals(180L, summary.elevatedSpanMs)
    }

    /** An empty observation reports nothing rather than failing. */
    @Test
    fun summarize_handlesEmptySampleList() {
        val summary = AudioPlaybackObservation.summarize(
            playersBefore = 0,
            samples = emptyList()
        )

        assertEquals(0, summary.playersPeak)
        assertEquals(0, summary.elevatedSamples)
        assertNull(summary.elevatedSpanMs)
    }

    private companion object {
        /** Matches AUDIO_POLL_INTERVAL_MS in the notification service. */
        const val POLL_INTERVAL_MS = 60L
    }
}
