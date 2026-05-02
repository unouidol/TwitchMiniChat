package com.fs.twitchminichat

import org.json.JSONObject

/**
 * Passive state reported by the PCG Pokédex reader.
 *
 * This state only describes what the user currently has open in PCG. It does
 * not mean that TMC changed any filter or tab.
 */
data class PcgPokedexState(
    val onPokedexTab: Boolean,
    val spawnable: Boolean,
    val obtained: Boolean,
    val activeNonSpawnableFilters: List<String>,
    val validForMissingDexUpload: Boolean,
    val reason: String?,
    val capturedAtMs: Long
) {
    companion object {
        /**
         * Parses the lightweight Pokédex state message sent by the page reader.
         */
        fun fromPayload(payload: JSONObject): PcgPokedexState {
            val filters = payload.optJSONObject("filters")
            val activeFiltersJson = filters?.optJSONArray("activeNonSpawnableFilters")

            val activeFilters = buildList {
                if (activeFiltersJson != null) {
                    for (i in 0 until activeFiltersJson.length()) {
                        val value = activeFiltersJson.optString(i).trim()
                        if (value.isNotEmpty()) add(value)
                    }
                }
            }

            return PcgPokedexState(
                onPokedexTab = payload.optBoolean("onPokedexTab", false),
                spawnable = filters?.optBoolean("spawnable", false) == true,
                obtained = filters?.optBoolean("obtained", false) == true,
                activeNonSpawnableFilters = activeFilters,
                validForMissingDexUpload = payload.optBoolean("validForMissingDexUpload", false),
                reason = payload.optString("reason").takeIf { it.isNotBlank() && it != "null" },
                capturedAtMs = payload.optLong("capturedAtMs", 0L)
            )
        }
    }
}