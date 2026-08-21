package com.fs.twitchminichat

import org.json.JSONObject

/** Optional Smart Catch statistics and aliases supplied by the legacy type Dex. */
internal data class PokemonTypeDexMetadataEntry(
    val pcgName: String,
    val weightKg: Double?,
    val baseSpeed: Int?,
    val baseHp: Int?,
    val evolvesTwice: Boolean?,
    val aliases: List<String>,
    val mappingKind: String?,
    val locked: Boolean,
    val featured: Boolean
)

/** Parses the legacy Smart Catch metadata without assigning canonical ownership. */
internal object PokemonTypeDexMetadataParser {

    /** Converts the bundled legacy statistics document into neutral metadata rows. */
    fun parse(json: String): List<PokemonTypeDexMetadataEntry> {
        val root = JSONObject(json)
        val items = root.getJSONArray("entries")
        val result = buildList(items.length()) {
            for (index in 0 until items.length()) {
                add(parseEntry(items.getJSONObject(index)))
            }
        }

        val declaredCount = root.optInt("count", -1)
        require(declaredCount < 0 || declaredCount == result.size) {
            "Smart Catch metadata count does not match its entries"
        }

        return result
    }

    private fun parseEntry(item: JSONObject): PokemonTypeDexMetadataEntry {
        val sourceKey = item.optString("sourceKey").trim()
        val displayName = item.optString("displayName").trim()
        val pcgName = item.getString("pcgName").trim()
        require(pcgName.isNotBlank()) {
            "Smart Catch metadata contains a blank PCG name"
        }

        val aliases = buildList {
            add(sourceKey)
            add(displayName)
            add(pcgName)

            val aliasesJson = item.optJSONArray("aliases")
            if (aliasesJson != null) {
                for (index in 0 until aliasesJson.length()) {
                    add(aliasesJson.optString(index).trim())
                }
            }
        }.filter(String::isNotBlank).distinct()

        return PokemonTypeDexMetadataEntry(
            pcgName = pcgName,
            weightKg = item.optionalDouble("weightKg"),
            baseSpeed = item.optionalInt("baseSpeed"),
            baseHp = item.optionalInt("baseHp"),
            evolvesTwice = item.optionalBoolean("evolvesTwice"),
            aliases = aliases,
            mappingKind = item.optString("mappingKind").trim().ifBlank { null },
            locked = item.optBoolean("locked", false),
            featured = item.optBoolean("featured", false)
        )
    }

    private fun JSONObject.optionalDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key).takeIf(Double::isFinite)
    }

    private fun JSONObject.optionalInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun JSONObject.optionalBoolean(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return optBoolean(key)
    }
}
