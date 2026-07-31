package com.fs.twitchminichat

import java.util.Locale

/**
 * Resolves the canonical backend profile identifier for one saved account.
 *
 * Accounts created by the backend use [AccountConfig.profileId] as their source
 * of truth. The username fallback is retained only for accounts saved before
 * backend profile identifiers were persisted.
 */
object AccountProfileIdResolver {

    /**
     * Returns the canonical normalized profile identifier for [account].
     */
    fun resolve(account: AccountConfig): String {
        val explicitProfileId = normalize(account.profileId)
        if (explicitProfileId.isNotBlank()) {
            return explicitProfileId
        }

        return ProfileIdUtil.fromUsername(account.username)
    }

    /**
     * Normalizes one backend profile identifier without deriving a different identity.
     */
    fun normalize(profileId: String): String {
        return profileId.trim().lowercase(Locale.ROOT)
    }
}