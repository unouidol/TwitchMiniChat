package com.fs.twitchminichat

/**
 * Builds SharedPreferences keys from a canonical PCG profile identifier.
 *
 * Returning null for a blank identity makes profile-scoped stores fail closed
 * instead of accidentally sharing one fallback key.
 */
internal object ProfileScopedPreferenceKey {

    fun create(prefix: String, profileId: String): String? {
        require(prefix.isNotBlank()) { "Preference key prefix must not be blank" }

        val canonicalProfileId = AccountProfileIdResolver.normalize(profileId)
        if (canonicalProfileId.isBlank()) return null

        return "${prefix}_${canonicalProfileId}"
    }
}
