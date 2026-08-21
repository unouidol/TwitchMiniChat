package com.fs.twitchminichat

/**
 * Plans non-destructive legacy preset copies for existing profiles.
 *
 * The planner is pure so profile separation and no-overwrite behavior can be
 * covered by local unit tests without Android SharedPreferences.
 */
internal object CatchPresetProfileMigrationPlanner {

    fun missingTargetKeys(
        profileIds: Iterable<String>,
        existingKeys: Set<String>,
        keyPrefix: String
    ): List<String> {
        return profileIds
            .mapNotNull { profileId ->
                ProfileScopedPreferenceKey.create(
                    prefix = keyPrefix,
                    profileId = profileId
                )
            }
            .distinct()
            .filterNot(existingKeys::contains)
    }
}
