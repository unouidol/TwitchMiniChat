package com.fs.twitchminichat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the guard that keeps one gate on screen instead of one per resume.
 *
 * The case it protects is real: the gate opens the policy pages in their own
 * activity, so the activity behind it resumes while the terms are still unaccepted.
 */
class TermsGatePolicyTest {

    @Test
    fun notAcceptedAndNothingOnScreen_showsTheGate() {
        assertTrue(
            TermsGatePolicy.shouldShow(accepted = false, alreadyOnScreen = false)
        )
    }

    @Test
    fun notAcceptedButAlreadyOnScreen_doesNotStackASecondGate() {
        assertFalse(
            TermsGatePolicy.shouldShow(accepted = false, alreadyOnScreen = true)
        )
    }

    @Test
    fun accepted_neverShowsTheGate() {
        assertFalse(
            TermsGatePolicy.shouldShow(accepted = true, alreadyOnScreen = false)
        )
        assertFalse(
            TermsGatePolicy.shouldShow(accepted = true, alreadyOnScreen = true)
        )
    }
}
