package com.fs.twitchminichat

import android.content.Context

/**
 * Builds the complete quick catch menu model.
 *
 * This class is the glue between:
 * - enabled user presets;
 * - current spawn;
 * - inventory counts;
 * - Smart/User recommendation logic;
 * - visual menu row construction.
 *
 * Keeping this outside ChatFragment helps the Fragment stay focused on UI:
 * opening the dialog, wiring buttons, handling clicks, and storing view refs.
 */
object QuickCatchMenuModelFactory {

    /**
     * Builds the menu entries and spawn header consumed by the Quick Catch panel.
     *
     * The returned menu list can contain:
     * - section headers;
     * - Smart Preset rows;
     * - User Preset rows.
     *
     * Smart Presets are based on the active spawn and profile-owned collection
     * facts. User Presets are based only on enabled saved user presets.
     */
    fun build(
        context: Context,
        profileId: String?,
        spawn: SpawnSnapshot?,
        nowMs: Long = System.currentTimeMillis()
    ): QuickCatchPanelModel {
        /**
         * Source of the "User presets" section.
         *
         * This intentionally respects enabled=false and does not recreate rows
         * from CatchBallCatalog.
         */
        val userPresetSnapshot = UserCatchPresetSource.loadSnapshot(
            context = context,
            profileId = profileId
        )
        val userPresets = userPresetSnapshot.enabledCommandPresets
        val profileSpawnContext = QuickCatchProfileSpawnContextProvider.load(
            context = context,
            profileId = profileId,
            spawn = spawn
        )

        /**
         * Builds Smart/User recommendation groups.
         *
         * Smart recommendations use the internal catalog.
         * User recommendations only decorate already-enabled user presets.
         */
        val recommendationSet = QuickCatchRecommendationProvider.build(
            context = context,
            profileId = profileId,
            userPresets = userPresets,
            spawn = spawn,
            profileSpawnContext = profileSpawnContext
        )

        /**
         * Inventory counts are profile-scoped.
         *
         * If there is no active profile, rows will show "-" where a ball count
         * would normally be shown.
         */
        val countsByBallId = if (!profileId.isNullOrBlank()) {
            InventoryBallStore.getDisplayCounts(context, profileId)
        } else {
            emptyMap()
        }

        /**
         * Converts data/recommendations into the actual visual menu entries.
         */
        val menuEntries = QuickCatchMenuBuilder.build(
            context = context,
            userPresets = userPresets,
            countsByBallId = countsByBallId,
            profileId = profileId,
            recommendationSet = recommendationSet,
            hasSavedUserPresets = userPresetSnapshot.hasSavedCommandPresets
        )

        return QuickCatchPanelModel(
            menuEntries = menuEntries,
            spawnHeader = QuickCatchSpawnHeaderFormatter.build(
                context = context,
                spawn = spawn,
                lastKnownSpawn = CurrentSpawnStore.loadLastKnown(context),
                profileSpawnContext = profileSpawnContext,
                nowMs = nowMs
            )
        )
    }
}
