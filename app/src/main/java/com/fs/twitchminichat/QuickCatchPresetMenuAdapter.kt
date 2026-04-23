package com.fs.twitchminichat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuickCatchPresetMenuAdapter(
    private var items: List<QuickCatchPresetRow>,
    private val onPresetClicked: (CatchPreset) -> Unit,
    private val onBuyClicked: (CatchPreset) -> Unit,
    private val onBuddyClicked: (CatchPreset) -> Unit
) : RecyclerView.Adapter<QuickCatchPresetMenuAdapter.RowViewHolder>() {

    fun updateItems(newItems: List<QuickCatchPresetRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_quick_catch_preset, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val root: LinearLayout = itemView.findViewById(R.id.rootQuickCatchPreset)
        private val textLabel: TextView = itemView.findViewById(R.id.textQuickCatchLabel)
        private val textSubtitle: TextView = itemView.findViewById(R.id.textQuickCatchSubtitle)
        private val textCount: TextView = itemView.findViewById(R.id.textQuickCatchCount)
        private val btnBuy: ImageButton = itemView.findViewById(R.id.btnQuickCatchBuy)
        private val btnBuddy: ImageButton = itemView.findViewById(R.id.btnQuickCatchBuddy)

        fun bind(row: QuickCatchPresetRow) {
            textLabel.text = row.label
            textCount.text = row.countText

            val subtitle = row.subtitle?.trim().orEmpty()
            if (subtitle.isNotBlank()) {
                textSubtitle.text = subtitle
                textSubtitle.visibility = View.VISIBLE
            } else {
                textSubtitle.visibility = View.GONE
            }

            btnBuy.visibility = if (row.showBuyButton) View.VISIBLE else View.GONE
            btnBuddy.visibility = if (row.showBuddyButton) View.VISIBLE else View.GONE

            root.setOnClickListener {
                onPresetClicked(row.preset)
            }

            btnBuy.setOnClickListener {
                onBuyClicked(row.preset)
            }

            btnBuddy.setOnClickListener {
                onBuddyClicked(row.preset)
            }
        }
    }
}