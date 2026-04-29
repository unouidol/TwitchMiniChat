package com.fs.twitchminichat

import android.content.Context

/**
 * Rebuilds the default user preset editor list.
 *
 * This exists outside CatchPresetSettingsBottomSheet so the bottom sheet remains
 * mostly UI glue: buttons, adapter, and user interaction.
 *
 * Restore is intentionally based on the catalog, then filtered with
 * UserCatchPresetEditorFilter so the editor does not get repopulated with all
 * the Smart-preset-style balls.
 */
object CatchPresetDefaultRestorer {

    fun buildRestoredEditorPresets(
        context: Context,
        profileId: String
    ): List<CatchPreset> {
        val baseDefaults = CatchBallCatalog.entries
            .mapIndexed { index, entry ->
                CatchBallCatalog.createDefaultPreset(entry, index)
            }
            .filter { preset ->
                UserCatchPresetEditorFilter.shouldShowInEditor(preset)
            }

        val inventoryBalls = if (profileId.isNotBlank()) {
            InventoryBallStore.loadRealSnapshot(context, profileId)
        } else {
            emptyList()
        }

        /*
         * Keep inventory-discovered balls available after restore, but still run
         * the editor filter afterward so context-driven balls do not flood the
         * preset management screen again.
         */
        return CatchPresetStore.mergeMissingInventoryPresets(
            existing = baseDefaults,
            inventoryBalls = inventoryBalls
        ).filter { preset ->
            UserCatchPresetEditorFilter.shouldShowInEditor(preset)
        }
    }
}