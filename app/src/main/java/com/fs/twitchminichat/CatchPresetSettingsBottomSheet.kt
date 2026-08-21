package com.fs.twitchminichat

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    private lateinit var editPresetSearch: EditText

    private lateinit var adapter: CatchPresetEditAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    /**
     * Snapshot of the last saved editor state.
     *
     * We compare the current adapter model against this list when the user tries
     * to close the sheet. This means changing something and then changing it back
     * does not trigger a useless unsaved-changes prompt.
     */
    private var savedPresetSnapshot: List<CatchPreset> = emptyList()

    /**
     * Cached dirty state for the editor.
     *
     * The adapter owns the editable model. This flag is only used by the bottom
     * sheet close flow to decide whether to show Save / Discard / Cancel.
     */
    private var hasUnsavedPresetChanges: Boolean = false

    private val currentProfileId: String by lazy {
        requireArguments().getString(ARG_PROFILE_ID).orEmpty().trim().lowercase()
    }

    private val host: Host?
        get() = parentFragment as? Host

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(
            "CATCH_SHEET",
            "onViewCreated hostReady=${host != null} profileSelected=${currentProfileId.isNotBlank()}"
        )

        recyclerPresets = view.findViewById(R.id.recyclerPresets)
        btnAddPreset = view.findViewById(R.id.btnAddPreset)
        btnSavePresets = view.findViewById(R.id.btnSavePresets)
        btnPresetEditorPokebuddy = view.findViewById(R.id.btnPresetEditorPokebuddy)
        btnRestorePresets = view.findViewById(R.id.btnRestorePresets)
        checkEnableAllPresets = view.findViewById(R.id.checkEnableAllPresets)
        editPresetSearch = view.findViewById(R.id.editPresetSearch)

        setupRecycler()
        setupButtons()
        setupSearch()
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

        /*
         * This editor has important unsaved state, so swipe-to-dismiss would be too
         * easy to trigger accidentally. Back is handled below and can show the
         * Save / Discard / Cancel prompt.
         */
        behavior.isDraggable = false
        bottomSheetDialog.setCanceledOnTouchOutside(false)

        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                requestClosePresetEditor()
                true
            } else {
                false
            }
        }
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

        val savedPresets = CatchPresetStore.loadAll(
            context = context,
            profileId = currentProfileId
        )

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
            },
            onPresetChanged = {
                refreshPresetEditorDirtyState()
                refreshToggleAllState()
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

        captureSavedPresetSnapshot()
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
             * This is intentionally user-triggered: the command is requested only when
             * the user taps this button. The actual IRC send and buddy response
             * tracking stay inside ChatFragment.
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

        btnRestorePresets.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_presets_confirm_title)
                .setMessage(R.string.restore_presets_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.restore_presets_confirm_positive) { _, _ ->
                    /*
                     * Restore is a user-confirmed reset of the preset editor list.
                     *
                     * The restored list is saved immediately so it also becomes the new
                     * source of truth for the quick catch menu the next time it opens.
                     */
                    val restoredPresets = CatchPresetDefaultRestorer.buildRestoredEditorPresets(
                        context = requireContext(),
                        profileId = currentProfileId
                    )

                    CatchPresetStore.saveAll(
                        context = requireContext(),
                        profileId = currentProfileId,
                        presets = restoredPresets
                    )

                    setupRecycler()

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.restore_presets_restored),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .show()
        }

        btnSavePresets.setOnClickListener {
            savePresetEditorChangesAndDismiss()
        }
    }

    private fun setupSearch() {
        editPresetSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (!::adapter.isInitialized) return

                    /*
                     * Search is only a visual filter.
                     *
                     * It must not discard presets from the editor model, otherwise
                     * saving after a search would accidentally delete hidden presets.
                     */
                    adapter.setSearchQuery(s?.toString().orEmpty())

                    /*
                     * The enable-all checkbox represents the currently edited model.
                     * Refresh it after filtering so its visual state remains coherent.
                     */
                    refreshToggleAllState()
                }

                override fun afterTextChanged(s: Editable?) = Unit
            }
        )
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

    /**
     * Stores the current adapter model as the last saved state.
     *
     * This is called after loading presets from storage and after saving.
     */
    private fun captureSavedPresetSnapshot() {
        savedPresetSnapshot = if (::adapter.isInitialized) {
            adapter.currentItems()
        } else {
            emptyList()
        }

        refreshPresetEditorDirtyState()
    }

    /**
     * Recomputes whether the current editor state differs from the last saved state.
     */
    private fun refreshPresetEditorDirtyState() {
        hasUnsavedPresetChanges =
            ::adapter.isInitialized && adapter.currentItems() != savedPresetSnapshot
    }

    /**
     * Saves the current editor list to storage and closes the bottom sheet.
     *
     * The editor currently has only "save and leave" flows:
     * - the main Save button
     * - the Save action in the unsaved-changes confirmation popup
     */
    private fun savePresetEditorChangesAndDismiss() {
        if (!::adapter.isInitialized) return

        CatchPresetStore.saveAll(
            context = requireContext(),
            profileId = currentProfileId,
            presets = adapter.currentItems()
        )
        captureSavedPresetSnapshot()

        Toast.makeText(
            requireContext(),
            getString(R.string.catch_presets_saved),
            Toast.LENGTH_SHORT
        ).show()

        dismiss()
    }

    /**
     * Handles any user attempt to close the preset editor.
     *
     * If nothing changed, it closes immediately. If there are unsaved changes,
     * the user can save, discard, or cancel and keep editing.
     */
    private fun requestClosePresetEditor() {
        refreshPresetEditorDirtyState()

        if (!hasUnsavedPresetChanges) {
            dismiss()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.catch_presets_unsaved_title)
            .setMessage(R.string.catch_presets_unsaved_message)
            .setPositiveButton(R.string.catch_presets_unsaved_save) { _, _ ->
                savePresetEditorChangesAndDismiss()
            }
            .setNegativeButton(R.string.catch_presets_unsaved_discard) { _, _ ->
                dismiss()
            }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
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
                    "buyClick handled=$handled quantity=$quantity"
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
