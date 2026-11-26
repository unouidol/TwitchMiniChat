package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The acknowledged selection's alt-dex category (`docs/design-alt-dex-alert.md`, *f* and *j*). */
class PcgProfileAlertAcknowledgementStoreTest {

    private val preferences = ThreadSafeTestSharedPreferences()
    private val profileId = "profile-a"

    private val active = PcgProfileAlertSelection(
        spawnSettings = PcgSpawnAlertSettings(PcgSpawnAlertMode.DEX_ONLY, eventSpawnsEnabled = true),
        mostWantedEnabled = true,
        altDexFormAlertsEnabled = true
    )

    private fun read() = PcgProfileAlertAcknowledgementStore.read(preferences, profileId)

    @Test
    fun aConfirmedCategoryRoundTrips() {
        PcgProfileAlertAcknowledgementStore.record(preferences, profileId, active)
        PcgProfileAlertAcknowledgementStore.setAltDexFormAlertsConfirmed(
            preferences,
            profileId,
            confirmed = true
        )

        assertEquals(active, read())
    }

    /** An entry written before the category existed reads as off, the server's default too. */
    @Test
    fun anEntryWithoutTheKeyReadsAsOff() {
        preferences.edit()
            .putInt("regular_mode_$profileId", PcgSpawnAlertMode.DEX_ONLY.id)
            .putBoolean("event_spawns_$profileId", true)
            .putBoolean("most_wanted_$profileId", true)
            .commit()

        assertEquals(active.copy(altDexFormAlertsEnabled = false), read())
    }

    /**
     * A delivery answer does not raise the category: the alert also needs the list on the
     * server, and only the turn-on's upload step confirms that.
     */
    @Test
    fun recordingADeliveryDoesNotRaiseTheCategory() {
        PcgProfileAlertAcknowledgementStore.record(preferences, profileId, active)

        assertEquals(active.copy(altDexFormAlertsEnabled = false), read())
    }

    @Test
    fun recordingADeliveryKeepsAConfirmedCategoryAndLowersOneTurnedOff() {
        PcgProfileAlertAcknowledgementStore.record(preferences, profileId, active)
        PcgProfileAlertAcknowledgementStore.setAltDexFormAlertsConfirmed(preferences, profileId, true)

        PcgProfileAlertAcknowledgementStore.record(preferences, profileId, active)
        assertEquals(true, read()?.altDexFormAlertsEnabled)

        PcgProfileAlertAcknowledgementStore.record(
            preferences,
            profileId,
            active.copy(altDexFormAlertsEnabled = false)
        )
        assertEquals(false, read()?.altDexFormAlertsEnabled)

        PcgProfileAlertAcknowledgementStore.record(preferences, profileId, active)
        assertEquals(false, read()?.altDexFormAlertsEnabled)
    }

    @Test
    fun theCategoryIsMovedOnlyWhereAnAcknowledgementExists() {
        assertFalse(
            PcgProfileAlertAcknowledgementStore.setAltDexFormAlertsConfirmed(preferences, profileId, true)
        )
        assertFalse(
            PcgProfileAlertAcknowledgementStore.setAltDexFormAlertsConfirmed(preferences, profileId, false)
        )
        assertTrue(preferences.all.isEmpty())
        assertNull(read())
        assertTrue(PcgProfileAlertAcknowledgementStore.knownProfileIds(preferences).isEmpty())
    }

    @Test
    fun movingTheCategoryLeavesTheOtherCategoriesAsAcknowledged() {
        PcgProfileAlertAcknowledgementStore.record(preferences, profileId, active)

        assertTrue(
            PcgProfileAlertAcknowledgementStore.setAltDexFormAlertsConfirmed(preferences, profileId, true)
        )
        assertEquals(active, read())

        assertTrue(
            PcgProfileAlertAcknowledgementStore.setAltDexFormAlertsConfirmed(preferences, profileId, false)
        )
        assertEquals(active.copy(altDexFormAlertsEnabled = false), read())
    }
}
