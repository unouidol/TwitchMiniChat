package com.fs.twitchminichat.chat

/*
 * The way back, from a position among the shown rows to a row's index in the timeline.
 *
 * Until ChatTimelineViewSync moved into the unit tests, these were members of
 * ChatTimelineShownRows, and that sync was their only caller outside the tests. It keeps a view
 * for every row and places each at its timeline index, so it needs them; the list sync, which feeds
 * a list holding only the shown rows, never does. So they moved with it. They are extensions so that
 * every call still reads ChatTimelineShownRows.timelineIndexOf, as it did when they were members,
 * and the tests that pin them, ChatTimelineShownRowsTest and ChatTimelineShownRowsCostTest, did not
 * change.
 */

/**
 * The timeline index of the row at [position] among the shown rows. [hiddenRowCount] must
 * be how many of [rows] are not shown.
 */
fun ChatTimelineShownRows.timelineIndexOf(rows: List<ChatTimelineRow>, hiddenUsers: Set<String>, hiddenRowCount: Int, position: Int): Int {
    val shownCount = rows.size - hiddenRowCount
    if (position !in 0 until shownCount) throw IndexOutOfBoundsException("position $position of $shownCount shown rows")
    if (hiddenRowCount == 0) return position

    val isShownAt = { index: Int -> ChatTimelineVisibility.isShown(rows[index], hiddenUsers) }
    val fromEnd = shownCount - 1 - position
    if (position < fromEnd) return timelineIndexOf(rows.size, position, isShownAt)

    /*
     * Nearer the end, or as near: the shown row that many shown rows before the last. The
     * last shown position, where an appended row lands, is always counted from the end,
     * in one read, however many rows before it are hidden.
     */
    var seen = 0
    for (index in rows.size - 1 downTo 0) {
        if (!isShownAt(index)) continue
        if (seen == fromEnd) return index
        seen++
    }
    throw IndexOutOfBoundsException("position $position of $shownCount shown rows")
}

/** The index, among [size] rows, of the one at [position] among those [isShownAt] says are shown. */
fun ChatTimelineShownRows.timelineIndexOf(size: Int, position: Int, isShownAt: (Int) -> Boolean): Int {
    var seen = 0
    for (index in 0 until size) {
        if (!isShownAt(index)) continue
        if (seen == position) return index
        seen++
    }
    throw IndexOutOfBoundsException("position $position of $seen shown rows")
}
