package com.fs.twitchminichat

enum class CatchPresetMenuSection {
    SMART,
    USER
}

data class CatchPresetMenuItem(
    val id: String,
    val section: CatchPresetMenuSection,
    val title: String,
    val subtitle: String,
    val command: String,
    val editable: Boolean
)