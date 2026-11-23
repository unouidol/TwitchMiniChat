package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that profile collection facts never leak into the global spawn snapshot, and
 * that the main-Pokédex fact is judged by species against the bundled catalog.
 *
 * The Cacnea tests now pass the catalog's species index: without one the resolver reports
 * UNKNOWN, because REGISTERED needs the catalog to say that a Spawnable-only capture covers
 * the species. Their expectations are unchanged.
 */
class QuickCatchProfileSpawnContextResolverTest {

    private val index = BundledPcgCatalogFixture.speciesIndex

    @Test
    fun registeredDexEntryEnablesRepeatBallFact() {
        val context = resolve(spawn("Cacnea"), missing = setOf("pikachu"))

        assertEquals(QuickCatchDexEntryStatus.REGISTERED, context.dexEntryStatus)
        assertEquals(true, context.isAlreadyCaught)
        assertFalse(context.isMostWanted ?: true)
    }

    @Test
    fun missingDexEntryDisablesRepeatBallFact() {
        val context = resolve(spawn("Flabébé (Red)"), missing = setOf("flabebe red"))

        assertEquals(QuickCatchDexEntryStatus.MISSING, context.dexEntryStatus)
        assertEquals(false, context.isAlreadyCaught)
    }

    @Test
    fun absentPokedexSnapshotKeepsDexFactUnknown() {
        val context = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Cacnea"),
            pokedexSnapshot = null,
            mostWantedNames = emptySet(),
            speciesIndex = index
        )

