package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Replays long random sequences of every operation against [ChatTimelineListSync] and against
 * [ChatTimelineViewSync], the sync the page used before 5d, and requires after every operation
 * that a plain list fed only the list sync's updates shows what the old sync's layout shows: the
 * same rows, in the same order, the hidden ones left out.
 *
 * The old sync is the reference because it is already checked, on every operation, against its
 * own transcription at 4850a83 (ChatTimelineViewSyncShownRowsDifferentialTest), so the chain ends
 * at frozen code. The same random choices drive both, with one clock, so both timelines take the
 * same rows at the same positions.
 *
 * Checked after every operation:
 * - **the list against the layout:** the list built from the update stream equals the rows of the
 *   layout's views that are not hidden, in order;
 * - **the stream against the sync itself:** the list equals [ChatTimelineListSync.shownRows], so
 *   applying every update, each at the position the updates before it left, reproduces the shown
 *   rows (a position outside the list fails at once, as it would in RecyclerView);
 * - **the answers:** the row inserted, whether a hide changed anything, whether an echo moved, and
 *   the position a removed echo leaves;
 * - **the counts:** [ChatTimelineListSync.hiddenRowCount] against the old sync's, and
 *   [ChatTimelineListSync.shownRowCount] against the list's size;
 * - **the changes:** each one carries ECHO_STATUS and lands on the echo it moved.
 */
class ChatTimelineListSyncDifferentialTest {

    @Test(timeout = 600_000L)
    fun everyOperation_leavesTheListShowingWhatTheOldSyncsLayoutShows() {
        var largest = 0
        var steps = 0L
        var stepsWithHiddenRows = 0L
        val updatesSeen = HashMap<String, Int>()

        repeat(SEEDS) { seed ->
            val random = Random(seed)
            var clockMillis = 1_000_000L
            val layout = ShowingLayout()
            val old = ChatTimelineViewSync(layout, ChatTimeline { clockMillis })
            val sync = ChatTimelineListSync(ChatTimeline { clockMillis })
            val list = ArrayList<ChatTimelineRow>()
            val taken = ArrayList<ChatTimelinePosition>()

            fun apply(where: String, updates: List<ShownRowsUpdate>) {
                for (update in updates) {
                    updatesSeen.merge(update.javaClass.simpleName, 1, Int::plus)
                    applyTo(list, update)
                    if (update is ShownRowsUpdate.Change) {
                        assertEquals("$where change payload", ChatTimelineChange.ECHO_STATUS, update.change)
                        assertTrue("$where change lands on an echo", list[update.position] is PendingEchoRow)
                    }
                }
            }

            repeat(OPERATIONS_PER_SEED) { step ->
                val where = "seed $seed step $step"
                clockMillis += random.nextLong(0L, 2_000L)

                when (random.nextInt(100)) {
                    in 0..44 -> {
                        val timestampSec = TIMESTAMPS[random.nextInt(TIMESTAMPS.size)]
                        val preserved = if (taken.isNotEmpty() && random.nextInt(4) == 0) taken.removeAt(random.nextInt(taken.size)) else null
                        val kind = random.nextInt(10)
                        val user = USERS[random.nextInt(USERS.size)]
                        val create = { position: ChatTimelinePosition -> row(kind, user, position) }
                        val expected = old.insert(timestampSec, preserved, create)
                        val inserted = sync.insert(timestampSec, preserved, create)
                        assertEquals("$where insert", expected, inserted.row)
                        apply(where, listOfNotNull(inserted.update))
                    }
                    in 45..52 -> {
                        val user = USERS[random.nextInt(USERS.size)].trim().lowercase()
                        val changed = old.hideUser(user)
                        val updates = sync.hideUser(user)
                        assertEquals("$where hideUser", changed, updates.isNotEmpty())
                        apply(where, updates)
                    }
                    in 53..60 -> {
                        val users = USERS.filter { random.nextInt(3) == 0 }.map { user -> user.trim().lowercase() }.toSet()
                        val changed = old.setHiddenUsers(users)
                        val updates = sync.setHiddenUsers(users)
                        assertEquals("$where setHiddenUsers", changed, updates.isNotEmpty())
                        apply(where, updates)
                    }
                    in 61..74 -> {
                        val localId = "local-${random.nextInt(0, 9)}"
                        val event = PendingEchoEvent.values()[random.nextInt(PendingEchoEvent.values().size)]
                        val moved = old.applyEchoEvent(localId, event)
                        val change = sync.applyEchoEvent(localId, event)
                        assertEquals("$where applyEchoEvent", moved, change != null)
                        apply(where, listOfNotNull(change))
                        if (change != null) {
                            assertEquals("$where moved echo", localId, (list[change.position] as PendingEchoRow).localId)
                        }
                    }
                    in 75..96 -> {
                        /* Echo ids are local-0 to local-6; 7 and 8 name no echo. */
                        val localId = "local-${random.nextInt(0, 9)}"
                        val expected = old.removeEcho(localId)
                        val removed = sync.removeEcho(localId)
                        assertEquals("$where removeEcho", expected, removed?.position)
                        apply(where, listOfNotNull(removed?.update))
                        if (expected != null) taken += expected
                    }
                    else -> if (seed % 3 == 0) {
                        old.clear()
                        apply(where, listOf(sync.clear()))
                        taken.clear()
                    }
                }

                assertEquals("$where list against the layout", layout.shownRows(), list)
                assertEquals("$where list against shownRows", sync.shownRows, list)
                assertEquals("$where hiddenRowCount", old.hiddenRowCount, sync.hiddenRowCount)
                assertEquals("$where shownRowCount", list.size, sync.shownRowCount)
                steps++
                largest = maxOf(largest, layout.views.size)
                if (layout.hidden.isNotEmpty()) stepsWithHiddenRows++
            }
        }
        println("seeds=$SEEDS operations=$OPERATIONS_PER_SEED steps=$steps largestTimeline=$largest stepsWithHiddenRows=$stepsWithHiddenRows updates=$updatesSeen")
        /* A comparison that never saw a kind of update proves nothing about it. */
        for (kind in listOf("Insert", "Remove", "Change", "InsertRange", "RemoveRange", "Reset")) {
            assertTrue("no $kind update was ever applied: $updatesSeen", (updatesSeen[kind] ?: 0) > 0)
        }
        assertTrue("no step had a hidden row", stepsWithHiddenRows > 0)
    }

