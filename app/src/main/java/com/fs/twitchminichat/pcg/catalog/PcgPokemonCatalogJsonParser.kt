package com.fs.twitchminichat.pcg.catalog

import org.json.JSONObject

/** Pure parser shared by the Android repository and catalog regression tests. */
internal object PcgPokemonCatalogJsonParser {

    /** Converts and validates one versioned canonical PCG catalog document. */
    fun parse(json: String): PcgPokemonCatalog {
        val root = JSONObject(json)
        val schemaVersion = root.getInt("schemaVersion")
        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported PCG catalog schema: $schemaVersion"
        }

        val pokemonArray = root.getJSONArray("pokemon")
        val entries = buildList(pokemonArray.length()) {
            for (index in 0 until pokemonArray.length()) {
                add(parseEntry(pokemonArray.getJSONObject(index)))
            }
        }

        require(entries.isNotEmpty()) {
            "The PCG catalog is empty"
        }
        require(entries.map(PcgPokemonCatalogEntry::displayName).distinct().size == entries.size) {
            "The PCG catalog contains duplicate display names"
        }

        return PcgPokemonCatalog(
            schemaVersion = schemaVersion,
            catalogVersion = root.getString("catalogVersion"),
            entries = entries
        )
    }

    private fun parseEntry(item: JSONObject): PcgPokemonCatalogEntry {
        val typesArray = item.getJSONArray("types")
        val types = buildSet(typesArray.length()) {
            for (index in 0 until typesArray.length()) {
                add(PcgPokemonType.valueOf(typesArray.getString(index)))
            }
        }

        return PcgPokemonCatalogEntry(
            displayName = item.getString("displayName"),
            normalizedName = item.getString("normalizedName"),
            tier = PcgPokemonTier.valueOf(item.getString("tier")),
            normallySpawnable = item.getBoolean("normallySpawnable"),
            starterFamily = item.getBoolean("starterFamily"),
            variantKind = PcgVariantKind.valueOf(item.getString("variantKind")),
            sourceSpecies = item.getString("sourceSpecies"),
            generation = item.getInt("generation"),
            types = types,
            evolutionStage = PcgEvolutionStage.valueOf(
                item.getString("evolutionStage")
            ),
            legendary = item.getBoolean("legendary"),
            mythical = item.getBoolean("mythical")
        )
    }

    private const val SUPPORTED_SCHEMA_VERSION = 1
}
