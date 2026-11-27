package com.fs.twitchminichat

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bell menu's Missing Alt Dex form checkbox, its hint, and the message after OK (A2 of
 * `docs/design-alt-dex-alert.md`, section *f*).
 *
 * The menu is built from views in code and these are local tests, so what cannot run here is
 * pinned in the source: the checkbox is what OK reads, and the hint follows the same condition
 * as the turn-on's `NO_READING`.
 */
class PcgAltDexFormAlertMenuTest {

    @Test
    fun theHintShowsOnlyWhileTickedWithNoReading() {
        assertTrue(PcgSpawnAlertModeMenu.showsNoAltDexReadingHint(checked = true, readingStored = false))
        assertFalse(PcgSpawnAlertModeMenu.showsNoAltDexReadingHint(checked = true, readingStored = true))
        assertFalse(PcgSpawnAlertModeMenu.showsNoAltDexReadingHint(checked = false, readingStored = false))
        assertFalse(PcgSpawnAlertModeMenu.showsNoAltDexReadingHint(checked = false, readingStored = true))
    }

    /** OK sends what the checkbox shows, and the checkbox opens on what this phone holds. */
    @Test
    fun okReadsTheCheckboxWhichOpensOnThePhonesSelection() {
        val menu = mainSource("PcgSpawnAlertModeMenu.kt")

        assertTrue(menu.contains("text = context.getString(R.string.pcg_alt_dex_form_alert_label)"))
        assertTrue(menu.contains("isChecked = currentSelection.altDexFormAlertsEnabled"))
        assertTrue(menu.contains("altDexFormAlertsEnabled = altDexCheckBox.isChecked"))
        assertFalse(menu.contains("altDexFormAlertsEnabled =\n                        currentSelection"))
        assertTrue(menu.contains("altDexCheckBox.setOnCheckedChangeListener"))
        assertTrue(
            menu.contains(
                "showsNoAltDexReadingHint(altDexCheckBox.isChecked, altDexReadingStored)"
            )
        )
    }

    /**
     * The hint's condition is the turn-on's: the menu is told whether a reading is stored by
     * the same load the `ALT_DEX_UPLOAD` step uses to decide `NO_READING`.
     */
    @Test
    fun theMenuIsToldOfAReadingByTheTurnOnsOwnLoad() {
        val fragment = mainSource("ChatFragment.kt")
        val sync = mainSource("PcgAltDexAlertUpload.kt")

        assertTrue(
            fragment.contains(
                "altDexReadingStored =\n" +
                    "                PcgAltDexSnapshotStore.load(requireContext(), profileId) != null"
            )
        )
        assertTrue(sync.contains("PcgAltDexSnapshotStore.load(appContext, profileId)"))
        assertTrue(fragment.contains("PcgAltDexAlertSync.messageAfterTurnOn(altDexOutcome)"))
    }

    /**
     * The bell button's description names the category while it is on, as it does the others.
     * The description is built by [PcgAlertBellDescription]; that the chat writes it only from
     * there is pinned by `BellContentDescriptionSourceTest`.
     */
    @Test
    fun theBellsDescriptionNamesTheCategoryWhileOn() {
        val on = PcgProfileAlertSelection.DISABLED.copy(altDexFormAlertsEnabled = true)

        assertTrue(
            R.string.pcg_alt_dex_form_alerts_name in PcgAlertBellDescription.labelResIds(on)
        )
        assertFalse(
            R.string.pcg_alt_dex_form_alerts_name in
                PcgAlertBellDescription.labelResIds(PcgProfileAlertSelection.DISABLED)
        )
    }

    @Test
    fun afterOkTheTurnOnOwesTheHintOrTheRefusalAndNothingElse() {
        assertEquals(
            R.string.pcg_alt_dex_form_alert_no_reading,
            PcgAltDexAlertSync.messageAfterTurnOn(PcgAltDexAlertSyncOutcome.NO_READING)
        )
        assertEquals(
            R.string.pcg_alt_dex_alert_upload_alerts_off,
            PcgAltDexAlertSync.messageAfterTurnOn(PcgAltDexAlertSyncOutcome.ALERTS_OFF)
        )
        assertNull(PcgAltDexAlertSync.messageAfterTurnOn(PcgAltDexAlertSyncOutcome.STORED))
        assertNull(PcgAltDexAlertSync.messageAfterTurnOn(PcgAltDexAlertSyncOutcome.FAILED))
        assertNull(PcgAltDexAlertSync.messageAfterTurnOn(null))
    }

    /**
     * The texts send the user where the app really is: the 409's to the menu by its title and
     * to its OK, and the hint to the button by its label.
     */
    @Test
    fun theTextsNameTheMenuAndTheButtonAsTheAppShowsThem() {
        val strings = strings()
        val menuTitle = checkNotNull(strings["pcg_spawn_alert_dialog_title"])
        val button = checkNotNull(strings["pcg_register_alt_dex"])

        val refusal = checkNotNull(strings["pcg_alt_dex_alert_upload_alerts_off"])
        assertTrue(refusal.contains("Open $menuTitle and press OK"))

        val hint = checkNotNull(strings["pcg_alt_dex_form_alert_no_reading"])
        assertTrue(hint.contains("press $button."))
        assertTrue(checkNotNull(strings["pcg_alt_dex_alert_upload_failed"]).contains("Press $button"))
    }

    /**
     * One name for the category everywhere (owner's review of #148): the checkbox reads
     * "Enable" and the name, as "Enable Most Wanted alerts" does; the bell's description reads
     * the name alone, as it reads "Most Wanted alerts"; the 409's string and the terms gate use
     * the same name.
     */
    @Test
    fun theCategoryHasOneNameEverywhere() {
        val strings = strings()
        val name = checkNotNull(strings["pcg_alt_dex_form_alerts_name"])

        assertEquals("Missing Alt Dex form alerts", name)
        assertEquals("Enable $name", strings["pcg_alt_dex_form_alert_label"])
        assertEquals("Enable ${strings["pcg_most_wanted_alert_label"]}", strings["pcg_most_wanted_enabled"])
        assertTrue(checkNotNull(strings["pcg_alt_dex_alert_upload_alerts_off"]).contains(name))
        assertTrue(checkNotNull(strings["terms_gate_alt_dex"]).contains(name))
    }

    private fun mainSource(name: String): String {
        val directory = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/java/com/fs/twitchminichat")
        val path = Files.walk(directory).use { paths ->
            paths.filter { it.fileName.toString() == name }.findFirst().orElseThrow()
        }
        return String(Files.readAllBytes(path), Charsets.UTF_8).replace("\r\n", "\n")
    }

    private fun strings(): Map<String, String> {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/res/values/strings.xml")
        val text = String(Files.readAllBytes(path), Charsets.UTF_8)
        return Regex("""<string name="([^"]+)">(.*?)</string>""")
            .findAll(text)
            .associate { match -> match.groupValues[1] to match.groupValues[2] }
    }
}