    /* What a list holding only the shown rows does with [update]: ChatTimelineAdapter.apply, without RecyclerView. */
    private fun applyTo(list: MutableList<ChatTimelineRow>, update: ShownRowsUpdate) {
        when (update) {
            is ShownRowsUpdate.Insert -> list.add(update.position, update.row)
            is ShownRowsUpdate.Remove -> list.removeAt(update.position)
            is ShownRowsUpdate.Change -> list[update.position] = update.row
            is ShownRowsUpdate.InsertRange -> list.addAll(update.position, update.rows)
            is ShownRowsUpdate.RemoveRange -> {
                check(update.position + update.count <= list.size) { "range $update past the end of ${list.size} rows" }
                list.subList(update.position, update.position + update.count).clear()
            }
            is ShownRowsUpdate.Reset -> {
                list.clear()
                list.addAll(update.rows)
            }
        }
    }

    private fun row(kind: Int, user: String, position: ChatTimelinePosition): ChatTimelineRow = when (kind) {
        in 0..5 -> ChatMessageRow(position, user, user.trim().lowercase(), null, "text", 1.0, null, null)
        6 -> SystemLineRow(position, user)
        /* Echo ids repeat across inserts, so an id can name an echo that is gone, or two at once. */
        else -> PendingEchoRow(position, "local-${position.sequence % 7}", "me", "text", null, null, 1.0, PendingEchoStatus.SENDING)
    }

    /** The old sync's layout, reduced to what it shows: its views in order, which are hidden, each view's row. */
    private class ShowingLayout : ChatTimelineViews<Int> {
        val views = mutableListOf<Int>()
        val hidden = mutableSetOf<Int>()
        private val rows = HashMap<Int, ChatTimelineRow>()
        private var nextView = 0

        fun shownRows(): List<ChatTimelineRow> = views.filter { view -> view !in hidden }.map { view -> rows.getValue(view) }

        override fun create(row: ChatTimelineRow): Int = nextView++

        override fun add(view: Int, row: ChatTimelineRow, index: Int) {
            views.add(index, view)
            rows[view] = row
        }

        override fun removeAt(index: Int) {
            val view = views.removeAt(index)
            hidden -= view
            rows -= view
        }

        override fun removeAll() {
            views.clear()
            hidden.clear()
            rows.clear()
        }

        override fun setShown(index: Int, shown: Boolean) {
            if (shown) hidden -= views[index] else hidden += views[index]
        }

        override fun rebind(index: Int, row: ChatTimelineRow, change: ChatTimelineChange) {
            rows[views[index]] = row
        }
    }

    private companion object {
        const val SEEDS = 200
        const val OPERATIONS_PER_SEED = 500
        val USERS = listOf("Alice", " bob ", "carol", "DAVE", "eve", "  ")
        val TIMESTAMPS: List<Double?> = listOf(1.0, 1.0, 2.0, 2.5, 3.0, 4.0, 5.0, null, 0.0, Double.NaN)
    }
}
