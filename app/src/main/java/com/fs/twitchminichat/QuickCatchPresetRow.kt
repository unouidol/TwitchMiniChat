package com.fs.twitchminichat

data class QuickCatchPresetRow(
    val preset: CatchPreset,
    val label: String,
    val countText: String,
    val subtitle: String? = null,
    val showBuyButton: Boolean,
    val showBuddyButton: Boolean
)