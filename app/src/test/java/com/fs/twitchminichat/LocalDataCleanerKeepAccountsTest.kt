package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins what the keep-accounts reset leaves in shared preferences.
 *
 * Erasing the device credential there orphaned the phone's server registration:
 * it kept sending alerts while no deletion option could address it any more.
 */
class LocalDataCleanerKeepAccountsTest {

    @Test
    fun keepAccounts_keepsTheDeviceCredential() {
        val kept = LocalDataCleaner.keepAccountsExclusions(
            setOf(AccountRepository.LEGACY_PREFERENCES_NAME)
        )

        assertTrue(DeviceCredentialStore.PREFERENCES_NAME in kept)
    }

    @Test
    fun keepAccounts_keepsTheCallersAccountFiles() {
        val kept = LocalDataCleaner.keepAccountsExclusions(
            setOf(AccountRepository.LEGACY_PREFERENCES_NAME)
        )

        assertTrue(AccountRepository.LEGACY_PREFERENCES_NAME in kept)
    }

    @Test
    fun keepAccounts_keepsNothingElse() {
        val kept = LocalDataCleaner.keepAccountsExclusions(
            setOf(" accounts_file ", "")
        )

        assertEquals(
            setOf("accounts_file", DeviceCredentialStore.PREFERENCES_NAME),
            kept
        )
    }
}
