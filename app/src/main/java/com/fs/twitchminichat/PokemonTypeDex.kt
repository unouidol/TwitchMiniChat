package com.fs.twitchminichat

import android.content.Context
import android.util.Log
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogRepository

/** Resolves Smart Catch metadata from the complete canonical PCG catalog. */
object PokemonTypeDex {

    @Volatile
    private var catalog: PokemonTypeDexCatalog? = null

    /** Resolves a PCG spawn name without allowing ambiguous aliases to guess. */
    fun findByPokemonName(context: Context, rawName: String): PokemonTypeEntry? {
        return ensureLoaded(context).find(rawName)
    }

    private fun ensureLoaded(context: Context): PokemonTypeDexCatalog {
        catalog?.let { return it }

        return synchronized(this) {
            catalog ?: loadCatalog(context.applicationContext).also { loaded ->
                catalog = loaded
            }
        }
    }

    private fun loadCatalog(context: Context): PokemonTypeDexCatalog {
        val pcgCatalog = PcgPokemonCatalogRepository(context)
            .load()
            .getOrThrow()

        val metadataEntries = runCatching {
            val metadataJson = context.assets
                .open(METADATA_ASSET_PATH)
                .bufferedReader()
                .use { reader -> reader.readText() }

            PokemonTypeDexMetadataParser.parse(metadataJson)
        }.getOrElse { error ->
            Log.w(
                LOG_TAG,
                "Optional Smart Catch statistics unavailable " +
                    "errorType=${DiagnosticError.typeOf(error)}"
            )
            emptyList()
        }

        return PokemonTypeDexCatalog.build(
            pcgEntries = pcgCatalog.entries,
            metadataEntries = metadataEntries
        )
    }

    private const val METADATA_ASSET_PATH = "pokemon_type_dex.json"
    private const val LOG_TAG = "SMART_CATCH_CATALOG"
}
