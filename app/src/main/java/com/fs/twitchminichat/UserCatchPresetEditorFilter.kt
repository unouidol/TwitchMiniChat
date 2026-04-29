package com.fs.twitchminichat

/**
 * Decides which presets are shown in the user preset editor.
 *
 * This filter is intentionally only for the editor/settings screen.
 *
 * It does not change Smart recommendations, and it does not change the internal
 * ball catalog. It only keeps the manual preset list smaller and easier to
 * manage.
 *
 * Balls that are mostly context-driven are hidden here because Smart presets
 * are better at presenting them only when they are relevant:
 *
 * - type-based balls
 * - weight-based balls
 * - speed/HP-based balls
 * - evolution-stage balls
 */
object UserCatchPresetEditorFilter {

    private val hiddenBallIds = setOf(
        // Type-based balls.
        "frozen_ball",
        "night_ball",
        "phantom_ball",
        "cipher_ball",
        "magnet_ball",
        "net_ball",
        "sun_ball",
        "fantasy_ball",
        "geo_ball",
        "basic_ball",
        "mach_ball",

        // Weight-based balls.
        "heavy_ball",
        "feather_ball",

        // Stat/evolution based balls.
        "fast_ball",
        "heal_ball",
        "nest_ball"
    )

    fun shouldShowInEditor(preset: CatchPreset): Boolean {
        val ballId = preset.ballId ?: return true
        return ballId !in hiddenBallIds
    }
}