package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the independent profile-alert delivery and synchronization policy. */
class PcgProfileAlertSelectionTest {

    @Test
    fun mostWantedOnly_keepsFirebaseDeliveryActive() {
        val selection = selection(
            regularMode = PcgSpawnAlertMode.NONE,
            eventSpawnsEnabled = false,
            mostWantedEnabled = true
        )

        assertTrue(selection.requiresFirebaseDelivery)
    }

    @Test
    fun eventOnly_keepsFirebaseDeliveryActive() {
        val selection = selection(
            regularMode = PcgSpawnAlertMode.NONE,
            eventSpawnsEnabled = true,
            mostWantedEnabled = false
        )

        assertTrue(selection.requiresFirebaseDelivery)
    }

    @Test
    fun everythingDisabled_turnsFirebaseDeliveryOff() {
        assertFalse(disabledSelection().requiresFirebaseDelivery)
    }

    @Test
    fun mostWantedOnly_registrationRestoresSelectionAfterTokenUpload() {
        val plan = PcgProfileRegistrationSyncPlanner.buildPlan(
            selection(
                regularMode = PcgSpawnAlertMode.NONE,
                eventSpawnsEnabled = false,
                mostWantedEnabled = true
            )
        )

        assertEquals(
            listOf(
                PcgProfileRegistrationSyncStep.REGISTER_TOKEN,
                PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION
            ),
            plan
        )
    }

    @Test
    fun disabledProfile_registrationPerformsNoNetworkSteps() {
        assertEquals(
            emptyList<PcgProfileRegistrationSyncStep>(),
            PcgProfileRegistrationSyncPlanner.buildPlan(
                disabledSelection()
            )
        )
    }

    @Test
    fun enablingMostWantedFromDisabled_registersDeliveryBeforeWatchlist() {
        val plan = PcgProfileAlertSyncPlanner.buildPlan(
            current = disabledSelection(),
            requested = selection(
                regularMode = PcgSpawnAlertMode.NONE,
                eventSpawnsEnabled = false,
                mostWantedEnabled = true
            )
        )

        assertEquals(
            listOf(
                PcgProfileAlertSyncStep.FIREBASE_DELIVERY,
                PcgProfileAlertSyncStep.MOST_WANTED
            ),
            plan
        )
    }

    @Test
    fun enablingMostWantedOnActiveProfile_doesNotReregisterDelivery() {
        val current = selection(
            regularMode = PcgSpawnAlertMode.DEX_ONLY,
            eventSpawnsEnabled = false,
            mostWantedEnabled = false
        )

        val plan = PcgProfileAlertSyncPlanner.buildPlan(
            current = current,
            requested = current.copy(mostWantedEnabled = true)
        )

        assertEquals(
            listOf(PcgProfileAlertSyncStep.MOST_WANTED),
            plan
        )
    }

    @Test
    fun switchingFromRegularToMostWanted_updatesWatchlistBeforeMode() {
        val plan = PcgProfileAlertSyncPlanner.buildPlan(
            current = selection(
                regularMode = PcgSpawnAlertMode.DEX_ONLY,
                eventSpawnsEnabled = false,
                mostWantedEnabled = false
            ),
            requested = selection(
                regularMode = PcgSpawnAlertMode.NONE,
                eventSpawnsEnabled = false,
                mostWantedEnabled = true
            )
        )

        assertEquals(
            listOf(
                PcgProfileAlertSyncStep.MOST_WANTED,
                PcgProfileAlertSyncStep.FIREBASE_DELIVERY
            ),
            plan
        )
    }

    @Test
    fun disablingLastMostWantedAlert_updatesWatchlistBeforeUnregistering() {
        val plan = PcgProfileAlertSyncPlanner.buildPlan(
            current = selection(
                regularMode = PcgSpawnAlertMode.NONE,
                eventSpawnsEnabled = false,
                mostWantedEnabled = true
            ),
            requested = disabledSelection()
        )

        assertEquals(
            listOf(
                PcgProfileAlertSyncStep.MOST_WANTED,
                PcgProfileAlertSyncStep.FIREBASE_DELIVERY
            ),
            plan
        )
    }

    @Test
    fun regularOrEventChangeWithStableMostWanted_updatesOnlyDelivery() {
        val current = selection(
            regularMode = PcgSpawnAlertMode.NONE,
            eventSpawnsEnabled = false,
            mostWantedEnabled = true
        )

        val plan = PcgProfileAlertSyncPlanner.buildPlan(
            current = current,
            requested = current.copy(
                spawnSettings = PcgSpawnAlertSettings(
                    regularMode = PcgSpawnAlertMode.NONE,
                    eventSpawnsEnabled = true
                )
            )
        )

        assertEquals(
            listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY),
            plan
        )
    }

    /** Builds one explicit selection without relying on persisted defaults. */
    private fun selection(
        regularMode: PcgSpawnAlertMode,
        eventSpawnsEnabled: Boolean,
        mostWantedEnabled: Boolean
    ): PcgProfileAlertSelection {
        return PcgProfileAlertSelection(
            spawnSettings = PcgSpawnAlertSettings(
                regularMode = regularMode,
                eventSpawnsEnabled = eventSpawnsEnabled
            ),
            mostWantedEnabled = mostWantedEnabled
        )
    }

    private fun disabledSelection(): PcgProfileAlertSelection {
        return selection(
            regularMode = PcgSpawnAlertMode.NONE,
            eventSpawnsEnabled = false,
            mostWantedEnabled = false
        )
    }
}
