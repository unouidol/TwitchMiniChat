package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgEvolutionStage
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgPokemonType
import com.fs.twitchminichat.pcg.catalog.PcgVariantKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the fixed-universe Most Wanted selection review. */
class PcgMostWantedSelectionReviewTest {

    /** Current or imported selections retain canonical catalog order. */
    @Test
    fun entriesForSelection_keepsOnlySelectedCatalogEntriesInOrder() {
        val entries = listOf(
            entry("Bulbasaur"),
            entry("Charmander"),
            entry("Squirtle")
        )

        val result = PcgMostWantedSelectionReview.entriesForSelection(
            catalogEntries = entries,
            selectedDisplayNames = setOf("Squirtle", "Bulbasaur")
        )

        assertEquals(
            listOf("Bulbasaur", "Squirtle"),
            result.map(PcgPokemonCatalogEntry::displayName)
        )
    }

    /** Gender symbols remain usable as distinct review-search terms. */
    @Test
    fun filterEntries_distinguishesGenderSymbols() {
        val female = "Nidoran\u2640"
        val male = "Nidoran\u2642"
        val entries = listOf(entry(female), entry(male))

        assertEquals(
            listOf(female),
            PcgMostWantedSelectionReview.filterEntries(
                reviewEntries = entries,
                searchText = "\u2640"
            ).map(PcgPokemonCatalogEntry::displayName)
        )
        assertEquals(
            listOf(male),
            PcgMostWantedSelectionReview.filterEntries(
                reviewEntries = entries,
                searchText = "\u2642"
            ).map(PcgPokemonCatalogEntry::displayName)
        )
    }

    /** Applying a review emits only checked names in catalog order. */
    @Test
    fun selectedNames_usesCheckedSubsetInCatalogOrder() {
        val entries = listOf(
            entry("Bulbasaur"),
            entry("Charmander"),
            entry("Squirtle")
        )

        assertEquals(
            linkedSetOf("Bulbasaur", "Squirtle"),
            PcgMostWantedSelectionReview.selectedNames(
                reviewEntries = entries,
                selectedDisplayNames = setOf(
                    "Squirtle",
                    "Unknown",
                    "Bulbasaur"
                )
            )
        )
    }

    /** A complete deselection can apply empty while rows remain reviewable. */
    @Test
    fun selectedNames_allowsCompleteResetOfFixedReviewUniverse() {
        val reviewEntries = listOf(
            entry("Bulbasaur"),
            entry("Charmander")
        )

        assertEquals(
            emptySet<String>(),
            PcgMostWantedSelectionReview.selectedNames(
                reviewEntries = reviewEntries,
                selectedDisplayNames = emptySet()
            )
        )
        assertEquals(2, reviewEntries.size)
    }

    /** Creates the minimum complete catalog row required by review tests. */
    private fun entry(displayName: String): PcgPokemonCatalogEntry {
        return PcgPokemonCatalogEntry(
            displayName = displayName,
            normalizedName = PcgPokemonNameNormalizer.normalize(displayName),
            tier = PcgPokemonTier.C,
            normallySpawnable = true,
            starterFamily = false,
            variantKind = PcgVariantKind.NONE,
            sourceSpecies = displayName,
            generation = 1,
            types = setOf(PcgPokemonType.NORMAL),
            evolutionStage = PcgEvolutionStage.SINGLE,
            legendary = false,
            mythical = false
        )
    }
}
