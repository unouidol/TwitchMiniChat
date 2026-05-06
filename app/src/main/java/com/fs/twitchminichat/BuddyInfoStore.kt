package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/**
 * Local snapshot of the known PCG buddy information for an account.
 *
 * These values are mainly used by the smart catch menu, for example to suggest
 * Friend Ball when the current spawn type matches one of the buddy types.
 */
data class BuddyInfo(
    val rawName: String,
    val level: Int?,
    val avgIv: Int?,
    val primaryType: String?,
    val secondaryType: String?,
    val isKnownPokemon: Boolean,
    val updatedAtMs: Long
)

/**
 * Small local store used to save and read buddy information per profile.
 *
 * This store is intentionally separated from the UI and from ChatFragment, so
 * the buddy data source stays clear and can be reused by different parts of the app.
 */
object BuddyInfoStore {

    private const val PREFS_NAME = "buddy_info_store"

    /**
     * Builds the SharedPreferences key associated with a profile.
     *
     * The profileId is normalized here as well for safety, so future calls with
     * uppercase letters or extra spaces do not create duplicate storage entries.
     */
    private fun key(profileId: String): String {
        return "buddy_${profileId.trim().lowercase()}"
    }

    /**
     * Saves buddy information for the given profile.
     *
     * Optional fields are saved as "JSONObject.NULL" when they are null, so the load
     * path can distinguish between a missing value and a numeric value.
     */
    fun save(context: Context, profileId: String, info: BuddyInfo) {
        val normalized = profileId.trim().lowercase()
        if (normalized.isBlank()) return

        val json = JSONObject()
            .put("rawName", info.rawName)
            .put("level", info.level ?: JSONObject.NULL)
            .put("avgIv", info.avgIv ?: JSONObject.NULL)
            .put("primaryType", info.primaryType ?: "")
            .put("secondaryType", info.secondaryType ?: "")
            .put("isKnownPokemon", info.isKnownPokemon)
            .put("updatedAtMs", info.updatedAtMs)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                /*
                 * Use the KTX SharedPreferences.edit { ... } extension.
                 * By default, it applies changes asynchronously, equivalent to the old
                 * edit().putString(...).apply() chain.
                 */
                putString(key(normalized), json.toString())
            }
    }

    /**
     * Loads the saved buddy information for the given profile.
     *
     * Returns null when no data exists, when the profileId is blank, or when the
     * local JSON is damaged/unreadable.
     */
    fun load(context: Context, profileId: String): BuddyInfo? {
        val normalized = profileId.trim().lowercase()
        if (normalized.isBlank()) return null

        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key(normalized), null)
            ?: return null

        return try {
            val json = JSONObject(raw)

            /*
             * Backward compatibility with older saved data:
             * if rawName does not exist, try to recover pokemonName.
             */
            val rawName = json.optString("rawName").trim()
                .ifBlank { json.optString("pokemonName").trim() }

            val level = if (json.isNull("level")) {
                null
            } else {
                json.optInt("level")
            }

            val avgIv = if (json.isNull("avgIv")) {
                null
            } else {
                json.optInt("avgIv")
            }

            val primaryType = json.optString("primaryType")
                .trim()
                .ifBlank { null }

            val secondaryType = json.optString("secondaryType")
                .trim()
                .ifBlank { null }

            /*
             * If older JSON did not include isKnownPokemon, infer it from whether
             * at least the primaryType is known.
             */
            val isKnownPokemon = json.optBoolean(
                "isKnownPokemon",
                !primaryType.isNullOrBlank()
            )

            val updatedAtMs = json.optLong("updatedAtMs", 0L)

            if (rawName.isBlank()) {
                null
            } else {
                BuddyInfo(
                    rawName = rawName,
                    level = level,
                    avgIv = avgIv,
                    primaryType = primaryType,
                    secondaryType = secondaryType,
                    isKnownPokemon = isKnownPokemon,
                    updatedAtMs = updatedAtMs
                )
            }
        } catch (_: Exception) {
            /*
             * If the local JSON is corrupted, do not crash the app.
             * The next buddy update can simply write a valid snapshot again.
             */
            null
        }
    }

    /**
     * Deletes the saved PCG buddy snapshot for one profile.
     */
    fun clearProfile(context: Context, profileId: String) {
        val normalized = profileId.trim().lowercase()
        if (normalized.isBlank()) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(key(normalized))
            }
    }
}