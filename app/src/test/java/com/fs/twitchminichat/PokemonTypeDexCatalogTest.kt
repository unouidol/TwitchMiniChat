package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalog
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogJsonParser
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards complete and deterministic Smart Catch resolution for the PCG catalog. */
class PokemonTypeDexCatalogTest {

    @Test
    fun everyCanonicalPcgEntryResolvesToItself() {
        val fixture = loadFixture()
        val mismatches = fixture.pcgCatalog.entries.mapNotNull { expected ->
            val resolvedName = fixture.typeDex.find(expected.displayName)?.pcgName
            if (resolvedName == expected.displayName) {
                null
            } else {
                "${expected.displayName} -> $resolvedName"
            }
        }

        assertEquals(EXPECTED_PCG_ENTRY_COUNT, fixture.pcgCatalog.entries.size)
        assertEquals(EXPECTED_PCG_ENTRY_COUNT, fixture.typeDex.entries.size)
        assertEquals(
            EXPECTED_PCG_ENTRY_COUNT,
            fixture.typeDex.entries.map(PokemonTypeEntry::sourceKey).distinct().size
        )
        assertTrue(
            "Canonical PCG names failed resolution: ${mismatches.take(20)}",
            mismatches.isEmpty()
        )
        assertTrue(
            "Every canonical Smart Catch entry must retain an elemental type",
            fixture.typeDex.entries.all { entry -> entry.type1.isNotBlank() }
        )
    }

    @Test
    fun coloredFlorgesFormsRemainDistinctFromTheBaseForm() {
        val typeDex = loadFixture().typeDex
        val expectedNames = listOf(
            "Florges",
            "Florges (Blue)",
            "Florges (Orange)",
            "Florges (White)",
            "Florges (Yellow)"
        )

        val resolvedNames = expectedNames.map { name ->
            typeDex.find(name)?.pcgName
        }

        assertEquals(expectedNames, resolvedNames)
        assertEquals("Florges (Blue)", typeDex.find("Florges-Blue")?.pcgName)
    }

    @Test
    fun baseNamesAndGenderSymbolsCannotBeOverwrittenByFormAliases() {
        val typeDex = loadFixture().typeDex

        assertEquals("Pikachu", typeDex.find("Pikachu")?.pcgName)
        assertEquals("Pikachu (F)", typeDex.find("Pikachu (F)")?.pcgName)
        assertEquals(
            "Pikachu (Rockstar)",
            typeDex.find("Pikachu (Rockstar)")?.pcgName
        )
        assertEquals("Nidoran♀", typeDex.find("Nidoran♀")?.pcgName)
        assertEquals("Nidoran♂", typeDex.find("Nidoran♂")?.pcgName)
    }

    @Test
    fun ambiguousNoncanonicalAliasIsNotResolvedByLoadOrder() {
        val typeDex = loadFixture().typeDex

        assertNull(typeDex.find("Unown"))
    }

    @Test
    fun uniqueRegionalAliasesRemainAvailable() {
        val typeDex = loadFixture().typeDex

        assertEquals("Alo Rattata", typeDex.find("Alolan Rattata")?.pcgName)
        assertEquals("Alo Rattata", typeDex.find("Rattata-Alola")?.pcgName)
    }

    @Test
    fun entriesMissingFromLegacyMetadataStillHaveCatalogTypesAndTier() {
        val typeDex = loadFixture().typeDex

        val ogerpon = typeDex.find("Ogerpon")
        assertEquals("Grass", ogerpon?.type1)
        assertEquals(PcgPokemonTier.S, ogerpon?.tier)
        assertNull(ogerpon?.weightKg)

        val paldeanWooper = typeDex.find("Pal Wooper")
        assertEquals("Poison", paldeanWooper?.type1)
        assertEquals("Ground", paldeanWooper?.type2)
        assertEquals(PcgPokemonTier.C, paldeanWooper?.tier)
    }

    @Test
    fun exactLegacyMetadataStillEnrichesCanonicalEntries() {
        val cacnea = loadFixture().typeDex.find("Cacnea")

        assertEquals(51.3, cacnea?.weightKg ?: Double.NaN, 0.0)
        assertEquals(35, cacnea?.baseSpeed)
        assertEquals(50, cacnea?.baseHp)
        assertEquals(false, cacnea?.evolvesTwice)
    }

    @Test
    fun canonicalCatalogRemainsUsableWithoutOptionalLegacyMetadata() {
        val fixture = loadFixture()
        val typeDex = PokemonTypeDexCatalog.build(
            pcgEntries = fixture.pcgCatalog.entries,
            metadataEntries = emptyList()
        )

        assertEquals(EXPECTED_PCG_ENTRY_COUNT, typeDex.entries.size)
        assertEquals("Florges (Yellow)", typeDex.find("Florges (Yellow)")?.pcgName)
        assertEquals("Fairy", typeDex.find("Florges (Yellow)")?.type1)
        assertNull(typeDex.find("Florges (Yellow)")?.weightKg)
    }

    private data class Fixture(
        val pcgCatalog: PcgPokemonCatalog,
        val typeDex: PokemonTypeDexCatalog
    )

    private fun loadFixture(): Fixture {
        val assets = findAppModuleDirectory().resolve("src/main/assets")
        val pcgJson = Files.readString(
            assets.resolve("pcg_catalog/pcg_pokemon_catalog_v1.json")
        )
        val metadataJson = Files.readString(
            assets.resolve("pokemon_type_dex.json")
        )

        val pcgCatalog = PcgPokemonCatalogJsonParser.parse(pcgJson)
        val metadata = PokemonTypeDexMetadataParser.parse(metadataJson)

        return Fixture(
            pcgCatalog = pcgCatalog,
            typeDex = PokemonTypeDexCatalog.build(
                pcgEntries = pcgCatalog.entries,
                metadataEntries = metadata
            )
        )
    }

    private fun findAppModuleDirectory(): Path {
        val start = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath()
            .normalize()

        return generateSequence(start) { current -> current.parent }
            .take(MAX_PARENT_SEARCH_DEPTH)
            .flatMap { current -> sequenceOf(current.resolve("app"), current) }
            .firstOrNull { candidate ->
                Files.isDirectory(candidate.resolve("src/main/assets"))
            }
            ?: error("Unable to locate app/src/main/assets from $start")
    }

    private companion object {
        const val EXPECTED_PCG_ENTRY_COUNT = 1_374
        const val MAX_PARENT_SEARCH_DEPTH = 6
    }
}
