package com.fs.twitchminichat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AccountsAdapter(
    private val onClick: (AccountConfig) -> Unit,
    private val onDelete: (AccountConfig) -> Unit,
    private val onLongPressDelete: (AccountConfig) -> Unit,
    private val onStartDragRequest: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<AccountsAdapter.VH>() {

    private val items = mutableListOf<AccountConfig>()

    init {
        setHasStableIds(true)
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

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return items[position].id.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.textUser.text =
            context.getString(R.string.account_username, item.username)

        holder.textChannel.text =
            context.getString(R.string.account_channel, item.channel)

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

    fun submitAccounts(list: List<AccountConfig>) {
        items.clear()
        items.addAll(list.sortedBy { it.sortOrder })
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        if (fromPosition !in items.indices) return
        if (toPosition !in items.indices) return

        val moved = items.removeAt(fromPosition)
        items.add(toPosition, moved)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun currentAccounts(): List<AccountConfig> = items.toList()
}