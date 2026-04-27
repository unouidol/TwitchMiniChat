package com.fs.twitchminichat

/**
 * Represents one visual entry inside the quick catch menu.
 *
 * We keep section headers separate from real preset rows so Smart Presets can be
 * shown as temporary runtime suggestions without being mixed into the saved user
 * preset list.
 */
sealed class QuickCatchPresetMenuEntry {

    data class Header(
        val title: String
    ) : QuickCatchPresetMenuEntry()

    data class PresetRow(
        val row: QuickCatchPresetRow
    ) : QuickCatchPresetMenuEntry()
}