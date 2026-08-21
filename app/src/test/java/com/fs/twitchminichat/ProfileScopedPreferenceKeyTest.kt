package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileScopedPreferenceKeyTest {

    @Test
    fun distinctProfilesNeverShareOneStorageKey() {
        val first = ProfileScopedPreferenceKey.create("inventory", "profile_a")
        val second = ProfileScopedPreferenceKey.create("inventory", "profile_b")

        assertNotEquals(first, second)
    }

    @Test
    fun profileIdentityIsCanonicalizedBeforeKeyCreation() {
        assertEquals(
            "presets_profile_a",
            ProfileScopedPreferenceKey.create("presets", "  PROFILE_A  ")
        )
    }

    @Test
    fun blankProfileFailsClosed() {
        assertNull(ProfileScopedPreferenceKey.create("presets", "   "))
    }
}
