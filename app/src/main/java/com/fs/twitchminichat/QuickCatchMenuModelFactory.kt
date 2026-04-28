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
     * Builds the list consumed by QuickCatchPresetMenuAdapter.
     *
     * The returned list can contain:
     * - section headers;
     * - Smart Preset rows;
     * - User Preset rows.
     *
     * Smart Presets are based on the active spawn.
     * User Presets are based only on enabled saved user presets.
     */
    fun build(
        context: Context,
        profileId: String?,
        spawn: SpawnSnapshot?
    ): List<QuickCatchPresetMenuEntry> {
        /**
         * Source of the "User presets" section.
         *
         * This intentionally respects enabled=false and does not recreate rows
         * from CatchBallCatalog.
         */
        val userPresetSnapshot = UserCatchPresetSource.loadSnapshot(context)
        val userPresets = userPresetSnapshot.enabledCommandPresets

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
            spawn = spawn
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
        return QuickCatchMenuBuilder.build(
            context = context,
            userPresets = userPresets,
            countsByBallId = countsByBallId,
            profileId = profileId,
            recommendationSet = recommendationSet,
            hasSavedUserPresets = userPresetSnapshot.hasSavedCommandPresets
        )
    }
}