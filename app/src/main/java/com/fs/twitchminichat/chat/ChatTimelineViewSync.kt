package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition

/**
 * The views of the timeline, held in the same order as the rows of a [ChatTimeline].
 * Implemented over the layout that shows them. Main thread.
 */
interface ChatTimelineViews<V> {

    /** Puts [view], which shows [row], at [index]. */
    fun add(view: V, row: ChatTimelineRow, index: Int)

    /** Removes the view at [index]. */
    fun removeAt(index: Int)

    /** Removes every view. */
    fun removeAll()

    /**
     * Shows or hides the view at [index], leaving it where it is.
     *
     * While every row's view stays in the layout, this is how the layout presents
     * [ChatTimelineVisibility]; a list fed with only the shown rows would not need it.
     */
    fun setShown(index: Int, shown: Boolean)

    /**
     * Updates the view at [index], which showed the row [row] replaced, to show [row].
     *
     * Only what [change] names is applied, to the view that is already there. The view is
     * not rebuilt, so nothing it loaded - an echo's emotes - is loaded again.
     */
    fun rebind(index: Int, row: ChatTimelineRow, change: ChatTimelineChange)
}

/** A row's view that can show a changed row in place, without being rebuilt. */
interface RebindableTimelineView {

    /** Shows [row], applying only what [change] names. */
    fun rebind(row: ChatTimelineRow, change: ChatTimelineChange)
}

/** What changed in a row that is rebound in place: the payload of [ChatTimelineViews.rebind]. */
enum class ChatTimelineChange {
    /** A pending echo's status, and with it its status line and opacity. */
    ECHO_STATUS
}

/**
 * Keeps a timeline's views in the order of its rows: every change goes to the
 * [timeline] first, and the [views] follow it at the index it gives. Main thread.
 *
 * Nothing else may add or remove a view, so the view at an index is always the view of
 * the row at that index, and a view is found again by its index alone.
 *
 * Which views are shown follows [ChatTimelineVisibility] over the timeline and the
 * hidden users given to it: a view is hidden or shown again in place, never removed
 * and rebuilt, and its row never leaves the timeline. Nothing else may change whether
 * a view is shown, so each view always shows what the derivation says of its row.
 *
 * Each change is decided as a [ShownRowsUpdate] - an update to the list of shown rows,
 * addressed by position in that list, as a list holding only those rows needs it - and the
 * views apply it where they keep each row's view: at the timeline index that
 * [ChatTimelineShownRows] translates the position back to. A row that is not shown is in no
 * such list and has no update; its view, which these views keep hidden in its place, is
 * placed and removed at its timeline index.
 */
