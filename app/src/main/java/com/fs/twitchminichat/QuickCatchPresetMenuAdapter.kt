package com.fs.twitchminichat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the quick catch menu RecyclerView.
 *
 * This adapter can show two kinds of visual entries:
 *
 * 1) Section headers
 *    Example: "Smart presets", "User presets"
 *
 * 2) Real preset rows
 *    Example: "Ultra Ball", "Great Ball", "!pokecheck"
 *
 * This separation lets us show temporary Smart Presets without mixing them into
 * the user's saved preset list.
 */
class QuickCatchPresetMenuAdapter(
    private var items: List<QuickCatchPresetMenuEntry>,
    private val onPresetClicked: (CatchPreset) -> Unit,
    private val onBuyClicked: (CatchPreset) -> Unit,
    private val onBuddyClicked: (CatchPreset) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Replaces the current menu content.
     *
     * DiffUtil compares old and new entries and tells RecyclerView exactly which
     * rows changed, instead of forcing the whole menu to redraw.
     */
    fun updateItems(newItems: List<QuickCatchPresetMenuEntry>) {
        val oldItems = items
        val updatedItems = newItems.toList()

        val diffResult = DiffUtil.calculateDiff(
            QuickCatchPresetMenuDiffCallback(
                oldItems = oldItems,
                newItems = updatedItems
            )
        )

        items = updatedItems
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Tells RecyclerView which layout to use for this position.
     *
     * Headers and preset rows use different XML layouts, so they need different
     * view types.
     */
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is QuickCatchPresetMenuEntry.Header -> VIEW_TYPE_HEADER
            is QuickCatchPresetMenuEntry.PresetRow -> VIEW_TYPE_PRESET
        }
    }

    /**
     * Creates the correct ViewHolder for the requested row type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = inflater.inflate(
                    R.layout.row_quick_catch_section_header,
                    parent,
                    false
                )
                HeaderViewHolder(view)
            }

            VIEW_TYPE_PRESET -> {
                val view = inflater.inflate(
                    R.layout.row_quick_catch_preset,
                    parent,
                    false
                )
                RowViewHolder(
                    itemView = view,
                    onPresetClicked = onPresetClicked,
                    onBuyClicked = onBuyClicked,
                    onBuddyClicked = onBuddyClicked
                )
            }

            else -> error("Unsupported quick catch menu viewType=$viewType")
        }
    }

    /**
     * Binds the current item data to an existing recycled ViewHolder.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is QuickCatchPresetMenuEntry.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }

            is QuickCatchPresetMenuEntry.PresetRow -> {
                (holder as RowViewHolder).bind(item.row)
            }
        }
    }

    /**
     * Total number of visual entries.
     *
     * This includes both section headers and preset rows.
     */
    override fun getItemCount(): Int = items.size

    /**
     * Header row ViewHolder.
     *
     * It only displays a title, so it does not need click callbacks.
     */
    private class HeaderViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val textHeader: TextView =
            itemView.findViewById(R.id.textQuickCatchSectionHeader)

        fun bind(header: QuickCatchPresetMenuEntry.Header) {
            textHeader.text = header.title
        }
    }

    /**
     * Real preset row ViewHolder.
     *
     * This row can be tapped to use a preset, and can optionally show extra
     * actions like buy/buddy buttons.
     */
    private class RowViewHolder(
        itemView: View,
        private val onPresetClicked: (CatchPreset) -> Unit,
        private val onBuyClicked: (CatchPreset) -> Unit,
        private val onBuddyClicked: (CatchPreset) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val root: LinearLayout =
            itemView.findViewById(R.id.rootQuickCatchPreset)

        private val textLabel: TextView =
            itemView.findViewById(R.id.textQuickCatchLabel)

        private val textSubtitle: TextView =
            itemView.findViewById(R.id.textQuickCatchSubtitle)

        private val textCount: TextView =
            itemView.findViewById(R.id.textQuickCatchCount)

        private val btnBuy: ImageButton =
            itemView.findViewById(R.id.btnQuickCatchBuy)

        private val btnBuddy: ImageButton =
            itemView.findViewById(R.id.btnQuickCatchBuddy)

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

    /**
     * DiffUtil callback for menu entries.
     *
     * areItemsTheSame checks whether two entries represent the same logical row.
     * areContentsTheSame checks whether the visible content of that row changed.
     */
    private class QuickCatchPresetMenuDiffCallback(
        private val oldItems: List<QuickCatchPresetMenuEntry>,
        private val newItems: List<QuickCatchPresetMenuEntry>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]

            return when {
                oldItem is QuickCatchPresetMenuEntry.Header &&
                        newItem is QuickCatchPresetMenuEntry.Header -> {
                    oldItem.title == newItem.title
                }

                oldItem is QuickCatchPresetMenuEntry.PresetRow &&
                        newItem is QuickCatchPresetMenuEntry.PresetRow -> {
                    oldItem.row.preset.id == newItem.row.preset.id
                }

                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 1
        const val VIEW_TYPE_PRESET = 2
    }
}