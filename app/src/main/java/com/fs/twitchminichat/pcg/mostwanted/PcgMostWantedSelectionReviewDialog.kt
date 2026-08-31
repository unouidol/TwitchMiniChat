package com.fs.twitchminichat.pcg.mostwanted

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import com.fs.twitchminichat.R
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry

/**
 * Reviews one fixed Most Wanted selection before replacing the editor draft.
 *
 * Unchecked rows remain in the review universe, so every edit is reversible
 * until Apply is pressed. This dialog never persists or synchronizes data.
 */
class PcgMostWantedSelectionReviewDialog(
    private val context: Context,
    private val catalogEntries: List<PcgPokemonCatalogEntry>,
    private val initialSelectedDisplayNames: Set<String>,
    @StringRes private val titleRes: Int,
    private val onApply: (Set<String>) -> Unit
) {

    /** Inflates, initializes and displays the bounded selection editor. */
    fun show() {
        val view = LayoutInflater.from(context).inflate(
            R.layout.dialog_pcg_most_wanted_selection_review,
            null,
            false
        )
        val editSearch = view.findViewById<AppCompatEditText>(
            R.id.editMostWantedReviewSearch
        )
        val textCount = view.findViewById<TextView>(
            R.id.txtMostWantedReviewCount
        )
        val textEmpty = view.findViewById<TextView>(
            R.id.txtMostWantedReviewEmpty
        )
        val listPokemon = view.findViewById<ListView>(
            R.id.listMostWantedReviewPokemon
        )
        val buttonSelectAll = view.findViewById<Button>(
            R.id.btnMostWantedReviewSelectAll
        )
        val buttonDeselectAll = view.findViewById<Button>(
            R.id.btnMostWantedReviewDeselectAll
        )

        val reviewEntries =
            PcgMostWantedSelectionReview.entriesForSelection(
                catalogEntries = catalogEntries,
                selectedDisplayNames = initialSelectedDisplayNames
            )
        val selectedNames = reviewEntries
            .mapTo(linkedSetOf()) { entry -> entry.displayName }
        var visibleEntries = reviewEntries
        lateinit var adapter: PcgMostWantedListAdapter

        /** Refreshes checkbox state, counts and complete-review actions. */
        fun refreshSelectionState() {
            adapter.updateSelection(selectedNames)
            textCount.text = context.getString(
                R.string.pcg_most_wanted_review_count,
                selectedNames.size,
                visibleEntries.size,
                reviewEntries.size
            )
            buttonSelectAll.isEnabled = reviewEntries.any { entry ->
                entry.displayName !in selectedNames
            }
            buttonDeselectAll.isEnabled = selectedNames.isNotEmpty()
        }

        /** Applies search only to the immutable review universe. */
        fun renderEntries() {
            visibleEntries = PcgMostWantedSelectionReview.filterEntries(
                reviewEntries = reviewEntries,
                searchText = editSearch.text?.toString().orEmpty()
            )
            adapter.submit(
                entries = visibleEntries,
                selectedDisplayNames = selectedNames
            )
            refreshSelectionState()
        }

        adapter = PcgMostWantedListAdapter(
            context = context,
            onSelectionChanged = { entry, selected ->
                if (selected) {
                    selectedNames.add(entry.displayName)
                } else {
                    selectedNames.remove(entry.displayName)
                }
                refreshSelectionState()
            }
        )
        listPokemon.adapter = adapter
        listPokemon.emptyView = textEmpty

        editSearch.doAfterTextChanged {
            renderEntries()
        }
        buttonSelectAll.setOnClickListener {
            selectedNames.addAll(
                reviewEntries.map(PcgPokemonCatalogEntry::displayName)
            )
            refreshSelectionState()
        }
        buttonDeselectAll.setOnClickListener {
            selectedNames.clear()
            refreshSelectionState()
        }

        if (reviewEntries.isEmpty()) {
            textEmpty.setText(R.string.pcg_most_wanted_review_empty)
        }
        renderEntries()

        val dialog = AlertDialog.Builder(context)
            .setTitle(titleRes)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.pcg_most_wanted_review_apply) { _, _ ->
                onApply(
                    PcgMostWantedSelectionReview.selectedNames(
                        reviewEntries = reviewEntries,
                        selectedDisplayNames = selectedNames
                    )
                )
            }
            .create()

        dialog.show()
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            (
                context.resources.displayMetrics.heightPixels *
                    DIALOG_HEIGHT_RATIO
                ).toInt()
        )
    }

    private companion object {

        /** Leaves a small system-bar margin around the review dialog. */
        private const val DIALOG_HEIGHT_RATIO = 0.88
    }
}
