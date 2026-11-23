package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgVariantKind
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Characterizes the main-Pokédex species rule over the whole bundled catalog. */
class MainDexSpeciesIndexTest {

    private val entries = BundledPcgCatalogFixture.pcgCatalog.entries
    private val index = BundledPcgCatalogFixture.speciesIndex

    @Test
    fun everyCatalogNameResolvesToItsCleanedSourceSpecies() {
        val speciesKeysBySource = entries
            .groupBy { entry -> MainDexSpeciesIndex.cleanSourceSpecies(entry.sourceSpecies) }
            .mapValues { (_, forms) ->
                forms.mapTo(LinkedHashSet()) { entry -> index.speciesOf(entry.displayName)?.key }
            }

        assertEquals(1_374, entries.size)
        assertEquals(1_025, speciesKeysBySource.size)
        assertEquals(1_025, index.species.size)
        // Every form of one source species lands on one species, and no two share one.
        assertTrue(speciesKeysBySource.values.all { keys -> keys.size == 1 && null !in keys })
        assertEquals(1_025, speciesKeysBySource.values.map { keys -> keys.single() }.toSet().size)
    }

    @Test
    fun catalogNamesAndStoredKeysResolveAlike() {
        val mismatches = entries.filter { entry ->
            index.speciesOf(entry.displayName) != index.speciesOf(entry.normalizedName) ||
                index.speciesOfCatalogName(entry.displayName) != index.speciesOf(entry.displayName)
        }

        assertEquals(emptyList<PcgPokemonCatalogEntry>(), mismatches)
    }

    @Test
    fun mainDexEntryNamesResolveToTheirSpecies() {
        val expected = mapOf(
            "Vivillon (Meadow)" to "Vivillon",
            "Unown (A)" to "Unown",
            "Deerling (Spring)" to "Deerling",
            "Gastrodon (West)" to "Gastrodon",
            "Flabébé (Red)" to "Flabébé",
            "Shellos (West)" to "Shellos",
            "Sawsbuck (Spring)" to "Sawsbuck"
        )

        expected.forEach { (dexName, speciesName) ->
            assertEquals(dexName, speciesName, index.speciesOf(dexName)?.name)
            // The stored snapshot keeps the normalized key, with the parentheses lost.
            assertEquals(
                dexName,
                speciesName,
                index.speciesOf(PcgPokemonNameNormalizer.normalize(dexName))?.name
            )
        }
        assertNull(index.speciesOfCatalogName("Vivillon (Meadow)"))

        val ordinaryNames = entries.filter { entry -> entry.variantKind == PcgVariantKind.NONE }
        assertEquals(1_017, ordinaryNames.size)
        assertTrue(
            ordinaryNames.all { entry -> index.speciesOf(entry.normalizedName)?.name == entry.displayName }
        )
    }

    @Test
    fun longestSpeciesPrefixNeverPicksAShorterLookAlike() {
        val multiWordAndLookAlike = listOf(
            "Mr. Mime", "Mime Jr.", "Mr. Rime", "Type: Null", "Tapu Koko", "Tapu Lele",
            "Tapu Bulu", "Tapu Fini", "Porygon", "Porygon2", "Porygon-Z", "Farfetch’d",
            "Sirfetch'd", "Nidoran♀", "Nidoran♂", "Ho-Oh", "Jangmo-o", "Hakamo-o",
            "Kommo-o", "Chi-Yu", "Chien-Pao", "Ting-Lu", "Wo-Chien", "Great Tusk",
            "Scream Tail", "Brute Bonnet", "Flutter Mane", "Slither Wing", "Sandy Shocks",
            "Roaring Moon", "Walking Wake", "Gouging Fire", "Raging Bolt", "Iron Treads",
            "Iron Bundle", "Iron Hands", "Iron Jugulis", "Iron Moth", "Iron Thorns",
            "Iron Valiant", "Iron Leaves", "Iron Boulder", "Iron Crown"
        )

        multiWordAndLookAlike.forEach { name ->
            assertEquals(name, name, index.speciesOf(name)?.name)
            assertEquals(name, name, index.speciesOf("$name (Meadow)")?.name)
        }
        // Every species, followed by any suffix, keeps its own species.
        index.species.forEach { species ->
            assertEquals(species.key, species, index.speciesOf(species.key + " zzz"))
        }
        // The one species key that is a word prefix of another species key.
        assertEquals("Porygon", index.speciesOf("porygon zzz")?.name)
        assertEquals("Porygon-Z", index.speciesOf("porygon z")?.name)
        assertEquals("Porygon-Z", index.speciesOf("porygon z zzz")?.name)
        // The two Nidoran stay two species, and both apostrophes name one Farfetch'd.
        assertFalse(index.speciesOf("Nidoran♀") == index.speciesOf("Nidoran♂"))
        assertEquals(index.speciesOf("Farfetch’d"), index.speciesOf("Farfetch'd"))
    }

