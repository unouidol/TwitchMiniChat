package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

/**
 * Drives [ChatTimelineViewSync] over a list standing in for the layout, and checks that
 * the views always follow the rows: same order, each view handed the row it shows.
 *
 * The last test replays random operations against a transcription of
 * ChatTimelineController as it was at fc60273, and requires the same view order and the
 * same returned positions after every one of them.
 */
class ChatTimelineViewSyncTest {

    private var clockMillis = 1_000_000L
    private val views = ListViews()
    private val timeline = ChatTimeline(currentTimeMillis = { clockMillis })
    private val sync = ChatTimelineViewSync(views, timeline)

    @Test
    fun aViewGoesWhereTheTimelinePutsItsRow() {
        insert("live1", 10.0)
        insert("live2", 11.0)
        insert("history", 5.0)

        assertEquals(listOf("history", "live1", "live2"), views.shown)
        assertEquals(timeline.rows, views.rowsShown)
    }

    @Test
    fun eachViewIsHandedTheRowItShows_andInsertReturnsThatRow() {
        val row = insert("only", 7.0)

        assertEquals(listOf(row), views.rowsShown)
        assertEquals(timeline.rows.single(), row)
    }

    @Test
    fun removeAndTakePosition_removesTheViewAndItsRow_andReturnsTheRowsPosition() {
        insert("a", 1.0)
        insert("b", 2.0)
        insert("c", 3.0)

        val position = sync.removeAndTakePosition("b")

        assertEquals(ChatTimelinePosition(timestampMillis = 2_000L, sequence = 1L), position)
        assertEquals(listOf("a", "c"), views.shown)
        assertEquals(timeline.rows, views.rowsShown)
    }

    @Test
    fun removeAndTakePosition_ofAViewNotInTheTimeline_returnsNull_andChangesNothing() {
        insert("a", 1.0)

        assertNull(sync.removeAndTakePosition("stranger"))
        assertEquals(listOf("a"), views.shown)
        assertEquals(1, timeline.rows.size)
    }

    @Test
    fun remove_removesTheViewAndItsRow() {
        insert("a", 1.0)
        insert("b", 2.0)

        sync.remove("a")

        assertEquals(listOf("b"), views.shown)
        assertEquals(timeline.rows, views.rowsShown)
    }

    @Test
    fun clear_removesEveryViewAndRow_andTheNextRowStartsTheSequenceAgain() {
        insert("a", 1.0)
        insert("b", 2.0)

        sync.clear()
        val next = insert("afterClear", 3.0)

        assertEquals(listOf("afterClear"), views.shown)
        assertEquals(0L, next.position.sequence)
    }

    @Test
    fun anEchoReplacedByItsCanonicalMessage_keepsItsPlaceBeforeABotReplyOfTheSameSecond() {
        sync.insert("echo", 10.0, null) { position ->
            PendingEchoRow(position, "local-echo", "me", "echo", null, null, 10.0, PendingEchoStatus.SENDING)
        }
        insert("botReply", 10.0)

        val taken = sync.removeEcho("local-echo")
        insert("canonical", 10.0, preserved = taken)

        assertEquals(listOf("canonical", "botReply"), views.shown)
    }

