package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgEvolutionStage
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgPokemonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for metadata recognized in Most Wanted text search. */
class PcgMostWantedSearchParserTest {

    /** Parses tier, generation and type from one combined expression. */
    @Test
    fun parse_combinesTierGenerationAndType() {
        val result = PcgMostWantedSearchParser.parse(
            "tier A + gen 1 + fire"
        )

        assertEquals(setOf(PcgPokemonTier.A), result.tiers)
        assertEquals(setOf(1), result.generations)
        assertEquals(setOf(PcgPokemonType.FIRE), result.types)
        assertTrue(result.nameTerms.isEmpty())
    }

    /** Maps first stage to the intermediate MIDDLE catalog stage. */
    @Test
    fun parse_mapsFirstStageToMiddleAndPreservesVariantName() {
        val result = PcgMostWantedSearchParser.parse(
            "first stage + alo raichu"
        )

        assertEquals(
            setOf(PcgEvolutionStage.MIDDLE),
            result.evolutionStages
        )
        assertEquals(listOf("alo raichu"), result.nameTerms)
    }

    /** Treats repeated values from one group as OR alternatives. */
    @Test
    fun parse_collectsMultipleValuesFromTheSameGroup() {
        val result = PcgMostWantedSearchParser.parse(
            "tier A + B tier + water + ground"
        )

        assertEquals(
            setOf(PcgPokemonTier.A, PcgPokemonTier.B),
            result.tiers
        )
        assertEquals(
            setOf(PcgPokemonType.WATER, PcgPokemonType.GROUND),
            result.types
        )
    }
}