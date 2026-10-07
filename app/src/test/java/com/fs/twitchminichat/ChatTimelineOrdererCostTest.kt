package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins how many positions [ChatTimelineOrderer.insertionIndex] reads, counted by a list that
 * counts every read: one for a candidate not older than the last position, and at most one
 * more than the number of halvings of the list otherwise.
 *
 * The ordering tests cannot see this: a scan from the start finds the same index. The scan it
 * replaced read every position to append a message, and the timeline appends one per message.
 */
class ChatTimelineOrdererCostTest {

    @Test(timeout = 60_000L)
    fun appendingReadsOnlyTheLastPosition_whateverTheSize() {
        for (n in SIZES) {
            val existing = sorted(n)
            val newer = ChatTimelinePosition(timestampMillis = (n + 1) * 1_000L, sequence = n.toLong())
            assertReads("n $n newer", existing, newer, expectedIndex = n, maximumReads = 1)
            assertReads("n $n equal to last", existing, existing.last(), expectedIndex = n, maximumReads = 1)
        }
    }

    @Test(timeout = 60_000L)
    fun anyOtherPlace_readsAtMostOneMoreThanTheHalvingsOfTheList() {
        for (n in SIZES) {
            val existing = sorted(n)
            val limit = 1 + halvings(n)
            for (target in 0 until n) {
                /* Between the row before target and the row at target, so it lands at target. */
                val candidate = ChatTimelinePosition(timestampMillis = (target + 1) * 1_000L - 500L, sequence = n.toLong())
                assertReads("n $n target $target", existing, candidate, expectedIndex = target, maximumReads = limit)
            }
        }
    }

    @Test(timeout = 60_000L)
    fun anEmptyTimeline_readsNothing() {
        assertReads("empty", emptyList(), ChatTimelinePosition(1_000L, 0L), expectedIndex = 0, maximumReads = 0)
    }

    @Test(timeout = 60_000L)
    fun fourHundredMessagesAppendedToFourHundredRows_readFourHundredPositions() {
        val positions = sorted(400).toMutableList()
        var reads = 0L
        repeat(400) { j ->
            val counting = CountingList(positions)
            val candidate = ChatTimelinePosition(timestampMillis = (401 + j) * 1_000L, sequence = (400 + j).toLong())
            positions.add(ChatTimelineOrderer.insertionIndex(counting, candidate), candidate)
            reads += counting.reads
        }
        assertEquals(400L, reads)
    }

    private fun assertReads(
        where: String,
        existing: List<ChatTimelinePosition>,
        candidate: ChatTimelinePosition,
        expectedIndex: Int,
        maximumReads: Int
    ) {
        val counting = CountingList(existing)
        assertEquals(where, expectedIndex, ChatTimelineOrderer.insertionIndex(counting, candidate))
        assertTrue("$where read ${counting.reads}, more than $maximumReads", counting.reads <= maximumReads)
    }

    /** How many times n can be halved, rounding up, before one position is left: ceil(log2 n). */
    private fun halvings(n: Int): Int {
        var halvings = 0
        while ((1 shl halvings) < n) halvings++
        return halvings
    }

    private fun sorted(n: Int): List<ChatTimelinePosition> =
        List(n) { i -> ChatTimelinePosition(timestampMillis = (i + 1) * 1_000L, sequence = i.toLong()) }

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
        val SIZES = (1..64).toList() + listOf(100, 400, 1_000, 6_400)
    }
}
