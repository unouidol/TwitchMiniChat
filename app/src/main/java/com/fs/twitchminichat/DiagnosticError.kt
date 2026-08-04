package com.fs.twitchminichat

/**
 * Produces bounded failure labels that are safe to include in Logcat.
 *
 * Exception messages and stack traces are intentionally excluded because network,
 * parser, and browser libraries may embed URLs, payload fragments, identifiers, or
 * credentials in them.
 */
internal object DiagnosticError {

    /** Returns only the exception class name, or `none` when no failure is present. */
    fun typeOf(error: Throwable?): String {
        if (error == null) return "none"

        return error.javaClass.simpleName.ifBlank { "Throwable" }
    }
}
