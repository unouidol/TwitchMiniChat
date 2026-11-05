package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
 *
 * The sync shares its deciders, the functions of [ChatTimelineShownRows], with
 * [ChatTimelineListSync], so this test pins the list sync's decisions only where a fault in a
 * decider changes the sync's layout. For two decisions it does not, because the sync absorbs the
 * fault: a null from `replaced`, and an InsertRange that starts too early in `hiddenUsersChanged`.
 * Mutating either left the walk passing. The last two cases check those decisions against the
 * transcription's layout directly, with the sync taking no part, so nothing can absorb them.
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
                        /*
                         * The view's name is the driver's own counter, given to both sides: straight to
                         * the frozen copy, and to today's sync through the layout it creates views with.
                         * Neither side's view comes from the other.
                         */
                        val expected = old.insert(view, timestampSec, preserved, create)
                        newLayout.nextView = view
                        assertEquals("$where insert", expected, sync.insert(timestampSec, preserved, create))
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

    /**
     * The first hole, pinned: the decision on an echo's change of status. With `replaced` mutated to
     * return null, the walk above still passed. On null the sync rebinds the echo's view at its
     * timeline index, which is the call the decision would have led to. So after every event that
     * moves an echo, `replaced` is checked against the transcription's layout: it must be a Change of
     * that echo, at the number of views before the echo's view that the layout does not hide.
     */
    @Test(timeout = 600_000L)
    fun anEchosChangeOfStatus_isAChange_atTheEchosPositionAmongTheViewsTheLayoutShows() {
        var changes = 0
        var changesPastTheFirstPosition = 0

        repeat(SEEDS) { seed ->
            val walk = TranscriptionWalk(seed)
            repeat(OPERATIONS_PER_SEED) { step ->
                val applied = walk.step()
                if (applied is Applied.EchoMoved) {
                    val layout = walk.layout
                    val rows = walk.rows()
                    val shownBefore = layout.views.take(applied.index).count { view -> view !in layout.hidden }
                    val expected = if (layout.views[applied.index] in layout.hidden) {
                        null
                    } else {
                        ShownRowsUpdate.Change(shownBefore, rows[applied.index], ChatTimelineChange.ECHO_STATUS)
                    }
                    assertEquals(
                        "seed $seed step $step replaced",
                        expected,
                        ChatTimelineShownRows.replaced(rows, walk.hiddenUsers, layout.hidden.size, applied.index, ChatTimelineChange.ECHO_STATUS)
                    )
                    changes++
                    if (shownBefore > 0) changesPastTheFirstPosition++
                }
            }
        }
        println("echoChanges=$changes pastTheFirstPosition=$changesPastTheFirstPosition")
        /* A check that never ran, or never away from position 0, proves nothing about the position. */
        assertTrue("no echo's status ever moved", changes > 0)
        assertTrue("no echo that moved had a shown row before it", changesPastTheFirstPosition > 0)
    }

    /**
     * The second hole, pinned: where a change of hidden users puts the rows it shows again. With
     * `hiddenUsersChanged` mutated to start an InsertRange one position early, the walk above still
     * passed. The sync finds each row of a range by a forward search from the shown row before the
     * range, and an early start only makes that search start sooner. So after every change of hidden
     * users, the ranges are checked against the transcription's layout. Applied in order to a list of
     * the rows the layout showed before the change, they must leave exactly the rows it shows after
     * the change, and none may fall outside the list.
     */
    @Test(timeout = 600_000L)
    fun aChangeOfHiddenUsers_isTheRangesThatTakeWhatTheLayoutShowedToWhatItShows() {
        var changes = 0
        var insertRangesPastTheFirstPosition = 0
        var removeRanges = 0

        repeat(SEEDS) { seed ->
            val walk = TranscriptionWalk(seed)
            repeat(OPERATIONS_PER_SEED) { step ->
                val applied = walk.step()
                if (applied is Applied.HiddenUsersChanged) {
                    val where = "seed $seed step $step"
                    val updates = ChatTimelineShownRows.hiddenUsersChanged(applied.rows, applied.previous, applied.next)
                    assertEquals("$where changed", applied.changed, updates.isNotEmpty())

                    val list = ArrayList(applied.shownBefore)
                    for (update in updates) {
                        when (update) {
                            is ShownRowsUpdate.InsertRange -> {
                                assertTrue("$where $update outside ${list.size} rows", update.position in 0..list.size)
                                list.addAll(update.position, update.rows)
                                if (update.position > 0) insertRangesPastTheFirstPosition++
                            }
                            is ShownRowsUpdate.RemoveRange -> {
                                assertTrue(
                                    "$where $update outside ${list.size} rows",
                                    update.position >= 0 && update.position + update.count <= list.size
                                )
                                list.subList(update.position, update.position + update.count).clear()
                                removeRanges++
                            }
                            else -> throw AssertionError("$where a change of hidden users only removes and inserts ranges: $update")
                        }
                    }
                    assertEquals("$where ranges", walk.shownRows(), list)
                    changes++
                }
            }
        }
        println("hiddenUserChanges=$changes insertRangesPastTheFirstPosition=$insertRangesPastTheFirstPosition removeRanges=$removeRanges")
        /* A comparison that never saw a kind of range, or never one away from position 0, proves nothing about it. */
        assertTrue("no change of hidden users", changes > 0)
        assertTrue("no InsertRange away from the first position", insertRangesPastTheFirstPosition > 0)
        assertTrue("no RemoveRange", removeRanges > 0)
    }

    /** What one step of [TranscriptionWalk] did, for the cases that check a decision against it. */
    private sealed interface Applied {

        /** An echo's status moved. [index] is its row's index, which the move leaves as it was. */
        data class EchoMoved(val index: Int) : Applied

        /**
         * The hidden users went from [previous] to [next]. [rows] are the transcription's rows,
         * which a change of hidden users leaves as they are, [shownBefore] the rows its layout
         * showed before the change, and [changed] the transcription's answer.
         */
        data class HiddenUsersChanged(
            val rows: List<ChatTimelineRow>,
            val shownBefore: List<ChatTimelineRow>,
            val previous: Set<String>,
            val next: Set<String>,
            val changed: Boolean
        ) : Applied

        /** Anything else. */
        object Other : Applied
    }

    /**
     * A random walk of the same operations as the first case, in the same proportions, driving the
     * transcription alone. Today's sync takes no part, so a decision checked against this walk's
     * layout is checked against frozen code with nothing in between. The rows are read from the
     * layout, which holds one view per row in timeline order. The hidden users are the ones the walk
     * gave the transcription, kept here so that the transcription need not expose its own.
     */
    private inner class TranscriptionWalk(private val seed: Int) {
        private val random = Random(seed)
        private var clockMillis = 1_000_000L
        val layout = RecordingLayout()
        private val old = Sync4850a83(layout) { clockMillis }
        private val taken = ArrayList<ChatTimelinePosition>()
        private var nextView = 0

        var hiddenUsers: Set<String> = emptySet()
            private set

        fun rows(): List<ChatTimelineRow> = layout.views.map { view -> layout.tags.getValue(view) }

        fun shownRows(): List<ChatTimelineRow> =
            layout.views.filter { view -> view !in layout.hidden }.map { view -> layout.tags.getValue(view) }

        fun step(): Applied {
            clockMillis += random.nextLong(0L, 2_000L)
            return when (random.nextInt(100)) {
                in 0..44 -> {
                    val view = "v${nextView++}"
                    val timestampSec = TIMESTAMPS[random.nextInt(TIMESTAMPS.size)]
                    val preserved = if (taken.isNotEmpty() && random.nextInt(4) == 0) taken.removeAt(random.nextInt(taken.size)) else null
                    val kind = random.nextInt(10)
                    val user = USERS[random.nextInt(USERS.size)]
                    old.insert(view, timestampSec, preserved) { position -> row(kind, user, position) }
                    Applied.Other
                }
                in 45..52 -> {
                    val user = USERS[random.nextInt(USERS.size)].trim().lowercase()
                    changeHiddenUsers(hiddenUsers + user) { old.hideUser(user) }
                }
                in 53..60 -> {
                    val users = USERS.filter { random.nextInt(3) == 0 }.map { user -> user.trim().lowercase() }.toSet()
                    changeHiddenUsers(users) { old.setHiddenUsers(users) }
                }
                in 61..74 -> {
                    /* Echo ids are local-0 to local-6; 7 and 8 name no echo. */
                    val localId = "local-${random.nextInt(0, 9)}"
                    val event = PendingEchoEvent.values()[random.nextInt(PendingEchoEvent.values().size)]
                    val index = rows().indexOfFirst { row -> row is PendingEchoRow && row.localId == localId }
                    if (old.applyEchoEvent(localId, event)) Applied.EchoMoved(index) else Applied.Other
                }
                in 75..96 -> {
                    val localId = "local-${random.nextInt(0, 9)}"
                    old.removeEcho(localId)?.let { position -> taken += position }
                    Applied.Other
                }
                else -> {
                    if (seed % 3 == 0) {
                        old.clear()
                        taken.clear()
                    }
                    Applied.Other
                }
            }
        }

        private fun changeHiddenUsers(next: Set<String>, change: () -> Boolean): Applied {
            val rows = rows()
            val shownBefore = shownRows()
            val previous = hiddenUsers
            val changed = change()
            hiddenUsers = next
            return Applied.HiddenUsersChanged(rows, shownBefore, previous, next, changed)
        }
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

        /*
         * The name the next view takes, set by the driver before each insert into today's sync and
         * taken by create. The 4850a83 copy never creates a view: it is given one, so it never reads this.
         */
        var nextView: String? = null

        override fun create(row: ChatTimelineRow): String {
            val view = checkNotNull(nextView) { "a view was created that the driver did not name: one create per insert" }
            nextView = null
            return view
        }

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
     * is PendingEchoStatus.after as it was there. Not exercised: where a row goes against one whose
     * position equals its own, the one case where an upper bound and a lower bound differ, so a
     * pass here says nothing about it. No candidate here can equal the position of a row still
     * present: the counter gives each sequence once until clear, which also empties the test's
     * taken positions, a preserved position lends the new row its sequence and never its
     * timestamp, and the test spends each preserved position once, after the echo that held it
     * was removed. Equal timestamps do occur; their sequences always differ.
     * ChatTimelineInsertDifferentialTest makes equal positions on purpose, by copying the
     * positions of rows still present.
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
