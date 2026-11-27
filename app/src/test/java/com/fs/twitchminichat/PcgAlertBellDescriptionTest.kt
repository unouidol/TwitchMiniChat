package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bell button's accessibility description names each active category by the menu's own
 * string resources, in the menu's order, and names "no standard spawn alerts" when none is on.
 * Its icon, tint and alpha say on exactly when the description names a category.
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

    @Test
    fun nothingOnDrawsTheBellOffRedAndDimmed() {
        assertEquals(
            PcgAlertBellDescription.Appearance(
                iconRes = R.drawable.ic_bell_off,
                tint = 0xFFFF6666.toInt(),
                alpha = 0.45f
            ),
            PcgAlertBellDescription.appearance(PcgProfileAlertSelection.DISABLED)
        )
    }

    @Test
    fun anyCategoryOnAloneDrawsTheBellOnGreenAndOpaque() {
        val on = PcgAlertBellDescription.Appearance(
            iconRes = R.drawable.ic_bell_on,
            tint = 0xFF66FF99.toInt(),
            alpha = 1.0f
        )
        val onlyOne = listOf(
            selection(altDex = true),
            selection(mostWanted = true),
            selection(event = true)
        ) + PcgSpawnAlertMode.entries
            .filter { it != PcgSpawnAlertMode.NONE }
            .map { selection(mode = it) }

        for (selection in onlyOne) {
            assertEquals(on, PcgAlertBellDescription.appearance(selection))
        }
    }

    /** The bell looks on exactly when its description names a category. */
    @Test
    fun theBellLooksOnExactlyWhenTheDescriptionNamesACategory() {
        val none = R.string.spawn_alert_mode_none
        for (mode in PcgSpawnAlertMode.entries) {
            for (event in listOf(false, true)) {
                for (mostWanted in listOf(false, true)) {
                    for (altDex in listOf(false, true)) {
                        val selection = selection(mode, event, mostWanted, altDex)
                        val namesACategory =
                            PcgAlertBellDescription.labelResIds(selection) != listOf(none)
                        assertEquals(
                            namesACategory,
                            PcgAlertBellDescription.appearance(selection).iconRes == R.drawable.ic_bell_on
                        )
                    }
                }
            }
        }
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
