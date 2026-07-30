package com.fs.twitchminichat.pcg.mostwanted

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.fs.twitchminichat.R
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogLabelProvider

/** Recycled ListView adapter for selectable PCG catalog entries. */
class PcgMostWantedListAdapter(
    context: Context,
    private val onSelectionChanged: (
        entry: PcgPokemonCatalogEntry,
        selected: Boolean
    ) -> Unit
) : BaseAdapter() {

    /** Layout inflater scoped to the host Activity theme. */
    private val inflater = LayoutInflater.from(context)

    /** Entries currently visible after search and filters. */
    private var entries: List<PcgPokemonCatalogEntry> = emptyList()

    /** Canonical display names selected in the current draft. */
    private var selectedDisplayNames: Set<String> = emptySet()

    /** Replaces visible rows and checkbox state in one adapter refresh. */
    fun submit(
        entries: List<PcgPokemonCatalogEntry>,
        selectedDisplayNames: Set<String>
    ) {
        this.entries = entries
        this.selectedDisplayNames = selectedDisplayNames.toSet()
        notifyDataSetChanged()
    }

    /** Refreshes checkbox state without replacing the visible entry list. */
    fun updateSelection(selectedDisplayNames: Set<String>) {
        this.selectedDisplayNames = selectedDisplayNames.toSet()
        notifyDataSetChanged()
    }

    /** Returns the number of currently visible catalog entries. */
    override fun getCount(): Int = entries.size

    /** Returns the catalog entry at one visible adapter position. */
    override fun getItem(position: Int): PcgPokemonCatalogEntry =
        entries[position]

    /** Uses stable catalog position only as the ListView row identifier. */
    override fun getItemId(position: Int): Long = position.toLong()

    /** Binds one recycled row without leaking an old checkbox listener. */
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val row: View
        val holder: ViewHolder

        if (convertView == null) {
            row = inflater.inflate(
                R.layout.item_pcg_most_wanted,
                parent,
                false
            )
            holder = ViewHolder(
                checkBox = row.findViewById(R.id.checkMostWantedPokemon),
                metadata = row.findViewById(R.id.txtMostWantedMetadata)
            )
            row.tag = holder
        } else {
            row = convertView
            holder = row.tag as ViewHolder
        }

        val entry = getItem(position)
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.text = entry.displayName
        holder.checkBox.isChecked =
            entry.displayName in selectedDisplayNames
        holder.metadata.text =
            PcgPokemonCatalogLabelProvider.metadata(row.context, entry)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onSelectionChanged(entry, isChecked)
        }

        row.setOnClickListener {
            holder.checkBox.performClick()
        }

        return row
    }

    /** Cached row views used by ListView recycling. */
    private data class ViewHolder(
        val checkBox: CheckBox,
        val metadata: TextView
    )
}