    @Test
    fun speciesWithoutAnOrdinaryEntryGroupAllTheirForms() {
        val withoutOrdinaryEntry = entries
            .groupBy { entry -> MainDexSpeciesIndex.cleanSourceSpecies(entry.sourceSpecies) }
            .filterValues { forms -> forms.none { entry -> entry.variantKind == PcgVariantKind.NONE } }

        assertEquals(
            setOf(
                "Unown", "Aegislash", "Basculegion", "Maushold",
                "Squawkabilly", "Palafin", "Tatsugiri", "Dudunsparce"
            ),
            withoutOrdinaryEntry.keys
        )
        assertEquals(28, withoutOrdinaryEntry.getValue("Unown").size)
        withoutOrdinaryEntry.forEach { (speciesName, forms) ->
            forms.forEach { form ->
                assertEquals(form.displayName, speciesName, index.speciesOf(form.displayName)?.name)
            }
            assertEquals(speciesName, speciesName, index.speciesOf(speciesName)?.name)
        }
    }

    @Test
    fun ordinaryEntryIsTheCatalogEntryNamedAfterItsSpecies() {
        val ordinaryKeys = entries
            .filter { entry -> entry.variantKind == PcgVariantKind.NONE }
            .mapTo(HashSet(), PcgPokemonCatalogEntry::normalizedName)
        val namedAfterTheirSpecies = entries
            .filter { entry -> index.speciesOf(entry.displayName)?.key == entry.normalizedName }
            .mapTo(HashSet(), PcgPokemonCatalogEntry::normalizedName)

        assertEquals(ordinaryKeys, namedAfterTheirSpecies)
    }

    /**
     * Every species is in the category the PCG page listed it under, species by species, and
     * only the 822 Spawnable ones are covered by today's Spawnable-only reading.
     *
     * The captured lists are the page's, not the catalog's: see [CapturedPcgMainDexCategories].
     * When this fails after a catalog change, capture the page again before deciding which
     * side is wrong.
     */
    @Test
    fun categoriesMatchTheCapturedPcgPageSpeciesBySpecies() {
        val captured = CapturedPcgMainDexCategories.entriesByCategory

        assertEquals(822, captured.getValue(MainDexCategory.SPAWNABLE).size)
        assertEquals(81, captured.getValue(MainDexCategory.STARTERS).size)
        assertEquals(122, captured.getValue(MainDexCategory.LEGENDARIES).size)
        assertEquals((1..1_025).toList(), captured.values.flatten().map { entry -> entry.number }.sorted())

        val capturedCategoryBySpecies = captured.flatMap { (category, pageEntries) ->
            pageEntries.map { entry ->
                requireNotNull(index.speciesOf(entry.name)) { "No species for ${entry.name}" }.key to category
            }
        }
        // Each page entry is a different species.
        assertEquals(1_025, capturedCategoryBySpecies.toMap().size)
        assertEquals(
            capturedCategoryBySpecies.toMap(),
            index.species.associate { species -> species.key to species.category }
        )
        assertEquals(822, index.species.count(MainDexSpecies::coveredBySpawnableSnapshot))
    }

    @Test
    fun aSpeciesFormsShareItsCategory() {
        // The catalog marks only "Typhlosion" a starter, yet every Typhlosion is in Starters.
        listOf(
            "Piplup", "Typhlosion", "His Typhlosion", "Blaziken (F)", "Venusaur (Clone)",
            "PCG Chikorita"
        ).forEach { name ->
            assertEquals(name, MainDexCategory.STARTERS, index.speciesOf(name)!!.category)
            assertFalse(name, index.speciesOf(name)!!.coveredBySpawnableSnapshot)
        }
        listOf("Arceus", "Fire Arceus", "Mewtwo").forEach { name ->
            assertEquals(name, MainDexCategory.LEGENDARIES, index.speciesOf(name)!!.category)
        }
        // "Basculegion (F)" is normally spawnable and "Basculegion (M)", its main-dex entry, is not.
        assertEquals(MainDexCategory.LEGENDARIES, index.speciesOf("Basculegion (F)")!!.category)
        listOf("Vivillon", "Vivillon (Jungle)", "Unown (Q)", "Cacnea").forEach { name ->
            assertTrue(name, index.speciesOf(name)!!.coveredBySpawnableSnapshot)
        }
    }

