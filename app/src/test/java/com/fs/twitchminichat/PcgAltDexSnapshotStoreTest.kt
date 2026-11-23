package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Store 3 of `docs/design-pcg-dex-readings.md`: what accepted "Update Alt Dex" readings
 * established, per profile.
 *
 * With "Obtained" off the alt dex lists only the forms not obtained, so a known form that a
 * reading does not list is obtained, and PCG never resets the alt dex: obtained forms only
 * grow, while the forms not obtained are whatever the last reading listed.
 */
class PcgAltDexSnapshotStoreTest {

    private val unownB = form("Unown (B)", 10510)
    private val unownC = form("Unown (C)", 10511)
    private val aloVulpix = form("Alo Vulpix", 10103)
    private val pcgAbsol = form("PCG Absol", 100401)
    private val bundled = listOf(unownB, unownC, aloVulpix)

    @Test
    fun firstReading_obtainsEveryKnownFormItDoesNotList() {
        val snapshot = PcgAltDexSnapshot.afterReading(
            previous = null,
            knownForms = bundled,
            listed = listOf(unownC),
            readAtMs = 5L
        )

        assertEquals(listOf(unownB, aloVulpix), snapshot.obtained)
        assertEquals(listOf(unownC), snapshot.missing)
        assertEquals(5L, snapshot.readAtMs)
    }

    @Test
    fun obtainedFormsOnlyGrow_evenWhenAReadingListsOneAgain() {
        val first = PcgAltDexSnapshot.afterReading(null, bundled, listOf(unownC, aloVulpix), 1L)
        assertEquals(listOf(unownB), first.obtained)

        val second = PcgAltDexSnapshot.afterReading(first, bundled, listOf(unownB, unownC), 2L)

        assertEquals(
            "a form once obtained stays obtained",
            listOf(unownB, aloVulpix),
            second.obtained
        )
        assertEquals(listOf(unownB, unownC), second.missing)
    }

    @Test
    fun missingFormsAreReplacedByEachReading() {
        val first = PcgAltDexSnapshot.afterReading(null, bundled, listOf(unownB, unownC), 1L)
        val second = PcgAltDexSnapshot.afterReading(first, bundled, listOf(unownC), 2L)

        assertEquals(listOf(unownC), second.missing)
        // Forms obtained earlier come first; those a reading newly obtains follow them.
        assertEquals(listOf(aloVulpix, unownB), second.obtained)
    }

    /** A form PCG adds after the bundled list becomes known once a reading lists it. */
    @Test
    fun aListedFormOutsideTheBundledListIsKnownFromThenOn() {
        val first = PcgAltDexSnapshot.afterReading(null, bundled, listOf(unownB, pcgAbsol), 1L)
        assertEquals(listOf(unownC, aloVulpix), first.obtained)

        val second = PcgAltDexSnapshot.afterReading(first, bundled, listOf(unownB), 2L)

        assertEquals(listOf(unownC, aloVulpix, pcgAbsol), second.obtained)
        assertEquals(listOf(unownB), second.missing)
    }

    @Test
    fun aRepeatedListedFormIsKeptOnce() {
        val snapshot = PcgAltDexSnapshot.afterReading(null, bundled, listOf(unownB, unownB), 1L)

        assertEquals(listOf(unownB), snapshot.missing)
    }

    @Test
    fun storedReadingsRoundTripPerProfile() {
        val preferences = InMemorySharedPreferences()

        val saved = PcgAltDexSnapshotStore.saveReading(
            preferences, PROFILE_A, bundled, listOf(unownC, pcgAbsol), 7L
        )
        val loaded = PcgAltDexSnapshotStore.load(preferences, PROFILE_A)

        assertNotNull(saved)
        assertEquals(saved, loaded)
        assertEquals(listOf(unownB, aloVulpix), loaded!!.obtained)
        assertEquals(listOf(unownC, pcgAbsol), loaded.missing)
        assertNull(PcgAltDexSnapshotStore.load(preferences, PROFILE_B))
    }

