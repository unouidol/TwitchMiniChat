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
}

/**
 * Keeps a timeline's views in the order of its rows: every change goes to the
 * [timeline] first, and the [views] follow it at the index it gives. Main thread.
 *
 * Nothing else may add or remove a view, so the view at an index is always the view of
 * the row at that index, and a view is found again by its index alone.
 */
class ChatTimelineViewSync<V>(
    private val views: ChatTimelineViews<V>,
    private val timeline: ChatTimeline
) {

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
        return row
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

    /** Removes every view and row, and restarts the timeline's sequence. */
    fun clear() {
        timeline.clear()
        views.removeAll()
    }
}
