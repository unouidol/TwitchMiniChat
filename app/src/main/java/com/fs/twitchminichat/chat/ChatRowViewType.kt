package com.fs.twitchminichat.chat

/**
 * The view type of each kind of timeline row, as the chat list's adapter reports it. Pure.
 *
 * One view type per row kind, and this is the only source of them. A RecyclerView keeps its pool
 * by view type and offers a holder only for the type it was built for, so a view built for one
 * kind is never bound to another. That matters beyond the cast: the flags a movement method forces
 * on a message view - focusable, clickable, long-clickable - are set by its first bind only
 * (TextView.setMovementMethod does nothing when given the movement method the view already has,
 * TextView.java:2808, API 36), so a type shared across kinds would carry them from a message into
 * a system line without anything failing.
 */
object ChatRowViewType {

    /** A chat message, shown in the page's message view. */
    const val MESSAGE = 0

    /** A line the app writes itself, shown in a plain TextView. */
    const val SYSTEM_LINE = 1

    /** A pending echo, shown in a PendingEchoView around a message view. */
    const val PENDING_ECHO = 2

    /** Every view type, one per row kind. */
    val all: List<Int> = listOf(MESSAGE, SYSTEM_LINE, PENDING_ECHO)

    /** The view type of [row]'s kind. */
    fun of(row: ChatTimelineRow): Int = when (row) {
        is ChatMessageRow -> MESSAGE
        is SystemLineRow -> SYSTEM_LINE
        is PendingEchoRow -> PENDING_ECHO
    }
}
