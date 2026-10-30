package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition

/**
 * Keeps a [ChatTimeline] and says, for every change to it, what the change does to the list of
 * shown rows, as the [ShownRowsUpdate]s a list holding only those rows needs. Pure, main thread.
 *
 * The same decisions as [ChatTimelineViewSync], on the same timeline and the same hidden users,
 * returned instead of applied: [ChatTimelineViewSync] keeps a view for every row, a hidden one
 * included, and hides it in place; a list fed by this class holds only the shown rows, so a row
 * that is not shown has no update and, in that list, no view at all. Each update's position is
 * the row's position in that list, so nothing translates it back to a timeline index.
 *
 * Every change goes to the timeline first; the updates follow from the timeline as it then is.
 * Nothing else may change the timeline, or the list would stop following it.
 */
class ChatTimelineListSync(
    private val timeline: ChatTimeline
) {

    /** A row inserted by [insert], and what it did to the shown rows: null when it is not shown. */
    data class Inserted(val row: ChatTimelineRow, val update: ShownRowsUpdate.Insert?)

    /** An echo removed by [removeEcho]: its position, for the canonical message, and its removal from the shown rows. */
    data class RemovedEcho(val position: ChatTimelinePosition, val update: ShownRowsUpdate.Remove)

    /* Trimmed and lowercased; kept across clear, like the hidden users themselves. */
    private var hiddenUsers: Set<String> = emptySet()

    /**
     * How many rows of the timeline are not shown, kept as rows come and go and as users are
     * hidden and shown again, so that no position has to count them: a row near either end of
     * the timeline reads a row or two. Internal only so the tests can check it, after every
     * operation, against a count made afresh; nothing else reads it.
     */
    internal var hiddenRowCount = 0
        private set

    /** The rows that are shown, in timeline order. */
    val shownRows: List<ChatTimelineRow>
        get() = ChatTimelineVisibility.shownRows(timeline.rows, hiddenUsers)

    /** How many rows are shown, without listing them. */
    val shownRowCount: Int
        get() = timeline.rows.size - hiddenRowCount

    /** Inserts the row [create] builds where the timeline places it, and returns it with its update. */
    fun insert(
        messageTimestampSec: Double?,
        preservedPosition: ChatTimelinePosition?,
        create: (ChatTimelinePosition) -> ChatTimelineRow
    ): Inserted {
        val index = timeline.insert(messageTimestampSec, preservedPosition, create)
        val row = timeline.rows[index]
        val update = ChatTimelineShownRows.inserted(timeline.rows, hiddenUsers, hiddenRowCount, index)
        if (update == null) hiddenRowCount++
        return Inserted(row, update)
    }

    /** Hides the rows of [usernameLower], a trimmed and lowercased name, on top of the users already hidden. */
    fun hideUser(usernameLower: String): List<ShownRowsUpdate> {
        return setHiddenUsers(hiddenUsers + usernameLower)
    }

    /**
     * Makes [users], trimmed and lowercased, the hidden users, and returns what that does to the
     * shown rows: ranges removed and inserted, in order. Empty when no row changes.
     */
    fun setHiddenUsers(users: Set<String>): List<ShownRowsUpdate> {
        val previous = hiddenUsers
        hiddenUsers = users.toSet()

        val updates = ChatTimelineShownRows.hiddenUsersChanged(timeline.rows, previous, hiddenUsers)
        for (update in updates) {
            when (update) {
                is ShownRowsUpdate.RemoveRange -> hiddenRowCount += update.count
                is ShownRowsUpdate.InsertRange -> hiddenRowCount -= update.rows.size
                else -> error("a change of hidden users only removes and inserts ranges: $update")
            }
        }
        return updates
    }

    /**
     * Moves the echo of [localId] by [event] and returns the change of its shown row, which carries
     * [ChatTimelineChange.ECHO_STATUS] so that its view is rebound in place, not rebuilt. Null when no
     * echo of [localId] is in the timeline, or when the event leaves its status as it was.
     *
     * @throws IllegalStateException if the echo is not shown, which only a change upstream to
     * [ChatTimelineVisibility] or to the row types can cause.
     */
    fun applyEchoEvent(localId: String, event: PendingEchoEvent): ShownRowsUpdate.Change? {
        val index = indexOfEcho(localId)
        if (index < 0) return null

        val echo = timeline.rows[index] as PendingEchoRow
        val status = echo.status.after(event)
        if (status == echo.status) return null

        timeline.replaceAt(index, echo.copy(status = status))
        return checkNotNull(
            ChatTimelineShownRows.replaced(timeline.rows, hiddenUsers, hiddenRowCount, index, ChatTimelineChange.ECHO_STATUS)
        ) { ECHO_NOT_SHOWN }
    }

    /**
     * Removes the echo of [localId] and returns its position, for the canonical message that replaces
     * it, with its removal from the shown rows; null when no echo of [localId] is in the timeline.
     *
     * An echo is always shown: [ChatTimelineVisibility] hides only a [ChatMessageRow], by its type,
     * whatever users are hidden. So removing it is always an update to the shown rows.
     *
     * @throws IllegalStateException if the echo is not shown, which only a change upstream to that
     * rule or to the row types can cause.
     */
    fun removeEcho(localId: String): RemovedEcho? {
        val index = indexOfEcho(localId)
        if (index < 0) return null

        /* Decided before the row goes. */
        val update = checkNotNull(ChatTimelineShownRows.removed(timeline.rows, hiddenUsers, hiddenRowCount, index)) {
            ECHO_NOT_SHOWN
        }
        val row = timeline.removeAt(index)
        return RemovedEcho(row.position, update)
    }

    /** Removes every row and restarts the timeline's sequence. The hidden users stay hidden. */
    fun clear(): ShownRowsUpdate.Reset {
        timeline.clear()
        hiddenRowCount = 0
        return ChatTimelineShownRows.cleared()
    }

    private fun indexOfEcho(localId: String): Int {
        return timeline.indexOfFirst { row -> row is PendingEchoRow && row.localId == localId }
    }

    private companion object {
        const val ECHO_NOT_SHOWN =
            "an echo in the timeline is not shown, yet ChatTimelineVisibility hides only ChatMessageRow, so a " +
                "PendingEchoRow is shown whatever users are hidden: that invariant was broken upstream, in " +
                "ChatTimelineVisibility or in the row types, not here"
    }
}
