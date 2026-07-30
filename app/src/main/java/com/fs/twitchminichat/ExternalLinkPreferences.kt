package com.fs.twitchminichat

import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit

/**
 * Persists device-wide choices for opening links from Twitch chat.
 *
 * The dedicated SharedPreferences file is intentionally separate from account
 * storage, so both choices are removed by every TMC local-data reset.
 */
class ExternalLinkPreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    /** Returns true until the user explicitly disables the external-link warning. */
    fun shouldShowExitWarning(): Boolean {
        return !preferences.getBoolean(KEY_SKIP_EXIT_WARNING, false)
    }

    /** Stores whether the external-link warning should be skipped. */
    fun setSkipExitWarning(skipWarning: Boolean) {
        preferences.edit {
            putBoolean(KEY_SKIP_EXIT_WARNING, skipWarning)
        }
    }

    /** Returns the remembered browser component, if one was selected. */
    fun rememberedBrowser(): ComponentName? {
        val flattenedComponent = preferences
            .getString(KEY_BROWSER_COMPONENT, null)
            ?.trim()
            .orEmpty()

        if (flattenedComponent.isBlank()) return null

        return ComponentName.unflattenFromString(flattenedComponent)
    }

    /** Stores one explicit browser component for future chat links. */
    fun setRememberedBrowser(componentName: ComponentName) {
        preferences.edit {
            putString(
                KEY_BROWSER_COMPONENT,
                componentName.flattenToString()
            )
        }
    }

    /** Removes a remembered browser that is no longer usable. */
    fun clearRememberedBrowser() {
        preferences.edit {
            remove(KEY_BROWSER_COMPONENT)
        }
    }

    /** Restores both external-link choices to their initial state. */
    fun reset() {
        preferences.edit {
            clear()
        }
    }

    companion object {
        /** Dedicated preferences file deleted by LocalDataCleaner. */
        const val PREFERENCES_NAME = "external_link_preferences"

        /** Whether the leave-app warning was disabled by the user. */
        private const val KEY_SKIP_EXIT_WARNING = "skip_exit_warning"

        /** Flattened Android component of the remembered browser. */
        private const val KEY_BROWSER_COMPONENT = "browser_component"
    }
}
