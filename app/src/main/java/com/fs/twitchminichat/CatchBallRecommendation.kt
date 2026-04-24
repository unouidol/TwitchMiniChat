package com.fs.twitchminichat

data class CatchBallRecommendation(
    val preset: CatchPreset,
    val score: Int,
    val reasonKeys: List<String>,
    val originalIndex: Int
)