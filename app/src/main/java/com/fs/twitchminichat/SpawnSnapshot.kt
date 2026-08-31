package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier

/** Global, profile-independent metadata for one active PCG spawn. */
data class SpawnSnapshot(
    val rawName: String,
    val dexKey: String?,
    val displayName: String,
    val tier: PcgPokemonTier?,
    val type1: String?,
    val type2: String?,
    val weightKg: Double?,
    val baseSpeed: Int?,
    val baseHp: Int?,
    val evolvesTwice: Boolean?,
    val seenAtMs: Long
)
