package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers profile-aware rules in the otherwise spawn-global recommendation engine. */
class CatchBallRecommenderTest {

    @Test
    fun repeatBallIsSuggestedOnlyForAffirmativelyRegisteredEntry() {
        val registered = recommendation(isAlreadyCaught = true)
        val missing = recommendation(isAlreadyCaught = false)
        val unknown = recommendation(isAlreadyCaught = null)

        assertEquals(75, registered.score)
        assertEquals(listOf("already_caught"), registered.reasonKeys)
        assertEquals(0, missing.score)
        assertEquals(0, unknown.score)
    }

    private fun recommendation(isAlreadyCaught: Boolean?): CatchBallRecommendation {
        return CatchBallRecommender.recommend(
            presets = listOf(
                CatchPreset(
                    id = "repeat",
                    label = "Repeat Ball",
                    command = "!pokecatch repeat ball",
                    ballId = "repeat_ball"
                )
            ),
            spawn = SpawnSnapshot(
                rawName = "Cacnea",
                dexKey = "cacnea",
                displayName = "Cacnea",
                tier = PcgPokemonTier.C,
                type1 = "Grass",
                type2 = null,
                weightKg = null,
                baseSpeed = null,
                baseHp = null,
                evolvesTwice = null,
                seenAtMs = System.currentTimeMillis()
            ),
            buddy = null,
            isAlreadyCaught = isAlreadyCaught
        ).single()
    }
}