class ChatTimelineViewSync<V>(
    private val views: ChatTimelineViews<V>,
    private val timeline: ChatTimeline
) {

    /* Trimmed and lowercased; kept across clear, like the hidden users themselves. */
    private var hiddenUsers: Set<String> = emptySet()

    /**
     * How many rows of the timeline are not shown, kept as rows come and go and as users are
     * hidden and shown again, so that no translation has to count them: one near either end
     * of the timeline reads a row or two. Internal only so the tests can check it, after
     * every operation, against a count made afresh; nothing else reads it.
     */
    internal var hiddenRowCount = 0
        private set

    /** The rows that are shown, in timeline order. */
    val shownRows: List<ChatTimelineRow>
        get() = ChatTimelineVisibility.shownRows(timeline.rows, hiddenUsers)

    /** Inserts [view] with the row [create] builds, where the timeline places it, and returns the row. */
    fun insert(
        view: V,
        messageTimestampSec: Double?,
        preservedPosition: ChatTimelinePosition?,
        create: (ChatTimelinePosition) -> ChatTimelineRow
    ): ChatTimelineRow {
        val index = timeline.insert(messageTimestampSec, preservedPosition, create)
        val row = timeline.rows[index]

        val update = ChatTimelineShownRows.inserted(timeline.rows, hiddenUsers, hiddenRowCount, index)
        if (update != null) {
            views.add(view, update.row, timelineIndexOf(update.position))
        } else {
            hiddenRowCount++
            views.add(view, row, index)
            views.setShown(index, false)
        }
        return row
    }

    /**
     * Hides the rows of [usernameLower], a trimmed and lowercased name, on top of the
     * users already hidden. Returns whether any view changed.
     */
    fun hideUser(usernameLower: String): Boolean {
        return setHiddenUsers(hiddenUsers + usernameLower)
    }

    /**
     * Makes [users], trimmed and lowercased, the hidden users, and shows or hides each view
     * whose row that changes. Returns whether any view changed.
     */
    fun setHiddenUsers(users: Set<String>): Boolean {
        val previous = hiddenUsers
        hiddenUsers = users.toSet()

        val rows = timeline.rows
        val updates = ChatTimelineShownRows.hiddenUsersChanged(rows, previous, hiddenUsers)

        /* Which rows are shown as the updates apply, one by one, from what was shown before. */
        val shown = BooleanArray(rows.size) { index -> ChatTimelineVisibility.isShown(rows[index], previous) }
        for (update in updates) {
            when (update) {
                is ShownRowsUpdate.RemoveRange -> repeat(update.count) {
                    /* Each removal leaves the next row of the range at the same position. */
                    val index = ChatTimelineShownRows.timelineIndexOf(rows.size, update.position) { i -> shown[i] }
                    shown[index] = false
                    hiddenRowCount++
                    views.setShown(index, false)
                }
                is ShownRowsUpdate.InsertRange -> {
                    /* The rows come in timeline order, after the shown row before the range. */
                    var from = if (update.position == 0) {
                        0
                    } else {
                        ChatTimelineShownRows.timelineIndexOf(rows.size, update.position - 1) { i -> shown[i] } + 1
                    }
                    for (row in update.rows) {
                        val index = indexOfRow(row, from)
                        shown[index] = true
                        hiddenRowCount--
                        views.setShown(index, true)
                        from = index + 1
                    }
                }
                else -> error("a change of hidden users only removes and inserts ranges: $update")
            }
        }
        return updates.isNotEmpty()
    }

    /**
     * Moves the echo of [localId] by [event] and rebinds its view when its status changes.
     * Returns whether it did: false when no echo of [localId] is in the timeline, or the
     * event leaves its status as it was.
     */
    fun applyEchoEvent(localId: String, event: PendingEchoEvent): Boolean {
        val index = indexOfEcho(localId)
        if (index < 0) return false

        val echo = timeline.rows[index] as PendingEchoRow
        val status = echo.status.after(event)
        if (status == echo.status) return false

        val updated = echo.copy(status = status)
        timeline.replaceAt(index, updated)

        val update = ChatTimelineShownRows.replaced(timeline.rows, hiddenUsers, hiddenRowCount, index, ChatTimelineChange.ECHO_STATUS)
        if (update != null) {
            views.rebind(timelineIndexOf(update.position), update.row, update.change)
        } else {
            views.rebind(index, updated, ChatTimelineChange.ECHO_STATUS)
        }
        return true
    }

    /**
     * Removes the echo of [localId] and its view, and returns its position for the
     * canonical message that replaces it; null when no echo of [localId] is in the timeline.
     */
    fun removeEcho(localId: String): ChatTimelinePosition? {
        val index = indexOfEcho(localId)
        if (index < 0) return null

        return removeRowAt(index).position
    }

    /** Removes every view and row, and restarts the timeline's sequence. The hidden users stay hidden. */
    fun clear() {
        timeline.clear()
        hiddenRowCount = 0
        /* The views can be reset only to no rows: they hold no view for a row they were not given. */
        val reset = ChatTimelineShownRows.cleared()
        check(reset.rows.isEmpty()) { "the views cannot be reset to rows they hold no view for" }
        views.removeAll()
    }

    /* Removes the row at [index] and its view. The update is decided before the row goes. */
    private fun removeRowAt(index: Int): ChatTimelineRow {
        val update = ChatTimelineShownRows.removed(timeline.rows, hiddenUsers, hiddenRowCount, index)
        val viewIndex = if (update != null) timelineIndexOf(update.position) else index
        val row = timeline.removeAt(index)
        if (update == null) hiddenRowCount--
        views.removeAt(viewIndex)
        return row
    }

    /* Where the views keep the row at [position] among the shown rows: its timeline index. */
    private fun timelineIndexOf(position: Int): Int {
        return ChatTimelineShownRows.timelineIndexOf(timeline.rows, hiddenUsers, hiddenRowCount, position)
    }

    /* The index of [row] itself, not of a row equal to it, from [from] on. */
    private fun indexOfRow(row: ChatTimelineRow, from: Int): Int {
        val rows = timeline.rows
        for (index in from until rows.size) {
            if (rows[index] === row) return index
        }
        error("a row the hidden users show is not in the timeline: $row")
    }

    private fun indexOfEcho(localId: String): Int {
        return timeline.indexOfFirst { row -> row is PendingEchoRow && row.localId == localId }
    }
}
