package com.fs.twitchminichat

import android.view.View
import android.view.ViewGroup
import com.fs.twitchminichat.chat.ChatTimeline
import com.fs.twitchminichat.chat.ChatTimelineRow
import com.fs.twitchminichat.chat.ChatTimelineViewSync
import com.fs.twitchminichat.chat.ChatTimelineViews

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

    /** Returns the index before the first position newer than the candidate. */
    fun insertionIndex(
        existingPositions: List<ChatTimelinePosition>,
        candidate: ChatTimelinePosition
    ): Int {
        val index = existingPositions.indexOfFirst { existing ->
            candidate < existing
        }
        return if (index >= 0) index else existingPositions.size
    }
}

/**
 * Keeps rendered chat rows ordered across asynchronous history and live delivery.
 *
 * The order is decided by a [ChatTimeline], the list of rows, and the container's
 * children follow it: the child at an index is the view of the row at that index, and
 * carries that row as its tag. This controller must be the only writer of the container.
 */
class ChatTimelineController(
    container: ViewGroup,
    currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val sync = ChatTimelineViewSync(
        views = ContainerTimelineViews(container),
        timeline = ChatTimeline(currentTimeMillis)
    )

    /** Inserts one row according to its Twitch timestamp, with the data [row] builds for it. */
    fun insert(
        view: View,
        messageTimestampSec: Double?,
        preservedPosition: ChatTimelinePosition? = null,
        row: (ChatTimelinePosition) -> ChatTimelineRow
    ): ChatTimelinePosition {
        return sync.insert(view, messageTimestampSec, preservedPosition, row).position
    }

    /** Removes one row and returns its former chronological position. */
    fun removeAndTakePosition(view: View): ChatTimelinePosition? {
        return sync.removeAndTakePosition(view)
    }

    /** Removes one row without preserving its chronological position. */
    fun remove(view: View) {
        sync.remove(view)
    }

    /** Clears the rendered timeline and its ordering metadata. */
    fun clear() {
        sync.clear()
    }

    /** Hides the messages of [usernameLower], a trimmed and lowercased name. Returns whether any row changed. */
    fun hideUser(usernameLower: String): Boolean {
        return sync.hideUser(usernameLower)
    }

    /** Makes [users], trimmed and lowercased, the hidden users. Returns whether any row changed. */
    fun setHiddenUsers(users: Set<String>): Boolean {
        return sync.setHiddenUsers(users)
    }
}

/** The timeline's views as the children of [container], each with its row as its tag. */
private class ContainerTimelineViews(
    private val container: ViewGroup
) : ChatTimelineViews<View> {

    override fun indexOf(view: View): Int = container.indexOfChild(view)

    override fun add(view: View, row: ChatTimelineRow, index: Int) {
        /* Set before the view is added, as the message rows' tag always was. */
        view.tag = row
        container.addView(view, index)
    }

    override fun removeAt(index: Int) {
        container.removeViewAt(index)
    }

    override fun removeAll() {
        container.removeAllViews()
    }

    override fun setShown(index: Int, shown: Boolean) {
        container.getChildAt(index).visibility = if (shown) View.VISIBLE else View.GONE
    }
}
