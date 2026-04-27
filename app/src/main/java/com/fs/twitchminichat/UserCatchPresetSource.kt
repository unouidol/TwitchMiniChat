package com.fs.twitchminichat

import android.content.Context

/**
 * Provides the saved user presets that are allowed to appear in the quick catch
 * menu under the "User presets" section.
 *
 * This class intentionally does NOT use CatchBallCatalog.
 *
 * Reason:
 * User Presets should reflect what the user configured in the preset settings.
 * If the user disables a preset, it must not be recreated from the catalog and
 * shown again in the quick menu.
 */
object UserCatchPresetSource {

    /**
     * Loads only user-enabled catch presets.
     *
     * These are the rows that should appear in the "User presets" section.
     *
     * Filtering rules:
     * - enabled must be true;
     * - command must not be blank, because a preset without a command cannot
     *   safely be sent to chat.
     */
    fun loadEnabled(context: Context): List<CatchPreset> {
        return CatchPresetStore.loadAll(context)
            .filter { preset -> preset.enabled }
            .filter { preset -> preset.command.isNotBlank() }
    }
}