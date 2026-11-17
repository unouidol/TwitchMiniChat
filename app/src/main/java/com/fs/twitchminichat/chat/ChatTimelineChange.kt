package com.fs.twitchminichat.chat

/** A row's view that can show a changed row in place, without being rebuilt. */
interface RebindableTimelineView {

    /** Shows [row], applying only what [change] names. */
    fun rebind(row: ChatTimelineRow, change: ChatTimelineChange)
}

/**
 * What changed in a row that is rebound in place: what a [ShownRowsUpdate.Change] carries, and the
 * payload the list's partial bind applies to the row's view, through [RebindableTimelineView.rebind].
 */
enum class ChatTimelineChange {
    /** A pending echo's status, and with it its status line and opacity. */
    ECHO_STATUS
}
