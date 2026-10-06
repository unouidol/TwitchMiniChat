package com.fs.twitchminichat.chat

/**
 * Which rows of the timeline are shown: a pure function of the rows and of the users
 * hidden in the app.
 *
 * Hiding never takes a row out of the timeline. It only leaves the row out of what is
 * shown, so un-hiding the user brings the same rows back in their places.
 *
 * Only chat messages can be hidden. A system line and the pending outgoing echo are
 * not anyone's message, and no user filter ever hides them.
 *
 * [hiddenUsers] holds names trimmed and lowercased, the form HiddenUsersStore keeps
 * and [ChatMessageRow.usernameLower] carries; they are compared as they are.
 */
object ChatTimelineVisibility {

    /** Whether [row] is shown while [hiddenUsers] are hidden. */
    fun isShown(row: ChatTimelineRow, hiddenUsers: Set<String>): Boolean {
        return row !is ChatMessageRow || row.usernameLower !in hiddenUsers
    }

    /** The rows that are shown, in timeline order. */
    fun shownRows(rows: List<ChatTimelineRow>, hiddenUsers: Set<String>): List<ChatTimelineRow> {
        return rows.filter { row -> isShown(row, hiddenUsers) }
    }
}
