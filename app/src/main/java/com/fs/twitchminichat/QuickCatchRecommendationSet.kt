package com.fs.twitchminichat

/**
 * Result of the quick catch recommendation step.
 *
 * Smart recommendations and user recommendations must stay separate:
 *
 * - Smart recommendations come from the internal ball catalog and are temporary.
 * - User recommendations decorate the user's saved manual presets.
 */
data class QuickCatchRecommendationSet(
    val visibleSmartRecommendations: List<CatchBallRecommendation>,
    val visibleUserRecommendations: List<CatchBallRecommendation>
)