        assertEquals(QuickCatchDexEntryStatus.UNKNOWN, context.dexEntryStatus)
        assertNull(context.isAlreadyCaught)
    }

    @Test
    fun sameGlobalSpawnUsesIndependentProfilePokedexSnapshots() {
        val globalSpawn = spawn("Cacnea")
        val missingForProfileA = resolve(globalSpawn, missing = setOf("cacnea"))
        val registeredForProfileB = resolve(globalSpawn, missing = emptySet())

        assertEquals(QuickCatchDexEntryStatus.MISSING, missingForProfileA.dexEntryStatus)
        assertEquals(QuickCatchDexEntryStatus.REGISTERED, registeredForProfileB.dexEntryStatus)
    }

    @Test
    fun unresolvedCatalogSpawnNeverInventsRegisteredDexEntry() {
        val unresolvedSpawn = spawn("Unmapped Form")

        assertNull(unresolvedSpawn.dexKey)
        assertEquals(UNKNOWN, status(unresolvedSpawn, missing = emptySet()))
    }

    @Test
    fun mostWantedMembershipUsesOnlyProvidedProfileSelection() {
        val selectedContext = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Cacnea"),
            pokedexSnapshot = null,
            mostWantedNames = setOf("Cacnea"),
            speciesIndex = index
        )
        val otherProfileContext = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Cacnea"),
            pokedexSnapshot = null,
            mostWantedNames = setOf("Pikachu"),
            speciesIndex = index
        )

        assertTrue(selectedContext.isMostWanted == true)
        assertFalse(otherProfileContext.isMostWanted ?: true)
    }

    @Test
    fun femaleFormMatchesItsSpeciesEntry() {
        assertEquals(MISSING, status(spawn("Meowstic (F)"), missing = setOf("meowstic")))
        assertEquals(REGISTERED, status(spawn("Meowstic (F)"), missing = emptySet()))
    }

    @Test
    fun vivillonPatternsMatchTheMeadowEntry() {
        listOf("Vivillon (Jungle)", "Vivillon (Fancy)", "Vivillon").forEach { name ->
            assertEquals(name, MISSING, status(spawn(name), missing = setOf("vivillon meadow")))
        }
        listOf("Vivillon (Jungle)", "Vivillon (Fancy)").forEach { name ->
            assertEquals(name, REGISTERED, status(spawn(name), missing = emptySet()))
        }
    }

    @Test
    fun officialFormsMatchTheirSpeciesEntry() {
        assertEquals(MISSING, status(spawn("Deerling (Autumn)"), missing = setOf("deerling spring")))
        assertEquals(MISSING, status(spawn("Rotom (Fan)"), missing = setOf("rotom")))
        assertEquals(MISSING, status(spawn("Flabébé (Blue)"), missing = setOf("flabebe red")))
    }

    @Test
    fun unownLettersMatchTheUnownEntry() {
        assertEquals(MISSING, status(spawn("Unown (Q)"), missing = setOf("unown a")))
        assertEquals(REGISTERED, status(spawn("Unown (Q)"), missing = emptySet()))
    }

    @Test
    fun regionalFormsMatchTheirSpeciesAndNotALookAlike() {
        assertEquals(MISSING, status(spawn("His Growlithe"), missing = setOf("growlithe")))
        assertEquals(MISSING, status(spawn("Gal Mr. Mime"), missing = setOf("mr mime")))
        assertEquals(REGISTERED, status(spawn("Mr. Rime"), missing = setOf("mr mime")))
        assertEquals(MISSING, status(spawn("Pal Tauros (Blaze)"), missing = setOf("tauros")))
    }

    /**
     * The prompt's expectation was "MISSING if Arceus is normally spawnable in the catalog,
     * else UNKNOWN". The catalog marks Arceus not normally spawnable, yet the result is
     * MISSING: an entry listed as missing is direct evidence, while coverage only decides
     * what the absence of an entry means. The rule's first line, the whole-catalog table
     * below and tmc-backend's alert all say MISSING here.
     */
    @Test
    fun arceusTypeFollowsTheArceusEntry() {
        assertFalse(index.speciesOf("Arceus")!!.coveredBySpawnableSnapshot)
        assertEquals(MISSING, status(spawn("Fire Arceus"), missing = setOf("arceus")))
        assertEquals(UNKNOWN, status(spawn("Fire Arceus"), missing = emptySet()))
    }

    @Test
    fun pcgSinAndCloneFormsMatchTheirSpecies() {
        assertEquals(MISSING, status(spawn("Sin Alo Marowak"), missing = setOf("marowak")))
        assertEquals(REGISTERED, status(spawn("Sin Alo Marowak"), missing = emptySet()))
        assertEquals(MISSING, status(spawn("Venusaur (Clone)"), missing = setOf("venusaur")))
        // Venusaur is a starter, which a Spawnable-only capture never lists.
        assertEquals(UNKNOWN, status(spawn("Venusaur (Clone)"), missing = emptySet()))
    }

    /**
     * PCG lists the 81 starters under "Starters", never under "Spawnable", so today's
     * Spawnable-only reading cannot show one missing. Its absence from the capture proves
     * nothing: UNKNOWN, and no Repeat Ball bonus. Until this rule, the panel said Registered.
     */
    @Test
    fun starterAbsentFromTheCaptureIsUnknownWithoutRepeatBall() {
        listOf("Piplup", "Bulbasaur", "His Typhlosion", "Venusaur (Clone)").forEach { name ->
            val starter = spawn(name)
            val context = resolve(starter, missing = setOf("pikachu"))

            assertEquals(name, UNKNOWN, context.dexEntryStatus)
            assertNull(name, context.isAlreadyCaught)
            assertEquals(name, 0, repeatBallScore(starter, context))
        }
        // A starter in the missing set is still direct evidence.
        assertEquals(MISSING, status(spawn("Piplup"), missing = setOf("piplup")))
    }

    /** The 122 legendaries are a category of their own too, and a capture never lists them. */
    @Test
    fun legendaryAbsentFromTheCaptureIsUnknownWithoutRepeatBall() {
        listOf("Mewtwo", "Fire Arceus", "Basculegion (F)").forEach { name ->
            val legendary = spawn(name)
            val context = resolve(legendary, missing = setOf("pikachu"))

            assertEquals(name, UNKNOWN, context.dexEntryStatus)
            assertNull(name, context.isAlreadyCaught)
            assertEquals(name, 0, repeatBallScore(legendary, context))
        }
    }

    @Test
    fun spawnableSpeciesAbsentFromTheCaptureIsRegisteredWithRepeatBall() {
        val cacnea = spawn("Cacnea")
        val context = resolve(cacnea, missing = setOf("pikachu"))

        assertEquals(REGISTERED, context.dexEntryStatus)
        assertEquals(true, context.isAlreadyCaught)
        assertEquals(75, repeatBallScore(cacnea, context))
    }

    @Test
    fun megaFormMatchesItsSpecies() {
        assertEquals(MISSING, status(spawn("Mega Medicham"), missing = setOf("medicham")))
    }

    @Test
    fun porygonAndPorygonZStayTwoSpecies() {
        assertEquals(REGISTERED, status(spawn("Porygon"), missing = setOf("porygon z")))
        assertEquals(REGISTERED, status(spawn("Porygon-Z"), missing = setOf("porygon")))
        assertEquals(MISSING, status(spawn("Porygon-Z"), missing = setOf("porygon z")))
    }

    @Test
    fun speciesNotNormallySpawnableStaysUnknownUnlessListedMissing() {
        assertFalse(index.speciesOf("Mewtwo")!!.coveredBySpawnableSnapshot)
        assertEquals(UNKNOWN, status(spawn("Mewtwo"), missing = setOf("pikachu")))
        assertEquals(MISSING, status(spawn("Mewtwo"), missing = setOf("mewtwo")))
    }

    @Test
    fun nameUnknownToTheCatalogIsNeverRegistered() {
        val unknownSpawn = spawn("MissingNo")

        assertEquals(UNKNOWN, status(unknownSpawn, missing = emptySet()))
        assertEquals(UNKNOWN, status(unknownSpawn, missing = setOf("missingno")))
        // A form the catalog lacks still reaches its species through the name it starts with.
        assertEquals(MISSING, status(spawn("Vivillon (Meadow)"), missing = setOf("vivillon")))
        assertEquals(UNKNOWN, status(spawn("Vivillon (Meadow)"), missing = emptySet()))
    }

    @Test
    fun catalogLookupFailureLeavesTheDexFactUnknown() {
        val failedIndex = QuickCatchProfileSpawnContextProvider.loadSpeciesIndex {
            throw IllegalStateException("catalog unavailable")
        }

        assertNull(failedIndex)
        listOf(emptySet(), setOf("vivillon meadow")).forEach { missing ->
            val context = QuickCatchProfileSpawnContextResolver.resolve(
                spawn = spawn("Vivillon (Jungle)"),
                pokedexSnapshot = PcgPokedexSnapshot(missingNameKeys = missing, updatedAtMs = 1_000L),
                mostWantedNames = emptySet(),
                speciesIndex = failedIndex
            )
            assertEquals(UNKNOWN, context.dexEntryStatus)
            assertNull(context.isAlreadyCaught)
        }
    }

    @Test
    fun missingKeyWithoutSpeciesMatchesNothingAndIsReportedOnceWithoutTheKey() {
        val reports = mutableListOf<String>()
        val reporter = UnresolvedMainDexKeyReporter(maxReportedKeys = 64, sink = reports::add)
        val missing = setOf("zzzmon", "vivillon meadow")

        repeat(3) {
            val context = QuickCatchProfileSpawnContextResolver.resolve(
                spawn = spawn("Cacnea"),
                pokedexSnapshot = PcgPokedexSnapshot(missingNameKeys = missing, updatedAtMs = 1_000L),
                mostWantedNames = emptySet(),
                speciesIndex = index,
                onUnresolvedMissingKey = reporter::report
            )
            assertEquals(REGISTERED, context.dexEntryStatus)
        }

        assertEquals(1, reports.size)
        assertFalse(reports.single().contains("zzzmon"))
    }

    @Test
    fun repeatBallFollowsTheDexFactFromResolverToRecommendation() {
        val vivillon = spawn("Vivillon (Jungle)")

        val missing = resolve(vivillon, missing = setOf("vivillon meadow"))
        val registered = resolve(vivillon, missing = emptySet())
        val unknown = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = vivillon,
            pokedexSnapshot = null,
            mostWantedNames = emptySet(),
            speciesIndex = index
        )

        assertEquals(MISSING, missing.dexEntryStatus)
        assertEquals(REGISTERED, registered.dexEntryStatus)
        assertEquals(UNKNOWN, unknown.dexEntryStatus)
        assertEquals(0, repeatBallScore(vivillon, missing))
        assertEquals(0, repeatBallScore(vivillon, unknown))
        assertEquals(75, repeatBallScore(vivillon, registered))
    }

    @Test
    fun mostWantedKeepsMatchingTheFullFormName() {
        val context = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Vivillon (Fancy)"),
            pokedexSnapshot = PcgPokedexSnapshot(
                missingNameKeys = setOf("vivillon meadow"),
                updatedAtMs = 1_000L
            ),
            mostWantedNames = setOf("Vivillon (Meadow)"),
            speciesIndex = index
        )

        assertEquals(MISSING, context.dexEntryStatus)
        assertEquals(false, context.isMostWanted)
    }

    /**
     * Every catalog name, as a spawn: MISSING with its own name or its species' name in the
     * missing set, MISSING with any other form of its species there, and otherwise
     * REGISTERED when the PCG page listed its species under Spawnable, UNKNOWN when it listed
     * it under Starters or Legendaries. A missing set holding every other species never makes
     * it MISSING.
     *
     * The expected status comes from the captured page, not from the index's own coverage,
     * so that the table cannot agree with a wrong coverage rule.
     */
    @Test
    fun everyCatalogNameFollowsTheRule() {
        val counts = sortedMapOf<String, Int>()
        val entries = BundledPcgCatalogFixture.pcgCatalog.entries
        val speciesKeyByName = entries.associate { entry ->
            entry.normalizedName to index.speciesOf(entry.normalizedName)!!.key
        }
        val capturedSpawnableKeys = CapturedPcgMainDexCategories.entriesByCategory
            .getValue(MainDexCategory.SPAWNABLE)
            .mapTo(HashSet()) { pageEntry -> index.speciesOf(pageEntry.name)!!.key }
        val failures = mutableListOf<String>()

        entries.forEach { entry ->
            val spawn = spawn(entry.displayName)
            val species = index.speciesOf(entry.displayName)!!
            val sameSpecies = speciesKeyByName.filterValues { key -> key == species.key }.keys
            val otherSpecies = speciesKeyByName.filterValues { key -> key != species.key }.keys

            fun check(label: String, missing: Set<String>, expected: QuickCatchDexEntryStatus) {
                val actual = status(spawn, missing)
                if (actual != expected) failures += "${entry.displayName} $label: $actual"
            }

            check("own name", setOf(entry.normalizedName), MISSING)
            check("species name", setOf(species.key), MISSING)
            sameSpecies.forEach { form -> check("form $form", setOf(form), MISSING) }

            val absentStatus = if (species.key in capturedSpawnableKeys) REGISTERED else UNKNOWN
            check("empty", emptySet(), absentStatus)
            check("every other species", otherSpecies, absentStatus)
            counts[absentStatus.name] = (counts[absentStatus.name] ?: 0) + 1
        }

        assertEquals(emptyList<String>(), failures)
        // 1,118 names of Spawnable species; 165 of legendary species and 91 of starter species.
        assertEquals(sortedMapOf("REGISTERED" to 1_118, "UNKNOWN" to 256), counts)
    }

    private fun repeatBallScore(
        spawn: SpawnSnapshot,
        context: QuickCatchProfileSpawnContext
    ): Int {
        return CatchBallRecommender.recommend(
            presets = CatchBallCatalog.createSmartCandidatePresets(),
            spawn = spawn,
            buddy = null,
            isAlreadyCaught = context.isAlreadyCaught
        ).single { recommendation -> recommendation.preset.ballId == "repeat_ball" }.score
    }

    private fun resolve(
        spawn: SpawnSnapshot,
        missing: Set<String>
    ): QuickCatchProfileSpawnContext {
        return QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn,
            pokedexSnapshot = PcgPokedexSnapshot(
                missingNameKeys = missing,
                updatedAtMs = 1_000L
            ),
            mostWantedNames = emptySet(),
            speciesIndex = index
        )
    }

    private fun status(spawn: SpawnSnapshot, missing: Set<String>): QuickCatchDexEntryStatus {
        return resolve(spawn, missing).dexEntryStatus
    }

    private fun spawn(name: String): SpawnSnapshot {
        return BundledPcgCatalogFixture.spawn(name).let { snapshot ->
            // Same tier and type as before for the names the catalog does not know.
            if (snapshot.dexKey == null) {
                snapshot.copy(tier = PcgPokemonTier.C, type1 = "Grass")
            } else {
                snapshot
            }
        }
    }

    private companion object {
        val REGISTERED = QuickCatchDexEntryStatus.REGISTERED
        val MISSING = QuickCatchDexEntryStatus.MISSING
        val UNKNOWN = QuickCatchDexEntryStatus.UNKNOWN
    }
}
