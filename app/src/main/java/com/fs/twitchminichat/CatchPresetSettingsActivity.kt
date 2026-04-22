package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CatchPresetSettingsActivity : AppCompatActivity(R.layout.activity_catch_preset_settings) {

    private lateinit var recyclerPresets: RecyclerView
    private lateinit var btnAddPreset: Button
    private lateinit var btnSavePresets: Button

    private lateinit var adapter: CatchPresetEditAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val currentProfileId: String by lazy {
        intent.getStringExtra(EXTRA_PROFILE_ID).orEmpty().trim().lowercase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recyclerPresets = findViewById(R.id.recyclerPresets)
        btnAddPreset = findViewById(R.id.btnAddPreset)
        btnSavePresets = findViewById(R.id.btnSavePresets)

        setupRecycler()
        setupButtons()
    }

    private fun setupRecycler() {
        val savedPresets = CatchPresetStore.loadAll(this)

        val inventoryBalls = if (currentProfileId.isNotBlank()) {
            InventoryBallStore.loadRealSnapshot(this, currentProfileId)
        } else {
            emptyList()
        }

        val mergedPresets = CatchPresetStore.mergeMissingInventoryPresets(
            existing = savedPresets,
            inventoryBalls = inventoryBalls
        )

        val inventoryCounts = if (currentProfileId.isNotBlank()) {
            InventoryBallStore.getDisplayCounts(this, currentProfileId)
        } else {
            emptyMap()
        }

        adapter = CatchPresetEditAdapter(
            initialItems = mergedPresets,
            onRemoveClicked = { position ->
                if (::adapter.isInitialized) {
                    adapter.removeAt(position)
                }
            },
            onStartDragRequested = { viewHolder ->
                if (::itemTouchHelper.isInitialized) {
                    itemTouchHelper.startDrag(viewHolder)
                }
            }
        )

        adapter.updateInventoryCounts(inventoryCounts)

        recyclerPresets.layoutManager = LinearLayoutManager(this)
        recyclerPresets.adapter = adapter

        itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition

                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                        return false
                    }

                    adapter.moveItem(from, to)
                    return true
                }

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    // no-op
                }

                override fun isLongPressDragEnabled(): Boolean = false
            }
        )

        itemTouchHelper.attachToRecyclerView(recyclerPresets)
    }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized && currentProfileId.isNotBlank()) {
            adapter.updateInventoryCounts(
                InventoryBallStore.getDisplayCounts(this, currentProfileId)
            )
        }
    }

    private fun setupButtons() {
        btnAddPreset.setOnClickListener {
            if (adapter.itemCount >= CatchPresetStore.MAX_SAVED_PRESETS) {
                Toast.makeText(
                    this,
                    getString(
                        R.string.catch_preset_limit_reached,
                        CatchPresetStore.MAX_SAVED_PRESETS
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val newPreset = CatchPresetStore.newEmptyPreset(adapter.itemCount)
            adapter.addPreset(newPreset)
            recyclerPresets.smoothScrollToPosition(adapter.itemCount - 1)
        }

        btnSavePresets.setOnClickListener {
            CatchPresetStore.saveAll(this, adapter.currentItems())
            Toast.makeText(
                this,
                getString(R.string.catch_presets_saved),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    companion object {
        private const val EXTRA_PROFILE_ID = "extra_profile_id"

        fun start(context: Context, profileId: String?) {
            context.startActivity(
                Intent(context, CatchPresetSettingsActivity::class.java).apply {
                    putExtra(EXTRA_PROFILE_ID, profileId)
                }
            )
        }
    }
}