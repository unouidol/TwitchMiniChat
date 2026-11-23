package com.fs.twitchminichat.pcg

import com.fs.twitchminichat.BundledPcgCatalogFixture
import com.fs.twitchminichat.CapturedPcgMainDexCategories
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the bundled list of the PCG alt dex's forms against what the two capture rounds of
 * 2026-11-23 recorded.
 *
 * The list is the "known forms" of reading 3 in `docs/design-pcg-dex-readings.md`: with
 * "Obtained" off the alt dex lists only the forms not obtained, so a form counts as obtained
 * only when it is known and an accepted reading does not list it. A list that lost, gained or
 * merged a form would turn that rule wrong without any other test noticing.
 */
class PcgAltDexFormsAssetTest {

    private val root: JSONObject by lazy {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory().resolve(ASSET)
        JSONObject(String(Files.readAllBytes(path), Charsets.UTF_8))
    }

    private val forms: List<Form> by lazy {
        val array = root.getJSONArray("forms")
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            assertEquals(setOf("number", "name", "locked"), item.keys().asSequence().toSet())
            Form(item.getInt("number"), item.getString("name"), item.getBoolean("locked"))
        }
    }

    @Test
    fun schemaIsVersionOne() {
        assertEquals(1, root.getInt("schemaVersion"))
        assertEquals(setOf("schemaVersion", "provenance", "forms"), root.keys().asSequence().toSet())
    }

    @Test
    fun holdsTheAltDexs371Forms() {
        assertEquals(371, forms.size)
    }

    @Test
    fun spriteNumbersAreDistinctAndInTheObservedRange() {
        val numbers = forms.map(Form::number)

        assertEquals(numbers.size, numbers.toSet().size)
        assertEquals(10004, numbers.min())
        assertEquals(100432, numbers.max())
        assertTrue("an alt-dex number is a main-dex number", numbers.none { it in 1..1025 })
    }

    @Test
    fun namesAreDistinctAsKeys() {
        val keys = forms.map { PcgPokemonNameNormalizer.normalize(it.name) }

        assertTrue(keys.none(String::isEmpty))
        assertEquals(forms.size, keys.toSet().size)
    }

    @Test
    fun namesCarryNoPrefixAndNoOuterSpace() {
        for (form in forms) {
            assertFalse(form.name, form.name.contains(LOCK) || form.name.contains(CONFETTI))
            assertEquals(form.name, form.name.trim(), form.name)
        }
    }

    @Test
    fun the46LockedFormsAreRecorded() {
        assertEquals(46, forms.count(Form::locked))
    }

    /** Base forms such as "Vivillon (Meadow)" are main-dex entries, never alt-dex forms. */
    @Test
    fun noFormIsAMainDexEntry() {
        val mainDexKeys = CapturedPcgMainDexCategories.entriesByCategory.values
            .flatten()
            .map { PcgPokemonNameNormalizer.normalize(it.name) }
            .toSet()
        assertEquals(1025, mainDexKeys.size)

        val shared = forms.filter { PcgPokemonNameNormalizer.normalize(it.name) in mainDexKeys }
        assertEquals(emptyList<Form>(), shared)
        assertFalse(forms.any { it.name == "Vivillon (Meadow)" })
    }

    @Test
    fun provenanceRecordsBothRoundsAndTheirAgreement() {
        val provenance = root.getJSONObject("provenance")
        val captures = provenance.getJSONArray("captures")
        val summary = List(captures.length()) { index ->
            val capture = captures.getJSONObject(index)
            Triple(capture.getInt("round"), capture.getString("capture"), capture.getString("counter"))
        }

        assertEquals(
            listOf(
                Triple(1, "011", "0 / 371"),
                Triple(1, "012", "0 / 371"),
                Triple(2, "006", "310 / 371"),
                Triple(2, "007", "0 / 61")
            ),
            summary
        )
        assertTrue(provenance.getString("agreement").contains("371"))
        assertTrue(provenance.getString("accounts").contains("no account name"))
    }

    private data class Form(val number: Int, val name: String, val locked: Boolean)

    private companion object {
        const val ASSET = "src/main/assets/pcg_alt_dex/pcg_alt_dex_forms.json"
        const val LOCK = "🔒"
        const val CONFETTI = "🎊"
    }
}
