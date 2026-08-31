package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgEvolutionStage
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgVariantKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the versioned, human-readable Most Wanted backup format. */
class PcgMostWantedBackupCodecTest {

    /** Export and import preserve exact names and current catalog order. */
    @Test
    fun roundTrip_preservesRegionalUnicodeAndGenderedNames() {
        val female = "Nidoran\u2640"
        val male = "Nidoran\u2642"
        val entries = listOf(
            entry("Applin"),
            entry(female, normalizedName = "nidoran\u2640"),
            entry(male, normalizedName = "nidoran\u2642"),
            entry("Pal Wooper"),
            entry("Farfetch\u2019d")
        )
        val encoded = PcgMostWantedBackupCodec.encode(
            catalogVersion = "audit-2026-08",
            catalogEntries = entries,
            selectedDisplayNames = listOf(
                "Farfetch\u2019d",
                "Pal Wooper",
                male,
                female
            )
        )

        val result = PcgMostWantedBackupCodec.decode(encoded, entries)

        assertTrue(result is PcgMostWantedBackupDecodeResult.Success)
        val backup = (result as PcgMostWantedBackupDecodeResult.Success)
            .backup
        assertEquals(
            linkedSetOf(
                female,
                male,
                "Pal Wooper",
                "Farfetch\u2019d"
            ),
            backup.selectedDisplayNames
        )
        assertEquals(0, backup.duplicateCount)
        assertEquals("audit-2026-08", backup.sourceCatalogVersion)
        assertFalse(encoded.contains("profile_id"))
        assertFalse(encoded.contains("device_id"))
    }

    /** Normalized spelling is accepted while duplicate aliases are counted. */
    @Test
    fun decode_acceptsOneUnambiguousAliasAndCollapsesDuplicates() {
        val displayName = "Flab\u00e9b\u00e9"
        val entries = listOf(entry(displayName))
        val text = listOf(
            "# TwitchMiniChat Most Wanted",
            "# format=1",
            "# catalog=test",
            "Flabebe",
            displayName
        ).joinToString(separator = "\n")

        val result = PcgMostWantedBackupCodec.decode(text, entries)

        assertTrue(result is PcgMostWantedBackupDecodeResult.Success)
        val backup = (result as PcgMostWantedBackupDecodeResult.Success)
            .backup
        assertEquals(linkedSetOf(displayName), backup.selectedDisplayNames)
        assertEquals(1, backup.duplicateCount)
    }

    /** A single unknown line rejects the whole document. */
    @Test
    fun decode_rejectsCompleteFileWhenAnyNameIsUnknown() {
        val text = """
            # TwitchMiniChat Most Wanted
            # format=1
            Applin
            Definitely Not A Pokemon
        """.trimIndent()

        val result = PcgMostWantedBackupCodec.decode(
            text,
            listOf(entry("Applin"))
        )

        assertTrue(result is PcgMostWantedBackupDecodeResult.Failure)
        val failure = (result as PcgMostWantedBackupDecodeResult.Failure)
            .error
        assertEquals(
            PcgMostWantedBackupDecodeError.UNKNOWN_NAMES,
            failure.reason
        )
        assertEquals(
            listOf("Definitely Not A Pokemon"),
            failure.unknownNames
        )
    }

    /** An empty app-generated selection remains a valid clear-list backup. */
    @Test
    fun decode_acceptsEmptyGeneratedSelection() {
        val entries = listOf(entry("Applin"))
        val encoded = PcgMostWantedBackupCodec.encode(
            catalogVersion = "test",
            catalogEntries = entries,
            selectedDisplayNames = emptySet()
        )

        val result = PcgMostWantedBackupCodec.decode(encoded, entries)

        assertTrue(result is PcgMostWantedBackupDecodeResult.Success)
        val backup = (result as PcgMostWantedBackupDecodeResult.Success)
            .backup
        assertTrue(backup.selectedDisplayNames.isEmpty())
    }

    /** Foreign and future document formats are never treated as selections. */
    @Test
    fun decode_rejectsMissingHeaderAndFutureFormat() {
        val entries = listOf(entry("Applin"))

        val missingHeader = PcgMostWantedBackupCodec.decode(
            "Applin",
            entries
        )
        val futureFormat = PcgMostWantedBackupCodec.decode(
            "# TwitchMiniChat Most Wanted\n# format=99\nApplin",
            entries
        )

        assertEquals(
            PcgMostWantedBackupDecodeError.MISSING_HEADER,
            (missingHeader as PcgMostWantedBackupDecodeResult.Failure)
                .error.reason
        )
        assertEquals(
            PcgMostWantedBackupDecodeError.UNSUPPORTED_FORMAT,
            (futureFormat as PcgMostWantedBackupDecodeResult.Failure)
                .error.reason
        )
    }

    /** Exact canonical names survive save validation even if aliases collide. */
    @Test
    fun selectionValidator_prefersExactCanonicalGenderNames() {
        val female = "Nidoran\u2640"
        val male = "Nidoran\u2642"
        val entries = listOf(
            entry(female, normalizedName = "nidoran\u2640"),
            entry(male, normalizedName = "nidoran\u2642")
        )

        assertEquals(
            linkedSetOf(female, male),
            PcgMostWantedSelectionValidator.sanitize(
                entries,
                listOf(female, male)
            )
        )
    }

    /** Creates a minimal catalog entry used by pure backup tests. */
    private fun entry(
        displayName: String,
        normalizedName: String =
            PcgPokemonNameNormalizer.normalize(displayName)
    ): PcgPokemonCatalogEntry {
        return PcgPokemonCatalogEntry(
            displayName = displayName,
            normalizedName = normalizedName,
            tier = PcgPokemonTier.C,
            normallySpawnable = true,
            starterFamily = false,
            variantKind = PcgVariantKind.NONE,
            sourceSpecies = displayName,
            generation = 1,
            types = emptySet(),
            evolutionStage = PcgEvolutionStage.SINGLE,
            legendary = false,
            mythical = false
        )
    }
}
