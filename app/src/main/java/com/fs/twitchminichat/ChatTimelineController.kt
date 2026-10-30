package com.fs.twitchminichat

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fs.twitchminichat.chat.ChatTimeline
import com.fs.twitchminichat.chat.ChatTimelineListSync
import com.fs.twitchminichat.chat.ChatTimelineRow
import com.fs.twitchminichat.chat.PendingEchoEvent
import com.fs.twitchminichat.chat.ShownRowsUpdate

/** Identifies the stable chronological position of one rendered chat row. */
data class ChatTimelinePosition(
    val timestampMillis: Long,
    val sequence: Long
) : Comparable<ChatTimelinePosition> {

    /** Sorts by Twitch timestamp and preserves source order for equal timestamps. */
    override fun compareTo(other: ChatTimelinePosition): Int {
        val timestampComparison = timestampMillis.compareTo(other.timestampMillis)
        if (timestampComparison != 0) return timestampComparison
        return sequence.compareTo(other.sequence)
    }
}

/** Calculates deterministic insertion points without Android framework dependencies. */
object ChatTimelineOrderer {

    /**
     * Returns the index before the first position newer than the candidate: after every
     * position equal to it or older, so a row whose position equals an existing one goes
     * after it.
     *
     * [existingPositions] must be in non-decreasing order, as the timeline keeps them. Over
     * such a list this is the index a scan from the start for the first newer position
     * finds, read in at most one position for a candidate not older than the last - a
     * message appended at the end, the usual case - and by binary search otherwise.
     */
    fun insertionIndex(
        existingPositions: List<ChatTimelinePosition>,
        candidate: ChatTimelinePosition
    ): Int {
        val size = existingPositions.size
        /* Nothing is newer than a candidate that is not older than the last position. */
        if (size == 0 || !(candidate < existingPositions[size - 1])) return size

        /* The last position is newer, so the first newer one is in low..high. */
        var low = 0
        var high = size - 1
        while (low < high) {
            val middle = low + (high - low) / 2
            if (candidate < existingPositions[middle]) {
                high = middle
            } else {
                low = middle + 1
            }
        }
        return low
    }
}

/**
 * Keeps rendered chat rows ordered across asynchronous history and live delivery, in the chat
 * list [list].
 *
 * The order is decided by a [ChatTimeline], the list of rows, and which rows are shown by the
 * hidden users; [ChatTimelineListSync] turns each change into updates to the shown rows, and the
 * list's adapter applies them. The list holds only the shown rows: the item at a position is the
 * shown row at that position, its view built and bound by [rowViews] when the list lays it out,
 * carrying that row as its tag. A row's view does not exist until then, and only rows the list lays
 * out get one. This controller must be the only writer of the list's adapter.
 *
 * The list is set up here, so that the page and the device harness get the same one: a vertical
 * LinearLayoutManager, no item animator (an animator could show two holders for one row,
 * ChatRowViewBinder item 4), and the adapter.
 */
class ChatTimelineController internal constructor(
    list: RecyclerView,
    rowViews: ChatRowViewBinder,
    currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val sync = ChatTimelineListSync(ChatTimeline(currentTimeMillis))
    private val adapter = ChatTimelineAdapter(rowViews)

    init {
        list.layoutManager = LinearLayoutManager(list.context)
        list.itemAnimator = null
        list.adapter = adapter
    }

    /** How many rows the list shows: the rows of the timeline, less those of hidden users. */
    val shownRowCount: Int
        get() = adapter.itemCount

    /**
     * Inserts one row according to its Twitch timestamp, with the data [row] builds for it. Its
     * view is built when the list lays it out, if it does.
     */
    fun insert(
        messageTimestampSec: Double?,
        preservedPosition: ChatTimelinePosition? = null,
        row: (ChatTimelinePosition) -> ChatTimelineRow
    ): ChatTimelinePosition {
        val inserted = sync.insert(messageTimestampSec, preservedPosition, row)
        inserted.update?.let(adapter::apply)
        return inserted.row.position
    }

    /** Clears the rendered timeline and its ordering metadata. */
    fun clear() {
        adapter.apply(sync.clear())
    }

    /** Hides the messages of [usernameLower], a trimmed and lowercased name. Returns whether any row changed. */
    fun hideUser(usernameLower: String): Boolean {
        return applyAll(sync.hideUser(usernameLower))
    }

    /** Makes [users], trimmed and lowercased, the hidden users. Returns whether any row changed. */
    fun setHiddenUsers(users: Set<String>): Boolean {
        return applyAll(sync.setHiddenUsers(users))
    }

    /**
     * Moves the echo of [localId] by [event]; its view, if it has one, is rebound in place with the
     * change as payload. Returns whether its status changed.
     */
    fun applyEchoEvent(localId: String, event: PendingEchoEvent): Boolean {
        val change = sync.applyEchoEvent(localId, event) ?: return false
        adapter.apply(change)
        return true
    }

    /** Removes the echo of [localId] and returns its position for the canonical message, or null. */
    fun removeEcho(localId: String): ChatTimelinePosition? {
        val removed = sync.removeEcho(localId) ?: return null
        adapter.apply(removed.update)
        return removed.position
    }

    private fun applyAll(updates: List<ShownRowsUpdate>): Boolean {
        updates.forEach(adapter::apply)
        return updates.isNotEmpty()
    }
}