    @Test
    fun randomOperations_produceTheSameTimeline_asTheControllerAtFc60273() {
        val timestamps = listOf(null, 0.0, -2.0, Double.NaN, 1.0, 1.0, 2.0, 2.5, 3.0, 3.0, 4.0)

        for (seed in 1..200) {
            val random = Random(seed)
            val old = Fc60273Controller(currentTimeMillis = { clockMillis })
            val views = ListViews()
            val sync = ChatTimelineViewSync(views, ChatTimeline(currentTimeMillis = { clockMillis }))
            val taken = mutableListOf<ChatTimelinePosition>()
            var nextName = 0

            repeat(300) { step ->
                clockMillis += random.nextLong(0L, 3_000L)
                val where = "seed $seed, step $step"

                when (random.nextInt(10)) {
                    in 0..4 -> {
                        val name = "v${nextName++}"
                        val timestampSec = timestamps[random.nextInt(timestamps.size)]
                        val preserved = if (taken.isNotEmpty() && random.nextInt(3) == 0) {
                            taken.removeAt(random.nextInt(taken.size))
                        } else {
                            null
                        }
                        val expected = old.insert(name, timestampSec, preserved)
                        val actual = sync.insert(name, timestampSec, preserved) { position ->
                            SystemLineRow(position, name)
                        }
                        assertEquals(where, expected, actual.position)
                    }
                    in 5..6 -> if (old.container.isNotEmpty()) {
                        val name = old.container[random.nextInt(old.container.size)]
                        val expected = old.removeAndTakePosition(name)
                        assertEquals(where, expected, sync.removeAndTakePosition(name))
                        if (expected != null) taken += expected
                    }
                    7 -> if (old.container.isNotEmpty()) {
                        val name = old.container[random.nextInt(old.container.size)]
                        old.remove(name)
                        sync.remove(name)
                    }
                    8 -> assertEquals(where, old.removeAndTakePosition("stranger"), sync.removeAndTakePosition("stranger"))
                    else -> if (random.nextInt(10) == 0) {
                        old.clear()
                        sync.clear()
                        taken.clear()
                    }
                }

                assertEquals(where, old.container, views.shown)
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun insert(name: String, timestampSec: Double?, preserved: ChatTimelinePosition? = null): ChatTimelineRow {
        return sync.insert(name, timestampSec, preserved) { position -> SystemLineRow(position, name) }
    }

    /** Stands in for the layout: the views, and the row each was handed, in order. */
    private class ListViews : ChatTimelineViews<String> {
        val shown = mutableListOf<String>()
        val rowsShown = mutableListOf<ChatTimelineRow>()

        override fun indexOf(view: String): Int = shown.indexOf(view)

        override fun add(view: String, row: ChatTimelineRow, index: Int) {
            shown.add(index, view)
            rowsShown.add(index, row)
        }

        override fun removeAt(index: Int) {
            shown.removeAt(index)
            rowsShown.removeAt(index)
        }

        override fun removeAll() {
            shown.clear()
            rowsShown.clear()
        }

        /* No user is hidden in these tests; ChatTimelineHiddenUsersTest covers this. */
        override fun setShown(index: Int, shown: Boolean) {
            error("no view should be shown or hidden here: index $index, shown $shown")
        }

        /* No echo changes status in these tests; ChatTimelineEchoTest covers this. */
        override fun rebind(index: Int, row: ChatTimelineRow, change: ChatTimelineChange) {
            error("no view should be rebound here: index $index, change $change")
        }
    }

    /**
     * ChatTimelineController at fc60273, with its ViewGroup replaced by a list.
     *
     * The comparison is written out here instead of calling ChatTimelinePosition and
     * ChatTimelineOrderer, so that a change to either cannot change this oracle with it.
     */
    private class Fc60273Controller(private val currentTimeMillis: () -> Long) {
        val container = mutableListOf<String>()
        private val positionsByView = HashMap<String, ChatTimelinePosition>()
        private var nextSequence = 0L

        fun insert(view: String, messageTimestampSec: Double?, preservedPosition: ChatTimelinePosition?): ChatTimelinePosition {
            val position = ChatTimelinePosition(
                timestampMillis = resolveTimestampMillis(messageTimestampSec),
                sequence = preservedPosition?.sequence ?: nextSequence++
            )
            val existingPositions = container.mapIndexed { index, child ->
                positionsByView[child] ?: ChatTimelinePosition(timestampMillis = Long.MIN_VALUE, sequence = index.toLong())
            }
            val found = existingPositions.indexOfFirst { existing ->
                position.timestampMillis < existing.timestampMillis ||
                    (position.timestampMillis == existing.timestampMillis && position.sequence < existing.sequence)
            }
            positionsByView[view] = position
            container.add(if (found >= 0) found else container.size, view)
            return position
        }

        fun removeAndTakePosition(view: String): ChatTimelinePosition? {
            val position = positionsByView.remove(view)
            container.remove(view)
            return position
        }

        fun remove(view: String) {
            positionsByView.remove(view)
            container.remove(view)
        }

        fun clear() {
            positionsByView.clear()
            nextSequence = 0L
            container.clear()
        }

        private fun resolveTimestampMillis(messageTimestampSec: Double?): Long {
            return messageTimestampSec
                ?.takeIf { timestamp -> timestamp.isFinite() && timestamp > 0.0 }
                ?.times(1000.0)
                ?.toLong()
                ?: currentTimeMillis()
        }
    }
}
