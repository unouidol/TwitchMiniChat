package com.fs.twitchminichat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager

/**
 * Displays section headers and Twitch emotes in one recyclable grid.
 *
 * Every Glide request is cleared before a cell is rebound or recycled so an
 * asynchronous image result cannot appear in a different emote cell.
 */
class TwitchEmotePickerAdapter(
    private val requestManager: RequestManager,
    private val onEntryClicked: (TwitchEmoteCatalogEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** Flattened immutable rows currently displayed by the picker. */
    private var items: List<PickerItem> = emptyList()

    /** Replaces the visible sectioned result. */
    fun submitSections(sections: List<TwitchEmotePickerSection>) {
        items = buildList<PickerItem> {
            sections.forEach { section ->
                add(PickerItem.Header(section.kind))

                section.entries.forEach { entry ->
                    add(PickerItem.Emote(entry))
                }
            }
        }

        notifyDataSetChanged()
    }

    /** Returns a full-width span for headers and a regular span for emotes. */
    fun spanSizeAt(position: Int, spanCount: Int): Int {
        return if (items.getOrNull(position) is PickerItem.Header) {
            spanCount
        } else {
            1
        }
    }

    /** Returns the row type used to inflate a header or emote cell. */
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PickerItem.Header -> VIEW_TYPE_HEADER
            is PickerItem.Emote -> VIEW_TYPE_EMOTE
        }
    }

    /** Creates one section header or emote cell. */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    inflater.inflate(
                        R.layout.item_twitch_emote_section,
                        parent,
                        false
                    )
                )
            }

            else -> {
                EmoteViewHolder(
                    inflater.inflate(
                        R.layout.item_twitch_emote,
                        parent,
                        false
                    )
                )
            }
        }
    }

    /** Binds one section title or Twitch emote preview. */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = items[position]) {
            is PickerItem.Header -> {
                (holder as HeaderViewHolder).bind(item.kind)
            }

            is PickerItem.Emote -> {
                bindEmote(
                    holder = holder as EmoteViewHolder,
                    entry = item.entry
                )
            }
        }
    }

    /** Returns the number of headers and emote cells. */
    override fun getItemCount(): Int = items.size

    /** Clears the previous request before RecyclerView reuses an emote cell. */
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is EmoteViewHolder) {
            holder.itemView.setOnClickListener(null)
            requestManager.clear(holder.imageView)
            holder.imageView.setImageDrawable(null)
        }

        super.onViewRecycled(holder)
    }

    /** Binds one emote entry and its official Twitch preview. */
    private fun bindEmote(
        holder: EmoteViewHolder,
        entry: TwitchEmoteCatalogEntry
    ) {
        val context = holder.itemView.context

        holder.nameView.text = entry.name
        holder.imageView.contentDescription = context.getString(
            R.string.emote_picker_item_description,
            entry.name
        )
        holder.itemView.contentDescription = holder.imageView.contentDescription

        requestManager.clear(holder.imageView)
        holder.imageView.setImageDrawable(null)

        val format = if (
            entry.formats.any { formatName ->
                formatName.equals("animated", ignoreCase = true)
            }
        ) {
            TwitchEmoteFormat.ANIMATED
        } else {
            TwitchEmoteFormat.STATIC
        }

        val renderSizePx = holder.imageView.layoutParams.width.coerceAtLeast(1)
        val url = TwitchEmoteUrlFactory.build(
            emoteId = entry.id,
            format = format,
            theme = TwitchEmoteTheme.DARK,
            scale = TwitchEmoteUrlFactory.scaleForRenderSize(renderSizePx)
        )

        if (url != null) {
            requestManager
                .load(url)
                .override(renderSizePx, renderSizePx)
                .into(holder.imageView)
        }

        holder.itemView.setOnClickListener {
            onEntryClicked(entry)
        }
    }

    /** Holds one full-width section title. */
    private class HeaderViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val titleView: TextView = itemView as TextView

        /** Selects the localized title belonging to this section. */
        fun bind(kind: TwitchEmotePickerSectionKind) {
            titleView.setText(kind.titleResource())
        }

        /** Maps section types to user-visible string resources. */
        @StringRes
        private fun TwitchEmotePickerSectionKind.titleResource(): Int {
            return when (this) {
                TwitchEmotePickerSectionKind.RECENT ->
                    R.string.emote_picker_section_recent

                TwitchEmotePickerSectionKind.CHANNEL ->
                    R.string.emote_picker_section_channel

                TwitchEmotePickerSectionKind.OTHER ->
                    R.string.emote_picker_section_other

                TwitchEmotePickerSectionKind.GLOBAL ->
                    R.string.emote_picker_section_global
            }
        }
    }

    /** Holds the preview and name belonging to one emote cell. */
    private class EmoteViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById(R.id.imageEmote)
        val nameView: TextView = itemView.findViewById(R.id.textEmoteName)
    }

    /** Internal row displayed by the heterogeneous adapter. */
    private sealed interface PickerItem {
        /** Full-width group title. */
        data class Header(
            val kind: TwitchEmotePickerSectionKind
        ) : PickerItem

        /** Regular emote grid cell. */
        data class Emote(
            val entry: TwitchEmoteCatalogEntry
        ) : PickerItem
    }

    private companion object {
        /** RecyclerView type used by full-width headers. */
        private const val VIEW_TYPE_HEADER = 1

        /** RecyclerView type used by regular emote cells. */
        private const val VIEW_TYPE_EMOTE = 2
    }
}