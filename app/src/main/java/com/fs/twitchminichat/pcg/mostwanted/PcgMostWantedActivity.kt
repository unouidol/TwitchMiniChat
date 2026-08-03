package com.fs.twitchminichat.pcg.mostwanted

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.fs.twitchminichat.R
import com.fs.twitchminichat.pcg.catalog.PcgEvolutionStage
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogRepository
import com.fs.twitchminichat.ui.insets.SystemBarsInsetHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Edits one profile-scoped PCG Most Wanted watchlist.
 *
 * The screen only manages a manually selected informative watchlist. It never
 * sends chat messages, triggers catches or starts gameplay actions.
 */
class PcgMostWantedActivity :
    AppCompatActivity(R.layout.activity_pcg_most_wanted) {

    /** Profile receiving the local watchlist configuration. */
    private var profileId: String = ""

    /** Bundled catalog source used to render every selectable PCG name. */
    private val catalogRepository by lazy {
        PcgPokemonCatalogRepository(this)
    }

    /** Local profile-scoped preference store. */
    private val mostWantedStore by lazy {
        PcgMostWantedStore(this)
    }

    /** Efficient recycled adapter for the catalog rows. */
    private lateinit var listAdapter: PcgMostWantedListAdapter

    /** Complete immutable catalog currently loaded by this screen. */
    private var catalogEntries: List<PcgPokemonCatalogEntry> = emptyList()

    /** Last state confirmed in SharedPreferences. */
    private var persistedState = PcgMostWantedState()

    /** Editable selection that is saved only after an explicit user tap. */
    private val draftSelectedNames = linkedSetOf<String>()

    /** Advanced catalog filters selected by the user. */
    private var filterState = PcgMostWantedFilterState()

    /** Indicates whether the current selection differs from persisted state. */
    private var hasUnsavedChanges = false

    /** Keeps Save available after a failed, user-triggered server sync. */
    private var serverSyncPending = false

    /** One-shot backend client used only after an explicit Save tap. */
    private val syncClient by lazy {
        PcgMostWantedSyncClient(this)
    }

    /** Top application bar containing title and back navigation. */
    private lateinit var toolbar: MaterialToolbar

    /** Structured text search field for names and catalog metadata. */
    private lateinit var editSearch: AppCompatEditText

    /** Multi-select controls for evolution stages. */
    private lateinit var stageToggleGroup: MaterialButtonToggleGroup

    /** Opens the remaining catalog filter dialog. */
    private lateinit var buttonFilters: AppCompatButton

    /** Returns to the bell alert menu without saving the current draft. */
    private lateinit var buttonBackToAlerts: Button

    /** Commits the complete manual selection atomically. */
    private lateinit var buttonSave: Button

    /** Displays selected and visible item counts. */
    private lateinit var textSelectionCount: TextView

    /** Displays catalog load failures or an empty filtered result. */
    private lateinit var textEmpty: TextView

    /** Displays catalog entries with checkbox state. */
    private lateinit var listPokemon: ListView

    /** Displays blocking load/save progress. */
    private lateinit var progress: ProgressBar

    /** Initializes navigation, widgets and profile data loading. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profileId = intent.getStringExtra(EXTRA_PROFILE_ID).orEmpty()
        if (profileId.isBlank()) {
            Toast.makeText(
                this,
                R.string.pcg_most_wanted_missing_profile,
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        SystemBarsInsetHelper.enableEdgeToEdgeWithSafePadding(
            window = window,
            rootView = findViewById(R.id.mostWantedRoot)
        )
        bindViews()
        setupInteractions()
        loadProfileState()
    }

    /** Resolves every required view from the AppCompat XML layout. */
    private fun bindViews() {
        toolbar = findViewById(R.id.toolbarMostWanted)
        editSearch = findViewById(R.id.editMostWantedSearch)
        stageToggleGroup = findViewById(R.id.groupMostWantedStages)
        buttonFilters = findViewById(R.id.btnMostWantedFilters)
        buttonBackToAlerts = findViewById(
            R.id.btnBackToMostWantedAlerts
        )
        buttonSave = findViewById(R.id.btnSaveMostWanted)
        textSelectionCount = findViewById(R.id.txtMostWantedSelectionCount)
        textEmpty = findViewById(R.id.txtMostWantedEmpty)
        listPokemon = findViewById(R.id.listMostWantedPokemon)
        progress = findViewById(R.id.progressMostWanted)

        listAdapter = PcgMostWantedListAdapter(
            context = this,
            onSelectionChanged = ::onPokemonSelectionChanged
        )
        listPokemon.adapter = listAdapter
        listPokemon.emptyView = textEmpty
    }

    /** Connects visible user actions without starting any gameplay command. */
    private fun setupInteractions() {
        toolbar.setNavigationOnClickListener {
            requestClose()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                /** Routes system back through the unsaved-draft guard. */
                override fun handleOnBackPressed() {
                    requestClose()
                }
            }
        )

        editSearch.doAfterTextChanged {
            renderFilteredEntries()
        }

        stageToggleGroup.addOnButtonCheckedListener { _, _, _ ->
            updateStageFilterFromButtons()
        }

        buttonFilters.setOnClickListener {
            PcgMostWantedFilterDialog(
                context = this,
                initialState = filterState,
                onApply = { updatedState ->
                    filterState = updatedState
                    updateFilterButton()
                    renderFilteredEntries()
                }
            ).show()
        }

        buttonBackToAlerts.setOnClickListener {
            requestClose()
        }

        buttonSave.setOnClickListener {
            saveDraft()
        }
    }

    /** Loads the catalog and validated profile state away from the main thread. */
    private fun loadProfileState() {
        setBusy(true)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val catalog = catalogRepository.load().getOrThrow()
                    val state = mostWantedStore
                        .getState(profileId)
                        .getOrThrow()
                    catalog.entries to state
                }
            }

            result.onSuccess { (entries, state) ->
                catalogEntries = entries
                persistedState = state
                draftSelectedNames.clear()
                draftSelectedNames.addAll(state.selectedDisplayNames)
                hasUnsavedChanges = false
                serverSyncPending = false
                updateFilterButton()
                renderFilteredEntries()
            }.onFailure {
                textEmpty.setText(R.string.pcg_most_wanted_load_failed)
                Toast.makeText(
                    this@PcgMostWantedActivity,
                    R.string.pcg_most_wanted_load_failed,
                    Toast.LENGTH_LONG
                ).show()
            }

            setBusy(false)
        }
    }

    /** Applies structured search and every visible filter in catalog order. */
    private fun renderFilteredEntries() {
        if (catalogEntries.isEmpty()) {
            updateSelectionCount(visibleCount = 0)
            return
        }

        val visibleEntries = PcgMostWantedUiFilter.apply(
            entries = catalogEntries,
            searchText = editSearch.text?.toString().orEmpty(),
            filterState = filterState,
            selectedDisplayNames = draftSelectedNames
        )

        textEmpty.setText(R.string.pcg_most_wanted_no_results)
        listAdapter.submit(
            entries = visibleEntries,
            selectedDisplayNames = draftSelectedNames
        )
        updateSelectionCount(visibleCount = visibleEntries.size)
    }

    /** Rebuilds the stage set from the four independent toggle buttons. */
    private fun updateStageFilterFromButtons() {
        val checkedIds = stageToggleGroup.checkedButtonIds
        val selectedStages = buildSet {
            if (R.id.btnMostWantedStageBase in checkedIds) {
                add(PcgEvolutionStage.BASE)
            }
            if (R.id.btnMostWantedStageMiddle in checkedIds) {
                add(PcgEvolutionStage.MIDDLE)
            }
            if (R.id.btnMostWantedStageFinal in checkedIds) {
                add(PcgEvolutionStage.FINAL)
            }
            if (R.id.btnMostWantedStageSingle in checkedIds) {
                add(PcgEvolutionStage.SINGLE)
            }
        }

        filterState = filterState.copy(evolutionStages = selectedStages)
        updateFilterButton()
        renderFilteredEntries()
    }

    /**
     * Updates one in-memory checkbox after a direct user tap.
     *
     * Persistence remains behind the explicit Save button.
     */
    private fun onPokemonSelectionChanged(
        entry: PcgPokemonCatalogEntry,
        selected: Boolean
    ) {
        if (selected) {
            draftSelectedNames.add(entry.displayName)
        } else {
            draftSelectedNames.remove(entry.displayName)
        }

        updateDirtyState()

        if (filterState.selectedOnly) {
            renderFilteredEntries()
        } else {
            listAdapter.updateSelection(draftSelectedNames)
            updateSelectionCount(listAdapter.count)
        }
    }

    /**
     * Saves the selection and performs one sync for the explicit user tap.
     *
     * The enabled flag remains controlled by the bell menu. A failed server
     * request is never retried automatically: Save stays available so the
     * user can request one new attempt manually.
     */
    private fun saveDraft() {
        if (!hasUnsavedChanges && !serverSyncPending) return

        setBusy(true)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                mostWantedStore.replaceState(
                    profileId = profileId,
                    enabled = mostWantedStore.isEnabled(profileId),
                    requestedNames = draftSelectedNames
                ).mapCatching { savedState ->
                    savedState to syncClient.sync(
                        profileId = profileId,
                        state = savedState
                    )
                }
            }

            result.onSuccess { (savedState, syncResult) ->
                persistedState = savedState
                draftSelectedNames.clear()
                draftSelectedNames.addAll(savedState.selectedDisplayNames)
                hasUnsavedChanges = false
                serverSyncPending = !syncResult.ok
                updateSaveButton()
                renderFilteredEntries()

                Toast.makeText(
                    this@PcgMostWantedActivity,
                    if (syncResult.ok) {
                        R.string.pcg_most_wanted_saved_and_synced
                    } else {
                        R.string.pcg_most_wanted_saved_sync_pending
                    },
                    if (syncResult.ok) {
                        Toast.LENGTH_SHORT
                    } else {
                        Toast.LENGTH_LONG
                    }
                ).show()
            }.onFailure {
                Toast.makeText(
                    this@PcgMostWantedActivity,
                    R.string.pcg_most_wanted_save_failed,
                    Toast.LENGTH_LONG
                ).show()
            }

            setBusy(false)
        }
    }

    /** Recomputes whether the selected names differ from persisted data. */
    private fun updateDirtyState() {
        hasUnsavedChanges =
            draftSelectedNames != persistedState.selectedDisplayNames
        updateSaveButton()
    }

    /** Updates the filter button label with active filter-group count. */
    private fun updateFilterButton() {
        val activeCount = filterState.activeFilterCount()
        buttonFilters.text = if (activeCount == 0) {
            getString(R.string.pcg_most_wanted_filters)
        } else {
            getString(
                R.string.pcg_most_wanted_filters_active,
                activeCount
            )
        }
    }

    /** Updates selected and visible counts using one localized format string. */
    private fun updateSelectionCount(visibleCount: Int) {
        textSelectionCount.text = getString(
            R.string.pcg_most_wanted_selection_count,
            draftSelectedNames.size,
            visibleCount
        )
    }

    /** Enables the Save action only for a loaded, changed, non-busy draft. */
    private fun updateSaveButton() {
        buttonSave.isEnabled =
            (hasUnsavedChanges || serverSyncPending) &&
                progress.visibility != View.VISIBLE
    }

    /** Applies one blocking progress state to controls that mutate the draft. */
    private fun setBusy(busy: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        editSearch.isEnabled = !busy
        buttonFilters.isEnabled = !busy
        buttonBackToAlerts.isEnabled = !busy
        listPokemon.isEnabled = !busy

        for (index in 0 until stageToggleGroup.childCount) {
            stageToggleGroup.getChildAt(index).isEnabled = !busy
        }

        updateSaveButton()
    }

    /**
     * Returns to the bell menu immediately or asks before discarding changes.
     *
     * Navigation never saves or synchronizes the draft implicitly.
     */
    private fun requestClose() {
        if (!hasUnsavedChanges) {
            finishAndReturnToAlertMenu()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.pcg_most_wanted_unsaved_title)
            .setMessage(R.string.pcg_most_wanted_unsaved_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.pcg_most_wanted_discard) { _, _ ->
                finishAndReturnToAlertMenu()
            }
            .show()
    }

    /** Finishes with an explicit request to reopen the bell alert menu. */
    private fun finishAndReturnToAlertMenu() {
        setResult(
            android.app.Activity.RESULT_OK,
            Intent().putExtra(EXTRA_RETURN_TO_ALERT_MENU, true)
        )
        finish()
    }

    /** Intent factory for opening one profile-scoped watchlist editor. */
    companion object {

        /** Intent extra containing the local application profile ID. */
        private const val EXTRA_PROFILE_ID = "profile_id"

        /** Result flag requesting that the caller reopen the bell menu. */
        private const val EXTRA_RETURN_TO_ALERT_MENU =
            "return_to_alert_menu"

        /** Creates the profile-scoped editor Intent for an Activity launcher. */
        fun createIntent(context: Context, profileId: String): Intent {
            return Intent(
                context,
                PcgMostWantedActivity::class.java
            ).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
        }

        /** Opens Most Wanted when no navigation result is required. */
        fun start(context: Context, profileId: String) {
            context.startActivity(createIntent(context, profileId))
        }

        /** Checks whether an Activity result requests the bell alert menu. */
        fun shouldReturnToAlertMenu(
            resultCode: Int,
            data: Intent?
        ): Boolean {
            return resultCode == android.app.Activity.RESULT_OK &&
                data?.getBooleanExtra(
                    EXTRA_RETURN_TO_ALERT_MENU,
                    false
                ) == true
        }
    }
}
