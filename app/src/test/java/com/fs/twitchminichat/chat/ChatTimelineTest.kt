package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Characterizes [ChatTimeline] against what ChatTimelineController did at fc60273:
 * where a row lands, which sequence it gets, which timestamp it is ordered by, and what
 * removal, re-insertion with a preserved position and clear do to both.
 *
 * Rows are system lines named after themselves, so an order reads as a list of names.
 */
class ChatTimelineTest {

    private var clockMillis = 1_000_000L
    private var clockReads = 0

    private val timeline = ChatTimeline(
        currentTimeMillis = {
            clockReads++
            clockMillis
        }
    )

    // ---------------------------------------------------------------------------
    // Order
    // ---------------------------------------------------------------------------

    @Test
    fun historyArrivingAfterLiveMessages_landsBeforeThem_inItsOwnOrder() {
        add("live1", 10.0)
        add("live2", 11.0)

        assertEquals(0, add("history1", 5.0))
        assertEquals(1, add("history2", 6.0))
        assertEquals(2, add("history3", 7.0))

        assertEquals(listOf("history1", "history2", "history3", "live1", "live2"), order())
    }

    @Test
    fun aMessageOlderThanSomeRowsAndNewerThanOthers_landsBetweenThem() {
        add("ten", 10.0)
        add("twenty", 20.0)

        assertEquals(1, add("fifteen", 15.0))
        assertEquals(listOf("ten", "fifteen", "twenty"), order())
    }

    @Test
    fun equalTimestamps_keepTheOrderTheRowsWereInsertedIn() {
        add("first", 5.0)
        add("second", 5.0)
        add("third", 5.0)

        assertEquals(listOf("first", "second", "third"), order())
    }

    @Test
    fun aLaterRowWithAnExistingTimestamp_goesAfterEveryRowOfThatTimestamp() {
        add("five", 5.0)
        add("six", 6.0)
        add("fiveAgain", 5.0)

        assertEquals(listOf("five", "fiveAgain", "six"), order())
    }

    // ---------------------------------------------------------------------------
    // The sequence counter
    // ---------------------------------------------------------------------------

    @Test
    fun sequencesStartAtZero_andRiseByOnePerInsertedRow() {
        add("a", 30.0)
        add("b", 10.0)
        add("c", 20.0)

        assertEquals(listOf(1L, 2L, 0L), timeline.rows.map { row -> row.position.sequence })
    }

    @Test
    fun aRowReinsertedWithItsPreservedPosition_keepsItsSequence_andTakesTheNewTimestamp() {
        add("pending", 10.0)
        val taken = timeline.removeAt(0).position

        add("canonical", 12.5, preserved = taken)

        val canonical = timeline.rows.single()
        assertEquals(ChatTimelinePosition(timestampMillis = 12_500L, sequence = 0L), canonical.position)
    }

    @Test
    fun aReplacedEcho_staysBeforeABotReplyOfTheSameSecond_thatArrivedBeforeTheReplacement() {
        add("echo", 10.0)
        add("botReply", 10.0)
        val taken = timeline.removeAt(0).position

        assertEquals(0, add("canonical", 10.0, preserved = taken))
        assertEquals(listOf("canonical", "botReply"), order())
    }

    @Test
    fun aPreservedInsertion_doesNotAdvanceTheSequence() {
        add("a", 1.0)
        add("b", 2.0)
        val taken = timeline.removeAt(0).position

        add("aReplaced", 1.0, preserved = taken)
        add("c", 3.0)

        assertEquals(listOf(0L, 1L, 2L), timeline.rows.map { row -> row.position.sequence })
    }

    @Test
    fun aPreservedPositionEqualToAnExistingRow_goesAfterThatRow() {
        add("a", 1.0)
        add("b", 1.0)

        /* Sequence 0 again, as only a preserved position can give it; same timestamp as a. */
        add("twin", 1.0, preserved = ChatTimelinePosition(timestampMillis = 0L, sequence = 0L))

        assertEquals(listOf("a", "twin", "b"), order())
    }

    // ---------------------------------------------------------------------------
    // The timestamp a row is ordered by
    // ---------------------------------------------------------------------------

    @Test
    fun aTimestampInSeconds_isOrderedByItsMilliseconds_truncated() {
        add("a", 1.9996)

        assertEquals(1_999L, timeline.rows.single().position.timestampMillis)
    }

    @Test
    fun aMessageWithNoTimestamp_isOrderedByTheLocalClock() {
        clockMillis = 4_000L
        add("three", 3.0)
        add("five", 5.0)

        add("noTimestamp", null)

        assertEquals(4_000L, row("noTimestamp").position.timestampMillis)
        assertEquals(listOf("three", "noTimestamp", "five"), order())
    }

    @Test
    fun aZeroTimestamp_isOrderedByTheLocalClock() = assertOrderedByClock(0.0)

    @Test
    fun aNegativeTimestamp_isOrderedByTheLocalClock() = assertOrderedByClock(-1.0)

    @Test
    fun aTimestampThatIsNotANumber_isOrderedByTheLocalClock() = assertOrderedByClock(Double.NaN)

