package com.fs.twitchminichat.chat

/**
 * Decides which chat rows' animated emotes run, and tells a row only when its answer
 * changes. Main thread.
 *
 * Pure: the chat page reports what it knows - whether it is started, and whether it is the
 * pager's current page - and supplies [setAnimating], which applies the answer. Nothing here
 * touches a view.
 *
 * A row's emotes are wanted only while both hold:
 * - **The page is started.** A stopped page - the app in the background, or another
 *   activity over it - runs nothing.
 * - **The page is the pager's current one.** The pager keeps its neighbours started,
 *   attached and laid out, and a neighbour that never becomes current is never resumed
 *   and never paused, so neither callback can tell it to stop. This signal does not
 *   depend on them: it is reported by whoever owns the pager.
 *
 * Whether the row is on screen is not decided here. The timeline is a RecyclerView (5d), which
 * attaches exactly the rows it lays out and detaches a row as it scrolls away, and the row's own
 * emote session runs its emotes only while they are wanted and the row is attached
 * (TwitchEmoteImageLoader). Until 5d the timeline was a ScrollView that kept every row attached,
 * so this gate also took the viewport, sampled after scrolls and layouts; a detach now answers
 * that at each scroll step, where the sampling answered it up to 100 ms late.
 *
 * Nothing is wanted until both are known to hold. A page that is started but never
 * current therefore runs nothing from its first row on, without waiting for a callback
 * it would never receive.
 *
 * Only rows that hold animated emotes are tracked, and none is told anything while its
 * answer stays the same.
 */
class EmoteAnimationGate<R : Any>(
    private val setAnimating: (R, Boolean) -> Unit
) {
    private var started = false
    private var current = false

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
        val animating = started && current
        if (rows[row] == animating) return

        rows[row] = animating
        setAnimating(row, animating)
    }
}
