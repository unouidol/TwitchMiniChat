package com.fs.twitchminichat

import android.os.Bundle
import android.text.InputType
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

    companion object {
        private const val ARG_PROFILE_ID = "arg_profile_id"

        fun newInstance(profileId: String?): CatchPresetSettingsBottomSheet {
            return CatchPresetSettingsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROFILE_ID, profileId)
                }
            }
        }
    }

    private val currentProfileId: String
        get() = arguments?.getString(ARG_PROFILE_ID).orEmpty().trim().lowercase()

    private val host: Host?
        get() = parentFragment as? Host

    private lateinit var recyclerPresets: RecyclerView
    private lateinit var btnAddPreset: Button
    private lateinit var btnSavePresets: Button
    private lateinit var checkEnableAllPresets: CheckBox

    private lateinit var adapter: CatchPresetEditAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    showBuyBallDialogFromSheet(preset)
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

        checkEnableAllPresets.isChecked =
            adapter.currentItems().isNotEmpty() &&
                    adapter.currentItems().all { it.enabled }
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

    private fun canBuyFromPreset(preset: CatchPreset): Boolean {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball",
            "great_ball",
            "ultra_ball" -> true
            else -> false
        }
    }

    private fun resolveShopBallNameForPreset(preset: CatchPreset): String? {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball" -> "poke ball"
            "great_ball" -> "great ball"
            "ultra_ball" -> "ultra ball"
            else -> null
        }
    }

    private fun resolveBoughtBallIdForPreset(preset: CatchPreset): String? {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball" -> "poke_ball"
            "great_ball" -> "great_ball"
            "ultra_ball" -> "ultra_ball"
            else -> null
        }
    }

    private fun showBuyBallDialogFromSheet(preset: CatchPreset) {
        if (!canBuyFromPreset(preset)) return

        val shopBallName = resolveShopBallNameForPreset(preset) ?: return
        val boughtBallId = resolveBoughtBallIdForPreset(preset) ?: return

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

                val sent = host?.onCatchPresetBuyRequested(
                    profileId = currentProfileId,
                    ballId = boughtBallId,
                    shopBallName = shopBallName,
                    quantity = quantity,
                    label = preset.label
                ) ?: false

                if (!sent) {
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
}