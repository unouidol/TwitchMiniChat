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

    /**
     * Rejects an implementation that clears the owed-work record on this reset. The
     * record holds server-facing work the user already asked for and the network
     * refused; dropping it here cancels that work silently, and the only symptom is
     * alerts that never stop arriving.
     */
    @Test
    fun keepAccounts_keepsTheOwedWorkRecord() {
        val kept = LocalDataCleaner.keepAccountsExclusions(
            setOf(AccountRepository.LEGACY_PREFERENCES_NAME)
        )

        assertTrue(OwedServerOperationStore.PREFERENCES_NAME in kept)
    }

    /**
     * Rejects an implementation that forgets what the backend last confirmed. Without it
     * an owed disable becomes invisible, and the next start-up cannot tell "already sent"
     * from "never sent" either.
     */
    @Test
    fun keepAccounts_keepsTheAcknowledgedSelections() {
        val kept = LocalDataCleaner.keepAccountsExclusions(
            setOf(AccountRepository.LEGACY_PREFERENCES_NAME)
        )

        assertTrue(PcgProfileAlertAcknowledgementStore.PREFERENCES_NAME in kept)
    }

    @Test
    fun keepAccounts_keepsNothingElse() {
        val kept = LocalDataCleaner.keepAccountsExclusions(
            setOf(" accounts_file ", "")
        )

        assertEquals(
            setOf(
                "accounts_file",
                DeviceCredentialStore.PREFERENCES_NAME,
                OwedServerOperationStore.PREFERENCES_NAME,
                PcgProfileAlertAcknowledgementStore.PREFERENCES_NAME
            ),
            kept
        )
    }
}
