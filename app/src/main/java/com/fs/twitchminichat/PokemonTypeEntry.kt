package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier

/**
 * One name of the bundled PCG catalog, as Smart Catch uses it.
 *
 * @property mainDexSpecies the main-Pokédex species this name belongs to, named as
 * tmc-backend's `main_dex_species_table_v1.json` names it ("Marowak" for "Sin Alo Marowak").
 * See [MainDexSpeciesIndex].
 * @property normallySpawnable the catalog's `normallySpawnable` for this exact name.
 */
data class PokemonTypeEntry(
    val sourceKey: String,
    val displayName: String,
    val pcgName: String,
    val tier: PcgPokemonTier,
    val type1: String,
    val type2: String?,
    val weightKg: Double?,
    val baseSpeed: Int?,
    val baseHp: Int?,
    val evolvesTwice: Boolean?,
    val aliases: List<String>,
    val mappingKind: String?,
    val locked: Boolean,
    val featured: Boolean,
    val mainDexSpecies: String,
    val normallySpawnable: Boolean
) {
    // backward-compatible alias for old code
    val key: String
        get() = sourceKey
}
