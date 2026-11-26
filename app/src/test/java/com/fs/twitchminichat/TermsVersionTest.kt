package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the terms version that 5.7.0 raises, and that raising it re-shows the gate.
 *
 * Version 4 says that the server receives the list of forms missing from an account's Alt Dex
 * while Missing Alt Dex form alerts are on: data newly collected, so by the criterion recorded
 * on 2026-09-26 an acceptance of version 3 must no longer count. Version 3 named GitHub as a
 * recipient, by the same criterion.
 */
class TermsVersionTest {

    @Test
    fun theCurrentTermsAreVersion4() {
        assertEquals(4, TermsPrefs.CURRENT_TERMS_VERSION)
    }

    /** Every user of 5.6.x accepted version 3, so each sees the gate once on 5.7.0. */
    @Test
    fun anAcceptanceOfVersion3_noLongerCounts() {
        assertFalse(TermsPrefs.isAccepted(3))
        assertFalse(TermsPrefs.isAccepted(2))
        assertFalse(TermsPrefs.isAccepted(1))
    }

    /** No acceptance stored reads as 0: a fresh install, or after a reset. */
    @Test
    fun noAcceptance_doesNotCount() {
        assertFalse(TermsPrefs.isAccepted(0))
    }

    @Test
    fun anAcceptanceOfVersion4_counts() {
        assertTrue(TermsPrefs.isAccepted(4))
    }
}
