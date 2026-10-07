package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * The property a list of shown rows relies on: applying, in order, the updates
 * [ChatTimelineShownRows] gives for a change of the timeline to a plain list holding the shown
 * rows from before yields exactly the shown rows after, element by element.
 *
 * Random timelines of messages from a few users, system lines and echoes go through every
 * change the sync makes - insert, removal, replacement in place, a change of hidden users and
 * clear - with hidden users drawn from the same few names, so runs of hidden and shown rows
 * interleave.
 */
class ChatTimelineShownRowsPropertyTest {

    @Test(timeout = 300_000L)
    fun applyingTheUpdatesToAPlainList_yieldsTheShownRows_afterEveryChange() {
        var updatesApplied = 0L
        var largest = 0

        repeat(SEEDS) { seed ->
            val random = Random(seed)
            val rows = ArrayList<ChatTimelineRow>()
            var hidden: Set<String> = emptySet()
            var nextSequence = 0L

            repeat(CHANGES_PER_SEED) { step ->
                val where = "seed $seed step $step"
                val list = ChatTimelineVisibility.shownRows(rows, hidden).toMutableList()
                val updates: List<ShownRowsUpdate>

                when (random.nextInt(100)) {
                    in 0..49 -> {
                        val index = random.nextInt(rows.size + 1)
                        rows.add(index, randomRow(random, ChatTimelinePosition(random.nextLong(0L, 50L), nextSequence++)))
                        updates = listOfNotNull(ChatTimelineShownRows.inserted(rows, hidden, hiddenRowCount(rows, hidden), index))
                    }
                    in 50..64 -> {
                        if (rows.isEmpty()) return@repeat
                        val index = random.nextInt(rows.size)
                        updates = listOfNotNull(ChatTimelineShownRows.removed(rows, hidden, hiddenRowCount(rows, hidden), index))
                        rows.removeAt(index)
                    }
                    in 65..74 -> {
                        val echoes = rows.indices.filter { index -> rows[index] is PendingEchoRow }
                        if (echoes.isEmpty()) return@repeat
                        val index = echoes[random.nextInt(echoes.size)]
                        val echo = rows[index] as PendingEchoRow
                        rows[index] = echo.copy(status = PendingEchoStatus.values()[random.nextInt(PendingEchoStatus.values().size)])
                        updates = listOfNotNull(
                            ChatTimelineShownRows.replaced(rows, hidden, hiddenRowCount(rows, hidden), index, ChatTimelineChange.ECHO_STATUS)
                        )
                    }
                    in 75..97 -> {
                        val next = USERS.filter { random.nextInt(3) == 0 }.map { user -> user.trim().lowercase() }.toSet()
                        updates = ChatTimelineShownRows.hiddenUsersChanged(rows, hidden, next)
                        hidden = next
                    }
                    else -> {
                        rows.clear()
                        updates = listOf(ChatTimelineShownRows.cleared())
                    }
                }

                updates.forEach { update -> apply(update, list) }
                updatesApplied += updates.size
                largest = maxOf(largest, rows.size)
                assertEquals(where, ChatTimelineVisibility.shownRows(rows, hidden), list)
            }
        }
        println("seeds=$SEEDS changes=$CHANGES_PER_SEED updatesApplied=$updatesApplied largestTimeline=$largest")
    }

    @Test
    fun aChangeOfHiddenUsers_neverEmitsAnEmptyRange_andMergesRunsSeparatedOnlyByRowsShownNeitherBeforeNorAfter() {
        repeat(2_000) { seed ->
            val random = Random(seed)
            val rows = List(random.nextInt(0, 40)) { i -> randomRow(random, ChatTimelinePosition(i.toLong(), i.toLong())) }
            val previous = USERS.filter { random.nextBoolean() }.map { user -> user.trim().lowercase() }.toSet()
            val next = USERS.filter { random.nextBoolean() }.map { user -> user.trim().lowercase() }.toSet()

            val updates = ChatTimelineShownRows.hiddenUsersChanged(rows, previous, next)

            updates.forEach { update ->
                when (update) {
                    is ShownRowsUpdate.RemoveRange -> check(update.count > 0) { "seed $seed: $update" }
                    is ShownRowsUpdate.InsertRange -> check(update.rows.isNotEmpty()) { "seed $seed: $update" }
                    else -> error("seed $seed: a change of hidden users emitted $update")
                }
            }
            /* Two removals, or two insertions, in a row would be one run: they are always merged. */
            updates.zipWithNext().forEach { (first, second) ->
                check(first::class != second::class || positionsApart(first, second)) { "seed $seed: $first then $second" }
            }
        }
    }

    /* A removal leaves the next removal at the same position, an insertion moves the next past its rows. */
    private fun positionsApart(first: ShownRowsUpdate, second: ShownRowsUpdate): Boolean = when (first) {
        is ShownRowsUpdate.RemoveRange -> (second as ShownRowsUpdate.RemoveRange).position != first.position
        is ShownRowsUpdate.InsertRange -> (second as ShownRowsUpdate.InsertRange).position != first.position + first.rows.size
        else -> true
    }

    /** What a list holding the shown rows does with each update. */
    private fun apply(update: ShownRowsUpdate, list: MutableList<ChatTimelineRow>) {
        when (update) {
            is ShownRowsUpdate.Insert -> list.add(update.position, update.row)
            is ShownRowsUpdate.Remove -> list.removeAt(update.position)
            is ShownRowsUpdate.Change -> list[update.position] = update.row
            is ShownRowsUpdate.InsertRange -> list.addAll(update.position, update.rows)
            is ShownRowsUpdate.RemoveRange -> repeat(update.count) { list.removeAt(update.position) }
            is ShownRowsUpdate.Reset -> {
                list.clear()
                list.addAll(update.rows)
            }
        }
    }

    private fun randomRow(random: Random, position: ChatTimelinePosition): ChatTimelineRow {
        return when (random.nextInt(10)) {
            in 0..6 -> {
                val user = USERS[random.nextInt(USERS.size)]
                ChatMessageRow(position, user, user.trim().lowercase(), null, "m${position.sequence}", 1.0, null, null)
            }
            7 -> SystemLineRow(position, "s${position.sequence}")
            else -> PendingEchoRow(position, "local-${position.sequence}", "me", "e", null, null, 1.0, PendingEchoStatus.SENDING)
        }
    }

    private companion object {
        const val SEEDS = 500
        const val CHANGES_PER_SEED = 400
        val USERS = listOf("Alice", " bob ", "carol", "DAVE", "eve")
    }

    /* How many of [rows] are not shown, counted afresh here: the count the sync keeps. */
    private fun hiddenRowCount(rows: List<ChatTimelineRow>, hidden: Set<String>): Int =
        rows.count { row -> !ChatTimelineVisibility.isShown(row, hidden) }
}
