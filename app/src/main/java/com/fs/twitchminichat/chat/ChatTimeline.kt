package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelineOrderer
import com.fs.twitchminichat.ChatTimelinePosition

/**
 * The chat timeline as an ordered list of rows: the source of truth for what the
 * timeline holds and in which order. Main thread.
 *
 * Rows are ordered by [ChatTimelinePosition]: the message's Twitch timestamp, and for
 * equal timestamps the order the rows were inserted in, which the sequence counter
 * records. History that arrives after live messages therefore lands before them, and
 * two messages from the same second keep the order they came in.
 *
 * Pure: no Android types. The clock is passed in, and is read only for a row whose
 * timestamp cannot be used.
 */
class ChatTimeline(
    private val currentTimeMillis: () -> Long
) {
    private val entries = ArrayList<ChatTimelineRow>()

    /* The sequence the next row gets, unless it keeps one it had before. */
    private var nextSequence = 0L

    /** The rows, oldest first. */
    val rows: List<ChatTimelineRow>
        get() = entries

    /**
     * Inserts the row [create] builds at its chronological place and returns that index.
     *
     * The row's position is the message's timestamp, or the local time when there is no
     * usable one, and a new sequence number - or the sequence of [preservedPosition], for
     * a row that replaces one removed with it, which keeps the replaced row's place among
     * messages of the same second. [create] receives that position and must use it.
     */
    fun insert(
        messageTimestampSec: Double?,
        preservedPosition: ChatTimelinePosition?,
        create: (ChatTimelinePosition) -> ChatTimelineRow
    ): Int {
        val position = ChatTimelinePosition(
            timestampMillis = resolveTimestampMillis(messageTimestampSec),
            sequence = preservedPosition?.sequence ?: nextSequence++
        )
        val row = create(position)
        require(row.position == position) { "A timeline row must take the position it is given." }

        val index = ChatTimelineOrderer.insertionIndex(
            existingPositions = entries.map { existing -> existing.position },
            candidate = position
        )
        entries.add(index, row)
        return index
    }

    /** Removes and returns the row at [index]. */
    fun removeAt(index: Int): ChatTimelineRow = entries.removeAt(index)

    /** Removes every row and starts the sequence again from zero. */
    fun clear() {
        entries.clear()
        nextSequence = 0L
    }

    /** Converts a valid Twitch timestamp to milliseconds or uses local time as fallback. */
    private fun resolveTimestampMillis(messageTimestampSec: Double?): Long {
        return messageTimestampSec
            ?.takeIf { timestamp -> timestamp.isFinite() && timestamp > 0.0 }
            ?.times(1000.0)
            ?.toLong()
            ?: currentTimeMillis()
    }
}
