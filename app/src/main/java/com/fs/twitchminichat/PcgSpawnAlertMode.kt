package com.fs.twitchminichat

/**
 * User-selected Pokémon Community Game spawn notification mode.
 *
 * This replaces the old "push enabled / disabled" model with an explicit
 * per-profile preference. The backend stores the selected mode and applies the
 * final spawn filtering rules when deciding whether to send an FCM notification.
 */
enum class PcgSpawnAlertMode(
    val id: Int,
    val titleRes: Int
) {
    DEX_AND_TIER_A(
        id = 0,
        titleRes = R.string.spawn_alert_mode_dex_tier_a
    ),

    DEX_ONLY(
        id = 1,
        titleRes = R.string.spawn_alert_mode_dex_only
    ),

    ALL_SPAWNS(
        id = 2,
        titleRes = R.string.spawn_alert_mode_all_spawns
    ),

    NONE(
        id = 3,
        titleRes = R.string.spawn_alert_mode_none
    );

    /**
     * Compatibility bridge for older push-enabled logic.
     *
     * Modes 0, 1, and 2 still mean "this profile can receive spawn pushes".
     * Mode 3 means "disable spawn pushes for this profile".
     */
    val isPushEnabledForCompatibility: Boolean
        get() = this != NONE

    companion object {
        /**
         * Default mode for existing users and new installations.
         *
         * This is the safest useful default: missing Dex entries plus important
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