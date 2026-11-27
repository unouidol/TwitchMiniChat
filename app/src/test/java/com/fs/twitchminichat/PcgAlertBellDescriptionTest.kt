package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bell button's accessibility description names each active category by the menu's own
 * string resources, in the menu's order, and names "no standard spawn alerts" when none is on.
 */
class PcgAlertBellDescriptionTest {

    @Test
    fun nothingOnNamesTheNoAlertsTitleAlone() {
        assertEquals(
            listOf(R.string.spawn_alert_mode_none),
            PcgAlertBellDescription.labelResIds(PcgProfileAlertSelection.DISABLED)
        )
    }

    @Test
    fun missingAltDexFormAlertsAloneAreNamedByTheirName() {
        assertEquals(
            listOf(R.string.pcg_alt_dex_form_alerts_name),
            PcgAlertBellDescription.labelResIds(selection(altDex = true))
        )
    }

    @Test
    fun mostWantedAloneIsNamedByItsLabel() {
        assertEquals(
            listOf(R.string.pcg_most_wanted_alert_label),
            PcgAlertBellDescription.labelResIds(selection(mostWanted = true))
        )
    }

    @Test
    fun eventSpawnsAloneAreNamedByTheirLabel() {
        assertEquals(
            listOf(R.string.pcg_event_spawn_alert_label),
            PcgAlertBellDescription.labelResIds(selection(event = true))
        )
    }

    @Test
    fun eachStandardModeIsNamedByItsTitle() {
        for (mode in PcgSpawnAlertMode.entries.filter { it != PcgSpawnAlertMode.NONE }) {
            assertEquals(
                listOf(mode.titleRes),
                PcgAlertBellDescription.labelResIds(selection(mode = mode))
            )
        }
    }

    @Test
    fun everyCategoryOnIsNamedInTheMenusOrder() {
        assertEquals(
            listOf(
                R.string.spawn_alert_mode_dex_only,
                R.string.pcg_event_spawn_alert_label,
                R.string.pcg_most_wanted_alert_label,
                R.string.pcg_alt_dex_form_alerts_name
            ),
            PcgAlertBellDescription.labelResIds(
                selection(
                    mode = PcgSpawnAlertMode.DEX_ONLY,
                    event = true,
                    mostWanted = true,
                    altDex = true
                )
            )
        )
    }

    private fun selection(
        mode: PcgSpawnAlertMode = PcgSpawnAlertMode.NONE,
        event: Boolean = false,
        mostWanted: Boolean = false,
        altDex: Boolean = false
    ) = PcgProfileAlertSelection(
        spawnSettings = PcgSpawnAlertSettings(regularMode = mode, eventSpawnsEnabled = event),
        mostWantedEnabled = mostWanted,
        altDexFormAlertsEnabled = altDex
    )
}
