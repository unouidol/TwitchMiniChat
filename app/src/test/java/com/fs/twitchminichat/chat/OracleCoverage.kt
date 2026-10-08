package com.fs.twitchminichat.chat

/**
 * What a randomised differential test reached, counted as it runs and printed at its end, so
 * that a change to the operations it generates can be compared with what it generated before:
 * a narrower oracle that passes is not evidence unless it still visits the same states.
 */
internal class OracleCoverage(private val oracle: String, private val seeds: Int, private val operations: Int) {
    private var steps = 0L
    private var stepsWithHiddenViews = 0L
    private var removals = 0L
    private var echoRemovals = 0L
    private var otherRowRemovals = 0L
    private var hiddenRowRemovals = 0L
    private var missedRemovals = 0L
    private var clearsOfRows = 0L
    private var largestTimeline = 0

    /** A removal took a row out of the timeline: an echo or another kind of row, hidden or shown. */
    fun removed(echo: Boolean, hidden: Boolean) {
        removals++
        if (echo) echoRemovals++ else otherRowRemovals++
        if (hidden) hiddenRowRemovals++
    }

    /** A removal was asked for and found nothing to remove. */
    fun missed() {
        missedRemovals++
    }

    /** A clear emptied a timeline of [rows] rows. */
    fun cleared(rows: Int) {
        if (rows > 0) clearsOfRows++
    }

    /** One operation ended with [timelineRows] rows, [hiddenViews] of them hidden. */
    fun step(timelineRows: Int, hiddenViews: Int) {
        steps++
        if (hiddenViews > 0) stepsWithHiddenViews++
        largestTimeline = maxOf(largestTimeline, timelineRows)
    }

    override fun toString(): String =
        "COVERAGE $oracle: seeds=$seeds operations=$operations steps=$steps " +
            "stepsWithHiddenViews=$stepsWithHiddenViews removals=$removals echoRemovals=$echoRemovals " +
            "otherRowRemovals=$otherRowRemovals hiddenRowRemovals=$hiddenRowRemovals " +
            "missedRemovals=$missedRemovals clearsOfRows=$clearsOfRows largestTimeline=$largestTimeline"
}
