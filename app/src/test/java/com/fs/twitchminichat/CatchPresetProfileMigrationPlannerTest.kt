package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

class CatchPresetProfileMigrationPlannerTest {

    @Test
    fun legacySnapshotIsCopiedToEveryExistingProfile() {
        val keys = CatchPresetProfileMigrationPlanner.missingTargetKeys(
            profileIds = listOf("profile_a", "profile_b"),
            existingKeys = emptySet(),
            keyPrefix = "presets_json"
        )

        assertEquals(
            listOf("presets_json_profile_a", "presets_json_profile_b"),
            keys
        )
    }

    @Test
    fun existingProfileDataIsNeverOverwritten() {
        val keys = CatchPresetProfileMigrationPlanner.missingTargetKeys(
            profileIds = listOf("profile_a", "profile_b"),
            existingKeys = setOf("presets_json_profile_a"),
            keyPrefix = "presets_json"
        )

        assertEquals(listOf("presets_json_profile_b"), keys)
    }

    @Test
    fun duplicateAndBlankProfilesDoNotCreateSharedTargets() {
        val keys = CatchPresetProfileMigrationPlanner.missingTargetKeys(
            profileIds = listOf("PROFILE_A", " profile_a ", ""),
            existingKeys = emptySet(),
            keyPrefix = "presets_json"
        )

        assertEquals(listOf("presets_json_profile_a"), keys)
    }
}
