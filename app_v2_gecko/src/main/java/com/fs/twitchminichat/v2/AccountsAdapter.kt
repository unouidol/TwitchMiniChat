package com.fs.twitchminichat.v2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AccountsAdapter(
    private val onClick: (AccountConfig) -> Unit,
    private val onDelete: (AccountConfig) -> Unit,
    private val onLongPressDelete: (AccountConfig) -> Unit
) : ListAdapter<AccountConfig, AccountsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<AccountConfig>() {
        override fun areItemsTheSame(oldItem: AccountConfig, newItem: AccountConfig): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AccountConfig, newItem: AccountConfig): Boolean =
            oldItem == newItem
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val textUser: TextView = v.findViewById(R.id.textUser)
        val textChannel: TextView = v.findViewById(R.id.textChannel)
        val btnDelete: TextView = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_account, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.textUser.text = "@${item.username}"
        holder.textChannel.text = "#${item.channel}"

        holder.itemView.setOnClickListener { onClick(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.itemView.setOnLongClickListener {
            onLongPressDelete(item)
            true
        }
    }
}
