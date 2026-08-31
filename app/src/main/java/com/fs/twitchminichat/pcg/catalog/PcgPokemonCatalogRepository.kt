package com.fs.twitchminichat.pcg.catalog

import android.content.Context

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

            PcgPokemonCatalogJsonParser.parse(catalogJson)
        }
    }

    companion object {
        /** Asset path of the bundled PCG catalog. */
        private const val CATALOG_ASSET_PATH =
            "pcg_catalog/pcg_pokemon_catalog_v1.json"
    }
}
