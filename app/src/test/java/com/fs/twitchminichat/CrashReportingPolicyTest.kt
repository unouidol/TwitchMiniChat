package com.fs.twitchminichat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the crash reporting default.
 *
 * The default decides what leaves every device that has never touched the switch, so
 * it is pinned here: changing it must require changing a test that says so out loud.
 */
class CrashReportingPolicyTest {

    /** An installation that never expressed a choice reports crashes. */
    @Test
    fun noStoredChoice_usesTheDocumentedDefault() {
        assertTrue(CrashReportingPolicy.DEFAULT_ENABLED)
        assertTrue(CrashReportingPolicy.isEnabled(storedChoice = null))
    }

    /** An explicit opt-out is honoured and never overridden by the default. */
    @Test
    fun explicitOptOutIsHonoured() {
        assertFalse(CrashReportingPolicy.isEnabled(storedChoice = false))
    }

    /** An explicit opt-in is honoured. */
    @Test
    fun explicitOptInIsHonoured() {
        assertTrue(CrashReportingPolicy.isEnabled(storedChoice = true))
    }
}
