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
 * ChatTimelineVisibility, the timeline as it was at 4850a83 instead of calling [ChatTimeline],
 * and an echo's next status instead of calling PendingEchoStatus.after, so a change to any of
 * them cannot move the oracle with it.
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
            val old = Sync4850a83(oldLayout) { clockMillis }
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
                    in 61..74 -> {
                        val localId = "local-${random.nextInt(0, nextView + 2)}"
                        val event = PendingEchoEvent.values()[random.nextInt(PendingEchoEvent.values().size)]
                        assertEquals("$where applyEchoEvent", old.applyEchoEvent(localId, event), sync.applyEchoEvent(localId, event))
                    }
                    in 75..96 -> {
                        /* The page's one removal. Echo ids are local-0 to local-6; 7 and 8 name no echo. */
                        val localId = "local-${random.nextInt(0, 9)}"
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
     * Frozen at 4850a83: which rows are shown, where each change puts its view, the order of
     * the rows and an echo's next status. Read from 4850a83, not from today's code: the order is
     * ChatTimeline's scan for the first newer position, not #75's binary search, and the status
     * is PendingEchoStatus.after as it was there.
     *
     * ChatTimelineViewSync at 4850a83: every change to the timeline first, then the views at
     * the same index. Which rows are shown, the timeline it changed and the echo's status
     * transitions are written out here.
     *
     * Self-contained: nothing here calls ChatTimeline, ChatTimelineOrderer, ChatTimelinePosition's
     * comparison, ChatTimelineVisibility, PendingEchoStatus.after or the sync, so a later change to
     * any of them cannot change this oracle with it. The rows are the test's own, built by the
     * create function it is given; the layout is the test's RecordingLayout.
     */
    private class Sync4850a83<V>(private val views: ChatTimelineViews<V>, private val currentTimeMillis: () -> Long) {
        private var hiddenUsers: Set<String> = emptySet()

        /* ChatTimeline at 4850a83: its rows, oldest first, and the sequence the next row gets. */
        private val rows = ArrayList<ChatTimelineRow>()
        private var nextSequence = 0L

        val shownRows: List<ChatTimelineRow>
            get() = rows.filter { row -> isShown(row, hiddenUsers) }

        /* Read only by the hidden-row count invariant; the transcription itself never needs it. */
        val rowCount: Int
            get() = rows.size

        fun insert(
            view: V,
            messageTimestampSec: Double?,
            preservedPosition: ChatTimelinePosition?,
            create: (ChatTimelinePosition) -> ChatTimelineRow
        ): ChatTimelineRow {
            val index = timelineInsert(messageTimestampSec, preservedPosition, create)
            val row = rows[index]
            views.add(view, row, index)
            if (!isShown(row, hiddenUsers)) views.setShown(index, false)
            return row
        }

        fun hideUser(usernameLower: String): Boolean = setHiddenUsers(hiddenUsers + usernameLower)

        fun setHiddenUsers(users: Set<String>): Boolean {
            val previous = hiddenUsers
            hiddenUsers = users.toSet()
            var changed = false
            rows.forEachIndexed { index, row ->
                val shown = isShown(row, hiddenUsers)
                if (shown != isShown(row, previous)) {
                    views.setShown(index, shown)
                    changed = true
                }
            }
            return changed
        }

        fun applyEchoEvent(localId: String, event: PendingEchoEvent): Boolean {
            val index = indexOfEcho(localId)
            if (index < 0) return false
            val echo = rows[index] as PendingEchoRow
            val status = statusAfter(echo.status, event)
            if (status == echo.status) return false
            val updated = echo.copy(status = status)
            timelineReplaceAt(index, updated)
            views.rebind(index, updated, ChatTimelineChange.ECHO_STATUS)
            return true
        }

        fun removeEcho(localId: String): ChatTimelinePosition? {
            val index = indexOfEcho(localId)
            if (index < 0) return null
            val row = rows.removeAt(index)
            views.removeAt(index)
            return row.position
        }

        fun clear() {
            rows.clear()
            nextSequence = 0L
            views.removeAll()
        }

        private fun indexOfEcho(localId: String): Int =
            rows.indexOfFirst { row -> row is PendingEchoRow && row.localId == localId }

        /* ChatTimelineVisibility at 4850a83, written out. */
        private fun isShown(row: ChatTimelineRow, hidden: Set<String>): Boolean =
            row !is ChatMessageRow || row.usernameLower !in hidden

        /*
         * ChatTimeline.insert at 4850a83: the timestamp, or the clock when it cannot be used; a
         * new sequence, or the preserved one; then the scan from the first row for the first
         * newer position, compared field by field, timestamp first, then sequence.
         */
        private fun timelineInsert(
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
            val row = create(position)
            require(samePosition(row.position, position)) { "A timeline row must take the position it is given." }
            val found = rows.indexOfFirst { existing ->
                position.timestampMillis < existing.position.timestampMillis ||
                    (position.timestampMillis == existing.position.timestampMillis && position.sequence < existing.position.sequence)
            }
            val index = if (found >= 0) found else rows.size
            rows.add(index, row)
            return index
        }

        /* ChatTimeline.replaceAt at 4850a83. */
        private fun timelineReplaceAt(index: Int, row: ChatTimelineRow) {
            require(samePosition(row.position, rows[index].position)) { "A replacing row must keep the position of the row it replaces." }
            rows[index] = row
        }

        private fun samePosition(a: ChatTimelinePosition, b: ChatTimelinePosition): Boolean =
            a.timestampMillis == b.timestampMillis && a.sequence == b.sequence

        /* PendingEchoStatus.after at 4850a83, written out. */
        private fun statusAfter(status: PendingEchoStatus, event: PendingEchoEvent): PendingEchoStatus {
            val awaiting = status == PendingEchoStatus.SENDING || status == PendingEchoStatus.UNCONFIRMED
            return when (event) {
                PendingEchoEvent.TIMEOUT -> if (status == PendingEchoStatus.SENDING) PendingEchoStatus.UNCONFIRMED else status
                PendingEchoEvent.USERSTATE -> if (awaiting) PendingEchoStatus.CONFIRMED else status
                PendingEchoEvent.NOTICE -> if (awaiting) PendingEchoStatus.REJECTED else status
            }
        }
    }

    private companion object {
        const val SEEDS = 300
        const val OPERATIONS_PER_SEED = 600
        val USERS = listOf("Alice", " bob ", "carol", "DAVE", "eve", "  ")
        val TIMESTAMPS: List<Double?> = listOf(1.0, 1.0, 2.0, 2.5, 3.0, 4.0, 5.0, null, 0.0, Double.NaN)
    }
}
