package com.fs.twitchminichat

import android.content.Context
import org.json.JSONObject

data class BuddyInfo(
    val rawName: String,
    val level: Int?,
    val avgIv: Int?,
    val primaryType: String?,
    val secondaryType: String?,
    val isKnownPokemon: Boolean,
    val updatedAtMs: Long
)

object BuddyInfoStore {

    private const val PREFS_NAME = "buddy_info_store"

    private fun key(profileId: String): String {
        return "buddy_${profileId.trim().lowercase()}"
    }

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
            .edit()
            .putString(key(normalized), json.toString())
            .apply()
    }

    fun load(context: Context, profileId: String): BuddyInfo? {
        val normalized = profileId.trim().lowercase()
        if (normalized.isBlank()) return null

        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key(normalized), null)
            ?: return null

        return try {
            val json = JSONObject(raw)

            // backward compatibility: se avevi il vecchio campo pokemonName, lo riusa
            val rawName = json.optString("rawName").trim()
                .ifBlank { json.optString("pokemonName").trim() }

            val level = if (json.isNull("level")) null else json.optInt("level")
            val avgIv = if (json.isNull("avgIv")) null else json.optInt("avgIv")
            val primaryType = json.optString("primaryType").trim().ifBlank { null }
            val secondaryType = json.optString("secondaryType").trim().ifBlank { null }
            val isKnownPokemon = json.optBoolean("isKnownPokemon", !primaryType.isNullOrBlank())
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
            null
        }
    }

    fun clear(context: Context, profileId: String) {
        val normalized = profileId.trim().lowercase()
        if (normalized.isBlank()) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key(normalized))
            .apply()
    }
}