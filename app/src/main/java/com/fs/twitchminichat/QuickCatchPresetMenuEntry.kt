package com.fs.twitchminichat

/**
 * Visual entry rendered inside the quick catch preset menu.
 *
 * The menu is section-based:
 * - headers separate Smart presets and User presets
 * - preset rows are clickable command shortcuts
 * - empty states explain why a section has no rows
 */
sealed class QuickCatchPresetMenuEntry {

    data class Header(
        val title: String
    ) : QuickCatchPresetMenuEntry()

    data class PresetRow(
        val row: QuickCatchPresetRow
    ) : QuickCatchPresetMenuEntry()

    data class EmptyState(
        val message: String
    ) : QuickCatchPresetMenuEntry()
}