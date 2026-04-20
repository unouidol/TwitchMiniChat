package com.fs.twitchminichat

data class CatchPreset(
    val id: String,
    val label: String,
    val command: String,
    val enabled: Boolean = true
)