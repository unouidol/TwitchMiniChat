package com.fs.twitchminichat

import android.content.Context
import org.json.JSONObject

object PokemonTypeDex {

    @Volatile
    private var loaded = false

    private val byAlias = LinkedHashMap<String, PokemonTypeEntry>()

    fun findByPokemonName(context: Context, rawName: String): PokemonTypeEntry? {
        ensureLoaded(context)
        val normalized = PokemonNameNormalizer.normalize(rawName)
        if (normalized.isBlank()) return null
        return byAlias[normalized]
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return

        synchronized(this) {
            if (loaded) return

            val jsonText = context.assets
                .open("pokemon_type_dex.json")
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(jsonText)
            val entries = root.getJSONArray("entries")

            byAlias.clear()

            for (i in 0 until entries.length()) {
                val obj = entries.getJSONObject(i)

                val sourceKey = obj.optString("sourceKey")
                    .trim()
                    .ifBlank { obj.optString("key").trim() }

                val displayName = obj.optString("displayName").trim()
                val pcgName = obj.optString("pcgName")
                    .trim()
                    .ifBlank { displayName }

                val type1 = obj.optString("type1").trim()
                val type2 = obj.optString("type2").trim().ifBlank { null }

                val weightKg = when {
                    obj.has("weightKg") && !obj.isNull("weightKg") -> obj.optDouble("weightKg")
                    else -> null
                }

                val baseSpeed = when {
                    obj.has("baseSpeed") && !obj.isNull("baseSpeed") -> obj.optInt("baseSpeed")
                    else -> null
                }

                val baseHp = when {
                    obj.has("baseHp") && !obj.isNull("baseHp") -> obj.optInt("baseHp")
                    else -> null
                }

                val evolvesTwice = when {
                    obj.has("evolvesTwice") && !obj.isNull("evolvesTwice") -> obj.optBoolean("evolvesTwice")
                    else -> null
                }

                val mappingKind = obj.optString("mappingKind").trim().ifBlank { null }
                val locked = obj.optBoolean("locked", false)
                val featured = obj.optBoolean("featured", false)

                if (sourceKey.isBlank() || displayName.isBlank() || pcgName.isBlank() || type1.isBlank()) {
                    continue
                }

                val aliasesJson = obj.optJSONArray("aliases")
                val aliases = mutableListOf<String>()

                aliases += sourceKey
                aliases += displayName
                aliases += pcgName

                if (aliasesJson != null) {
                    for (j in 0 until aliasesJson.length()) {
                        aliases += aliasesJson.optString(j).trim()
                    }
                }

                val entry = PokemonTypeEntry(
                    sourceKey = sourceKey,
                    displayName = displayName,
                    pcgName = pcgName,
                    type1 = type1,
                    type2 = type2,
                    weightKg = weightKg,
                    baseSpeed = baseSpeed,
                    baseHp = baseHp,
                    evolvesTwice = evolvesTwice,
                    aliases = aliases.distinct(),
                    mappingKind = mappingKind,
                    locked = locked,
                    featured = featured
                )

                for (alias in entry.aliases) {
                    val normalized = PokemonNameNormalizer.normalize(alias)
                    if (normalized.isNotBlank()) {
                        byAlias[normalized] = entry
                    }
                }
            }

            loaded = true
        }
    }
}