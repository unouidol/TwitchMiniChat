package com.fs.twitchminichat

import android.content.Context

/**
 * Builds the recommendation data used by the quick catch menu.
 *
 * This class keeps the important separation between:
 *
 * - Smart recommendations:
 *   generated from the internal CatchBallCatalog, so they keep working even if
 *   the user deletes all saved/default presets.
 *
 * - User recommendations:
 *   generated from the presets currently shown in the user/manual section.
 *
 * ChatFragment should not need to know which balls are core, hidden, or scored.
 * It should only provide the current profile/spawn/user preset state.
 */
object QuickCatchRecommendationProvider {

    fun build(
        context: Context,
        profileId: String?,
        userPresets: List<CatchPreset>,
        spawn: SpawnSnapshot?
    ): QuickCatchRecommendationSet {
        val buddy = if (!profileId.isNullOrBlank()) {
            BuddyInfoStore.load(context, profileId)
        } else {
            null
        }

        val smartRecommendations = if (spawn != null) {
            CatchBallRecommender.recommend(
                presets = CatchBallCatalog.createSmartCandidatePresets(),
                spawn = spawn,
                buddy = buddy
            )
        } else {
            emptyList()
        }

        val userRecommendations = CatchBallRecommender.recommend(
            presets = userPresets,
            spawn = spawn,
            buddy = buddy
        )

        return QuickCatchRecommendationSet(
            visibleSmartRecommendations = visibleSmartRecommendations(smartRecommendations),
            visibleUserRecommendations = visibleUserRecommendations(userRecommendations)
        )
    }

    /**
     * Filters recommendations for the quick catch menu.
     *
     * A row is visible when:
     * - it has a positive recommendation score;
     * - or it is one of the core standard catch presets.
     *
     * Some special balls can still be hidden from the quick menu by
     * CatchPresetBallHelper.shouldHideFromQuickMenu(...).
     */
    /**
     * Filters the automatic Smart Presets section.
     *
     * Smart Presets should only show meaningful spawn-based suggestions.
     *
     * We intentionally exclude the basic always-good balls:
     * - Poke / basic auto catch
     * - Great
     * - Ultra
     *
     * Those can still appear under User Presets if the user enables them manually.
     */
    private fun visibleSmartRecommendations(
        recommendations: List<CatchBallRecommendation>
    ): List<CatchBallRecommendation> {
        return recommendations
            .filter { recommendation ->
                recommendation.score > 0
            }
            .filterNot { recommendation ->
                isExcludedFromSmartPresets(recommendation.preset)
            }
            .filterNot { recommendation ->
                CatchPresetBallHelper.shouldHideFromQuickMenu(recommendation.preset)
            }
    }

    /**
     * Filters recommendation data used to decorate User Presets.
     *
     * User Preset rows are built from the user's enabled preset list, not from this
     * filter. This filter only decides whether a user row gets a recommendation
     * subtitle/reason.
     */
    private fun visibleUserRecommendations(
        recommendations: List<CatchBallRecommendation>
    ): List<CatchBallRecommendation> {
        return recommendations
            .filter { recommendation ->
                recommendation.score > 0 ||
                        CatchPresetBallHelper.isCoreStandardPreset(recommendation.preset)
            }
            .filterNot { recommendation ->
                CatchPresetBallHelper.shouldHideFromQuickMenu(recommendation.preset)
            }
    }

    /**
     * Balls that should not appear as automatic Smart Presets.
     *
     * They are generic fallback balls, not interesting spawn-specific suggestions.
     * If the user wants them in the menu, they can enable them as User Presets.
     */
    private fun isExcludedFromSmartPresets(preset: CatchPreset): Boolean {
        val ballId = CatchPresetBallHelper.effectiveBallId(preset) ?: return false

        return ballId == CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC ||
                ballId == "poke_ball" ||
                ballId == "great_ball" ||
                ballId == "ultra_ball"
    }
}