package com.fs.twitchminichat

data class SpawnSnapshot(
    val rawName: String,
    val dexKey: String?,
    val displayName: String,
    val type1: String?,
    val type2: String?,
    val weightKg: Double?,
    val baseSpeed: Int?,
    val baseHp: Int?,
    val evolvesTwice: Boolean?,
    val seenAtMs: Long,
    val isAlreadyCaught: Boolean? = null
)