package com.fs.twitchminichat.pcg.mostwanted

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.time.LocalDate
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

    /** Version written to backups for compatibility diagnostics. */
    private var catalogVersion: String = ""

    /** Catalog entries currently shown after search and filters. */
    private var visibleEntries: List<PcgPokemonCatalogEntry> = emptyList()

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

    /** Creates a user-owned text document through Android document storage. */
    private val createBackupDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(TEXT_MIME_TYPE)
    ) { uri ->
        uri?.let(::exportDraftToDocument)
    }

    /** Opens a user-selected text document without broad storage permission. */
    private val openBackupDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(::importBackupDocument)
    }

    /** Top application bar containing title and back navigation. */
    private lateinit var toolbar: MaterialToolbar

    /** Structured text search field for names and catalog metadata. */
    private lateinit var editSearch: AppCompatEditText

    /** Multi-select controls for evolution stages. */
    private lateinit var stageToggleGroup: MaterialButtonToggleGroup

    /** Opens the remaining catalog filter dialog. */
    private lateinit var buttonFilters: AppCompatButton

    /** Selects every catalog entry currently shown by search and filters. */
    private lateinit var buttonSelectShown: Button

    /** Deselects every shown entry without changing hidden selections. */
    private lateinit var buttonDeselectShown: Button

    /** Opens a focused editor for the complete current draft selection. */
    private lateinit var buttonReviewSelected: MaterialButton

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
        buttonSelectShown = findViewById(
            R.id.btnMostWantedSelectShown
        )
        buttonDeselectShown = findViewById(
            R.id.btnMostWantedDeselectShown
        )
        buttonReviewSelected = findViewById(
            R.id.btnMostWantedReviewSelected
        )
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
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionMostWantedBackup -> {
                    showBackupMenu()
                    true
                }
                else -> false
            }
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

        buttonSelectShown.setOnClickListener {
            applyBulkSelection(select = true)
        }

        buttonDeselectShown.setOnClickListener {
            applyBulkSelection(select = false)
        }

        buttonReviewSelected.setOnClickListener {
            showCurrentSelectionReview()
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
                    Triple(
                        catalog.catalogVersion,
                        catalog.entries,
                        state
                    )
                }
            }

            result.onSuccess { (loadedCatalogVersion, entries, state) ->
                catalogVersion = loadedCatalogVersion
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

    /** Opens the two explicit backup actions requested by the user. */
    private fun showBackupMenu() {
        if (
            catalogEntries.isEmpty() ||
            progress.visibility == View.VISIBLE
        ) {
            return
        }

        val content = layoutInflater.inflate(
            R.layout.dialog_pcg_most_wanted_backup,
            null
        )
        val buttonExport = content.findViewById<MaterialButton>(
            R.id.btnMostWantedExportBackup
        )
        val buttonImport = content.findViewById<MaterialButton>(
            R.id.btnMostWantedImportBackup
        )
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.pcg_most_wanted_backup_title)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .create()

        buttonExport.setOnClickListener {
            dialog.dismiss()
            createBackupDocumentLauncher.launch(
                getString(
                    R.string.pcg_most_wanted_backup_default_filename,
                    LocalDate.now().toString()
                )
            )
        }
        buttonImport.setOnClickListener {
            dialog.dismiss()
            openBackupDocumentLauncher.launch(
                arrayOf(TEXT_MIME_TYPE, BINARY_MIME_TYPE)
            )
        }

        dialog.show()
    }

    /** Writes a snapshot of the current editor draft to the selected URI. */
    private fun exportDraftToDocument(uri: Uri) {
        val draftSnapshot = draftSelectedNames.toList()
        val catalogSnapshot = catalogEntries
        val versionSnapshot = catalogVersion
        setBusy(true)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val backupText = PcgMostWantedBackupCodec.encode(
                        catalogVersion = versionSnapshot,
                        catalogEntries = catalogSnapshot,
                        selectedDisplayNames = draftSnapshot
                    )
                    val outputStream = contentResolver.openOutputStream(
                        uri,
                        DOCUMENT_WRITE_MODE
                    ) ?: error("Selected document cannot be opened")

                    PcgMostWantedBackupDocumentIo.writeUtf8(
                        outputStream,
                        backupText
                    )
                }
            }

            result.onSuccess {
                Toast.makeText(
                    this@PcgMostWantedActivity,
                    getString(
                        R.string.pcg_most_wanted_backup_exported,
                        draftSnapshot.size
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    this@PcgMostWantedActivity,
                    R.string.pcg_most_wanted_backup_export_failed,
                    Toast.LENGTH_LONG
                ).show()
            }

            setBusy(false)
        }
    }

    /** Reads and validates a selected document without mutating saved state. */
    private fun importBackupDocument(uri: Uri) {
        val catalogSnapshot = catalogEntries
        setBusy(true)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val inputStream = contentResolver.openInputStream(uri)
                        ?: error("Selected document cannot be opened")
                    val text = inputStream.use(
                        PcgMostWantedBackupDocumentIo::readUtf8
                    )

                    PcgMostWantedBackupCodec.decode(
                        text = text,
                        catalogEntries = catalogSnapshot
                    )
                }
            }

            setBusy(false)

            result.onSuccess { decodeResult ->
                when (decodeResult) {
                    is PcgMostWantedBackupDecodeResult.Success -> {
                        showImportPreview(decodeResult.backup)
                    }
                    is PcgMostWantedBackupDecodeResult.Failure -> {
                        showDecodeFailure(decodeResult.error)
                    }
                }
            }.onFailure(::showDocumentReadFailure)
        }
    }

    /** Shows the complete replacement summary before changing the draft. */
    private fun showImportPreview(backup: PcgMostWantedBackupImport) {
        val sourceCatalogVersion = backup.sourceCatalogVersion
        val messageParts = mutableListOf(
            getString(
                R.string.pcg_most_wanted_backup_import_count,
                backup.selectedDisplayNames.size
            )
        )

        if (backup.duplicateCount > 0) {
            messageParts.add(
                resources.getQuantityString(
                    R.plurals.pcg_most_wanted_backup_duplicate_count,
                    backup.duplicateCount,
                    backup.duplicateCount
                )
            )
        }

        if (
            !sourceCatalogVersion.isNullOrBlank() &&
            sourceCatalogVersion != catalogVersion
        ) {
            messageParts.add(
                getString(
                    R.string.pcg_most_wanted_backup_catalog_changed,
                    sanitizeBackupNameForDisplay(
                        sourceCatalogVersion
                    ),
                    catalogVersion
                )
            )
        }

        messageParts.add(
            getString(R.string.pcg_most_wanted_backup_import_review)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.pcg_most_wanted_backup_import_preview_title)
            .setMessage(messageParts.joinToString(separator = "\n\n"))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                R.string.pcg_most_wanted_backup_review_list
            ) { _, _ ->
                showImportedSelectionReview(
                    backup.selectedDisplayNames
                )
            }
            .show()
    }

    /** Reviews an imported selection before it can replace the editor draft. */
    private fun showImportedSelectionReview(importedNames: Set<String>) {
        PcgMostWantedSelectionReviewDialog(
            context = this,
            catalogEntries = catalogEntries,
            initialSelectedDisplayNames = importedNames,
            titleRes = R.string.pcg_most_wanted_review_import_title,
            onApply = ::applyImportedDraft
        ).show()
    }

    /** Replaces only the in-memory draft; Save remains the commit boundary. */
    private fun applyImportedDraft(importedNames: Set<String>) {
        draftSelectedNames.clear()
        draftSelectedNames.addAll(importedNames)
        updateDirtyState()
        renderFilteredEntries()

        Toast.makeText(
            this,
            if (hasUnsavedChanges) {
                R.string.pcg_most_wanted_backup_loaded_into_draft
            } else {
                R.string.pcg_most_wanted_backup_matches_saved
            },
            Toast.LENGTH_LONG
        ).show()
    }

    /** Maps structural validation failures to resource-backed safe copy. */
    private fun showDecodeFailure(
        failure: PcgMostWantedBackupDecodeFailure
    ) {
        val message = when (failure.reason) {
            PcgMostWantedBackupDecodeError.EMPTY_DOCUMENT -> {
                getString(R.string.pcg_most_wanted_backup_empty)
            }
            PcgMostWantedBackupDecodeError.UNSUPPORTED_FORMAT -> {
                getString(R.string.pcg_most_wanted_backup_unsupported)
            }
            PcgMostWantedBackupDecodeError.TOO_MANY_ENTRIES -> {
                getString(R.string.pcg_most_wanted_backup_too_many_entries)
            }
            PcgMostWantedBackupDecodeError.UNKNOWN_NAMES -> {
                val shownNames = failure.unknownNames
                    .take(MAX_UNKNOWN_NAMES_SHOWN)
                    .map(::sanitizeBackupNameForDisplay)
                    .joinToString(separator = "\n")
                val remainingCount = (
                    failure.unknownNames.size - MAX_UNKNOWN_NAMES_SHOWN
                ).coerceAtLeast(0)
                val nameSummary = if (remainingCount > 0) {
                    getString(
                        R.string.pcg_most_wanted_backup_unknown_names_more,
                        shownNames,
                        remainingCount
                    )
                } else {
                    shownNames
                }

                resources.getQuantityString(
                    R.plurals.pcg_most_wanted_backup_unknown_names,
                    failure.unknownNames.size,
                    failure.unknownNames.size,
                    nameSummary
                )
            }
            PcgMostWantedBackupDecodeError.MISSING_HEADER,
            PcgMostWantedBackupDecodeError.MISSING_FORMAT -> {
                getString(R.string.pcg_most_wanted_backup_invalid)
            }
        }

        showImportError(message)
    }

    /** Maps bounded input failures without displaying raw exception details. */
    private fun showDocumentReadFailure(error: Throwable) {
        val messageRes = when (
            (error as? PcgMostWantedBackupDocumentException)?.reason
        ) {
            PcgMostWantedBackupDocumentError.TOO_LARGE -> {
                R.string.pcg_most_wanted_backup_too_large
            }
            PcgMostWantedBackupDocumentError.INVALID_UTF8 -> {
                R.string.pcg_most_wanted_backup_invalid_utf8
            }
            null -> R.string.pcg_most_wanted_backup_read_failed
        }

        showImportError(getString(messageRes))
    }

    /** Limits untrusted file text before displaying it in an error dialog. */
    private fun sanitizeBackupNameForDisplay(name: String): String {
        return name
            .filterNot { character -> character.isISOControl() }
            .take(MAX_UNKNOWN_NAME_LENGTH)
    }

    /** Displays one non-destructive import error. */
    private fun showImportError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.pcg_most_wanted_backup_import_failed_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /** Applies structured search and every visible filter in catalog order. */
    private fun renderFilteredEntries() {
        if (catalogEntries.isEmpty()) {
            visibleEntries = emptyList()
            updateSelectionCount(visibleCount = 0)
            return
        }

        visibleEntries = PcgMostWantedUiFilter.apply(
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

    /** Applies one explicit bulk action to the currently shown catalog rows. */
    private fun applyBulkSelection(select: Boolean) {
        val shownDisplayNames = visibleEntries.map(
            PcgPokemonCatalogEntry::displayName
        )
        val updatedNames = if (select) {
            PcgMostWantedBulkSelection.selectShown(
                selectedDisplayNames = draftSelectedNames,
                shownDisplayNames = shownDisplayNames
            )
        } else {
            PcgMostWantedBulkSelection.deselectShown(
                selectedDisplayNames = draftSelectedNames,
                shownDisplayNames = shownDisplayNames
            )
        }

        if (updatedNames == draftSelectedNames) return

        draftSelectedNames.clear()
        draftSelectedNames.addAll(updatedNames)
        updateDirtyState()
        renderFilteredEntries()
    }

    /** Opens the complete current selection without applying screen filters. */
    private fun showCurrentSelectionReview() {
        if (
            draftSelectedNames.isEmpty() ||
            catalogEntries.isEmpty() ||
            progress.visibility == View.VISIBLE
        ) {
            return
        }

        PcgMostWantedSelectionReviewDialog(
            context = this,
            catalogEntries = catalogEntries,
            initialSelectedDisplayNames = draftSelectedNames,
            titleRes = R.string.pcg_most_wanted_review_current_title,
            onApply = ::applyReviewedDraft
        ).show()
    }

    /** Applies reviewed names to memory while preserving explicit Save. */
    private fun applyReviewedDraft(reviewedNames: Set<String>) {
        if (reviewedNames == draftSelectedNames) return

        draftSelectedNames.clear()
        draftSelectedNames.addAll(reviewedNames)
        updateDirtyState()
        renderFilteredEntries()

        Toast.makeText(
            this,
            R.string.pcg_most_wanted_review_applied,
            Toast.LENGTH_LONG
        ).show()
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
        buttonReviewSelected.text = getString(
            R.string.pcg_most_wanted_review_selected_count,
            draftSelectedNames.size
        )
        updateBulkSelectionButtons()
        updateReviewSelectionButton()
    }

    /** Enables each bulk action only when it would change the loaded draft. */
    private fun updateBulkSelectionButtons() {
        val busy = progress.visibility == View.VISIBLE
        buttonSelectShown.isEnabled = !busy && visibleEntries.any { entry ->
            entry.displayName !in draftSelectedNames
        }
        buttonDeselectShown.isEnabled = !busy && visibleEntries.any { entry ->
            entry.displayName in draftSelectedNames
        }
    }

    /** Enables review only when a loaded selection is available to inspect. */
    private fun updateReviewSelectionButton() {
        buttonReviewSelected.isEnabled =
            progress.visibility != View.VISIBLE &&
                draftSelectedNames.isNotEmpty()
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
        toolbar.menu.findItem(R.id.actionMostWantedBackup)?.isEnabled =
            !busy && catalogEntries.isNotEmpty()
        editSearch.isEnabled = !busy
        buttonFilters.isEnabled = !busy
        buttonSelectShown.isEnabled = false
        buttonDeselectShown.isEnabled = false
        buttonReviewSelected.isEnabled = false
        buttonBackToAlerts.isEnabled = !busy
        listPokemon.isEnabled = !busy

        for (index in 0 until stageToggleGroup.childCount) {
            stageToggleGroup.getChildAt(index).isEnabled = !busy
        }

        updateSaveButton()
        if (!busy) {
            updateBulkSelectionButtons()
            updateReviewSelectionButton()
        }
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

        /** MIME type used by the Android Storage Access Framework. */
        private const val TEXT_MIME_TYPE = "text/plain"

        /** Fallback accepted for providers that expose .txt as raw bytes. */
        private const val BINARY_MIME_TYPE = "application/octet-stream"

        /** Truncating write mode for a newly created document. */
        private const val DOCUMENT_WRITE_MODE = "wt"

        /** Maximum number of invalid names shown in one error dialog. */
        private const val MAX_UNKNOWN_NAMES_SHOWN = 5

        /** Maximum displayed length for one untrusted backup line. */
        private const val MAX_UNKNOWN_NAME_LENGTH = 80

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
