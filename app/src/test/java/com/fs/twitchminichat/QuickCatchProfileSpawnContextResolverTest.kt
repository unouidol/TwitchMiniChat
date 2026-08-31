package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that profile collection facts never leak into the global spawn snapshot. */
class QuickCatchProfileSpawnContextResolverTest {

    @Test
    fun registeredDexEntryEnablesRepeatBallFact() {
        val context = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Cacnea"),
            pokedexSnapshot = PcgPokedexSnapshot(
                missingNameKeys = setOf("pikachu"),
                updatedAtMs = 1_000L
            ),
            mostWantedNames = emptySet()
        )

        assertEquals(QuickCatchDexEntryStatus.REGISTERED, context.dexEntryStatus)
        assertEquals(true, context.isAlreadyCaught)
        assertFalse(context.isMostWanted ?: true)
    }

    @Test
    fun missingDexEntryDisablesRepeatBallFact() {
        val context = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Flabébé (Red)"),
            pokedexSnapshot = PcgPokedexSnapshot(
                missingNameKeys = setOf("flabebe red"),
                updatedAtMs = 1_000L
            ),
            mostWantedNames = emptySet()
        )

        assertEquals(QuickCatchDexEntryStatus.MISSING, context.dexEntryStatus)
        assertEquals(false, context.isAlreadyCaught)
    }

    @Test
    fun absentPokedexSnapshotKeepsDexFactUnknown() {
        val context = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Cacnea"),
            pokedexSnapshot = null,
            mostWantedNames = emptySet()
        )

        assertEquals(QuickCatchDexEntryStatus.UNKNOWN, context.dexEntryStatus)
        assertNull(context.isAlreadyCaught)
    }

    @Test
    fun sameGlobalSpawnUsesIndependentProfilePokedexSnapshots() {
        val globalSpawn = spawn("Cacnea")
        val missingForProfileA = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = globalSpawn,
            pokedexSnapshot = PcgPokedexSnapshot(
                missingNameKeys = setOf("cacnea"),
                updatedAtMs = 1_000L
            ),
            mostWantedNames = emptySet()
        )
        val registeredForProfileB = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = globalSpawn,
            pokedexSnapshot = PcgPokedexSnapshot(
                missingNameKeys = emptySet(),
                updatedAtMs = 1_000L
            ),
            mostWantedNames = emptySet()
        )

        assertEquals(QuickCatchDexEntryStatus.MISSING, missingForProfileA.dexEntryStatus)
        assertEquals(
            QuickCatchDexEntryStatus.REGISTERED,
            registeredForProfileB.dexEntryStatus
        )
    }

    @Test
    fun unresolvedCatalogSpawnNeverInventsRegisteredDexEntry() {
        val unresolvedSpawn = spawn("Unmapped Form").copy(dexKey = null)
        val context = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = unresolvedSpawn,
            pokedexSnapshot = PcgPokedexSnapshot(
                missingNameKeys = emptySet(),
                updatedAtMs = 1_000L
            ),
            mostWantedNames = emptySet()
        )

        assertEquals(QuickCatchDexEntryStatus.UNKNOWN, context.dexEntryStatus)
        assertNull(context.isAlreadyCaught)
    }

    @Test
    fun mostWantedMembershipUsesOnlyProvidedProfileSelection() {
        val selectedContext = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Cacnea"),
            pokedexSnapshot = null,
            mostWantedNames = setOf("Cacnea")
        )
        val otherProfileContext = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn("Cacnea"),
            pokedexSnapshot = null,
            mostWantedNames = setOf("Pikachu")
        )

        assertTrue(selectedContext.isMostWanted == true)
        assertFalse(otherProfileContext.isMostWanted ?: true)
    }

    private fun spawn(name: String): SpawnSnapshot {
        return SpawnSnapshot(
            rawName = name,
            dexKey = name,
            displayName = name,
            tier = PcgPokemonTier.C,
            type1 = "Grass",
            type2 = null,
            weightKg = null,
            baseSpeed = null,
            baseHp = null,
            evolvesTwice = null,
            seenAtMs = 1_000L
        )
    }
}
