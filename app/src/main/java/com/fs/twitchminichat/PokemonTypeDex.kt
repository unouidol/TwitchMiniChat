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

                val key = obj.optString("key").trim()
                val displayName = obj.optString("displayName").trim()
                val type1 = obj.optString("type1").trim()
                val type2 = obj.optString("type2").trim().ifBlank { null }

                if (key.isBlank() || displayName.isBlank() || type1.isBlank()) continue

                val aliasesJson = obj.optJSONArray("aliases")
                val aliases = mutableListOf<String>()

                aliases += key
                aliases += displayName

                if (aliasesJson != null) {
                    for (j in 0 until aliasesJson.length()) {
                        aliases += aliasesJson.optString(j).trim()
                    }
                }

                val entry = PokemonTypeEntry(
                    key = key,
                    displayName = displayName,
                    type1 = type1,
                    type2 = type2,
                    aliases = aliases.distinct()
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