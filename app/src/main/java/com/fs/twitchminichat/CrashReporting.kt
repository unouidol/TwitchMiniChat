package com.fs.twitchminichat

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Decides whether crash reporting is active for one installation.
 *
 * Kept pure so the default is pinned by a test: silently flipping it would change
 * what leaves every user's device, which is exactly the kind of regression that must
 * not pass unnoticed.
 */
internal object CrashReportingPolicy {

    /**
     * State of an installation that has never expressed a choice.
     *
     * Reporting is on by default, disclosed in the terms gate the user must accept
     * before using the application, and switchable at any time in Safety and Privacy.
     */
    const val DEFAULT_ENABLED = true

    /** Returns the effective state for a stored choice, or its absence. */
    fun isEnabled(storedChoice: Boolean?): Boolean = storedChoice ?: DEFAULT_ENABLED
}

/**
 * Lets each distinct failure description through once per process.
 *
 * Handled failures are reported from paths that repeat many times in one session: an
 * unreadable account store is consulted again on every screen that lists accounts.
 * Without this, a single broken installation would produce hundreds of copies of one
 * root cause and bury everything else. What matters when reading the console is how
 * many installations hit a failure, not how many times each one retried.
 */
internal class FailureReportGate {

    private val alreadyReported = mutableSetOf<String>()

    /** Returns true the first time [description] is offered, false afterwards. */
    fun allowsReporting(description: String): Boolean = synchronized(alreadyReported) {
        alreadyReported.add(description)
    }
}

/**
 * Sends crash and error diagnostics, and nothing else.
 *
 * Collection never starts on its own: it is disabled in the manifest and switched on
 * here only after the stored user choice has been read. Reports deliberately carry no
 * account name, channel, profile identifier or message content — only a marker chosen
 * in code and the type of the failure.
 */
object CrashReporting {

    /** Applies the stored user choice. Call once during application start-up. */
    fun applyStoredPreference(context: Context) {
        setCollectionEnabled(isEnabled(context))
    }

    /** Returns whether this installation currently reports crashes. */
    fun isEnabled(context: Context): Boolean {
        val preferences = preferences(context)
        val storedChoice = if (preferences.contains(KEY_ENABLED)) {
            preferences.getBoolean(KEY_ENABLED, CrashReportingPolicy.DEFAULT_ENABLED)
        } else {
            null
        }

        return CrashReportingPolicy.isEnabled(storedChoice)
    }

    /** Stores the user choice and applies it immediately. */
    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit {
            putBoolean(KEY_ENABLED, enabled)
        }

        setCollectionEnabled(enabled)
        Log.d(TAG, "crash reporting enabled=$enabled")
    }

    /**
     * Reports one handled failure that would otherwise be invisible.
     *
     * Intended for the cases where the application knows it expected a result and got
     * none, which produce no crash and would never reach a crash report on their own.
     *
     * [marker] must be built from constants defined in code, never from user data or
     * from a server response; a caller may append fields drawn from a fixed vocabulary
     * it owns. The original error contributes only its type, through [DiagnosticError],
     * so an exception message can never carry content into a report. Each distinct
     * resulting description is reported once per process, see [FailureReportGate].
     */
    fun recordFailure(marker: String, error: Throwable? = null) {
        val normalizedMarker = marker.trim()
        if (normalizedMarker.isEmpty()) return

        val description = if (error == null) {
            normalizedMarker
        } else {
            "$normalizedMarker type=${DiagnosticError.typeOf(error)}"
        }

        if (!reportGate.allowsReporting(description)) return

        runCatching {
            FirebaseCrashlytics.getInstance()
                .recordException(NonFatalFailure(description))

            Log.w(TAG, "handled failure $description")
        }
    }

    /**
     * Switches Crashlytics collection without letting a failure crash the caller.
     *
     * Disabling collection only stops the upload: Crashlytics keeps writing crashes to
     * disk and sends that backlog the moment collection is enabled again. An opt-out
     * that merely flipped the flag would therefore end up delivering exactly the
     * reports the user refused, so the pending ones are discarded here.
     *
     * The order matters, because discarding is a no-op while collection is enabled.
     * This runs on every start-up of an opted-out installation, which is what clears a
     * crash that happened while the switch was off before any later opt-in can send it.
     */
    private fun setCollectionEnabled(enabled: Boolean) {
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.isCrashlyticsCollectionEnabled = enabled

            if (!enabled) {
                crashlytics.deleteUnsentReports()
            }
        }.onFailure { error ->
            Log.w(
                TAG,
                "could not apply crash reporting preference " +
                    "errorType=${DiagnosticError.typeOf(error)}"
            )
        }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Synthetic failure carrying only a marker chosen in code.
     *
     * Using a dedicated type keeps handled failures separate from real crashes in the
     * reporting console, and guarantees the message content is ours.
     */
    private class NonFatalFailure(description: String) : Exception(description)

    /** Keeps one repeating root cause from filling the console with duplicates. */
    private val reportGate = FailureReportGate()

    /** Logcat tag for reporting-state diagnostics. */
    private const val TAG = "CRASH_REPORTING"

    /** Dedicated preferences file deleted by LocalDataCleaner. */
    private const val PREFERENCES_NAME = "crash_reporting_prefs"

    /** Stores the explicit user choice; absence means the default applies. */
    private const val KEY_ENABLED = "crash_reporting_enabled"
}
