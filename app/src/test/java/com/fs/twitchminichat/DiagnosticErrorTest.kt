package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Verifies that failure diagnostics never retain exception messages. */
class DiagnosticErrorTest {

    @Test
    fun null_failure_has_a_stable_label() {
        assertEquals("none", DiagnosticError.typeOf(null))
    }

    @Test
    fun failure_label_contains_only_the_exception_type() {
        val secretMarker = "oauth-token-must-not-appear"

        val label = DiagnosticError.typeOf(IllegalStateException(secretMarker))

        assertEquals("IllegalStateException", label)
        assertFalse(label.contains(secretMarker))
    }

    @Test
    fun anonymous_failure_uses_a_bounded_fallback() {
        val failure = object : Throwable("private payload") {}

        assertEquals("Throwable", DiagnosticError.typeOf(failure))
    }
}
