package com.fs.twitchminichat

import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil

class CatchPresetEditAdapter(
    initialItems: List<CatchPreset>,
    private val onRemoveClicked: (Int) -> Unit,
    private val onStartDragRequested: (RecyclerView.ViewHolder) -> Unit,
    private val onBuyBallClicked: (CatchPreset) -> Unit,
    private val onPresetChanged: () -> Unit = {}
) : RecyclerView.Adapter<CatchPresetEditAdapter.PresetViewHolder>() {

    /*
     * Full editor model.
     *
     * Save must always use this list, not the filtered visible list.
     * Otherwise, saving while a search query is active would accidentally delete
     * every preset hidden by the filter.
     */
    private val allItems = initialItems.toMutableList()

    /*
     * Current visual list.
     *
     * Search only changes this list. It does not remove anything from allItems.
     */
    private val visibleItems = initialItems.toMutableList()

    private var searchQuery: String = ""
    private var inventoryCountsByBallId: Map<String, Int> = emptyMap()

    init {
        setHasStableIds(true)
    }

    companion object {
        /*
         * Payload used when only the inventory counter changed.
         *
         * This avoids rebinding the whole row just because the x12 / x4 count
         * changed after buying balls or refreshing PCG inventory.
         */
        private const val PAYLOAD_INVENTORY_COUNT = "payload_inventory_count"
    }

    override fun getItemId(position: Int): Long {
        return visibleItems[position].id.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_catch_preset_edit, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(
            item = visibleItems[position],
            visiblePosition = position
        )
    }

    override fun onBindViewHolder(
        holder: PresetViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_INVENTORY_COUNT)) {
            /*
             * Only the displayed inventory count changed.
             * Do not rebind EditTexts/CheckBox, because that can be annoying while
             * the user is editing a preset label or command.
             */
            holder.bindInventoryCount(visibleItems[position])
            return
        }

        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int = visibleItems.size

    /**
     * Returns the complete edited preset list.
     *
     * Important: this returns allItems, not visibleItems.
     */
    fun currentItems(): List<CatchPreset> {
        return allItems.map { preset -> preset.copy() }
    }

    fun addPreset(preset: CatchPreset) {
        allItems.add(preset)
        rebuildVisibleItems()
        onPresetChanged()
    }

    fun removeAt(position: Int) {
        val item = visibleItems.getOrNull(position) ?: return

        val removed = allItems.removeAll { preset ->
            preset.id == item.id
        }

        if (!removed) return

        rebuildVisibleItems()
        onPresetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        if (isSearchActive()) return
        if (fromPosition !in allItems.indices || toPosition !in allItems.indices) return

        /*
         * Reordering is only allowed when search is not active, because visible
         * positions then match the full saved order.
         */
        val moved = allItems.removeAt(fromPosition)
        allItems.add(toPosition, moved)

        rebuildVisibleItems()
        onPresetChanged()
    }

    fun updateInventoryCounts(newCounts: Map<String, Int>) {
        /*
         * Remember the old rendered count text before replacing the inventory map.
         * Then update only rows whose visible counter actually changed.
         */
        val oldCountTextByPresetId = visibleItems.associate { preset ->
            preset.id to formatDisplayedCount(preset)
        }

        inventoryCountsByBallId = newCounts.toMap()

        visibleItems.forEachIndexed { index, preset ->
            val oldText = oldCountTextByPresetId[preset.id]
            val newText = formatDisplayedCount(preset)

            if (oldText != newText) {
                notifyItemChanged(index, PAYLOAD_INVENTORY_COUNT)
            }
        }
    }

    fun setAllEnabled(enabled: Boolean) {
        var changed = false

        for (i in allItems.indices) {
            if (allItems[i].enabled != enabled) {
                allItems[i] = allItems[i].copy(enabled = enabled)
                changed = true
            }
        }

        if (!changed) return

        rebuildVisibleItems()
        onPresetChanged()
    }

    /**
     * Applies a visual search filter to the editor.
     *
     * The full preset list remains stored in allItems. Only visibleItems changes.
     */
    fun setSearchQuery(query: String) {
        searchQuery = query
        rebuildVisibleItems()
    }

    fun isSearchActive(): Boolean {
        return searchQuery.trim().isNotEmpty()
    }

    private fun rebuildVisibleItems() {
        val newVisibleItems = allItems.filter { preset ->
            CatchPresetEditorSearchMatcher.matches(
                preset = preset,
                rawQuery = searchQuery
            )
        }

        submitVisibleItems(newVisibleItems)
    }

    private fun submitVisibleItems(newVisibleItems: List<CatchPreset>) {
        /*
         * DiffUtil updates only inserted/removed/changed rows.
         *
         * This is especially useful for search filtering: typing in the search box
         * should not force RecyclerView to rebuild every row from scratch.
         */
        val oldVisibleItems = visibleItems.toList()

        val diffResult = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldVisibleItems.size

                override fun getNewListSize(): Int = newVisibleItems.size

                override fun areItemsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return oldVisibleItems[oldItemPosition].id ==
                            newVisibleItems[newItemPosition].id
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return oldVisibleItems[oldItemPosition] ==
                            newVisibleItems[newItemPosition]
                }
            }
        )

        visibleItems.clear()
        visibleItems.addAll(newVisibleItems)

        diffResult.dispatchUpdatesTo(this)
    }

    private fun updatePresetAtVisiblePosition(
        visiblePosition: Int,
        transform: (CatchPreset) -> CatchPreset
    ) {
        val visibleItem = visibleItems.getOrNull(visiblePosition) ?: return
        val allIndex = allItems.indexOfFirst { preset -> preset.id == visibleItem.id }
        if (allIndex == -1) return

        val oldPreset = allItems[allIndex]
        val updated = transform(oldPreset)

        if (updated == oldPreset) return

        allItems[allIndex] = updated

        val visibleIndex = visibleItems.indexOfFirst { preset -> preset.id == visibleItem.id }
        if (visibleIndex != -1) {
            visibleItems[visibleIndex] = updated
        }

        onPresetChanged()
    }

    private fun resolveDisplayedCount(item: CatchPreset): Int? {
        return BasicCatchPresetDisplayHelper.resolveDisplayedCount(
            preset = item,
            countsByBallId = inventoryCountsByBallId
        ) ?: when (item.ballId) {
            null -> null
            else -> inventoryCountsByBallId[item.ballId]
        }
    }

    private fun formatDisplayedCount(item: CatchPreset): String {
        val count = resolveDisplayedCount(item) ?: return "-"
        return "x$count"
    }

    private fun fullIndexOf(item: CatchPreset, fallbackVisiblePosition: Int): Int {
        val fullIndex = allItems.indexOfFirst { preset -> preset.id == item.id }
        return if (fullIndex >= 0) fullIndex else fallbackVisiblePosition
    }

    inner class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val txtPresetIndex: TextView = itemView.findViewById(R.id.txtPresetIndex)
        private val txtInventoryCount: TextView = itemView.findViewById(R.id.txtInventoryCount)
        private val checkEnabled: CheckBox = itemView.findViewById(R.id.checkPresetEnabled)
        private val editLabel: EditText = itemView.findViewById(R.id.editPresetLabel)
        private val editCommand: EditText = itemView.findViewById(R.id.editPresetCommand)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemovePreset)
        private val btnDrag: DragHandleImageButton = itemView.findViewById(R.id.btnDragPreset)
        private val btnBuyBall: ImageButton = itemView.findViewById(R.id.btnBuyBall)

        private var bindingNow = false

        init {
            editLabel.inputType = InputType.TYPE_CLASS_TEXT
            editCommand.inputType = InputType.TYPE_CLASS_TEXT

            checkEnabled.setOnCheckedChangeListener { _, isChecked ->
                if (bindingNow) return@setOnCheckedChangeListener

                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener

                updatePresetAtVisiblePosition(position) { preset ->
                    preset.copy(enabled = isChecked)
                }
            }

            editLabel.doAfterTextChanged { editable ->
                if (bindingNow) return@doAfterTextChanged

                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@doAfterTextChanged

                updatePresetAtVisiblePosition(position) { preset ->
                    preset.copy(label = editable?.toString().orEmpty())
                }
            }

            editCommand.doAfterTextChanged { editable ->
                if (bindingNow) return@doAfterTextChanged

                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@doAfterTextChanged

                updatePresetAtVisiblePosition(position) { preset ->
                    preset.copy(command = editable?.toString().orEmpty())
                }
            }

            btnRemove.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                onRemoveClicked(position)
            }

            btnDrag.setOnClickListener {
                // Kept for accessibility / performClick path.
            }

            btnDrag.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        onStartDragRequested(this)
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        true
                    }

                    MotionEvent.ACTION_CANCEL -> true
                    else -> false
                }
            }

            btnBuyBall.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val item = visibleItems.getOrNull(position) ?: return@setOnClickListener
                onBuyBallClicked(item)
            }
        }

        fun bindInventoryCount(item: CatchPreset) {
            /*
             * Small partial bind used when only the inventory count changed.
             *
             * This keeps text fields and checkbox state untouched while refreshing the
             * xN counter on the right side of the row.
             */
            txtInventoryCount.text = formatDisplayedCount(item)

            val displayedCount = resolveDisplayedCount(item)
            txtInventoryCount.alpha = if (displayedCount == null || displayedCount > 0) {
                1f
            } else {
                0.5f
            }
        }

        fun bind(item: CatchPreset, visiblePosition: Int) {
            bindingNow = true

            val fullIndex = fullIndexOf(
                item = item,
                fallbackVisiblePosition = visiblePosition
            )

            txtPresetIndex.text = itemView.context.getString(
                R.string.catch_preset_title,
                fullIndex + 1
            )

            txtInventoryCount.text = formatDisplayedCount(item)

            bindInventoryCount(item)

            if (checkEnabled.isChecked != item.enabled) {
                checkEnabled.isChecked = item.enabled
            }

            if (editLabel.text?.toString() != item.label) {
                editLabel.setText(item.label)
            }

            if (editCommand.text?.toString() != item.command) {
                editCommand.setText(item.command)
            }

            bindingNow = false

            val canBuyThisBall = when (item.ballId) {
                CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
                "poke_ball",
                "great_ball",
                "ultra_ball" -> true

                else -> false
            }

            btnBuyBall.visibility = if (canBuyThisBall) View.VISIBLE else View.GONE
        }
    }
}