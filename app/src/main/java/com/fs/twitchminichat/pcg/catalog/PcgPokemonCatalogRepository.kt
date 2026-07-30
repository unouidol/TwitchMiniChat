package com.fs.twitchminichat.pcg.catalog

import android.content.Context
import org.json.JSONObject

/**
 * Loads the versioned PokÃ©mon Community Game (PCG) catalog from app assets.
 *
 * Loading is exposed as Result so a damaged asset never has to crash the
 * settings screen that will consume the catalog in a later patch.
 */
class PcgPokemonCatalogRepository(context: Context) {

    /** Application context used only to read the bundled catalog asset. */
    private val applicationContext = context.applicationContext

    /** Loads and validates the complete bundled catalog. */
    fun load(): Result<PcgPokemonCatalog> {
        return runCatching {
            val catalogJson = applicationContext.assets
                .open(CATALOG_ASSET_PATH)
                .bufferedReader()
                .use { reader ->
                    reader.readText()
                }

            parseCatalog(catalogJson)
        }
    }

    /** Converts one catalog JSON document into immutable domain models. */
    private fun parseCatalog(json: String): PcgPokemonCatalog {
        val root = JSONObject(json)
        val schemaVersion = root.getInt("schemaVersion")
        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported PCG catalog schema: $schemaVersion"
        }

        val pokemonArray = root.getJSONArray("pokemon")
        val entries = buildList(pokemonArray.length()) {
            for (index in 0 until pokemonArray.length()) {
                val item = pokemonArray.getJSONObject(index)
                add(parseEntry(item))
            }
        }

        require(entries.isNotEmpty()) {
            "The PCG catalog is empty"
        }
        require(entries.map { entry -> entry.displayName }.distinct().size == entries.size) {
            "The PCG catalog contains duplicate display names"
        }

        return PcgPokemonCatalog(
            schemaVersion = schemaVersion,
            catalogVersion = root.getString("catalogVersion"),
            entries = entries
        )
    }

    /** Converts one JSON entry and validates its enum-backed fields. */
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

    companion object {
        /** Catalog schema supported by this application version. */
        private const val SUPPORTED_SCHEMA_VERSION = 1

        /** Asset path of the bundled PCG catalog. */
        private const val CATALOG_ASSET_PATH =
            "pcg_catalog/pcg_pokemon_catalog_v1.json"
    }
}
