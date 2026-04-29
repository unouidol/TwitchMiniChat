package com.fs.twitchminichat

/**
 * User-selected Pokémon Community Game spawn notification mode.
 *
 * This replaces the old mental model of "push enabled / disabled" with an
 * explicit per-profile preference. The server can then decide which detected
 * spawns are worth notifying for each registered profile.
 */
enum class PcgSpawnAlertMode(
    val id: Int,
    val titleRes: Int,
    val descriptionRes: Int
) {
    DEX_AND_TIER_A(
        id = 0,
        titleRes = R.string.spawn_alert_mode_dex_tier_a,
        descriptionRes = R.string.spawn_alert_mode_dex_tier_a_desc
    ),

    DEX_ONLY(
        id = 1,
        titleRes = R.string.spawn_alert_mode_dex_only,
        descriptionRes = R.string.spawn_alert_mode_dex_only_desc
    ),

    ALL_SPAWNS(
        id = 2,
        titleRes = R.string.spawn_alert_mode_all_spawns,
        descriptionRes = R.string.spawn_alert_mode_all_spawns_desc
    ),

    NONE(
        id = 3,
        titleRes = R.string.spawn_alert_mode_none,
        descriptionRes = R.string.spawn_alert_mode_none_desc
    );

    /**
     * Whether this mode should still keep this profile registered for push delivery.
     *
     * The final spawn filtering is server-side, but this keeps compatibility with
     * the existing enabled/disabled registration model.
     */
    val isPushEnabledForCompatibility: Boolean
        get() = this != NONE

    companion object {
        /**
         * Default mode for existing users and new installs.
         *
         * This matches the safest useful behavior: missing Dex entries plus high-value
         * Tier A spawns, without turning every spawn into a notification.
         */
        val DEFAULT: PcgSpawnAlertMode = DEX_AND_TIER_A

        /**
         * Converts a persisted integer value into a safe enum value.
         */
        fun fromId(id: Int?): PcgSpawnAlertMode {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}