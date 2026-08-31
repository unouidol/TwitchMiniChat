package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier

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
    val featured: Boolean
) {
    // backward-compatible alias for old code
    val key: String
        get() = sourceKey
}
