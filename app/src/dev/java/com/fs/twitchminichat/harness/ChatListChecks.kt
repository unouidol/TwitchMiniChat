package com.fs.twitchminichat.harness

import androidx.recyclerview.widget.RecyclerView
import com.fs.twitchminichat.ChatTimelineAdapter
import com.fs.twitchminichat.ChatTimelineController
import com.fs.twitchminichat.chat.ChatTimelineListSync
import org.json.JSONArray
import org.json.JSONObject

/**
 * The check the harness runs on a production list after every layout it makes: the adapter holds
 * the timeline's shown rows - the same rows, in the same order - and every attached view's tag is
 * the row at its holder's position.
 *
 * RecyclerView throws its own "Inconsistency detected" when a notification does not match the
 * adapter's data. This catches what it cannot see: data that matches the notifications but not the
 * timeline, and a view showing a row other than the one at its position.
 *
 * One record for the whole run, reported in the results file; a failure fails the run.
 */
internal object ChatListChecks {

    /** How many checks ran. */
    var checks = 0
        private set

    /** Every failure, with where it happened. */
    val failures = mutableListOf<String>()

    /** Starts a run's record. */
    fun reset() {
        checks = 0
        failures.clear()
    }

    /** Checks [list], which [controller] fills, as it stands after a layout; [where] names the step. */
    fun check(where: String, list: RecyclerView, controller: ChatTimelineController, targets: ReflectiveTargets) {
        checks++
        if (list.hasPendingAdapterUpdates()) {
            failures += "$where: checked with adapter updates still pending: the layout did not run"
            return
        }
        val adapter = list.adapter as ChatTimelineAdapter
        val sync = targets.timelineSync.get(controller) as ChatTimelineListSync
        val listed = adapter.shownRows
        val shown = sync.shownRows
        if (listed.size != shown.size || listed.indices.any { index -> listed[index] !== shown[index] }) {
            failures += "$where: the adapter holds ${listed.size} rows, the timeline shows ${shown.size}, or they differ"
        }
        if (adapter.itemCount != sync.shownRowCount) {
            failures += "$where: itemCount ${adapter.itemCount}, shownRowCount ${sync.shownRowCount}"
        }
        for (index in 0 until list.childCount) {
            val child = list.getChildAt(index)
            val position = list.getChildViewHolder(child).bindingAdapterPosition
            if (position !in listed.indices || child.tag !== listed[position]) {
                failures += "$where: the view of child $index shows a row other than the one at its position $position"
            }
        }
    }

    /** The record, for the results file. */
    fun toJson(): JSONObject = JSONObject()
        .put("checks", checks)
        .put("failures", JSONArray(failures.take(20)))
        .put("failureCount", failures.size)
        .put("passed", checks > 0 && failures.isEmpty())
}
