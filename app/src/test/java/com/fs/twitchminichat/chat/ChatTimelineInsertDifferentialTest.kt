package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelineOrderer
import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * Compares where [ChatTimeline] and [ChatTimelineOrderer] place a row with a transcription of
 * how they did it at 4850a83: a linear scan from the first row for the first position newer
 * than the candidate.
 *
 * The transcription writes its own comparison of timestamps and sequences instead of calling
 * [ChatTimelinePosition.compareTo], so a change to the comparison cannot move the oracle with it.
 * The inputs are chosen to collide: few timestamps, timestamps equal after truncation to the
 * millisecond, unusable timestamps that fall back to a clock with few values, and positions
 * preserved from removed rows or copied from rows still present.
 */
class ChatTimelineInsertDifferentialTest {

    @Test(timeout = 120_000L)
    fun insertionIndex_isTheFirstNewerPosition_asTheLinearScanFoundIt() {
        repeat(ORDERER_SEEDS) { seed ->
            val random = Random(seed)
            val existing = ArrayList<ChatTimelinePosition>()
            repeat(random.nextInt(0, 60)) {
                val candidate = collidingPosition(random, existing)
                existing.add(linearInsertionIndex(existing, candidate), candidate)
            }
            repeat(40) { probe ->
                val candidate = collidingPosition(random, existing)
                assertEquals(
                    "seed $seed probe $probe size ${existing.size} candidate $candidate",
                    linearInsertionIndex(existing, candidate),
                    ChatTimelineOrderer.insertionIndex(existing, candidate)
                )
            }
        }
    }

    @Test(timeout = 300_000L)
    fun timeline_holdsTheSameRowsInTheSameOrder_afterEveryOperation() {
        repeat(TIMELINE_SEEDS) { seed ->
            val random = Random(seed)
            var clockMillis = 0L
            val clock = { clockMillis }
            val timeline = ChatTimeline(clock)
            val reference = LinearTimeline(clock)
            val removedPositions = ArrayList<ChatTimelinePosition>()

            repeat(OPERATIONS_PER_SEED) { step ->
                val where = "seed $seed step $step"
                clockMillis = CLOCK_VALUES[random.nextInt(CLOCK_VALUES.size)]

                when (val choice = random.nextInt(100)) {
                    in 0..69 -> {
                        val timestampSec = TIMESTAMPS[random.nextInt(TIMESTAMPS.size)]
                        val preserved = when {
                            choice < 50 -> null
                            choice < 60 && removedPositions.isNotEmpty() -> removedPositions[random.nextInt(removedPositions.size)]
                            reference.rows.isNotEmpty() -> reference.rows[random.nextInt(reference.rows.size)].position
                            else -> null
                        }
                        val name = "r$step"
                        val expected = reference.insert(timestampSec, preserved) { p -> SystemLineRow(p, name) }
                        val actual = timeline.insert(timestampSec, preserved) { p -> SystemLineRow(p, name) }
                        assertEquals("$where insert index", expected, actual)
                    }
                    in 70..84 -> if (reference.rows.isNotEmpty()) {
                        val index = random.nextInt(reference.rows.size)
                        val expected = reference.removeAt(index)
                        assertEquals("$where removed row", expected, timeline.removeAt(index))
                        removedPositions += expected.position
                    }
                    in 85..96 -> if (reference.rows.isNotEmpty()) {
                        val index = random.nextInt(reference.rows.size)
                        val replacement = SystemLineRow(reference.rows[index].position, "replaced$step")
                        reference.replaceAt(index, replacement)
                        timeline.replaceAt(index, replacement)
                    }
                    /* Odd seeds never clear, so their timelines grow to hundreds of rows. */
                    else -> if (seed % 2 == 0) {
                        reference.clear()
                        timeline.clear()
                    }
                }

                assertEquals("$where rows", reference.rows, timeline.rows)
                largestTimeline = maxOf(largestTimeline, reference.rows.size)
            }
        }
        println("timeline seeds=$TIMELINE_SEEDS operations=$OPERATIONS_PER_SEED largest=$largestTimeline")
    }

    private var largestTimeline = 0

    /** A position likely to equal, or sit between, positions already present. */
    private fun collidingPosition(random: Random, existing: List<ChatTimelinePosition>): ChatTimelinePosition {
        if (existing.isNotEmpty() && random.nextInt(4) == 0) return existing[random.nextInt(existing.size)]
        return ChatTimelinePosition(
            timestampMillis = random.nextLong(0L, 6L) * 1_000L,
            sequence = random.nextLong(0L, 8L)
        )
    }

    /** ChatTimelineOrderer.insertionIndex at 4850a83, with the comparison written out. */
    private fun linearInsertionIndex(existing: List<ChatTimelinePosition>, candidate: ChatTimelinePosition): Int {
        val index = existing.indexOfFirst { position -> isNewer(position, candidate) }
        return if (index >= 0) index else existing.size
    }

    private fun isNewer(position: ChatTimelinePosition, than: ChatTimelinePosition): Boolean {
        if (position.timestampMillis != than.timestampMillis) return position.timestampMillis > than.timestampMillis
        return position.sequence > than.sequence
    }

    /** ChatTimeline at 4850a83, with its linear placement and comparison written out. */
    private inner class LinearTimeline(private val currentTimeMillis: () -> Long) {
        val rows = ArrayList<ChatTimelineRow>()
        private var nextSequence = 0L

        fun insert(
            messageTimestampSec: Double?,
            preservedPosition: ChatTimelinePosition?,
            create: (ChatTimelinePosition) -> ChatTimelineRow
        ): Int {
            val millis = messageTimestampSec
                ?.takeIf { timestamp -> timestamp.isFinite() && timestamp > 0.0 }
                ?.times(1000.0)
                ?.toLong()
                ?: currentTimeMillis()
            val position = ChatTimelinePosition(millis, preservedPosition?.sequence ?: nextSequence++)
            val index = linearInsertionIndex(rows.map { row -> row.position }, position)
            rows.add(index, create(position))
            return index
        }

        fun removeAt(index: Int): ChatTimelineRow = rows.removeAt(index)

        fun replaceAt(index: Int, row: ChatTimelineRow) {
            rows[index] = row
        }

        fun clear() {
            rows.clear()
            nextSequence = 0L
        }
    }

    private companion object {
        const val ORDERER_SEEDS = 2_000
        const val TIMELINE_SEEDS = 300
        const val OPERATIONS_PER_SEED = 500

        /* Few values, some equal after truncation to the millisecond, some unusable. */
        val TIMESTAMPS: List<Double?> = listOf(
            1.0, 1.0004, 1.0009, 2.0, 2.0, 3.5, 4.0, null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY
        )

        /* The clock an unusable timestamp falls back to, with values the usable ones also take. */
        val CLOCK_VALUES = longArrayOf(1_000L, 2_000L, 4_000L)
    }
}
