package com.fs.twitchminichat.pcg.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the pure PCG catalog filtering rules. */
class PcgPokemonCatalogFilterTest {

    /** Name matching is accent-insensitive and ignores PCG punctuation. */
    @Test
    fun apply_normalizesNameSearch() {
        val entries = listOf(
            entry(
                displayName = "FlabÃ©bÃ© (Blue)",
                normalizedName = "flabebe blue"
            ),
            entry(
                displayName = "Bulbasaur",
                normalizedName = "bulbasaur"
            )
        )

        val result = PcgPokemonCatalogFilter.apply(
            entries = entries,
            query = PcgPokemonCatalogQuery(nameQuery = "Flabebe blue")
        )

        assertEquals(listOf("FlabÃ©bÃ© (Blue)"), result.map { it.displayName })
    }

    /** Type, tier and generation constraints must all match. */
    @Test
    fun apply_combinesIndependentFilters() {
        val matchingEntry = entry(
            displayName = "Alo Raichu",
            normalizedName = "alo raichu",
            tier = PcgPokemonTier.A,
            generation = 7,
            types = setOf(
                PcgPokemonType.ELECTRIC,
                PcgPokemonType.PSYCHIC
            ),
            variantKind = PcgVariantKind.REGIONAL
        )
        val wrongTier = matchingEntry.copy(
            displayName = "Gal Zapdos",
            normalizedName = "gal zapdos",
            tier = PcgPokemonTier.S
        )

        val result = PcgPokemonCatalogFilter.apply(
            entries = listOf(matchingEntry, wrongTier),
            query = PcgPokemonCatalogQuery(
                tiers = setOf(PcgPokemonTier.A),
                types = setOf(PcgPokemonType.PSYCHIC),
                generations = setOf(7)
            )
        )

        assertEquals(listOf("Alo Raichu"), result.map { it.displayName })
    }

    /** Regional and PCG variants can be selected as one combined category. */
    @Test
    fun apply_filtersRegionalAndPcgVariants() {
        val entries = listOf(
            entry(
                displayName = "Alo Raichu",
                normalizedName = "alo raichu",
                variantKind = PcgVariantKind.REGIONAL
            ),
            entry(
                displayName = "PCG Magnemite",
                normalizedName = "pcg magnemite",
                variantKind = PcgVariantKind.PCG
            ),
            entry(
                displayName = "Raichu",
                normalizedName = "raichu"
            )
        )

        val result = PcgPokemonCatalogFilter.apply(
            entries = entries,
            query = PcgPokemonCatalogQuery(
                variantKinds = setOf(
                    PcgVariantKind.REGIONAL,
                    PcgVariantKind.PCG
                )
            )
        )

        assertEquals(
            listOf("Alo Raichu", "PCG Magnemite"),
            result.map { it.displayName }
        )
    }

    /** Special availability includes locked catalog entries only. */
    @Test
    fun apply_filtersSpecialAvailability() {
        val entries = listOf(
            entry(
                displayName = "Mewtwo",
                normalizedName = "mewtwo",
                normallySpawnable = false
            ),
            entry(
                displayName = "Dragonite",
                normalizedName = "dragonite",
                normallySpawnable = true
            )
        )

        val result = PcgPokemonCatalogFilter.apply(
            entries = entries,
            query = PcgPokemonCatalogQuery(
                spawnAvailability =
                    PcgSpawnAvailability.SPECIAL_AVAILABILITY
            )
        )

        assertEquals(listOf("Mewtwo"), result.map { it.displayName })
    }

    /** Builds one small immutable fixture with overridable filter metadata. */
    private fun entry(
        displayName: String,
        normalizedName: String,
        tier: PcgPokemonTier = PcgPokemonTier.C,
        normallySpawnable: Boolean = true,
        generation: Int = 1,
        types: Set<PcgPokemonType> = setOf(PcgPokemonType.NORMAL),
        variantKind: PcgVariantKind = PcgVariantKind.NONE
    ): PcgPokemonCatalogEntry {
        return PcgPokemonCatalogEntry(
            displayName = displayName,
            normalizedName = normalizedName,
            tier = tier,
            normallySpawnable = normallySpawnable,
            starterFamily = false,
            variantKind = variantKind,
            sourceSpecies = displayName,
            generation = generation,
            types = types,
            evolutionStage = PcgEvolutionStage.SINGLE,
            legendary = false,
            mythical = false
        )
    }
}
