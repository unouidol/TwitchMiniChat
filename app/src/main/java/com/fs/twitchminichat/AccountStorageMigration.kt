package com.fs.twitchminichat

/**
 * Decides whether the legacy plain-text account list must be re-stored encrypted.
 *
 * Kept pure so the upgrade path can be covered by local unit tests: losing accounts
 * during this one-time migration would log every existing user out.
 */
internal object AccountStorageMigration {

    /**
     * Returns the account list that must be written to the encrypted store, or null
     * when nothing has to be migrated.
     *
     * An unreadable encrypted store never triggers a migration: overwriting it with
     * older data could silently discard newer accounts, and a transient decryption
     * failure must not be turned into a permanent rollback.
     */
    fun jsonToMigrate(
        current: AccountJsonLookup,
        legacyJson: String?
    ): String? {
        if (current !is AccountJsonLookup.Missing) return null

        val normalizedLegacy = legacyJson?.trim().orEmpty()
        if (normalizedLegacy.isEmpty()) return null
        if (normalizedLegacy == EMPTY_ACCOUNT_LIST) return null

        return normalizedLegacy
    }

    /** Serialized form of a stored account list containing no accounts. */
    private const val EMPTY_ACCOUNT_LIST = "[]"
}
