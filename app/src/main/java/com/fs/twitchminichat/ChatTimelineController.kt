package com.fs.twitchminichat

import android.view.View
import android.view.ViewGroup
import java.util.WeakHashMap

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

/** Keeps rendered chat rows ordered across asynchronous history and live delivery. */
class ChatTimelineController(
    private val container: ViewGroup,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val positionsByView = WeakHashMap<View, ChatTimelinePosition>()
    private var nextSequence = 0L

    /** Inserts one row according to its Twitch timestamp. */
    fun insert(
        view: View,
        messageTimestampSec: Double?,
        preservedPosition: ChatTimelinePosition? = null
    ): ChatTimelinePosition {
        val position = ChatTimelinePosition(
            timestampMillis = resolveTimestampMillis(messageTimestampSec),
            sequence = preservedPosition?.sequence ?: nextSequence++
        )

        val existingPositions = buildList {
            for (index in 0 until container.childCount) {
                val child = container.getChildAt(index)
                add(
                    positionsByView[child] ?: ChatTimelinePosition(
                        timestampMillis = Long.MIN_VALUE,
                        sequence = index.toLong()
                    )
                )
            }
        }

        val insertionIndex = ChatTimelineOrderer.insertionIndex(
            existingPositions = existingPositions,
            candidate = position
        )

        positionsByView[view] = position
        container.addView(view, insertionIndex)
        return position
    }

    /** Removes one row and returns its former chronological position. */
    fun removeAndTakePosition(view: View): ChatTimelinePosition? {
        val position = positionsByView.remove(view)
        container.removeView(view)
        return position
    }

    /** Removes one row without preserving its chronological position. */
    fun remove(view: View) {
        positionsByView.remove(view)
        container.removeView(view)
    }

    /** Clears the rendered timeline and its ordering metadata. */
    fun clear() {
        positionsByView.clear()
        nextSequence = 0L
        container.removeAllViews()
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
