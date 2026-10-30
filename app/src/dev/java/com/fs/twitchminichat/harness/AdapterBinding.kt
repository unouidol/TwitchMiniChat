package com.fs.twitchminichat.harness

import android.view.View
import android.view.ViewGroup
import com.fs.twitchminichat.ChatRowViewBinder
import com.fs.twitchminichat.ChatTimelineAdapter
import com.fs.twitchminichat.chat.ChatTimelineChange
import com.fs.twitchminichat.chat.ChatTimelineRow
import com.fs.twitchminichat.chat.ShownRowsUpdate

/**
 * The chat list's own adapter, used without a list: a holder created and bound as RecyclerView
 * creates and binds one, so that a proof binding a view to another row goes through the code a
 * holder back from the pool goes through.
 *
 * The adapter holds one row, the one being bound. A full bind is RecyclerView.Adapter.bindViewHolder,
 * which calls onBindViewHolder with no payload; a status change is onBindViewHolder with the payload
 * the list would deliver for it, as RecyclerView calls it when an item changed with a payload.
 */
internal class AdapterBinding(binder: ChatRowViewBinder) {

    private val adapter = ChatTimelineAdapter(binder)

    /** A holder of [row]'s view type, created in [parent] and bound to [row]: a view the list builds. */
    fun create(row: ChatTimelineRow, parent: ViewGroup): ChatTimelineAdapter.RowHolder {
        adapter.apply(ShownRowsUpdate.Reset(listOf(row)))
        val holder = adapter.createViewHolder(parent, adapter.getItemViewType(0))
        adapter.bindViewHolder(holder, 0)
        return holder
    }

    /** Binds [row] to [holder] in full, as the list binds a holder taken from the pool for it. */
    fun bindAgain(holder: ChatTimelineAdapter.RowHolder, row: ChatTimelineRow) {
        adapter.apply(ShownRowsUpdate.Reset(listOf(row)))
        check(holder.itemViewType == adapter.getItemViewType(0)) {
            "a holder of view type ${holder.itemViewType} bound to a row of type ${adapter.getItemViewType(0)}: " +
                "RecyclerView would never offer it for that row"
        }
        adapter.bindViewHolder(holder, 0)
    }

    /** Applies [change] of [row] to [holder], as the list does for an item changed with that payload. */
    fun rebind(holder: ChatTimelineAdapter.RowHolder, row: ChatTimelineRow, change: ChatTimelineChange) {
        adapter.apply(ShownRowsUpdate.Change(0, row, change))
        adapter.onBindViewHolder(holder, 0, mutableListOf(change))
    }

    /** Binds [holder] with [payload], which is not a change: the adapter's precondition must fire. */
    fun bindWithPayload(holder: ChatTimelineAdapter.RowHolder, payload: Any) {
        adapter.onBindViewHolder(holder, 0, mutableListOf(payload))
    }

    /** The view of [holder]. */
    fun viewOf(holder: ChatTimelineAdapter.RowHolder): View = holder.itemView
}
