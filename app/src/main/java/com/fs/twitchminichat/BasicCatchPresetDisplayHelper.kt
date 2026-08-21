package com.fs.twitchminichat

import android.content.Context

/**
 * UI helper for the user "Catch" / basic catch preset.
 *
 * This preset is special because it does not map to one fixed inventory ball.
 * When the user taps it, the actual spending logic prefers:
 *
 * 1) Poké Ball, if available
 * 2) Premier Ball, only when Poké Ball count is zero
 *
 * The quick catch menu must display the same priority, otherwise the user sees
 * a misleading counter. For example, showing poke_ball + premier_ball would hide
 * the moment where the preset switches from Poké Ball to Premier Ball.
 */
object BasicCatchPresetDisplayHelper {

    private const val POKE_BALL_ID = "poke_ball"
    private const val PREMIER_BALL_ID = "premier_ball"

    fun isBasicCatchPreset(preset: CatchPreset): Boolean {
        return preset.ballId == CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC
    }

    /**
     * Returns the count that should be shown near the "Catch" preset.
     *
     * Important:
     * - Do NOT sum Poké Ball + Premier Ball.
     * - Show Poké Ball count while it is above zero.
     * - Only show Premier Ball count once Poké Ball is zero.
     *
     * This mirrors the real consumption priority used when sending the command.
     */
    fun resolveDisplayedCount(
        preset: CatchPreset,
        countsByBallId: Map<String, Int>
    ): Int? {
        if (!isBasicCatchPreset(preset)) return null

        val pokeCount = countsByBallId[POKE_BALL_ID] ?: 0
        val premierCount = countsByBallId[PREMIER_BALL_ID] ?: 0

        return when {
            pokeCount > 0 -> pokeCount
            premierCount > 0 -> premierCount
            else -> 0
        }
    }

    /**
     * Returns the subtitle shown under the "Catch" preset.
     *
     * This deliberately shows only the currently active ball type.
     * We do not show the fallback count here because the goal is clarity:
     *
     * Catch     x12
     * Using Poké Ball
     *
     * then, when Poké Ball reaches zero:
     *
     * Catch     x4
     * Using Premier Ball
     *
     * When a catch-rate recommendation is available, it is appended to the active
     * ball status instead of replacing it.
     */
    fun buildSubtitle(
        context: Context,
        preset: CatchPreset,
        countsByBallId: Map<String, Int>,
        catchRateSubtitle: String?
    ): String? {
        if (!isBasicCatchPreset(preset)) return null

        val pokeCount = countsByBallId[POKE_BALL_ID] ?: 0
        val premierCount = countsByBallId[PREMIER_BALL_ID] ?: 0

        val activeBallSubtitle = when {
            pokeCount > 0 ->
                context.getString(R.string.quick_catch_basic_using_poke_ball)

            premierCount > 0 ->
                context.getString(R.string.quick_catch_basic_using_premier_ball)

            else ->
                return context.getString(R.string.quick_catch_basic_no_basic_balls)
        }

        val cleanCatchRateSubtitle = catchRateSubtitle?.trim().orEmpty()
        if (cleanCatchRateSubtitle.isBlank()) {
            return activeBallSubtitle
        }

        return context.getString(
            R.string.quick_catch_basic_using_ball_with_rate,
            activeBallSubtitle,
            cleanCatchRateSubtitle
        )
    }
}