    @Test
    fun aStoredReadingGrowsWithTheNextOne() {
        val preferences = InMemorySharedPreferences()
        PcgAltDexSnapshotStore.saveReading(preferences, PROFILE_A, bundled, listOf(unownB, unownC), 1L)

        PcgAltDexSnapshotStore.saveReading(preferences, PROFILE_A, bundled, listOf(unownC), 2L)
        PcgAltDexSnapshotStore.saveReading(preferences, PROFILE_A, bundled, listOf(unownB, unownC), 3L)

        val loaded = PcgAltDexSnapshotStore.load(preferences, PROFILE_A)!!
        assertEquals(listOf(aloVulpix, unownB), loaded.obtained)
        assertEquals(listOf(unownB, unownC), loaded.missing)
        assertEquals(3L, loaded.readAtMs)
    }

    @Test
    fun clearingOneProfileKeepsTheOthers() {
        val preferences = InMemorySharedPreferences()
        PcgAltDexSnapshotStore.saveReading(preferences, PROFILE_A, bundled, listOf(unownB), 1L)
        PcgAltDexSnapshotStore.saveReading(preferences, PROFILE_B, bundled, listOf(unownC), 1L)

        PcgAltDexSnapshotStore.clearProfile(preferences, PROFILE_A)

        assertNull(PcgAltDexSnapshotStore.load(preferences, PROFILE_A))
        assertNotNull(PcgAltDexSnapshotStore.load(preferences, PROFILE_B))
        assertEquals(1, preferences.keys.size)
    }

    /** After a clear the next reading starts from nothing: nothing obtained survives it. */
    @Test
    fun aReadingAfterAClearStartsAgain() {
        val preferences = InMemorySharedPreferences()
        PcgAltDexSnapshotStore.saveReading(preferences, PROFILE_A, bundled, listOf(unownB), 1L)
        PcgAltDexSnapshotStore.clearProfile(preferences, PROFILE_A)

        val after = PcgAltDexSnapshotStore.saveReading(
            preferences, PROFILE_A, emptyList(), listOf(unownB), 2L
        )

        assertEquals(emptyList<PcgAltDexForm>(), after!!.obtained)
    }

    @Test
    fun aBlankProfileStoresNothing() {
        val preferences = InMemorySharedPreferences()

        assertNull(PcgAltDexSnapshotStore.saveReading(preferences, "  ", bundled, listOf(unownB), 1L))
        assertTrue(preferences.keys.isEmpty())
    }

    @Test
    fun anUnknownSchemaReadsAsNoReading() {
        val preferences = InMemorySharedPreferences()
        PcgAltDexSnapshotStore.saveReading(preferences, PROFILE_A, bundled, listOf(unownB), 1L)
        val key = preferences.keys.single()
        preferences.edit().putString(
            key,
            preferences.getString(key, null)!!.replace("\"schemaVersion\":1", "\"schemaVersion\":2")
        ).apply()

        assertNull(PcgAltDexSnapshotStore.load(preferences, PROFILE_A))
    }

    /** Store 3 is its own file, never store 1's. */
    @Test
    fun usesItsOwnPreferencesFile() {
        assertEquals("pcg_alt_dex_snapshots", PcgAltDexSnapshotStore.PREFERENCES_NAME)
        assertTrue(
            readAppFile("src/main/java/com/fs/twitchminichat/PcgPokedexSnapshotStore.kt")
                .contains("PREFERENCES_NAME = \"pcg_pokedex_snapshots\"")
        )
    }

    private fun form(name: String, number: Int) = PcgAltDexForm(
        key = com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer.normalize(name),
        name = name,
        number = number
    )

    private fun readAppFile(relativePath: String): String {
        return String(
            java.nio.file.Files.readAllBytes(
                BundledPcgCatalogFixture.findAppModuleDirectory().resolve(relativePath)
            ),
            Charsets.UTF_8
        )
    }

    private companion object {
        const val PROFILE_A = "profile-a"
        const val PROFILE_B = "profile-b"
    }
}
