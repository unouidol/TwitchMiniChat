package com.fs.twitchminichat

object CatchBallRecommender {
    private const val SPAWN_DURATION_SEC = 90
    private const val QUICK_BALL_WINDOW_SEC = 15
    private const val TIMER_BALL_START_SEC = 75

    private fun spawnAgeSec(spawn: SpawnSnapshot?): Int? {
        if (spawn == null) return null
        val ageMs = System.currentTimeMillis() - spawn.seenAtMs
        if (ageMs < 0L) return 0
        return (ageMs / 1000L).toInt()
    }

    private fun isQuickBallWindow(spawn: SpawnSnapshot?): Boolean {
        val age = spawnAgeSec(spawn) ?: return false
        return age in 0 until QUICK_BALL_WINDOW_SEC
    }

    private fun isTimerBallWindow(spawn: SpawnSnapshot?): Boolean {
        val age = spawnAgeSec(spawn) ?: return false
        return age in TIMER_BALL_START_SEC until SPAWN_DURATION_SEC
    }
    fun recommend(
        presets: List<CatchPreset>,
        spawn: SpawnSnapshot?,
        buddy: BuddyInfo?
    ): List<CatchBallRecommendation> {
        return presets.mapIndexed { index, preset ->
            val result = scorePreset(
                preset = preset,
                spawn = spawn,
                buddy = buddy
            )

            CatchBallRecommendation(
                preset = preset,
                score = result.score,
                reasonKeys = result.reasonKeys,
                originalIndex = index
            )
        }.sortedWith(
            compareByDescending<CatchBallRecommendation> { it.score }
                .thenBy { it.originalIndex }
        )
    }

    private data class ScoreResult(
        val score: Int,
        val reasonKeys: List<String>
    )

    private fun scorePreset(
        preset: CatchPreset,
        spawn: SpawnSnapshot?,
        buddy: BuddyInfo?
    ): ScoreResult {
        val ballId = CatchPresetBallHelper.effectiveBallId(preset).orEmpty()
        val types = spawnTypes(spawn)
        val buddyTypes = buddyTypes(buddy)

        return when (ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            "poke_ball" -> ScoreResult(
                score = 30,
                reasonKeys = listOf("base_30")
            )

            "great_ball" -> ScoreResult(
                score = 55,
                reasonKeys = listOf("base_55")
            )

            "ultra_ball" -> ScoreResult(
                score = 80,
                reasonKeys = listOf("base_80")
            )

            "friend_ball" -> {
                if (types.isNotEmpty() && buddyTypes.isNotEmpty() && types.any { it in buddyTypes }) {
                    ScoreResult(
                        score = 70,
                        reasonKeys = listOf("buddy_shared_type")
                    )
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "fast_ball" -> {
                if ((spawn?.baseSpeed ?: 0) >= 100) {
                    ScoreResult(80, listOf("speed_100_plus"))
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "feather_ball" -> {
                val weightKg = spawn?.weightKg
                if (weightKg != null && weightKg < 10.0) {
                    ScoreResult(80, listOf("weight_under_10kg"))
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "heavy_ball" -> {
                val weightKg = spawn?.weightKg
                if (weightKg != null && weightKg > 100.0) {
                    ScoreResult(80, listOf("weight_over_100kg"))
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "heal_ball" -> {
                if ((spawn?.baseHp ?: 0) >= 100) {
                    ScoreResult(80, listOf("hp_100_plus"))
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "nest_ball" -> {
                if (spawn?.evolvesTwice == true) {
                    ScoreResult(90, listOf("evolves_twice"))
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "repeat_ball" -> {
                if (spawn?.isAlreadyCaught == true) {
                    ScoreResult(75, listOf("already_caught"))
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "quick_ball" -> {
                if (isQuickBallWindow(spawn)) {
                    ScoreResult(
                        score = 90,
                        reasonKeys = listOf("quick_window")
                    )
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "timer_ball" -> {
                if (isTimerBallWindow(spawn)) {
                    ScoreResult(
                        score = 90,
                        reasonKeys = listOf("timer_window")
                    )
                } else {
                    ScoreResult(0, emptyList())
                }
            }

            "frozen_ball" -> typeBallScore(types, setOf("ice"), "type_ice")
            "night_ball" -> typeBallScore(types, setOf("dark"), "type_dark")
            "phantom_ball" -> typeBallScore(types, setOf("ghost"), "type_ghost")
            "cipher_ball" -> typeBallScore(types, setOf("poison", "psychic"), "type_poison_psychic")
            "magnet_ball" -> typeBallScore(types, setOf("electric", "steel"), "type_electric_steel")
            "net_ball" -> typeBallScore(types, setOf("water", "bug"), "type_water_bug")
            "sun_ball" -> typeBallScore(types, setOf("fire", "grass"), "type_fire_grass")
            "fantasy_ball" -> typeBallScore(types, setOf("fairy", "dragon"), "type_fairy_dragon")
            "geo_ball" -> typeBallScore(types, setOf("rock", "ground"), "type_rock_ground")
            "basic_ball" -> typeBallScore(types, setOf("normal"), "type_normal")
            "mach_ball" -> typeBallScore(types, setOf("fighting", "flying"), "type_fighting_flying")

            else -> ScoreResult(0, emptyList())
        }
    }

    private fun typeBallScore(
        spawnTypes: Set<String>,
        requiredTypes: Set<String>,
        reasonKey: String
    ): ScoreResult {
        return if (spawnTypes.any { it in requiredTypes }) {
            ScoreResult(80, listOf(reasonKey))
        } else {
            ScoreResult(0, emptyList())
        }
    }

    private fun spawnTypes(spawn: SpawnSnapshot?): Set<String> {
        if (spawn == null) return emptySet()

        return buildSet {
            spawn.type1?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { add(it) }
            spawn.type2?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    private fun buddyTypes(buddy: BuddyInfo?): Set<String> {
        if (buddy == null) return emptySet()

        return buildSet {
            buddy.primaryType?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { add(it) }
            buddy.secondaryType?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}