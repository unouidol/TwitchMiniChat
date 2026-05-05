package com.fs.twitchminichat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for the login/account list.
 *
 * This adapter intentionally uses a small mutable in-memory list instead of
 * ListAdapter/DiffUtil because account rows are manually reordered with
 * ItemTouchHelper. Drag-and-drop needs immediate item movement notifications,
 * while submitList(...) can be asynchronous and may fight the active drag.
 */
class AccountsAdapter(
    private val onClick: (AccountConfig) -> Unit,
    private val onDelete: (AccountConfig) -> Unit,
    private val onStartDragRequest: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<AccountsAdapter.VH>() {

    private val accounts = mutableListOf<AccountConfig>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val accountContent: LinearLayout = v.findViewById(R.id.accountContent)
        val textUser: TextView = v.findViewById(R.id.textUser)
        val textChannel: TextView = v.findViewById(R.id.textChannel)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_account, parent, false)

        return VH(view)
    }

    override fun getItemCount(): Int {
        return accounts.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = accounts[position]
        val context = holder.itemView.context

        holder.textUser.text =
            context.getString(R.string.account_username, item.username)

        holder.textChannel.text =
            context.getString(R.string.account_channel, item.channel)

        /*
         * Clear listeners before rebinding. RecyclerView reuses row views, so
         * every row must be rebound from a known clean state.
         */
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)
        holder.accountContent.setOnClickListener(null)
        holder.accountContent.setOnLongClickListener(null)
        holder.btnDelete.setOnClickListener(null)
        holder.btnDelete.setOnLongClickListener(null)

        holder.accountContent.setOnClickListener {
            onClick(item)
        }

        /*
         * Allow drag from the visible account content.
         */
        holder.accountContent.setOnLongClickListener {
            onStartDragRequest(holder)
            true
        }

        /*
         * Also allow drag from the whole row. This makes the gesture less fragile
         * if the user long-presses padding/empty row space instead of the inner
         * accountContent layout.
         */
        holder.itemView.setOnLongClickListener {
            onStartDragRequest(holder)
            true
        }

        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(R.string.delete_account_confirm_title)
                .setMessage(
                    context.getString(
                        R.string.delete_account_confirm_message,
                        item.username
                    )
                )
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_account_confirm_action) { _, _ ->
                    onDelete(item)
                }
                .show()
        }
    }

    /**
     * Replaces the visible account list.
     *
     * Used when the login screen is opened/resumed or after account deletion.
     */
    fun submitAccounts(newAccounts: List<AccountConfig>) {
        accounts.clear()
        accounts.addAll(newAccounts)
        notifyDataSetChanged()
    }

    /**
     * Returns the current visible account order.
     *
     * LoginFragment uses this after drag ends to persist the reordered account IDs.
     */
    fun currentAccounts(): List<AccountConfig> {
        return accounts.toList()
    }

    /**
     * Moves one row during an active ItemTouchHelper drag.
     *
     * Returns true only when the adapter actually moved an item. The immediate
     * notifyItemMoved(...) call keeps RecyclerView and ItemTouchHelper in sync.
     */
    fun moveItem(from: Int, to: Int): Boolean {
        if (from == to) return false
        if (from !in accounts.indices || to !in accounts.indices) return false

        val moved = accounts.removeAt(from)
        accounts.add(to, moved)

        notifyItemMoved(from, to)
        return true
    }
}