package com.fs.twitchminichat

object CatchPresetBallHelper {

    fun canBuyFromPreset(preset: CatchPreset): Boolean {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball",
            "great_ball",
            "ultra_ball" -> true
            else -> false
        }
    }

    fun resolveShopBallNameForPreset(preset: CatchPreset): String? {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball" -> "poke ball"
            "great_ball" -> "great ball"
            "ultra_ball" -> "ultra ball"
            else -> null
        }
    }

    fun resolveBoughtBallIdForPreset(preset: CatchPreset): String? {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball" -> "poke_ball"
            "great_ball" -> "great_ball"
            "ultra_ball" -> "ultra_ball"
            else -> null
        }
    }
}