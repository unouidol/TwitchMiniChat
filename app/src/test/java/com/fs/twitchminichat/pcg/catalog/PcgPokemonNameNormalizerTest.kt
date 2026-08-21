package com.fs.twitchminichat.pcg.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for stable PCG catalog name normalization. */
class PcgPokemonNameNormalizerTest {

    /** Female and male symbols remain distinct catalog-key characters. */
    @Test
    fun normalize_preservesGenderSymbols() {
        assertEquals(
            "nidoran\u2640",
            PcgPokemonNameNormalizer.normalize("Nidoran\u2640")
        )
        assertEquals(
            "nidoran\u2642",
            PcgPokemonNameNormalizer.normalize("Nidoran\u2642")
        )
    }
}
