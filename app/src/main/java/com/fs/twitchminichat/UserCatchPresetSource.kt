package com.fs.twitchminichat

import android.content.Context

/**
 * Snapshot of the user preset state used by the quick catch menu.
 *
 * The quick menu needs to distinguish between:
 * - no saved presets at all
 * - saved presets exist, but none are enabled
 * - enabled presets are available
 */
data class UserCatchPresetSnapshot(
    val savedCommandPresets: List<CatchPreset>,
    val enabledCommandPresets: List<CatchPreset>
) {
    val hasSavedCommandPresets: Boolean
        get() = savedCommandPresets.isNotEmpty()
}

/**
 * Loads user-created catch presets for quick menu presentation.
 *
 * This source deliberately does not build visual rows. It only exposes the
 * saved/enabled preset state so QuickCatchMenuBuilder can decide what message
 * should be shown in the User presets section.
 */
object UserCatchPresetSource {

    fun loadSnapshot(context: Context): UserCatchPresetSnapshot {
        val savedCommandPresets = CatchPresetStore.loadAll(context)
            .filter { preset -> preset.command.isNotBlank() }

        val enabledCommandPresets = savedCommandPresets
            .filter { preset -> preset.enabled }

        return UserCatchPresetSnapshot(
            savedCommandPresets = savedCommandPresets,
            enabledCommandPresets = enabledCommandPresets
        )
    }

}