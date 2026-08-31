package com.fs.twitchminichat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the independent ordinary/event notification invariant. */
class PcgSpawnAlertSettingsTest {

    @Test
    fun eventOnlySelectionRemainsActive() {
        val settings = PcgSpawnAlertSettings(
            regularMode = PcgSpawnAlertMode.NONE,
            eventSpawnsEnabled = true
        )

        assertTrue(settings.hasOrdinaryOrEventAlerts)
    }

    @Test
    fun regularOnlySelectionRemainsActive() {
        val settings = PcgSpawnAlertSettings(
            regularMode = PcgSpawnAlertMode.DEX_ONLY,
            eventSpawnsEnabled = false
        )

        assertTrue(settings.hasOrdinaryOrEventAlerts)
    }

    @Test
    fun disabledSelectionTurnsOffBothCategories() {
        assertFalse(
            PcgSpawnAlertSettings.DISABLED.hasOrdinaryOrEventAlerts
        )
    }
}
