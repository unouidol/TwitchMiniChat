package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Pins [ChatTimelineShownRows]'s mapping by value, both ways, on timelines at and past 400 rows.
 *
 * A page's timeline starts with up to 400 history rows, the history source's cap, and the app sets
 * no cap of its own: live messages take it past 400, and it keeps growing until a clear. The other
 * tests that check positions by value stop short of that, at 240 rows at most
 * (ChatTimelineViewSyncShownRowsDifferentialTest's walk). ChatTimelineShownRowsCostTest runs past
 * it, at 400 to 6,400 rows, but checks only how many rows an insert reads and that the inserted
 * row's position maps back, which a fault matched in both directions passes. Here every row's
 * position, and every position's row, is compared with a value worked out without the mapping.
 */
class ChatTimelineShownRowsPastTheCapTest {

    /**
     * The reference case of [ChatTimelineShownRowsTest], at the sizes a long session reaches, with
     * system lines added: random timelines of ordinary rows from four users, echoes and system
     * lines, some users hidden, and every answer compared with the shown rows' indices written out.
     */
    @Test
    fun atAndPastTheHistoryCap_everyRowsPosition_andEveryPositionsRow_areTheShownRowsWrittenOut() {
        val users = listOf("alice", "bob", "carol", "dave")
        for (size in SIZES) {
            repeat(SEEDS) { seed ->
                val random = Random(size * 31 + seed)
                val timeline = List(size) { i ->
                    when (random.nextInt(30)) {
                        0 -> SystemLineRow(position(i), "s$i")
                        1, 2 -> echo(i, "e$i")
                        else -> message(i, "m$i", users[random.nextInt(users.size)])
                    }
                }
                /* At least one user hidden, and at least one shown. */
                val drawn = users.filter { random.nextBoolean() }.toSet()
                val hidden = when (drawn.size) {
                    0 -> setOf("bob")
                    users.size -> drawn - "alice"
                    else -> drawn
                }
                val label = "size $size seed $seed hidden $hidden"
                val count = hiddenRowCount(timeline, hidden)
                /* Written out here: the shown rows' indices, in order, and each one's position. */
                val shownIndices = timeline.indices.filter { index -> ChatTimelineVisibility.isShown(timeline[index], hidden) }
                val positionByIndex = shownIndices.withIndex().associate { (position, index) -> index to position }

                assertTrue("$label: a system line", timeline.any { it is SystemLineRow })
                assertTrue("$label: an echo", timeline.any { it is PendingEchoRow })
                assertTrue("$label: a hidden row", count > 0)
                assertTrue("$label: an ordinary row shown", shownIndices.any { timeline[it] is ChatMessageRow })

                timeline.indices.forEach { index ->
                    assertEquals("$label index $index", positionByIndex[index], ChatTimelineShownRows.positionOf(timeline, hidden, count, index))
                }
                shownIndices.forEachIndexed { position, index ->
                    assertEquals("$label position $position", index, ChatTimelineShownRows.timelineIndexOf(timeline, hidden, count, position))
                }
            }
        }
    }

    /**
     * One timeline of 801 rows whose answers are counted by hand, not by [ChatTimelineVisibility].
     * bob writes every row at an index 4k + 1 and is hidden; a system line stands at every index
     * ending in 50, an echo at every one ending in 75, and alice writes the rest. Before index i, bob
     * wrote (i + 2) / 4 rows, and the shown rows come three in every four indices: 4k, 4k + 2, 4k + 3.
     */
    @Test
    fun aTimelineCountedByHand_pastTheCap_mapsEveryRow_andEveryPosition() {
        val timeline = List(801) { i ->
            when {
                i % 100 == 50 -> SystemLineRow(position(i), "s$i")
                i % 100 == 75 -> echo(i, "e$i")
                i % 4 == 1 -> message(i, "b$i", "bob")
                else -> message(i, "a$i", "alice")
            }
        }
        val hidden = setOf("bob")
        val count = hiddenRowCount(timeline, hidden)
        assertEquals("bob's rows, 1, 5, ..., 797", 200, count)

        timeline.indices.forEach { i ->
            val position = ChatTimelineShownRows.positionOf(timeline, hidden, count, i)
            if (i % 4 == 1) assertNull("index $i", position) else assertEquals("index $i", i - (i + 2) / 4, position)
        }
        (0 until timeline.size - count).forEach { p ->
            assertEquals("position $p", 4 * (p / 3) + intArrayOf(0, 2, 3)[p % 3], ChatTimelineShownRows.timelineIndexOf(timeline, hidden, count, p))
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun position(i: Int) = ChatTimelinePosition(timestampMillis = i * 1_000L, sequence = i.toLong())

    private fun message(i: Int, text: String, user: String) =
        ChatMessageRow(position(i), user, user, null, text, i.toDouble(), null, null)

    private fun echo(i: Int, localId: String) =
        PendingEchoRow(position(i), localId, "me", "text", null, null, i.toDouble(), PendingEchoStatus.SENDING)

    /* How many of [rows] are not shown, counted afresh here: the count the sync keeps. */
    private fun hiddenRowCount(rows: List<ChatTimelineRow>, hidden: Set<String>): Int =
        rows.count { row -> !ChatTimelineVisibility.isShown(row, hidden) }

    private companion object {
        /* The history source's cap, one past it, two pages' worth, one past that, and four pages'. */
        val SIZES = listOf(400, 401, 800, 801, 1_600)
        const val SEEDS = 8
    }
}
