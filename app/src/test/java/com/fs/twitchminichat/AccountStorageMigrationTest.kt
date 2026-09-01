package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for the one-time upgrade from plain-text to encrypted accounts. */
class AccountStorageMigrationTest {

    private val legacyList =
        """[{"id":"a","username":"user","channel":"chan","accessToken":"token","profileId":"p"}]"""

    /** A first launch after the upgrade moves the existing list into the new store. */
    @Test
    fun missingEncryptedStore_migratesLegacyList() {
        assertEquals(
            legacyList,
            AccountStorageMigration.jsonToMigrate(
                current = AccountJsonLookup.Missing,
                legacyJson = legacyList
            )
        )
    }

    /** Surrounding whitespace never reaches the encrypted store. */
    @Test
    fun legacyListIsTrimmed() {
        assertEquals(
            legacyList,
            AccountStorageMigration.jsonToMigrate(
                current = AccountJsonLookup.Missing,
                legacyJson = "  $legacyList\n"
            )
        )
    }

    /** An already migrated installation is never migrated again. */
    @Test
    fun presentEncryptedStore_doesNotMigrate() {
        assertNull(
            AccountStorageMigration.jsonToMigrate(
                current = AccountJsonLookup.Present(legacyList),
                legacyJson = legacyList
            )
        )
    }

    /**
     * An unreadable encrypted store is never overwritten with older accounts.
     *
     * A transient decryption failure must not silently roll the user back to a
     * previous account list.
     */
    @Test
    fun unavailableEncryptedStore_doesNotMigrate() {
        assertNull(
            AccountStorageMigration.jsonToMigrate(
                current = AccountJsonLookup.Unavailable,
                legacyJson = legacyList
            )
        )
    }

    /** An empty legacy list produces no stored file. */
    @Test
    fun emptyLegacyList_doesNotMigrate() {
        assertNull(
            AccountStorageMigration.jsonToMigrate(
                current = AccountJsonLookup.Missing,
                legacyJson = "[]"
            )
        )
    }

    /** A missing or blank legacy file is not an upgrade candidate. */
    @Test
    fun absentLegacyList_doesNotMigrate() {
        assertNull(
            AccountStorageMigration.jsonToMigrate(
                current = AccountJsonLookup.Missing,
                legacyJson = null
            )
        )
        assertNull(
            AccountStorageMigration.jsonToMigrate(
                current = AccountJsonLookup.Missing,
                legacyJson = "   "
            )
        )
    }
}
