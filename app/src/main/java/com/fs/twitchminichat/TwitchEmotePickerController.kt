package com.fs.twitchminichat

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import androidx.core.view.isVisible

/**
 * Coordinates the Twitch emote panel, filtering and composer insertion.
 *
 * Selecting an emote only edits local composer text. It never sends or queues a
 * Twitch chat message; sending remains a separate explicit user action.
 */
class TwitchEmotePickerController(
    private val panel: View,
    private val recyclerView: RecyclerView,
    private val searchInput: EditText,
    private val emptyText: TextView,
    private val toggleButton: ImageButton,
    private val closeButton: ImageButton,
    private val composer: EditText,
    requestManager: RequestManager,
    private val accountId: String,
    private val recentStore: TwitchEmoteRecentStore,
    private val catalogProvider: () -> TwitchEmoteCatalog,
    private val onBeforeOpen: () -> Unit,
    private val onAfterClose: (keepInputMethod: Boolean) -> Unit
) {
    private val adapter = TwitchEmotePickerAdapter(
        requestManager = requestManager,
        onEntryClicked = ::insertEntry
    )

    /** Account-and-channel catalog currently displayed by the picker. */
    private var currentCatalog: TwitchEmoteCatalog = TwitchEmoteCatalog.EMPTY

    /** Recent emote identifiers with stable slots; unseen emotes enter first. */
    private var recentEmoteIds: List<String> = recentStore.load(accountId)

    /** Watches the local search box without performing network requests. */
    private val searchWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            text: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) = Unit

        override fun onTextChanged(
            text: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            applyFilter()
        }

        override fun afterTextChanged(text: Editable?) = Unit
    }

    /** True while the integrated emote panel is visible. */
    val isOpen: Boolean
        get() = panel.isVisible

    init {
        val gridLayoutManager = GridLayoutManager(
            recyclerView.context,
            GRID_COLUMN_COUNT
        )

        /*
         * Section headers occupy the complete grid row while emotes continue to
         * occupy one of the six regular cells.
         */
        gridLayoutManager.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return adapter.spanSizeAt(
                        position = position,
                        spanCount = GRID_COLUMN_COUNT
                    )
                }
            }

        recyclerView.layoutManager = gridLayoutManager
        recyclerView.setHasFixedSize(true)

        searchInput.addTextChangedListener(searchWatcher)

        toggleButton.setOnClickListener {
            if (isOpen) {
                close()
            } else {
                open()
            }
        }

        closeButton.setOnClickListener {
            close()
        }
    }

    /** Opens the panel using the latest catalog and recent-emote history. */
    fun open() {
        recentEmoteIds = recentStore.load(accountId)
        submitCatalog(catalogProvider())
        onBeforeOpen()

        panel.visibility = View.VISIBLE
        toggleButton.isSelected = true
        toggleButton.contentDescription = toggleButton.context.getString(
            R.string.emote_picker_close
        )

        if (recyclerView.adapter == null) {
            recyclerView.adapter = adapter
        }

        applyFilter()
    }

    /**
     * Closes the panel and optionally keeps the Input Method Editor transition alive.
     */
    fun close(keepInputMethod: Boolean = false) {
        if (!isOpen) return

        recyclerView.adapter = null
        searchInput.clearFocus()
        panel.visibility = View.GONE
        toggleButton.isSelected = false
        toggleButton.contentDescription = toggleButton.context.getString(
            R.string.emote_picker_open
        )

        onAfterClose(keepInputMethod)
    }

    /** Closes the picker while focus is moving directly to the chat composer. */
    fun closeForComposerTouch() {
        close(keepInputMethod = true)
    }

    /** Closes an open picker and reports whether Back was consumed. */
    fun closeIfOpen(): Boolean {
        if (!isOpen) return false

        close()
        return true
    }

    /** Accepts a new account-and-channel catalog from the catalog controller. */
    fun submitCatalog(catalog: TwitchEmoteCatalog) {
        currentCatalog = catalog
        applyFilter()
    }

    /** Removes listeners and all visible Glide-backed cells. */
    fun release() {
        recyclerView.adapter = null
        searchInput.removeTextChangedListener(searchWatcher)
        toggleButton.setOnClickListener(null)
        closeButton.setOnClickListener(null)
        panel.visibility = View.GONE
    }

    /** Inserts one selected emote and records it without moving an existing slot. */
    private fun insertEntry(entry: TwitchEmoteCatalogEntry) {
        val currentText = composer.text?.toString().orEmpty()
        val selectionStart = composer.selectionStart
            .takeIf { position -> position >= 0 }
            ?: currentText.length
        val selectionEnd = composer.selectionEnd
            .takeIf { position -> position >= 0 }
            ?: selectionStart

        val insertion = TwitchEmoteTextInserter.insert(
            currentText = currentText,
            selectionStart = selectionStart,
            selectionEnd = selectionEnd,
            emoteName = entry.name
        )

        composer.setText(insertion.text)
        composer.setSelection(
            insertion.cursorPosition.coerceIn(0, insertion.text.length)
        )

        /*
         * A picker tap records a new local favorite. Existing favorites keep their
         * visual slots, and no Twitch chat command is sent or retried here.
         */
        val updatedRecentEmoteIds = recentStore.record(
            accountId = accountId,
            emoteId = entry.id
        )

        /*
         * Rebuilding an unchanged list is unnecessary and could disturb the
         * RecyclerView while the user repeatedly taps the same favorite.
         */
        if (updatedRecentEmoteIds != recentEmoteIds) {
            recentEmoteIds = updatedRecentEmoteIds
            applyFilter(preserveScrollPosition = true)
        }
    }

    /**
     * Applies the current query and rebuilds the ordered picker sections.
     *
     * When requested, the first visible item keeps the same screen offset while
     * the recent-emote section grows above the underlying catalog.
     */
    private fun applyFilter(
        preserveScrollPosition: Boolean = false
    ) {
        val visibleScrollAnchor = if (preserveScrollPosition) {
            captureVisibleScrollAnchor()
        } else {
            null
        }

        val query = searchInput.text?.toString()?.trim().orEmpty()

        val sections = TwitchEmotePickerSectionBuilder.build(
            catalog = currentCatalog,
            recentEmoteIds = recentEmoteIds,
            query = query
        )

        adapter.submitSections(sections)
        restoreVisibleScrollAnchor(visibleScrollAnchor)

        val visibleEntryCount = sections.sumOf { section ->
            section.entries.size
        }
        val isEmpty = visibleEntryCount == 0

        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (isEmpty) {
            val hasCatalogEntries = currentCatalog.entries.any { entry ->
                entry.id.isNotBlank() && entry.name.isNotBlank()
            }

            emptyText.setText(
                if (!hasCatalogEntries) {
                    R.string.emote_picker_empty
                } else {
                    R.string.emote_picker_no_results
                }
            )
        }
    }

    /**
     * Captures the first visible adapter item and its offset from the grid start.
     */
    private fun captureVisibleScrollAnchor(): VisibleScrollAnchor? {
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager
            ?: return null
        val position = layoutManager.findFirstVisibleItemPosition()

        if (position == RecyclerView.NO_POSITION) {
            return null
        }

        val itemView = layoutManager.findViewByPosition(position)
            ?: return null
        val itemAnchor = adapter.scrollAnchorAt(position)
            ?: return null

        return VisibleScrollAnchor(
            itemAnchor = itemAnchor,
            offsetFromStartPx = itemView.top - recyclerView.paddingTop
        )
    }

    /**
     * Restores a captured item after the adapter sections have been rebuilt.
     */
    private fun restoreVisibleScrollAnchor(
        visibleScrollAnchor: VisibleScrollAnchor?
    ) {
        if (visibleScrollAnchor == null) return

        val layoutManager = recyclerView.layoutManager as? GridLayoutManager
            ?: return
        val position = adapter.positionOfScrollAnchor(
            visibleScrollAnchor.itemAnchor
        )

        if (position == RecyclerView.NO_POSITION) {
            return
        }

        layoutManager.scrollToPositionWithOffset(
            position,
            visibleScrollAnchor.offsetFromStartPx
        )
    }

    /** Visible picker position retained across one adapter update. */
    private data class VisibleScrollAnchor(
        val itemAnchor: TwitchEmotePickerAdapter.ScrollAnchor,
        val offsetFromStartPx: Int
    )

    companion object {
        /** Number of emote cells displayed on each picker row. */
        private const val GRID_COLUMN_COUNT = 6
    }
}