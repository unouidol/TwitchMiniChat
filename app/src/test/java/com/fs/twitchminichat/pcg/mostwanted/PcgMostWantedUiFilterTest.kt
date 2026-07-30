package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgEvolutionStage
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgPokemonType
import com.fs.twitchminichat.pcg.catalog.PcgVariantKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for structured Most Wanted filter composition. */
class PcgMostWantedUiFilterTest {

    /** Combines text generation/type filters with the Base-form button. */
    @Test
    fun apply_combinesGenerationAndTypeTextWithBaseStageButton() {
        val result = PcgMostWantedUiFilter.apply(
            entries = entries(),
            searchText = "gen 1 + water",
            filterState = PcgMostWantedFilterState(
                evolutionStages = setOf(PcgEvolutionStage.BASE)
            ),
            selectedDisplayNames = emptySet()
        )

        assertEquals(listOf("Squirtle"), result.map { it.displayName })
    }

    /** Applies tier, generation and type text as three AND groups. */
    @Test
    fun apply_combinesTierGenerationAndTypeText() {
        val result = PcgMostWantedUiFilter.apply(
            entries = entries(),
            searchText = "tier A + gen 1 + fire",
            filterState = PcgMostWantedFilterState(),
            selectedDisplayNames = emptySet()
        )

        assertEquals(
            listOf("Charmander", "Vulpix"),
            result.map { it.displayName }
        )
    }

    /** Treats multiple selected values from one stage group as OR. */
    @Test
    fun apply_combinesMultipleStagesAsAlternatives() {
        val result = PcgMostWantedUiFilter.apply(
            entries = entries(),
            searchText = "gen 1 + water",
            filterState = PcgMostWantedFilterState(
                evolutionStages = setOf(
                    PcgEvolutionStage.BASE,
                    PcgEvolutionStage.MIDDLE
                )
            ),
            selectedDisplayNames = emptySet()
        )

        assertEquals(
            listOf("Squirtle", "Wartortle"),
            result.map { it.displayName }
        )
    }

    /** Keeps regional and PCG variant words available to name matching. */
    @Test
    fun apply_keepsVariantWordsInNameSearch() {
        val result = PcgMostWantedUiFilter.apply(
            entries = entries(),
            searchText = "alo + raichu",
            filterState = PcgMostWantedFilterState(),
            selectedDisplayNames = emptySet()
        )

        assertEquals(listOf("Alo Raichu"), result.map { it.displayName })
    }

    /** Reports active groups rather than counting each selected value. */
    @Test
    fun activeFilterCount_countsGroups() {
        val state = PcgMostWantedFilterState(
            tiers = setOf(PcgPokemonTier.A, PcgPokemonTier.B),
            generations = setOf(1),
            evolutionStages = setOf(
                PcgEvolutionStage.BASE,
                PcgEvolutionStage.MIDDLE
            ),
            categories = setOf(PcgMostWantedCategory.LEGENDARY)
        )

        assertEquals(4, state.activeFilterCount())
        assertEquals(0, PcgMostWantedFilterState().activeFilterCount())
    }

    /** Creates a representative catalog for filter-composition tests. */
    private fun entries(): List<PcgPokemonCatalogEntry> {
        return listOf(
            entry(
                "Squirtle",
                PcgPokemonTier.B,
                setOf(PcgPokemonType.WATER),
                1,
                PcgEvolutionStage.BASE,
                starter = true
            ),
            entry(
                "Wartortle",
                PcgPokemonTier.B,
                setOf(PcgPokemonType.WATER),
                1,
                PcgEvolutionStage.MIDDLE,
                starter = true
            ),
            entry(
                "Charmander",
                PcgPokemonTier.A,
                setOf(PcgPokemonType.FIRE),
                1,
                PcgEvolutionStage.BASE,
                starter = true
            ),
            entry(
                "Vulpix",
                PcgPokemonTier.A,
                setOf(PcgPokemonType.FIRE),
                1,
                PcgEvolutionStage.BASE
            ),
            entry(
                "Camerupt",
                PcgPokemonTier.A,
                setOf(
                    PcgPokemonType.FIRE,
                    PcgPokemonType.GROUND
                ),
                3,
                PcgEvolutionStage.FINAL
            ),
            entry(
                "Alo Raichu",
                PcgPokemonTier.B,
                setOf(
                    PcgPokemonType.ELECTRIC,
                    PcgPokemonType.PSYCHIC
                ),
                1,
                PcgEvolutionStage.FINAL,
                variant = PcgVariantKind.REGIONAL
            )
        )
    }

    /** Creates one complete catalog model for filter tests. */
    private fun entry(
        displayName: String,
        tier: PcgPokemonTier,
        types: Set<PcgPokemonType>,
        generation: Int,
        stage: PcgEvolutionStage,
        starter: Boolean = false,
        variant: PcgVariantKind = PcgVariantKind.NONE
    ): PcgPokemonCatalogEntry {
        return PcgPokemonCatalogEntry(
            displayName = displayName,
            normalizedName = PcgPokemonNameNormalizer.normalize(displayName),
            tier = tier,
            normallySpawnable = true,
            starterFamily = starter,
            variantKind = variant,
            sourceSpecies = displayName,
            generation = generation,
            types = types,
            evolutionStage = stage,
            legendary = false,
            mythical = false
        )
    }
}