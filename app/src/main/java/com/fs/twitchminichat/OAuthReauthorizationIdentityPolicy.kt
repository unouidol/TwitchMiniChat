package com.fs.twitchminichat

/**
 * Validates that an OAuth callback belongs to the existing account being refreshed.
 *
 * Backend profile identifiers are authoritative when both sides provide one. Username
 * matching is retained as a compatibility fallback for older accounts without a stored
 * profile identifier.
 */
internal object OAuthReauthorizationIdentityPolicy {

    /** Returns true only when the callback can safely replace the existing credentials. */
    fun matches(
        expectedUsername: String,
        expectedProfileId: String,
        actualUsername: String,
        actualProfileId: String
    ): Boolean {
        val expectedProfile = normalize(expectedProfileId)
        val actualProfile = normalize(actualProfileId)

        if (expectedProfile.isNotBlank() && actualProfile.isNotBlank()) {
            return expectedProfile == actualProfile
        }

        val expectedUser = normalize(expectedUsername)
        val actualUser = normalize(actualUsername)

        return expectedUser.isNotBlank() && expectedUser == actualUser
    }

    /** Normalizes case-insensitive Twitch and backend identifiers. */
    private fun normalize(value: String): String = value.trim().lowercase()
}
