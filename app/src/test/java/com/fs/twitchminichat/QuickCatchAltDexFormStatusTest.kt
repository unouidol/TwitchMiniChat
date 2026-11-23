package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.PcgAltDexKnownForms
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Smart Catch panel's Form line: shown only when the spawn's exact name is a known
 * alt-dex form, and read from the profile's accepted "Update Alt Dex" readings.
 *
 * Spawns are built from the bundled catalog and known forms from the bundled alt-dex list,
 * as on the device. The Dex entry line and the Repeat Ball rule must not move with it.
 */
class QuickCatchAltDexFormStatusTest {

    private val knownForms: List<PcgAltDexForm> by lazy {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/assets/pcg_alt_dex/pcg_alt_dex_forms.json")
        PcgAltDexKnownForms.parse(String(Files.readAllBytes(path), Charsets.UTF_8))
    }

    private val knownKeys: Set<String> by lazy { knownForms.mapTo(HashSet(), PcgAltDexForm::key) }

    /** Unown (B) obtained and Alo Vulpix not, as one accepted reading would leave them. */
    private val snapshot: PcgAltDexSnapshot by lazy {
        PcgAltDexSnapshot.afterReading(
            previous = null,
            knownForms = knownForms,
            listed = listOf(known("Alo Vulpix")),
            readAtMs = 1L
        )
    }

    @Test
    fun anObtainedFormReadsObtained() {
        assertEquals(QuickCatchAltDexFormStatus.OBTAINED, formStatus("Unown (B)", snapshot))
    }

    @Test
    fun aFormTheLastReadingListedReadsNotObtained() {
        assertEquals(QuickCatchAltDexFormStatus.NOT_OBTAINED, formStatus("Alo Vulpix", snapshot))
    }

    @Test
    fun aKnownFormWithNoReadingReadsUnknown() {
        assertEquals(QuickCatchAltDexFormStatus.UNKNOWN, formStatus("Unown (B)", altDex = null))
        assertEquals(QuickCatchAltDexFormStatus.UNKNOWN, formStatus("Alo Vulpix", altDex = null))
    }

    /** Base forms are main-dex entries, which a reset clears: the alt dex has no line for them. */
    @Test
    fun aMainDexBaseFormHasNoLine() {
        assertNull(formStatus("Vivillon (Meadow)", snapshot))
        assertNull(formStatus("Unown (A)", snapshot))
    }

    @Test
    fun aSpawnOutsideTheKnownFormsHasNoLine() {
        assertNull(formStatus("Cacnea", snapshot))
        assertNull(formStatus("Unmapped Form", snapshot))
    }

    /** The alt dex holds one entry per form: Vivillon (Fancy) obtained says nothing of Vivillon (Sun). */
    @Test
    fun formsAreMatchedByExactNameNeverBySpecies() {
        val onlyFancyListed = PcgAltDexSnapshot.afterReading(
            previous = null,
            knownForms = knownForms,
            listed = listOf(known("Vivillon (Sun)")),
            readAtMs = 1L
        )

        assertEquals(QuickCatchAltDexFormStatus.OBTAINED, formStatus("Vivillon (Fancy)", onlyFancyListed))
        assertEquals(QuickCatchAltDexFormStatus.NOT_OBTAINED, formStatus("Vivillon (Sun)", onlyFancyListed))
    }

    /** A form unknown to the catalog, such as a Mega form, is still matched by the name chat gave. */
    @Test
    fun aFormTheCatalogDoesNotKnowIsMatchedByItsName() {
        val megaSpawn = BundledPcgCatalogFixture.spawn("Mega Venusaur")

        assertNull(megaSpawn.dexKey)
        assertEquals(QuickCatchAltDexFormStatus.OBTAINED, resolve(megaSpawn, snapshot).altDexFormStatus)
    }

    /** A form a reading listed is known even when the bundled list could not be read. */
    @Test
    fun withoutTheBundledListOnlyStoredFormsAreKnown() {
        val context = resolve(
            BundledPcgCatalogFixture.spawn("Alo Vulpix"),
            snapshot,
            knownAltDexFormKeys = null
        )
        val noReading = resolve(
            BundledPcgCatalogFixture.spawn("Alo Vulpix"),
            altDex = null,
            knownAltDexFormKeys = null
        )

        assertEquals(QuickCatchAltDexFormStatus.NOT_OBTAINED, context.altDexFormStatus)
        assertNull(noReading.altDexFormStatus)
    }

    @Test
    fun theDexEntryAndTheRepeatBallFactIgnoreTheForm() {
        val pokedex = PcgPokedexSnapshot(missingNameKeys = setOf("cacnea"), updatedAtMs = 1L)
        val spawn = BundledPcgCatalogFixture.spawn("Alo Vulpix")

        val withForm = resolve(spawn, snapshot, pokedex = pokedex)
        val withoutForm = resolve(spawn, altDex = null, knownAltDexFormKeys = null, pokedex = pokedex)

        assertEquals(QuickCatchAltDexFormStatus.NOT_OBTAINED, withForm.altDexFormStatus)
        assertEquals(withoutForm.dexEntryStatus, withForm.dexEntryStatus)
        assertEquals(withoutForm.isAlreadyCaught, withForm.isAlreadyCaught)
        assertEquals(QuickCatchDexEntryStatus.REGISTERED, withForm.dexEntryStatus)
    }

    @Test
    fun eachStatusHasItsOwnLabel() {
        assertEquals(
            listOf(
                R.string.quick_catch_form_obtained,
                R.string.quick_catch_form_not_obtained,
                R.string.quick_catch_form_unknown
            ),
            QuickCatchAltDexFormStatus.entries.map(QuickCatchAltDexFormStatus::labelRes)
        )
    }

    @Test
    fun noSpawnHasNoLine() {
        val context = QuickCatchProfileSpawnContextResolver.resolve(
            spawn = null,
            pokedexSnapshot = null,
            mostWantedNames = null,
            speciesIndex = null,
            altDexSnapshot = snapshot,
            knownAltDexFormKeys = knownKeys
        )

        assertNull(context.altDexFormStatus)
    }

    private fun known(name: String): PcgAltDexForm {
        return knownForms.single { it.name == name }
    }

    private fun formStatus(spawnName: String, altDex: PcgAltDexSnapshot?): QuickCatchAltDexFormStatus? {
        return resolve(BundledPcgCatalogFixture.spawn(spawnName), altDex).altDexFormStatus
    }

    private fun resolve(
        spawn: SpawnSnapshot,
        altDex: PcgAltDexSnapshot?,
        knownAltDexFormKeys: Set<String>? = knownKeys,
        pokedex: PcgPokedexSnapshot? = null
    ): QuickCatchProfileSpawnContext {
        return QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn,
            pokedexSnapshot = pokedex,
            mostWantedNames = null,
            speciesIndex = BundledPcgCatalogFixture.speciesIndex,
            altDexSnapshot = altDex,
            knownAltDexFormKeys = knownAltDexFormKeys
        )
    }
}
