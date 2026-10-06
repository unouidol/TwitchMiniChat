package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition

/**
 * The views of the timeline, held in the same order as the rows of a [ChatTimeline].
 * Implemented over the layout that shows them. Main thread.
 */
interface ChatTimelineViews<V> {

    /** The index of [view], or -1 when it is not in the timeline. */
    fun indexOf(view: V): Int

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
 */
class ChatTimelineViewSync<V>(
    private val views: ChatTimelineViews<V>,
    private val timeline: ChatTimeline
) {

    /* Trimmed and lowercased; kept across clear, like the hidden users themselves. */
    private var hiddenUsers: Set<String> = emptySet()

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
        views.add(view, row, index)
        if (!ChatTimelineVisibility.isShown(row, hiddenUsers)) {
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

        var changed = false
        timeline.rows.forEachIndexed { index, row ->
            val shown = ChatTimelineVisibility.isShown(row, hiddenUsers)
            if (shown != ChatTimelineVisibility.isShown(row, previous)) {
                views.setShown(index, shown)
                changed = true
            }
        }
        return changed
    }

    /** Removes [view] and its row and returns the row's position, or null when [view] is not in the timeline. */
    fun removeAndTakePosition(view: V): ChatTimelinePosition? {
        val index = views.indexOf(view)
        if (index < 0) return null

        val row = timeline.removeAt(index)
        views.removeAt(index)
        return row.position
    }

    /** Removes [view] and its row, when it is in the timeline. */
    fun remove(view: V) {
        removeAndTakePosition(view)
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
        views.rebind(index, updated, ChatTimelineChange.ECHO_STATUS)
        return true
    }

    /**
     * Removes the echo of [localId] and its view, and returns its position for the
     * canonical message that replaces it; null when no echo of [localId] is in the timeline.
     */
    fun removeEcho(localId: String): ChatTimelinePosition? {
        val index = indexOfEcho(localId)
        if (index < 0) return null

        val row = timeline.removeAt(index)
        views.removeAt(index)
        return row.position
    }

    private fun indexOfEcho(localId: String): Int {
        return timeline.indexOfFirst { row -> row is PendingEchoRow && row.localId == localId }
    }

    /** Removes every view and row, and restarts the timeline's sequence. The hidden users stay hidden. */
    fun clear() {
        timeline.clear()
        views.removeAll()
    }
}
