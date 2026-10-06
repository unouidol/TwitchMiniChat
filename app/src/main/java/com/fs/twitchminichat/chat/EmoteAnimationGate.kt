package com.fs.twitchminichat.chat

/** A vertical extent in the timeline's own coordinates, from [top] inclusive to [bottom] exclusive. */
data class TimelineSpan(val top: Int, val bottom: Int)

/**
 * Decides which chat rows' animated emotes run, and tells a row only when its answer
 * changes. Main thread.
 *
 * Pure: the chat page reports what it knows - whether it is started, whether it is the
 * pager's current page, where the viewport is - and supplies [spanOf], where a row is,
 * and [setAnimating], which applies the answer. Nothing here touches a view.
 *
 * A row's emotes run only while all three hold:
 * - **The page is started.** A stopped page - the app in the background, or another
 *   activity over it - runs nothing.
 * - **The page is the pager's current one.** The pager keeps its neighbours started,
 *   attached and laid out, and a neighbour that never becomes current is never resumed
 *   and never paused, so neither callback can tell it to stop. This signal does not
 *   depend on them: it is reported by whoever owns the pager.
 * - **The row intersects the viewport.** The timeline is a ScrollView, not a recycling
 *   list, so a row that scrolled away stays attached for the life of the page.
 *
 * Nothing runs until all three are known to hold. A page that is started but never
 * current therefore runs nothing from its first row on, without waiting for a callback
 * it would never receive.
 *
 * Only rows that hold animated emotes are tracked, so a reconcile costs one [spanOf]
 * per such row rather than one per row of the timeline, and none at all while the page
 * is not animating.
 */
class EmoteAnimationGate<R : Any>(
    private val spanOf: (R) -> TimelineSpan?,
    private val setAnimating: (R, Boolean) -> Unit
) {
    private var started = false
    private var current = false

    /* The visible part of the timeline, null until the page first reports it. */
    private var viewport: TimelineSpan? = null

    /* Every tracked row, and whether its emotes were last told to run. */
    private val rows = LinkedHashMap<R, Boolean>()

    /** The page reached onStart. */
    fun onStarted() {
        started = true
        reconcile()
    }

    /** The page reached onStop: the app went to the background, or something covers it. */
    fun onStopped() {
        started = false
        reconcile()
    }

    /** Whether the page is now the pager's current one. */
    fun onCurrentPage(isCurrent: Boolean) {
        current = isCurrent
        reconcile()
    }

    /** The visible part of the timeline is now from [top] to [bottom], in its coordinates. */
    fun onViewport(top: Int, bottom: Int) {
        viewport = TimelineSpan(top = top, bottom = bottom)
        reconcile()
    }

    /**
     * Tracks [row], which has just come to hold animated emotes. They are not running
     * when it arrives; it is told to run them only if it should.
     */
    fun add(row: R) {
        if (row in rows) return

        rows[row] = false
        apply(row)
    }

    /** Stops tracking [row]. Its owner stops its emotes when it releases it. */
    fun remove(row: R) {
        rows.remove(row)
    }

    private fun reconcile() {
        rows.keys.toList().forEach(::apply)
    }

    private fun apply(row: R) {
        val animating = shouldAnimate(row)
        if (rows[row] == animating) return

        rows[row] = animating
        setAnimating(row, animating)
    }

    private fun shouldAnimate(row: R): Boolean {
        if (!started || !current) return false

        val visible = viewport ?: return false
        /* A row not in the timeline is not visible. */
        val span = spanOf(row) ?: return false

        /* Touching an edge is not intersecting: the row must overlap by a pixel. */
        return span.bottom > visible.top && span.top < visible.bottom
    }
}
