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

class CatchPresetEditAdapter(
    initialItems: List<CatchPreset>,
    private val onRemoveClicked: (Int) -> Unit,
    private val onStartDragRequested: (RecyclerView.ViewHolder) -> Unit,
    private val onBuyBallClicked: (CatchPreset) -> Unit
) : RecyclerView.Adapter<CatchPresetEditAdapter.PresetViewHolder>() {

    private val items = initialItems.toMutableList()
    private var inventoryCountsByBallId: Map<String, Int> = emptyMap()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position].id.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_catch_preset_edit, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(items[position], position)

    }

    override fun getItemCount(): Int = items.size

    fun currentItems(): List<CatchPreset> = items.map { it.copy() }

    fun addPreset(preset: CatchPreset) {
        items.add(preset)
        notifyItemInserted(items.lastIndex)
    }

    fun removeAt(position: Int) {
        if (position !in items.indices) return
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount - position)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        if (fromPosition !in items.indices || toPosition !in items.indices) return

        val moved = items.removeAt(fromPosition)
        items.add(toPosition, moved)

        notifyItemMoved(fromPosition, toPosition)

        val start = minOf(fromPosition, toPosition)
        val count = kotlin.math.abs(fromPosition - toPosition) + 1
        notifyItemRangeChanged(start, count)
    }

    fun updateInventoryCounts(newCounts: Map<String, Int>) {
        inventoryCountsByBallId = newCounts.toMap()
        notifyDataSetChanged()
    }

    private fun resolveDisplayedCount(item: CatchPreset): Int? {
        return when (item.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC -> {
                val poke = inventoryCountsByBallId["poke_ball"] ?: 0
                if (poke > 0) {
                    poke
                } else {
                    inventoryCountsByBallId["premier_ball"]
                }
            }

            null -> null
            else -> inventoryCountsByBallId[item.ballId]
        }
    }

    private fun formatDisplayedCount(item: CatchPreset): String {
        val count = resolveDisplayedCount(item) ?: return "-"
        return "x$count"
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

                items[position] = items[position].copy(enabled = isChecked)
            }

            editLabel.doAfterTextChanged { editable ->
                if (bindingNow) return@doAfterTextChanged

                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@doAfterTextChanged

                items[position] = items[position].copy(
                    label = editable?.toString().orEmpty()
                )
            }

            editCommand.doAfterTextChanged { editable ->
                if (bindingNow) return@doAfterTextChanged

                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@doAfterTextChanged

                items[position] = items[position].copy(
                    command = editable?.toString().orEmpty()
                )
            }

            btnRemove.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                onRemoveClicked(position)
            }

            btnDrag.setOnClickListener {
                // kept for accessibility / performClick path
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

                    MotionEvent.ACTION_CANCEL -> false
                    else -> false
                }
            }
            btnBuyBall.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                onBuyBallClicked(items[position])
            }
        }

        fun bind(item: CatchPreset, position: Int) {
            bindingNow = true

            txtPresetIndex.text = itemView.context.getString(
                R.string.catch_preset_title,
                position + 1
            )

            txtInventoryCount.text = formatDisplayedCount(item)

            val displayedCount = resolveDisplayedCount(item)
            txtInventoryCount.alpha = if (displayedCount == null || displayedCount > 0) {
                1f
            } else {
                0.5f
            }

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
    fun setAllEnabled(enabled: Boolean) {
        for (i in items.indices) {
            items[i] = items[i].copy(enabled = enabled)
        }
        notifyDataSetChanged()
    }
}