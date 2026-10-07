package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins how many rows the translation reads to insert one row, counted by a list that counts
 * every read, calling [ChatTimelineShownRows] as ChatTimelineViewSync.insert does: `inserted`,
 * then `timelineIndexOf` of the position it gives, with the count of hidden rows the sync keeps.
 *
 * An appended row costs at most three reads, whatever the size and however many rows are
 * hidden; with no row hidden - no user hidden, or a user hidden who has no row in the
 * timeline, as a blocked user never has - nothing is counted at any place. The ordering tests
 * cannot see this: counting every row gives the same answers.
 */
class ChatTimelineShownRowsCostTest {

    @Test(timeout = 60_000L)
    fun appendingReadsAtMostThreeRows_whateverTheSize_andHowManyRowsAreHidden() {
        for (n in SIZES) for ((name, every) in HIDDEN_SHARES) {
            val rows = rowsAfterInsert(n, every, at = n)
            assertReadsAtMost("n $n $name append", rows, setOf(SPAMMER), at = n, maximum = 3)
        }
    }

    @Test(timeout = 60_000L)
    fun appendingAfterRowsThatAreAllHidden_stillReadsAtMostThreeRows() {
        for (n in SIZES) {
            val rows = rowsAfterInsert(n, every = 1, at = n)
            assertReadsAtMost("n $n all hidden append", rows, setOf(SPAMMER), at = n, maximum = 3)
        }
    }

    @Test(timeout = 60_000L)
    fun aUserHiddenWithNoRowInTheTimeline_costsNoCounting_anywhere() {
        for (n in SIZES) {
            for (at in listOf(0, n / 2, n)) {
                val rows = rowsAfterInsert(n, every = null, at = at)
                assertReadsAtMost("n $n blocked user at $at", rows, setOf("blocked"), at = at, maximum = 2)
            }
        }
    }

    /*
     * Another row must be shown. When the inserted row is the only one shown, its position is
     * as near the end as the start, the search runs from the end, and it passes every hidden
     * row: the case a count of hidden rows cannot avoid, which these tests do not claim.
     */
    @Test(timeout = 60_000L)
    fun insertingAtTheFront_readsAtMostThreeRows_whenAnotherRowIsShown() {
        for (n in SIZES.filter { size -> size >= 2 }) for ((name, every) in HIDDEN_SHARES) {
            val rows = rowsAfterInsert(n, every, at = 0)
            assertReadsAtMost("n $n $name front", rows, setOf(SPAMMER), at = 0, maximum = 3)
        }
    }

    @Test(timeout = 60_000L)
    fun fourHundredAppendsAfterFourHundredRowsATenthHidden_readAtMostTwelveHundredRows() {
        val rows = ArrayList<ChatTimelineRow>()
        for (i in 0 until 400) rows += message(i, if (i % 10 == 0) SPAMMER else "alice")
        var reads = 0L
        repeat(400) { j ->
            rows += message(400 + j, "alice")
            reads += readsToInsert(rows, setOf(SPAMMER), rows.size - 1)
        }
        assertTrue("read $reads", reads <= 1_200L)
    }

    /* EXACTLY what ChatTimelineViewSync.insert calls, over a list that counts reads. */
    private fun readsToInsert(rows: List<ChatTimelineRow>, hidden: Set<String>, at: Int): Long {
        val hiddenRowCount = rows.count { row -> !ChatTimelineVisibility.isShown(row, hidden) }
        val counting = CountingList(rows)
        val update = ChatTimelineShownRows.inserted(counting, hidden, hiddenRowCount, at) ?: error("the inserted row is shown")
        assertEquals(at, ChatTimelineShownRows.timelineIndexOf(counting, hidden, hiddenRowCount, update.position))
        return counting.reads
    }

    private fun assertReadsAtMost(where: String, rows: List<ChatTimelineRow>, hidden: Set<String>, at: Int, maximum: Int) {
        val reads = readsToInsert(rows, hidden, at)
        assertTrue("$where read $reads, more than $maximum", reads <= maximum)
    }

    /** n rows from alice, every [every]th from [SPAMMER] (none when null), and a row from alice inserted at [at]. */
    private fun rowsAfterInsert(n: Int, every: Int?, at: Int): List<ChatTimelineRow> {
        val rows = ArrayList<ChatTimelineRow>(n + 1)
        for (i in 0 until n) rows += message(i, if (every != null && i % every == 0) SPAMMER else "alice")
        rows.add(at, message(n, "alice"))
        return rows
    }

    private fun message(i: Int, user: String) =
        ChatMessageRow(ChatTimelinePosition(i * 1_000L, i.toLong()), user, user, null, "text", i.toDouble(), null, null)

    /** A list that counts every element read, by index or by iteration. */
    private class CountingList<T>(private val inner: List<T>) : List<T> by inner {
        var reads = 0L

        override fun get(index: Int): T {
            reads++
            return inner[index]
        }

        override fun iterator(): Iterator<T> = listIterator(0)

        override fun listIterator(): ListIterator<T> = listIterator(0)

        override fun listIterator(index: Int): ListIterator<T> {
            val iterator = inner.listIterator(index)
            return object : ListIterator<T> by iterator {
                override fun next(): T {
                    reads++
                    return iterator.next()
                }

                override fun previous(): T {
                    reads++
                    return iterator.previous()
                }
            }
        }
    }

    private companion object {
        const val SPAMMER = "spammer"
        val SIZES = (1..64).toList() + listOf(100, 400, 1_000, 6_400)

        /*
         * No row hidden, a tenth, and half. Every row hidden is covered for appending by its own
         * test; inserting at the front then has to pass every hidden row, which a count cannot
         * avoid, and is not claimed here.
         */
        val HIDDEN_SHARES = listOf("none" to null, "a tenth" to 10, "half" to 2)
    }
}
