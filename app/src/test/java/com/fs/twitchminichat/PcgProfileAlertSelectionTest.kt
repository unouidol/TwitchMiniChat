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
            selection = selection(
                regularMode = PcgSpawnAlertMode.NONE,
                eventSpawnsEnabled = false,
                mostWantedEnabled = true
            ),
            acknowledged = null
        )

        assertEquals(
            listOf(
                PcgProfileRegistrationSyncStep.REGISTER_TOKEN,
                PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION
            ),
            plan
        )
    }

    /**
     * An active profile is registered even when the backend has already acknowledged
     * exactly this selection. Rejects an implementation that skips the whole pass on a
     * matching acknowledgement: the token itself may have changed, and the acknowledged
     * *alert selection* cannot tell.
     */
    @Test
    fun activeProfile_registersTokenEvenWhenSelectionIsAlreadyAcknowledged() {
        val active = selection(
            regularMode = PcgSpawnAlertMode.DEX_ONLY,
            eventSpawnsEnabled = false,
            mostWantedEnabled = false
        )

        assertEquals(
            listOf(
                PcgProfileRegistrationSyncStep.REGISTER_TOKEN,
                PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION
            ),
            PcgProfileRegistrationSyncPlanner.buildPlan(
                selection = active,
                acknowledged = active
            )
        )
    }

    /**
     * The case no test covered, and the one that matters. This replaces
     * `disabledProfile_registrationPerformsNoNetworkSteps`, which pinned the defect:
     * it asserted an empty plan for a profile with no category active, so a phone whose
     * alerts were all switched off never told the server, and the server kept sending.
     *
     * Rejects that implementation, and also one that sends the selection while
     * re-registering the token - which would put back the registration the disabled
     * selection exists to remove.
     */
    @Test
    fun disabledProfile_sendsTheDisabledSelectionAndRegistersNoToken() {
        assertEquals(
            listOf(PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION),
            PcgProfileRegistrationSyncPlanner.buildPlan(
                selection = disabledSelection(),
                acknowledged = null
            )
        )
    }

    /**
     * Rejects an implementation that sends the disable at every start, which would be one
     * request per launch for ever on a phone that has simply switched its alerts off.
     */
    @Test
    fun disabledProfile_sendsNothingOnceTheBackendHasAcknowledgedIt() {
        assertEquals(
            emptyList<PcgProfileRegistrationSyncStep>(),
            PcgProfileRegistrationSyncPlanner.buildPlan(
                selection = disabledSelection(),
                acknowledged = disabledSelection()
            )
        )
    }

    /**
     * Rejects an implementation that reads a missing acknowledgement as "already
     * disabled". An upgrading installation has acknowledged nothing, and it is exactly
     * the one whose server copy may still be active.
     */
    @Test
    fun disabledProfile_withAnActiveAcknowledgement_sendsTheDisable() {
        assertEquals(
            listOf(PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION),
            PcgProfileRegistrationSyncPlanner.buildPlan(
                selection = disabledSelection(),
                acknowledged = selection(
                    regularMode = PcgSpawnAlertMode.ALL_SPAWNS,
                    eventSpawnsEnabled = true,
                    mostWantedEnabled = true
                )
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
