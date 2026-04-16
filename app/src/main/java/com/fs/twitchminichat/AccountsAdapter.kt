package com.fs.twitchminichat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AccountsAdapter(
    private val onClick: (AccountConfig) -> Unit,
    private val onDelete: (AccountConfig) -> Unit,
    private val onLongPressDelete: (AccountConfig) -> Unit,
    private val onStartDragRequest: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<AccountConfig, AccountsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<AccountConfig>() {
        override fun areItemsTheSame(oldItem: AccountConfig, newItem: AccountConfig): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AccountConfig, newItem: AccountConfig): Boolean =
            oldItem == newItem
    }

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

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.textUser.text =
            context.getString(R.string.account_username, item.username)

        holder.textChannel.text =
            context.getString(R.string.account_channel, item.channel)

        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)
        holder.accountContent.setOnClickListener(null)
        holder.accountContent.setOnLongClickListener(null)
        holder.btnDelete.setOnClickListener(null)
        holder.btnDelete.setOnLongClickListener(null)

        holder.accountContent.setOnClickListener {
            onClick(item)
        }

        holder.accountContent.setOnLongClickListener {
            onStartDragRequest(holder)
            true
        }

        holder.btnDelete.setOnClickListener {
            onDelete(item)
        }

        holder.btnDelete.setOnLongClickListener {
            onLongPressDelete(item)
            true
        }
    }

    fun submitAccounts(accounts: List<AccountConfig>) {
        submitList(accounts.toList())
    }

    fun currentAccounts(): List<AccountConfig> {
        return currentList.toList()
    }

    fun moveItem(from: Int, to: Int) {
        if (from == to) return
        if (from !in currentList.indices || to !in currentList.indices) return

        val mutable = currentList.toMutableList()
        val moved = mutable.removeAt(from)
        mutable.add(to, moved)
        submitList(mutable.toList())
    }
}