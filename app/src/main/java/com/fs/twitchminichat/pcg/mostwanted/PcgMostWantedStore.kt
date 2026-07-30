package com.fs.twitchminichat.pcg.mostwanted

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogRepository
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Persists the manual PCG Most Wanted watchlist independently for each profile.
 *
 * The store contains local preferences only. It does not send chat messages,
 * start catches or synchronize with the backend.
 */
class PcgMostWantedStore(
    context: Context,
    private val catalogRepository: PcgPokemonCatalogRepository =
        PcgPokemonCatalogRepository(context)
) {

    /** Application-scoped preferences dedicated to custom watchlists. */
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Reads the validated state for one profile.
     *
     * Persisted names missing from the current catalog are ignored without
     * mutating preferences during a read.
     */
    @Synchronized
    fun getState(profileId: String): Result<PcgMostWantedState> {
        return catalogRepository.load().mapCatching { catalog ->
            readState(profileId, catalog.entries)
        }
    }

    /** Reads the enabled flag without loading or validating catalog names. */
    @Synchronized
    fun isEnabled(profileId: String): Boolean {
        val profileKey = createProfileKey(profileId)
        return preferences.getBoolean(
            enabledPreferenceKey(profileKey),
            false
        )
    }

    /** Updates only the enabled flag from the notification menu. */
    @Synchronized
    fun updateEnabled(
        profileId: String,
        enabled: Boolean
    ) {
        val profileKey = createProfileKey(profileId)
        preferences.edit {
            putBoolean(enabledPreferenceKey(profileKey), enabled)
        }
    }

    /**
     * Enables or disables informative Most Wanted handling and returns the
     * complete validated state.
     */
    @Synchronized
    fun setEnabled(
        profileId: String,
        enabled: Boolean
    ): Result<PcgMostWantedState> {
        return catalogRepository.load().mapCatching { catalog ->
            updateEnabled(profileId, enabled)
            readState(profileId, catalog.entries)
        }
    }

    /**
     * Replaces the complete selection after validating every requested name.
     *
     * This is intended for explicit save actions and future backend imports.
     */
    @Synchronized
    fun replaceSelection(
        profileId: String,
        requestedNames: Collection<String>
    ): Result<PcgMostWantedState> {
        return catalogRepository.load().mapCatching { catalog ->
            val profileKey = createProfileKey(profileId)
            val sanitizedNames = PcgMostWantedSelectionValidator.sanitize(
                catalog.entries,
                requestedNames
            )

            preferences.edit {
                putStringSet(
                    selectionPreferenceKey(profileKey),
                    sanitizedNames.toSet()
                )
            }

            readState(profileId, catalog.entries)
        }
    }

    /**
     * Changes one checkbox selection after resolving its canonical PCG name.
     *
     * One call represents one visible user action and only changes local state.
     */
    @Synchronized
    fun setSelected(
        profileId: String,
        requestedName: String,
        selected: Boolean
    ): Result<PcgMostWantedState> {
        return catalogRepository.load().mapCatching { catalog ->
            val canonicalName =
                PcgMostWantedSelectionValidator.resolveDisplayName(
                    catalog.entries,
                    requestedName
                )
                    ?: throw IllegalArgumentException(
                        "Unknown PCG catalog name: $requestedName"
                    )

            val profileKey = createProfileKey(profileId)
            val currentState = readState(profileId, catalog.entries)
            val updatedNames = currentState.selectedDisplayNames
                .toMutableSet()
                .apply {
                    if (selected) {
                        add(canonicalName)
                    } else {
                        remove(canonicalName)
                    }
                }

            preferences.edit {
                putStringSet(
                    selectionPreferenceKey(profileKey),
                    updatedNames.toSet()
                )
            }

            readState(profileId, catalog.entries)
        }
    }


    /**
     * Replaces enabled state and the complete selection in one transaction.
     *
     * This is the atomic save operation used by the Most Wanted editor.
     */
    @Synchronized
    fun replaceState(
        profileId: String,
        enabled: Boolean,
        requestedNames: Collection<String>
    ): Result<PcgMostWantedState> {
        return catalogRepository.load().mapCatching { catalog ->
            val profileKey = createProfileKey(profileId)
            val sanitizedNames = PcgMostWantedSelectionValidator.sanitize(
                catalog.entries,
                requestedNames
            )

            preferences.edit {
                putBoolean(enabledPreferenceKey(profileKey), enabled)
                putStringSet(
                    selectionPreferenceKey(profileKey),
                    sanitizedNames.toSet()
                )
            }

            readState(profileId, catalog.entries)
        }
    }
    /** Removes the enabled flag and selection belonging to one profile. */
    @Synchronized
    fun clearProfile(profileId: String) {
        val profileKey = createProfileKey(profileId)
        preferences.edit {
            remove(enabledPreferenceKey(profileKey))
            remove(selectionPreferenceKey(profileKey))
        }
    }

    /** Builds one validated state snapshot from preferences and catalog data. */
    private fun readState(
        profileId: String,
        catalogEntries: List<PcgPokemonCatalogEntry>
    ): PcgMostWantedState {
        val profileKey = createProfileKey(profileId)
        val persistedNames = preferences.getStringSet(
            selectionPreferenceKey(profileKey),
            emptySet()
        ).orEmpty()

        return PcgMostWantedState(
            enabled = preferences.getBoolean(
                enabledPreferenceKey(profileKey),
                false
            ),
            selectedDisplayNames =
                PcgMostWantedSelectionValidator.sanitize(
                    catalogEntries,
                    persistedNames
                )
        )
    }

    /** Produces a stable, non-reversible preference key for one profile ID. */
    private fun createProfileKey(profileId: String): String {
        val normalizedProfileId = profileId.trim()
        require(normalizedProfileId.isNotEmpty()) {
            "profileId must not be blank"
        }

        val digest = MessageDigest
            .getInstance(PROFILE_KEY_HASH_ALGORITHM)
            .digest(
                normalizedProfileId.toByteArray(StandardCharsets.UTF_8)
            )

        return digest.joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    /** Preference key containing the enabled flag for one profile. */
    private fun enabledPreferenceKey(profileKey: String): String {
        return "$profileKey.$ENABLED_KEY_SUFFIX"
    }

    /** Preference key containing selected display names for one profile. */
    private fun selectionPreferenceKey(profileKey: String): String {
        return "$profileKey.$SELECTION_KEY_SUFFIX"
    }

    companion object {
        /** Dedicated SharedPreferences file for custom watchlists. */
        private const val PREFERENCES_NAME = "pcg_custom_watchlist"

        /** Secure Hash Algorithm used to derive profile preference keys. */
        private const val PROFILE_KEY_HASH_ALGORITHM = "SHA-256"

        /** Key suffix storing whether one profile watchlist is enabled. */
        private const val ENABLED_KEY_SUFFIX = "enabled"

        /** Key suffix storing selected PCG display names. */
        private const val SELECTION_KEY_SUFFIX = "selected_names"
    }
}