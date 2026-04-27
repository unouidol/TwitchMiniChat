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
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

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

        /**
         * Requests buddy info through the chat.
         *
         * The bottom sheet does not send IRC messages directly. It only reports the
         * user tap to the host fragment, because ChatFragment already owns the real
         * chat connection state and the pending buddy response tracking.
         */
        fun onCatchPresetBuddyInfoRequested(): Boolean
    }

    private lateinit var recyclerPresets: RecyclerView
    private lateinit var btnAddPreset: Button
    private lateinit var btnSavePresets: Button

    private lateinit var btnPresetEditorPokebuddy: Button
    private lateinit var btnRestorePresets: Button
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
        btnPresetEditorPokebuddy = view.findViewById(R.id.btnPresetEditorPokebuddy)
        btnRestorePresets = view.findViewById(R.id.btnRestorePresets)
        checkEnableAllPresets = view.findViewById(R.id.checkEnableAllPresets)

        setupRecycler()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()

        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        /*
         * This screen manages a long list of presets, so a half-height bottom sheet
         * makes the Save button hard to reach.
         *
         * Expanding the Material bottom sheet to full height gives us a real
         * settings-like screen while keeping the existing BottomSheetDialogFragment
         * architecture.
         */
        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
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

        /*
 * The editor should stay focused on manual/generic presets.
 *
 * Context-driven balls are still available through Smart presets, so they do
 * not need to fill this management list.
 */
        val visibleEditorPresets = mergedPresets.filter { preset ->
            UserCatchPresetEditorFilter.shouldShowInEditor(preset)
        }

        val inventoryCounts = if (currentProfileId.isNotBlank()) {
            InventoryBallStore.getDisplayCounts(context, currentProfileId)
        } else {
            emptyMap()
        }

        adapter = CatchPresetEditAdapter(
            initialItems = visibleEditorPresets,
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

        btnPresetEditorPokebuddy.setOnClickListener {
            /*
             * Manual PCG helper action.
             *
             * The bottom sheet does not send !pokebuddy directly because ChatFragment
             * already has a complete buddy request flow:
             * - sends the chat command
             * - stores the pending profile/user
             * - waits for the buddy response
             * - updates the local buddy info
             */
            val handled = host?.onCatchPresetBuddyInfoRequested() == true

            if (!handled) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.connection_not_ready),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnPresetEditorPokebuddy.setOnClickListener {
            /*
             * Manual PCG helper action.
             *
             * This is intentionally user-triggered: the command is requested only when
             * the user taps this button. The actual IRC send and buddy response tracking
             * stay inside ChatFragment.
             */
            val handled = host?.onCatchPresetBuddyInfoRequested() == true

            if (!handled) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.connection_not_ready),
                    Toast.LENGTH_SHORT
                ).show()
            }
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