package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import org.json.JSONArray
import org.json.JSONObject

/** One manually captured, profile-scoped list of missing spawnable Pokédex entries. */
data class PcgPokedexSnapshot(
    val missingNameKeys: Set<String>,
    val updatedAtMs: Long
)

/**
 * Persists the last Pokédex snapshot explicitly captured for each PCG profile.
 *
 * PCG exposes the missing entries when the Pokédex is filtered to Spawnable only.
 * Keeping that accepted snapshot locally lets Smart Catch distinguish a registered
 * entry from a missing one without consulting another account or fabricating data.
 */
object PcgPokedexSnapshotStore {

    private const val PREFERENCES_NAME = "pcg_pokedex_snapshots"
    private const val SNAPSHOT_KEY_PREFIX = "missing_entries"
    private const val SCHEMA_VERSION = 1

    /** Saves one validated manual capture and preserves an empty completed list. */
    fun saveMissingEntries(
        context: Context,
        profileId: String,
        missingNames: Collection<String>,
        updatedAtMs: Long = System.currentTimeMillis()
    ): Boolean {
        val storageKey = storageKey(profileId) ?: return false
        val missingNameKeys = normalizeNames(missingNames)
        val namesArray = JSONArray()
        missingNameKeys.forEach { nameKey -> namesArray.put(nameKey) }

        val payload = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("updatedAtMs", updatedAtMs.coerceAtLeast(0L))
            .put("missingNameKeys", namesArray)

        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(storageKey, payload.toString())
            }

        return true
    }

    /** Returns null when this profile has never supplied a trustworthy snapshot. */
    fun load(
        context: Context,
        profileId: String
    ): PcgPokedexSnapshot? {
        val storageKey = storageKey(profileId) ?: return null
        val raw = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(storageKey, null)
            ?: return null

        return runCatching {
            val payload = JSONObject(raw)
            if (payload.optInt("schemaVersion", -1) != SCHEMA_VERSION) {
                return@runCatching null
            }

            val namesArray = payload.optJSONArray("missingNameKeys")
                ?: return@runCatching null
            val missingNameKeys = buildSet {
                for (index in 0 until namesArray.length()) {
                    PcgPokemonNameNormalizer.normalize(
                        namesArray.optString(index)
                    ).takeIf(String::isNotEmpty)?.let(::add)
                }
            }

            PcgPokedexSnapshot(
                missingNameKeys = missingNameKeys,
                updatedAtMs = payload.optLong("updatedAtMs", 0L).coerceAtLeast(0L)
            )
        }.getOrNull()
    }

    /** Deletes only the Pokédex snapshot owned by one profile. */
    fun clearProfile(
        context: Context,
        profileId: String
    ) {
        val storageKey = storageKey(profileId) ?: return
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(storageKey)
            }
    }

    private fun storageKey(profileId: String): String? {
        return ProfileScopedPreferenceKey.create(
            prefix = SNAPSHOT_KEY_PREFIX,
            profileId = profileId
        )
    }

    private fun normalizeNames(names: Collection<String>): Set<String> {
        return names
            .asSequence()
            .map(PcgPokemonNameNormalizer::normalize)
            .filter(String::isNotEmpty)
            .toCollection(linkedSetOf())
    }
}
