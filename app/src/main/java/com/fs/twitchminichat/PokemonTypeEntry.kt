package com.fs.twitchminichat

data class PokemonTypeEntry(
    val key: String,
    val displayName: String,
    val type1: String,
    val type2: String?,
    val aliases: List<String>
)