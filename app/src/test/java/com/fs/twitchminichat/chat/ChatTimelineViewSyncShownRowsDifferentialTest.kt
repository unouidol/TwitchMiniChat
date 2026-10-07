package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * Replays long random sequences of every operation [ChatTimelineViewSync] has against a
 * transcription of it at 4850a83, which spoke to the layout in child indices directly, and
 * requires the same layout after every operation: the same views in the same order, the same
 * views hidden, the same row on each view, the same shown rows, the same answers, and the
 * same calls on the layout in the same order.
 *
 * The transcription writes out which rows are shown instead of calling
 * ChatTimelineVisibility, so a change to the derivation cannot move the oracle with it. Both
 * sides order their rows with a [ChatTimeline] of their own: the order is not what changed.
 */
class ChatTimelineViewSyncShownRowsDifferentialTest {

    @Test(timeout = 600_000L)
    fun everyOperation_leavesTheLayoutAsTheSyncAt4850a83Did() {
        var largest = 0
        var hiddenSteps = 0L
        var countChecks = 0L
        val coverage = OracleCoverage("4850a83", seeds = SEEDS, operations = OPERATIONS_PER_SEED)

        repeat(SEEDS) { seed ->
            val random = Random(seed)
            var clockMillis = 1_000_000L
            val oldLayout = RecordingLayout()
            val newLayout = RecordingLayout()
            val old = Sync4850a83(oldLayout, ChatTimeline { clockMillis })
            val sync = ChatTimelineViewSync(newLayout, ChatTimeline { clockMillis })
            val taken = ArrayList<ChatTimelinePosition>()
            var nextView = 0

            repeat(OPERATIONS_PER_SEED) { step ->
                val where = "seed $seed step $step"
                clockMillis += random.nextLong(0L, 2_000L)

                when (random.nextInt(100)) {
                    in 0..44 -> {
                        val view = "v${nextView++}"
                        val timestampSec = TIMESTAMPS[random.nextInt(TIMESTAMPS.size)]
                        val preserved = if (taken.isNotEmpty() && random.nextInt(4) == 0) taken.removeAt(random.nextInt(taken.size)) else null
                        val kind = random.nextInt(10)
                        val user = USERS[random.nextInt(USERS.size)]
                        val create = { position: ChatTimelinePosition -> row(kind, user, position) }
                        val expected = old.insert(view, timestampSec, preserved, create)
                        assertEquals("$where insert", expected, sync.insert(view, timestampSec, preserved, create))
                    }
                    in 45..52 -> {
                        val user = USERS[random.nextInt(USERS.size)].trim().lowercase()
                        assertEquals("$where hideUser", old.hideUser(user), sync.hideUser(user))
                    }
                    in 53..60 -> {
                        val users = USERS.filter { random.nextInt(3) == 0 }.map { user -> user.trim().lowercase() }.toSet()
                        assertEquals("$where setHiddenUsers", old.setHiddenUsers(users), sync.setHiddenUsers(users))
                    }
                    in 61..66 -> {
                        val view = pickView(random, oldLayout)
                        coverageOfViewRemoval(coverage, newLayout, view)
                        val expected = old.removeAndTakePosition(view)
                        assertEquals("$where removeAndTakePosition", expected, sync.removeAndTakePosition(view))
                        if (expected != null) taken += expected
                    }
                    in 67..70 -> {
                        val view = pickView(random, oldLayout)
                        coverageOfViewRemoval(coverage, newLayout, view)
                        old.remove(view)
                        sync.remove(view)
                    }
                    in 71..84 -> {
                        val localId = "local-${random.nextInt(0, nextView + 2)}"
                        val event = PendingEchoEvent.values()[random.nextInt(PendingEchoEvent.values().size)]
                        assertEquals("$where applyEchoEvent", old.applyEchoEvent(localId, event), sync.applyEchoEvent(localId, event))
                    }
                    in 85..96 -> {
                        val localId = "local-${random.nextInt(0, nextView + 2)}"
                        val echoView = newLayout.views.firstOrNull { view -> (newLayout.tags[view] as? PendingEchoRow)?.localId == localId }
                        if (echoView != null) coverage.removed(echo = true, hidden = echoView in newLayout.hidden) else coverage.missed()
                        val expected = old.removeEcho(localId)
                        assertEquals("$where removeEcho", expected, sync.removeEcho(localId))
                        if (expected != null) taken += expected
                    }
                    else -> if (seed % 3 == 0) {
                        coverage.cleared(oldLayout.views.size)
                        old.clear()
                        sync.clear()
                        taken.clear()
                    }
                }

                assertEquals("$where views", oldLayout.views, newLayout.views)
                assertEquals("$where hidden", oldLayout.hidden, newLayout.hidden)
                assertEquals("$where tags", oldLayout.tags, newLayout.tags)
                assertEquals("$where calls", oldLayout.calls, newLayout.calls)
                assertEquals("$where shownRows", old.shownRows, sync.shownRows)
                /* The count the sync keeps, against one made afresh from the transcription's rows. */
                assertEquals("$where hiddenRowCount", old.rowCount - old.shownRows.size, sync.hiddenRowCount)
                countChecks++
                coverage.step(timelineRows = oldLayout.views.size, hiddenViews = oldLayout.hidden.size)
                largest = maxOf(largest, oldLayout.views.size)
                if (oldLayout.hidden.isNotEmpty()) hiddenSteps++
            }
        }
        println("seeds=$SEEDS operations=$OPERATIONS_PER_SEED largestTimeline=$largest stepsWithHiddenViews=$hiddenSteps hiddenRowCountChecks=$countChecks")
        println(coverage)
    }

