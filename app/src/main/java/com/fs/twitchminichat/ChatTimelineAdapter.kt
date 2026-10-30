package com.fs.twitchminichat

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fs.twitchminichat.chat.ChatRowViewType
import com.fs.twitchminichat.chat.ChatTimelineChange
import com.fs.twitchminichat.chat.ChatTimelineRow
import com.fs.twitchminichat.chat.ShownRowsUpdate

/**
 * The chat list's adapter: the shown rows of the timeline, each bound to its view by [binder].
 * Main thread.
 *
 * Kept thin on purpose: a mechanical map from [ShownRowsUpdate] to RecyclerView's notifications,
 * and from a holder to [binder]. Which rows are shown, and where, is decided by
 * ChatTimelineListSync; how a row looks, by [binder]. This class decides neither, so what it does
 * can be read in full here, and the device harness runs every pass through it.
 *
 * - A view is built empty for its row's kind ([ChatRowViewType]) and bound by a full bind, or by
 *   [binder]'s rebind when the bind carries a [ChatTimelineChange] payload: an echo's status change,
 *   which must not rebuild its message or reload its emotes.
 * - A recycled holder is unbound: what it loaded for its row ends there (ChatRowViewBinder, item 2).
 *
 * Not guarded, each relying on RecyclerView's own contract: a holder offered for another view type
 * (the cast in [ChatRowViewBinder.bind] fails at once), a position out of range, and a notification
 * that does not match the data (RecyclerView throws its own "Inconsistency detected").
 */
internal class ChatTimelineAdapter(
    private val binder: ChatRowViewBinder
) : RecyclerView.Adapter<ChatTimelineAdapter.RowHolder>() {

    /** One row's view. */
    class RowHolder(view: View) : RecyclerView.ViewHolder(view)

    private val rows = ArrayList<ChatTimelineRow>()

    /** The rows the list shows, in order. */
    val shownRows: List<ChatTimelineRow>
        get() = rows

    /** Applies [update] to the rows and tells the list what changed. */
    fun apply(update: ShownRowsUpdate) {
        when (update) {
            is ShownRowsUpdate.Insert -> {
                rows.add(update.position, update.row)
                notifyItemInserted(update.position)
            }
            is ShownRowsUpdate.Remove -> {
                rows.removeAt(update.position)
                notifyItemRemoved(update.position)
            }
            is ShownRowsUpdate.Change -> {
                rows[update.position] = update.row
                notifyItemChanged(update.position, update.change)
            }
            is ShownRowsUpdate.InsertRange -> {
                rows.addAll(update.position, update.rows)
                notifyItemRangeInserted(update.position, update.rows.size)
            }
            is ShownRowsUpdate.RemoveRange -> {
                rows.subList(update.position, update.position + update.count).clear()
                notifyItemRangeRemoved(update.position, update.count)
            }
            is ShownRowsUpdate.Reset -> {
                val removed = rows.size
                rows.clear()
                if (removed > 0) notifyItemRangeRemoved(0, removed)
                rows.addAll(update.rows)
                if (update.rows.isNotEmpty()) notifyItemRangeInserted(0, update.rows.size)
            }
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = ChatRowViewType.of(rows[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val probeBuild = com.fs.twitchminichat.probe.StartupProbe.buildIn(com.fs.twitchminichat.probe.StartupProbe.BUILD_CREATE)
        val view = binder.newView(viewType)
        com.fs.twitchminichat.probe.StartupProbe.buildOut(probeBuild, view)
        /* LinearLayoutManager's default is WRAP_CONTENT both ways, which would narrow short rows and their backgrounds. */
        view.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return RowHolder(view)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        val probeBuild = com.fs.twitchminichat.probe.StartupProbe.buildIn(com.fs.twitchminichat.probe.StartupProbe.BUILD_BIND)
        binder.bind(holder.itemView, rows[position])
        com.fs.twitchminichat.probe.StartupProbe.buildOut(probeBuild, holder.itemView)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        val row = rows[position]
        for (payload in payloads) {
            val change = checkNotNull(payload as? ChatTimelineChange) {
                "A row was bound with the payload ${payload.javaClass.name}. Invariant: the only payload is " +
                    "a ChatTimelineChange, which only ChatTimelineController sends, from a ShownRowsUpdate.Change."
            }
            binder.rebind(holder.itemView, row, change)
        }
    }

    override fun onViewRecycled(holder: RowHolder) {
        binder.unbind(holder.itemView)
    }
}
