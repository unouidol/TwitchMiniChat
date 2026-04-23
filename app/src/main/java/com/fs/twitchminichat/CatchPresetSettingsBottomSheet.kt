package com.fs.twitchminichat

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CatchPresetSettingsBottomSheet :
    BottomSheetDialogFragment(R.layout.activity_catch_preset_settings) {

    interface Host {
        fun onCatchPresetBuyRequested(
            profileId: String,
            ballId: String,
            shopBallName: String,
            quantity: Int,
            label: String
        ): Boolean
    }

    private lateinit var recyclerPresets: RecyclerView
    private lateinit var btnAddPreset: Button
    private lateinit var btnSavePresets: Button
    private lateinit var checkEnableAllPresets: CheckBox

    private lateinit var adapter: CatchPresetEditAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val currentProfileId: String by lazy {
        requireArguments().getString(ARG_PROFILE_ID).orEmpty().trim().lowercase()
    }

    private val host: Host?
        get() = parentFragment as? Host

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(
            "CATCH_SHEET",
            "onViewCreated sheet=${System.identityHashCode(this)} " +
                    "parentFragment=${parentFragment?.let { System.identityHashCode(it) }} " +
                    "hostReady=${host != null} " +
                    "profileId=$currentProfileId"
        )

        recyclerPresets = view.findViewById(R.id.recyclerPresets)
        btnAddPreset = view.findViewById(R.id.btnAddPreset)
        btnSavePresets = view.findViewById(R.id.btnSavePresets)
        checkEnableAllPresets = view.findViewById(R.id.checkEnableAllPresets)

        setupRecycler()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized && currentProfileId.isNotBlank()) {
            adapter.updateInventoryCounts(
                InventoryBallStore.getDisplayCounts(requireContext(), currentProfileId)
            )
        }
    }

    private fun setupRecycler() {
        val context = requireContext()

        val savedPresets = CatchPresetStore.loadAll(context)

        val inventoryBalls = if (currentProfileId.isNotBlank()) {
            InventoryBallStore.loadRealSnapshot(context, currentProfileId)
        } else {
            emptyList()
        }

        val mergedPresets = CatchPresetStore.mergeMissingInventoryPresets(
            existing = savedPresets,
            inventoryBalls = inventoryBalls
        )

        val inventoryCounts = if (currentProfileId.isNotBlank()) {
            InventoryBallStore.getDisplayCounts(context, currentProfileId)
        } else {
            emptyMap()
        }

        adapter = CatchPresetEditAdapter(
            initialItems = mergedPresets,
            onRemoveClicked = { position ->
                if (::adapter.isInitialized) {
                    adapter.removeAt(position)
                    refreshToggleAllState()
                }
            },
            onStartDragRequested = { viewHolder ->
                if (::itemTouchHelper.isInitialized) {
                    itemTouchHelper.startDrag(viewHolder)
                }
            },
            onBuyBallClicked = { preset ->
                if (currentProfileId.isBlank()) {
                    Toast.makeText(
                        context,
                        getString(R.string.missing_active_profile),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showBuyBallDialogFromSettings(preset)
                }
            }
        )

        adapter.updateInventoryCounts(inventoryCounts)

        recyclerPresets.layoutManager = LinearLayoutManager(context)
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

        refreshToggleAllState()
    }

    private fun setupButtons() {
        checkEnableAllPresets.setOnCheckedChangeListener { _, isChecked ->
            if (::adapter.isInitialized) {
                adapter.setAllEnabled(isChecked)
            }
        }

        btnAddPreset.setOnClickListener {
            if (adapter.itemCount >= CatchPresetStore.MAX_SAVED_PRESETS) {
                Toast.makeText(
                    requireContext(),
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
            refreshToggleAllState()
        }

        btnSavePresets.setOnClickListener {
            CatchPresetStore.saveAll(requireContext(), adapter.currentItems())
            Toast.makeText(
                requireContext(),
                getString(R.string.catch_presets_saved),
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }
    }

    private fun refreshToggleAllState() {
        if (!::adapter.isInitialized || !::checkEnableAllPresets.isInitialized) return

        checkEnableAllPresets.setOnCheckedChangeListener(null)

        checkEnableAllPresets.isChecked =
            adapter.currentItems().isNotEmpty() &&
                    adapter.currentItems().all { it.enabled }

        checkEnableAllPresets.setOnCheckedChangeListener { _, isChecked ->
            if (::adapter.isInitialized) {
                adapter.setAllEnabled(isChecked)
            }
        }
    }


        private fun showBuyBallDialogFromSettings(preset: CatchPreset) {
        if (!CatchPresetBallHelper.canBuyFromPreset(preset)) return

        val shopBallName = CatchPresetBallHelper.resolveShopBallNameForPreset(preset) ?: return
        val boughtBallId = CatchPresetBallHelper.resolveBoughtBallIdForPreset(preset) ?: return

        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.buy_ball_quantity_hint)
            setText("1")
            setSelection(text.length)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.buy_ball_title, preset.label))
            .setMessage(getString(R.string.buy_ball_message))
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.buy_ball_confirm) { _, _ ->
                val quantity = input.text?.toString()?.trim()?.toIntOrNull()
                if (quantity == null || quantity <= 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_quantity),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val handled = host?.onCatchPresetBuyRequested(
                    profileId = currentProfileId,
                    ballId = boughtBallId,
                    shopBallName = shopBallName,
                    quantity = quantity,
                    label = preset.label
                ) == true

                Log.d(
                    "CATCH_SHEET",
                    "buyClick sheet=${System.identityHashCode(this)} " +
                            "parentFragment=${parentFragment?.let { System.identityHashCode(it) }} " +
                            "handled=$handled profileId=$currentProfileId " +
                            "ballId=$boughtBallId quantity=$quantity"
                )

                if (!handled) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.connection_not_ready),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                adapter.updateInventoryCounts(
                    InventoryBallStore.getDisplayCounts(requireContext(), currentProfileId)
                )
            }
            .show()
    }

    companion object {
        const val TAG = "catch_preset_settings_sheet"
        private const val ARG_PROFILE_ID = "arg_profile_id"

        fun newInstance(profileId: String?): CatchPresetSettingsBottomSheet {
            return CatchPresetSettingsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROFILE_ID, profileId)
                }
            }
        }
    }
}