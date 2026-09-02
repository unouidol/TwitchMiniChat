package com.fs.twitchminichat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the duplicate suppression applied to handled failures.
 *
 * One broken installation can hit the same failing path hundreds of times in a
 * session, so the suppression is what keeps a real problem visible in the console
 * instead of buried under copies of itself.
 */
class FailureReportGateTest {

    /** The first occurrence of a description is always reported. */
    @Test
    fun firstOccurrenceIsReported() {
        assertTrue(FailureReportGate().allowsReporting("account_store_read_unavailable"))
    }

    /** Repeats of the same description are suppressed. */
    @Test
    fun repeatedOccurrencesAreSuppressed() {
        val gate = FailureReportGate()

        assertTrue(gate.allowsReporting("account_store_read_unavailable"))
        assertFalse(gate.allowsReporting("account_store_read_unavailable"))
        assertFalse(gate.allowsReporting("account_store_read_unavailable"))
    }

    /** A different cause is still reported, even after another one was suppressed. */
    @Test
    fun distinctDescriptionsAreReportedIndependently() {
        val gate = FailureReportGate()

        assertTrue(gate.allowsReporting("account_store_read_unavailable reason=decrypt_failed"))
        assertFalse(gate.allowsReporting("account_store_read_unavailable reason=decrypt_failed"))
        assertTrue(gate.allowsReporting("account_store_read_unavailable reason=file_shape"))
        assertTrue(gate.allowsReporting("account_store_write_failed reason=encrypt_failed"))
    }
}
