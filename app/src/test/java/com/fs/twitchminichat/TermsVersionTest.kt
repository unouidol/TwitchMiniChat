package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the terms version that 5.6.0 raises, and that raising it re-shows the gate.
 *
 * Version 3 names GitHub as a recipient. By the criterion recorded on 2026-09-26 a new
 * recipient is a material change, so an acceptance of version 2 must no longer count.
 */
class TermsVersionTest {

    @Test
    fun theCurrentTermsAreVersion3() {
        assertEquals(3, TermsPrefs.CURRENT_TERMS_VERSION)
    }

    /** Every user of 5.5.1 accepted version 2, so each sees the gate once on 5.6.0. */
    @Test
    fun anAcceptanceOfVersion2_noLongerCounts() {
        assertFalse(TermsPrefs.isAccepted(2))
        assertFalse(TermsPrefs.isAccepted(1))
    }

    /** No acceptance stored reads as 0: a fresh install, or after a reset. */
    @Test
    fun noAcceptance_doesNotCount() {
        assertFalse(TermsPrefs.isAccepted(0))
    }

    @Test
    fun anAcceptanceOfVersion3_counts() {
        assertTrue(TermsPrefs.isAccepted(3))
    }
}