    @Test
    fun namesOutsideEveryRuleHaveNoSpecies() {
        listOf("", "   ", "MissingNo", "zzz vivillon", "vivillo", "porygonz zzz").forEach { name ->
            assertNull(name, index.speciesOf(name))
        }
        assertNull(index.speciesOfCatalogName("Unown"))
    }

    /**
     * The app's species of every catalog name equals tmc-backend's, which sends the
     * missing-Dex alerts.
     *
     * `src/test/resources/main_dex_species/main_dex_species_table_v1.json` is a byte-for-byte
     * copy of `main_dex_species_table_v1.json` from unouidol/tmc-backend at `main` `8ccf210`
     * (git blob `e7eaa82`). That file was generated by its `generate_main_dex_species_table.py`
     * from this repository's catalog at `96f54f9` (blob `b998a08`), as its `source` records.
     *
     * When the bundled catalog changes, this test fails on the name set or on a species.
     * Regenerate the table in tmc-backend from the new catalog, deploy it there, and copy it
     * here again unchanged, so that the two cannot drift apart silently.
     */
    @Test
    fun speciesMatchTheBackendTableNameByName() {
        val tableStream = requireNotNull(
            javaClass.classLoader?.getResourceAsStream(BACKEND_TABLE_RESOURCE)
        ) { "Missing test resource $BACKEND_TABLE_RESOURCE" }
        val table = JSONObject(
            tableStream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
        )
        val speciesByName = table.getJSONObject("species_by_name")
        val backendSpecies = speciesByName.keys().asSequence().associateWith(speciesByName::getString)
        val appSpecies = BundledPcgCatalogFixture.typeDex.entries.associate { entry ->
            entry.displayName to entry.mainDexSpecies
        }

        assertEquals(1, table.getInt("schema_version"))
        assertEquals(1_374, table.getInt("name_count"))
        assertEquals(1_025, table.getInt("species_count"))
        assertEquals(backendSpecies.keys, appSpecies.keys)
        assertEquals(
            emptyMap<String, String>(),
            appSpecies.filter { (name, species) -> backendSpecies[name] != species }
        )
        assertEquals(1_025, backendSpecies.values.toSet().size)
        // The index names the same species the entries carry.
        BundledPcgCatalogFixture.typeDex.entries.forEach { entry ->
            assertEquals(entry.displayName, entry.mainDexSpecies, index.speciesOf(entry.displayName)?.name)
        }
    }

    @Test
    fun catalogNameEqualToAnotherSpeciesLeavesNoIndexButKeepsSpawnResolution() {
        // Unown has no ordinary entry, so "unown" is its species name; here a catalog
        // name "Unown" belongs to Ditto instead.
        val ambiguous = listOf(
            catalogEntry("Unown (A)", sourceSpecies = "Unown", kind = PcgVariantKind.OFFICIAL_FORM),
            catalogEntry("Ditto", sourceSpecies = "Ditto", kind = PcgVariantKind.NONE),
            catalogEntry("Unown", sourceSpecies = "Ditto", kind = PcgVariantKind.PCG)
        )

        val catalog = PokemonTypeDexCatalog.build(ambiguous, emptyList())

        assertNull(catalog.mainDexSpecies)
        assertTrue(catalog.mainDexSpeciesResult.isFailure)
        assertNotNull(catalog.find("Unown (A)"))
        assertNotNull(catalog.find("Unown"))
    }

    @Test
    fun twoSpeciesWithOneNormalizedNameLeaveNoIndex() {
        // Without an ordinary entry, "Mr-Mime" is named "Mr-Mime", which normalizes as "Mr. Mime".
        val ambiguous = listOf(
            catalogEntry("Mr. Mime", sourceSpecies = "Mr. Mime", kind = PcgVariantKind.NONE),
            catalogEntry("Mr-Mime (Clone)", sourceSpecies = "Mr-Mime", kind = PcgVariantKind.PCG)
        )

        val catalog = PokemonTypeDexCatalog.build(ambiguous, emptyList())

        assertNull(catalog.mainDexSpecies)
        assertNotNull(catalog.find("Mr-Mime (Clone)"))
    }

    private fun catalogEntry(
        name: String,
        sourceSpecies: String,
        kind: PcgVariantKind
    ): PcgPokemonCatalogEntry {
        val template = entries.first { entry -> entry.displayName == "Porygon" }
        return template.copy(
            displayName = name,
            normalizedName = PcgPokemonNameNormalizer.normalize(name),
            tier = PcgPokemonTier.C,
            variantKind = kind,
            sourceSpecies = sourceSpecies
        )
    }

    private companion object {
        const val BACKEND_TABLE_RESOURCE = "main_dex_species/main_dex_species_table_v1.json"
    }
}
