package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgEvolutionStage
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgPokemonType
import com.fs.twitchminichat.pcg.catalog.PcgVariantKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for catalog-backed Most Wanted selection validation. */
class PcgMostWantedSelectionValidatorTest {

    /** Canonicalizes valid input, removes duplicates and drops unknown names. */
    @Test
    fun sanitize_returnsCanonicalNamesInCatalogOrder() {
        val flabebeBlue = "Flab\u00E9b\u00E9 (Blue)"
        val entries = listOf(
            entry("Bulbasaur"),
            entry(flabebeBlue),
            entry("Alo Raichu")
        )

        val result = PcgMostWantedSelectionValidator.sanitize(
            entries,
            listOf(
                "alo raichu",
                "Flabebe Blue",
                "missing pokemon",
                "ALO RAICHU"
            )
        )

        assertEquals(
            linkedSetOf(flabebeBlue, "Alo Raichu"),
            result
        )
    }

    /** Resolves an input spelling to the exact name stored in the catalog. */
    @Test
    fun resolveDisplayName_returnsCanonicalCatalogSpelling() {
        val canonicalName = "Farfetch\u2019d"
        val result = PcgMostWantedSelectionValidator.resolveDisplayName(
            listOf(entry(canonicalName)),
            "Farfetch'd"
        )

        assertEquals(canonicalName, result)
    }

    /** Rejects blank and unknown names instead of persisting them. */
    @Test
    fun resolveDisplayName_rejectsInvalidNames() {
        val entries = listOf(entry("Bulbasaur"))

        assertNull(
            PcgMostWantedSelectionValidator.resolveDisplayName(entries, " ")
        )
        assertNull(
            PcgMostWantedSelectionValidator.resolveDisplayName(
                entries,
                "MissingNo"
            )
        )
    }

    /** Creates the minimum complete catalog model needed by these tests. */
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