package com.fs.twitchminichat.diagnostics

/**
 * One reading of how many audio players the framework reported as active.
 *
 * @param offsetMs milliseconds elapsed since the observation started.
 * @param playerCount active players the framework reported at that instant.
 */
data class AudioPlayerSample(
    val offsetMs: Long,
    val playerCount: Int
)

/**
 * What a sequence of player-count readings says about the alert sound.
 *
 * @param playersPeak highest player count seen, never below the starting count.
 * @param elevatedSamples how many readings were above the starting count.
 * @param firstRiseMs offset of the first elevated reading, null when none was.
 * @param lastRiseMs offset of the last elevated reading, null when none was.
 */
data class AudioPlaybackSummary(
    val playersPeak: Int,
    val elevatedSamples: Int,
    val firstRiseMs: Long?,
    val lastRiseMs: Long?
) {
    /**
     * Milliseconds between the first and the last elevated reading.
     *
     * This is a lower bound on how long the alert sounded, not the exact
     * duration: the player may have started shortly before the first elevated
     * reading and may have continued past the last one. The true duration lies
     * between this span and the span plus twice the polling interval.
     *
     * A span of zero alongside a single elevated sample is the signature this
     * field exists to expose: a player that appeared for one poll and was gone
     * by the next, which is indistinguishable from a sustained chime in every
     * field recorded before it.
     */
    val elevatedSpanMs: Long?
        get() {
            val first = firstRiseMs ?: return null
            val last = lastRiseMs ?: return null
            return last - first
        }
}

/**
 * Reduces raw player-count readings to the shape written to the journal.
 *
 * Kept apart from the service so the arithmetic can be tested against crafted
 * sequences instead of against whatever the audio framework happens to do on a
 * device.
 */
object AudioPlaybackObservation {

    /**
     * Summarises [samples] against the player count observed before the alert.
     *
     * A reading counts as elevated when it is strictly above [playersBefore],
     * so audio that was already playing when the alert was posted never reads
     * as an alert sound of its own.
     */
    fun summarize(
        playersBefore: Int,
        samples: List<AudioPlayerSample>
    ): AudioPlaybackSummary {
        var peak = playersBefore
        var elevatedSamples = 0
        var firstRiseMs: Long? = null
        var lastRiseMs: Long? = null

        for (sample in samples) {
            if (sample.playerCount > peak) {
                peak = sample.playerCount
            }

            if (sample.playerCount > playersBefore) {
                elevatedSamples++

                if (firstRiseMs == null) {
                    firstRiseMs = sample.offsetMs
                }

                lastRiseMs = sample.offsetMs
            }
        }

        return AudioPlaybackSummary(
            playersPeak = peak,
            elevatedSamples = elevatedSamples,
            firstRiseMs = firstRiseMs,
            lastRiseMs = lastRiseMs
        )
    }
}
