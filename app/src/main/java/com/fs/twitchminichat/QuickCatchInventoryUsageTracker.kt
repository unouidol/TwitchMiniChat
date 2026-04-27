package com.fs.twitchminichat

import android.content.Context

/**
 * Handles optimistic local inventory updates after the user uses a catch preset.
 *
 * This is intentionally outside ChatFragment because it is not UI rendering
 * logic. The Fragment only knows that the user tapped a preset; this class knows
 * how that preset affects the local ball inventory estimate.
 */
object QuickCatchInventoryUsageTracker {

    /**
     * Decrements the local inventory estimate for the ball used by a catch preset.
     *
     * This is optimistic because we update local counts immediately after sending
     * the command, before PCG confirms the result through chat.
     */
    fun notePresetUsedOptimistically(
        context: Context,
        profileId: String,
        preset: CatchPreset
    ) {
        val countsByBallId = InventoryBallStore.getDisplayCounts(
            context = context,
            profileId = profileId
        )

        val spentBallId = resolveBallIdToSpendForPreset(
            preset = preset,
            countsByBallId = countsByBallId
        ) ?: return

        InventoryBallStore.noteBallUsed(
            context = context,
            profileId = profileId,
            ballId = spentBallId
        )
    }

    /**
     * Resolves which inventory ball should be spent by this preset.
     *
     * Most presets spend their own explicit ballId.
     *
     * The special "auto/basic catch" preset is different:
     * - it sends the basic catch command;
     * - PCG can consume a normal Poké Ball first;
     * - if no Poké Ball is available, it can fall back to Premier Ball.
     */
    private fun resolveBallIdToSpendForPreset(
        preset: CatchPreset,
        countsByBallId: Map<String, Int>
    ): String? {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC -> {
                val poke = countsByBallId["poke_ball"] ?: 0

                when {
                    poke > 0 -> "poke_ball"
                    (countsByBallId["premier_ball"] ?: 0) > 0 -> "premier_ball"
                    else -> null
                }
            }

            null -> null

            else -> preset.ballId
        }
    }
}