    @Test
    fun anInfiniteTimestamp_isOrderedByTheLocalClock() {
        assertOrderedByClock(Double.POSITIVE_INFINITY)
        assertOrderedByClock(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun theClockIsRead_onlyForATimestampThatCannotBeUsed() {
        add("valid", 2.0)
        assertEquals(0, clockReads)

        add("missing", null)
        assertEquals(1, clockReads)
    }

    // ---------------------------------------------------------------------------
    // Removal and clear
    // ---------------------------------------------------------------------------

    @Test
    fun removeAt_returnsTheRow_andLeavesTheOthersInOrder() {
        add("a", 1.0)
        add("b", 2.0)
        add("c", 3.0)

        val removed = timeline.removeAt(1)

        assertEquals("b", (removed as SystemLineRow).text)
        assertEquals(listOf("a", "c"), order())
    }

    @Test
    fun clear_removesEveryRow_andStartsTheSequenceAgainAtZero() {
        add("a", 1.0)
        add("b", 2.0)

        timeline.clear()
        add("afterClear", 3.0)

        assertEquals(listOf("afterClear"), order())
        assertEquals(0L, timeline.rows.single().position.sequence)
    }

    // ---------------------------------------------------------------------------
    // Finding and replacing a row
    // ---------------------------------------------------------------------------

    @Test
    fun indexOfFirst_findsTheFirstMatchingRow_orMinusOne() {
        add("a", 1.0)
        add("b", 2.0)
        add("b", 3.0)

        assertEquals(1, timeline.indexOfFirst { row -> (row as SystemLineRow).text == "b" })
        assertEquals(-1, timeline.indexOfFirst { row -> (row as SystemLineRow).text == "z" })
    }

    @Test
    fun replaceAt_putsTheNewRowInTheSamePlace_andMovesNothing() {
        add("a", 1.0)
        add("b", 2.0)
        add("c", 3.0)
        val position = timeline.rows[1].position

        timeline.replaceAt(1, SystemLineRow(position, "b2"))

        assertEquals(listOf("a", "b2", "c"), order())
        assertEquals(position, timeline.rows[1].position)
    }

    @Test
    fun replaceAt_takesNoSequence() {
        add("a", 1.0)
        val position = timeline.rows[0].position
        timeline.replaceAt(0, SystemLineRow(position, "a2"))

        add("b", 2.0)

        assertEquals(1L, timeline.rows[1].position.sequence)
    }

    @Test
    fun indexOfFirst_returnsTheEarliestOfMatchesThatAreNotNeighbours() {
        add("x", 1.0)
        add("y", 2.0)
        add("x", 3.0)
        add("y", 4.0)

        assertEquals(0, timeline.indexOfFirst { row -> (row as SystemLineRow).text == "x" })
        assertEquals(1, timeline.indexOfFirst { row -> (row as SystemLineRow).text == "y" })
    }

    @Test
    fun aReplacingRowWithTheSameTimestampButAnotherSequence_isRefused() {
        add("a", 1.0)
        val position = timeline.rows[0].position

        assertThrows(IllegalArgumentException::class.java) {
            timeline.replaceAt(0, SystemLineRow(position.copy(sequence = position.sequence + 1), "a2"))
        }
    }

    @Test
    fun aReplacingRowWithTheSameSequenceButAnotherTimestamp_isRefused() {
        add("a", 1.0)
        val position = timeline.rows[0].position

        assertThrows(IllegalArgumentException::class.java) {
            timeline.replaceAt(0, SystemLineRow(position.copy(timestampMillis = position.timestampMillis + 1), "a2"))
        }
    }

    @Test
    fun aReplacingRowAtAnotherPosition_isRefused() {
        add("a", 1.0)
        add("b", 2.0)

        assertThrows(IllegalArgumentException::class.java) {
            timeline.replaceAt(0, SystemLineRow(timeline.rows[1].position, "moved"))
        }
        assertEquals(listOf("a", "b"), order())
    }

    @Test
    fun aRowThatDoesNotTakeThePositionItIsGiven_isRefused() {
        assertThrows(IllegalArgumentException::class.java) {
            timeline.insert(1.0, null) { _ ->
                SystemLineRow(ChatTimelinePosition(timestampMillis = 1_000L, sequence = 99L), "wrong")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun add(name: String, timestampSec: Double?, preserved: ChatTimelinePosition? = null): Int {
        return timeline.insert(timestampSec, preserved) { position -> SystemLineRow(position, name) }
    }

    private fun order(): List<String> = timeline.rows.map { row -> (row as SystemLineRow).text }

    private fun row(name: String): ChatTimelineRow = timeline.rows.single { row -> (row as SystemLineRow).text == name }

    private fun assertOrderedByClock(timestampSec: Double) {
        clockMillis += 1_000L
        add("unusable$timestampSec", timestampSec)

        assertEquals(
            "timestamp $timestampSec",
            clockMillis,
            row("unusable$timestampSec").position.timestampMillis
        )
    }
}