    /* A removal by view: of a row, hidden or not, when the view is in the layout; of nothing otherwise. */
    private fun coverageOfViewRemoval(coverage: OracleCoverage, layout: RecordingLayout, view: String) {
        val row = layout.tags[view]
        if (row == null) coverage.missed() else coverage.removed(echo = row is PendingEchoRow, hidden = view in layout.hidden)
    }

    /* A view in the layout, or one that never was, so the unknown-view path is replayed too. */
    private fun pickView(random: Random, layout: RecordingLayout): String {
        if (layout.views.isEmpty() || random.nextInt(8) == 0) return "stranger"
        return layout.views[random.nextInt(layout.views.size)]
    }

    private fun row(kind: Int, user: String, position: ChatTimelinePosition): ChatTimelineRow = when (kind) {
        in 0..5 -> ChatMessageRow(position, user, user.trim().lowercase(), null, "text", 1.0, null, null)
        6 -> SystemLineRow(position, user)
        /* Echo ids repeat across inserts, so an id can name an echo that is gone, or two at once. */
        else -> PendingEchoRow(position, "local-${position.sequence % 7}", "me", "text", null, null, 1.0, PendingEchoStatus.SENDING)
    }

    /** Stands in for the layout: the views in order, which are hidden, each view's row, and every call made on it. */
    private class RecordingLayout : ChatTimelineViews<String> {
        val views = mutableListOf<String>()
        val hidden = mutableSetOf<String>()
        val tags = HashMap<String, ChatTimelineRow>()
        val calls = mutableListOf<String>()

        override fun indexOf(view: String): Int = views.indexOf(view)

        override fun add(view: String, row: ChatTimelineRow, index: Int) {
            calls += "add $view at $index"
            views.add(index, view)
            tags[view] = row
        }

        override fun removeAt(index: Int) {
            calls += "removeAt $index"
            val view = views.removeAt(index)
            hidden -= view
            tags -= view
        }

        override fun removeAll() {
            calls += "removeAll"
            views.clear()
            hidden.clear()
            tags.clear()
        }

        override fun setShown(index: Int, shown: Boolean) {
            calls += "setShown $index $shown"
            if (shown) hidden -= views[index] else hidden += views[index]
        }

        override fun rebind(index: Int, row: ChatTimelineRow, change: ChatTimelineChange) {
            calls += "rebind $index $change ${(row as PendingEchoRow).status}"
            tags[views[index]] = row
        }
    }

    /**
     * ChatTimelineViewSync at 4850a83: every change to the timeline first, then the views at
     * the same index. Which rows are shown is written out here.
     */
    private class Sync4850a83<V>(private val views: ChatTimelineViews<V>, private val timeline: ChatTimeline) {
        private var hiddenUsers: Set<String> = emptySet()

        val shownRows: List<ChatTimelineRow>
            get() = timeline.rows.filter { row -> isShown(row, hiddenUsers) }

        /* Read only by the hidden-row count invariant; the transcription itself never needs it. */
        val rowCount: Int
            get() = timeline.rows.size

        fun insert(
            view: V,
            messageTimestampSec: Double?,
            preservedPosition: ChatTimelinePosition?,
            create: (ChatTimelinePosition) -> ChatTimelineRow
        ): ChatTimelineRow {
            val index = timeline.insert(messageTimestampSec, preservedPosition, create)
            val row = timeline.rows[index]
            views.add(view, row, index)
            if (!isShown(row, hiddenUsers)) views.setShown(index, false)
            return row
        }

        fun hideUser(usernameLower: String): Boolean = setHiddenUsers(hiddenUsers + usernameLower)

        fun setHiddenUsers(users: Set<String>): Boolean {
            val previous = hiddenUsers
            hiddenUsers = users.toSet()
            var changed = false
            timeline.rows.forEachIndexed { index, row ->
                val shown = isShown(row, hiddenUsers)
                if (shown != isShown(row, previous)) {
                    views.setShown(index, shown)
                    changed = true
                }
            }
            return changed
        }

        fun removeAndTakePosition(view: V): ChatTimelinePosition? {
            val index = views.indexOf(view)
            if (index < 0) return null
            val row = timeline.removeAt(index)
            views.removeAt(index)
            return row.position
        }

        fun remove(view: V) {
            removeAndTakePosition(view)
        }

        fun applyEchoEvent(localId: String, event: PendingEchoEvent): Boolean {
            val index = indexOfEcho(localId)
            if (index < 0) return false
            val echo = timeline.rows[index] as PendingEchoRow
            val status = echo.status.after(event)
            if (status == echo.status) return false
            val updated = echo.copy(status = status)
            timeline.replaceAt(index, updated)
            views.rebind(index, updated, ChatTimelineChange.ECHO_STATUS)
            return true
        }

        fun removeEcho(localId: String): ChatTimelinePosition? {
            val index = indexOfEcho(localId)
            if (index < 0) return null
            val row = timeline.removeAt(index)
            views.removeAt(index)
            return row.position
        }

        fun clear() {
            timeline.clear()
            views.removeAll()
        }

        private fun indexOfEcho(localId: String): Int =
            timeline.indexOfFirst { row -> row is PendingEchoRow && row.localId == localId }

        /* ChatTimelineVisibility at 4850a83, written out. */
        private fun isShown(row: ChatTimelineRow, hidden: Set<String>): Boolean =
            row !is ChatMessageRow || row.usernameLower !in hidden
    }

    private companion object {
        const val SEEDS = 300
        const val OPERATIONS_PER_SEED = 600
        val USERS = listOf("Alice", " bob ", "carol", "DAVE", "eve", "  ")
        val TIMESTAMPS: List<Double?> = listOf(1.0, 1.0, 2.0, 2.5, 3.0, 4.0, 5.0, null, 0.0, Double.NaN)
    }
}
