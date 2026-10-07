package com.fs.twitchminichat.chat

/**
 * One change to the list of shown rows - the rows [ChatTimelineVisibility] shows, in timeline
 * order - addressed by position in that list.
 *
 * These are the operations a list holding only the shown rows needs. In a sequence, each
 * position is taken in the list as the updates before it have left it.
 */
sealed interface ShownRowsUpdate {

    /** [row] is now shown at [position]. */
    data class Insert(val position: Int, val row: ChatTimelineRow) : ShownRowsUpdate

    /** The row at [position] is no longer shown. */
    data class Remove(val position: Int) : ShownRowsUpdate

    /** The row at [position] was replaced in place by [row]; [change] names what changed. */
    data class Change(val position: Int, val row: ChatTimelineRow, val change: ChatTimelineChange) : ShownRowsUpdate

    /** [rows] are now shown, in order, from [position] on. */
    data class InsertRange(val position: Int, val rows: List<ChatTimelineRow>) : ShownRowsUpdate

    /** The [count] rows from [position] on are no longer shown. */
    data class RemoveRange(val position: Int, val count: Int) : ShownRowsUpdate

    /** The shown rows are now [rows], whatever they were before. */
    data class Reset(val rows: List<ChatTimelineRow>) : ShownRowsUpdate
}

/**
 * Translates between a row's index in the timeline and its position among the shown rows,
 * and says what each change of the timeline does to the list of shown rows. Pure.
 *
 * Which rows are shown is [ChatTimelineVisibility] over the rows and the hidden users. With
 * no user hidden every row is shown, a row's position is its index, and nothing is counted;
 * otherwise a translation counts the shown rows before the one it is asked about.
 */
object ChatTimelineShownRows {

    /** The position among the shown rows of the row at [timelineIndex], or null when it is not shown. */
    fun positionOf(rows: List<ChatTimelineRow>, hiddenUsers: Set<String>, timelineIndex: Int): Int? {
        if (!ChatTimelineVisibility.isShown(rows[timelineIndex], hiddenUsers)) return null
        if (hiddenUsers.isEmpty()) return timelineIndex
        return positionOf(timelineIndex) { index -> ChatTimelineVisibility.isShown(rows[index], hiddenUsers) }
    }

    /** The timeline index of the row at [position] among the shown rows. */
    fun timelineIndexOf(rows: List<ChatTimelineRow>, hiddenUsers: Set<String>, position: Int): Int {
        if (hiddenUsers.isEmpty()) {
            if (position !in rows.indices) throw IndexOutOfBoundsException("position $position of ${rows.size} shown rows")
            return position
        }
        return timelineIndexOf(rows.size, position) { index -> ChatTimelineVisibility.isShown(rows[index], hiddenUsers) }
    }

    /** How many rows before [timelineIndex] are shown, as [isShownAt] says of each index. */
    fun positionOf(timelineIndex: Int, isShownAt: (Int) -> Boolean): Int {
        var position = 0
        for (index in 0 until timelineIndex) {
            if (isShownAt(index)) position++
        }
        return position
    }

    /** The index, among [size] rows, of the one at [position] among those [isShownAt] says are shown. */
    fun timelineIndexOf(size: Int, position: Int, isShownAt: (Int) -> Boolean): Int {
        var seen = 0
        for (index in 0 until size) {
            if (!isShownAt(index)) continue
            if (seen == position) return index
            seen++
        }
        throw IndexOutOfBoundsException("position $position of $seen shown rows")
    }

    /** What inserting the row now at [timelineIndex] of [rows] did to the shown rows; null when it is not shown. */
    fun inserted(rows: List<ChatTimelineRow>, hiddenUsers: Set<String>, timelineIndex: Int): ShownRowsUpdate.Insert? {
        val position = positionOf(rows, hiddenUsers, timelineIndex) ?: return null
        return ShownRowsUpdate.Insert(position, rows[timelineIndex])
    }

    /** What removing the row at [timelineIndex] of [rows], before it goes, does to the shown rows; null when it is not shown. */
    fun removed(rows: List<ChatTimelineRow>, hiddenUsers: Set<String>, timelineIndex: Int): ShownRowsUpdate.Remove? {
        val position = positionOf(rows, hiddenUsers, timelineIndex) ?: return null
        return ShownRowsUpdate.Remove(position)
    }

    /**
     * What replacing a row in place with the row now at [timelineIndex] of [rows] did to the
     * shown rows; null when it is not shown. The replacing row must be shown exactly when the
     * replaced one was, as an echo always is.
     */
    fun replaced(
        rows: List<ChatTimelineRow>,
        hiddenUsers: Set<String>,
        timelineIndex: Int,
        change: ChatTimelineChange
    ): ShownRowsUpdate.Change? {
        val position = positionOf(rows, hiddenUsers, timelineIndex) ?: return null
        return ShownRowsUpdate.Change(position, rows[timelineIndex], change)
    }

    /**
     * What changing the hidden users from [previous] to [next] does to the shown rows of
     * [rows]: a range removed for each run of rows that stop being shown, and a range inserted
     * for each run that start, in timeline order. Rows that are shown neither before nor after
     * are in no list, and do not break a run. Empty when nothing changes.
     */
    fun hiddenUsersChanged(rows: List<ChatTimelineRow>, previous: Set<String>, next: Set<String>): List<ShownRowsUpdate> {
        val updates = ArrayList<ShownRowsUpdate>()
        /* The position the next row takes or leaves, in the list as the updates so far leave it. */
        var position = 0
        var removedFrom = 0
        var removedCount = 0
        var insertedFrom = 0
        val inserted = ArrayList<ChatTimelineRow>()

        fun endRemoval() {
            if (removedCount == 0) return
            updates += ShownRowsUpdate.RemoveRange(removedFrom, removedCount)
            removedCount = 0
        }

        fun endInsertion() {
            if (inserted.isEmpty()) return
            updates += ShownRowsUpdate.InsertRange(insertedFrom, inserted.toList())
            inserted.clear()
        }

        for (row in rows) {
            val wasShown = ChatTimelineVisibility.isShown(row, previous)
            val isShown = ChatTimelineVisibility.isShown(row, next)
            when {
                wasShown && isShown -> {
                    endRemoval()
                    endInsertion()
                    position++
                }
                wasShown -> {
                    endInsertion()
                    if (removedCount == 0) removedFrom = position
                    removedCount++
                }
                isShown -> {
                    endRemoval()
                    if (inserted.isEmpty()) insertedFrom = position
                    inserted += row
                    position++
                }
            }
        }
        endRemoval()
        endInsertion()
        return updates
    }

    /** What clearing the timeline does to the shown rows. */
    fun cleared(): ShownRowsUpdate.Reset = ShownRowsUpdate.Reset(emptyList())
}
