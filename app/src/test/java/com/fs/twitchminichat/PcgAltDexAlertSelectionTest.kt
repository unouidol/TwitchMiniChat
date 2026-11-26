package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The alt-dex category in the alert selection, its plans and its policies
 * (`docs/design-alt-dex-alert.md`, *f* and *j*; decision 15; the owner's handling of a 409,
 * 2026-11-25).
 */
class PcgAltDexAlertSelectionTest {

    private val altDexOnly =
        PcgProfileAlertSelection.DISABLED.copy(altDexFormAlertsEnabled = true)

    private val dexOnly = PcgProfileAlertSelection(
        spawnSettings = PcgSpawnAlertSettings(PcgSpawnAlertMode.DEX_ONLY, eventSpawnsEnabled = false),
        mostWantedEnabled = false,
        altDexFormAlertsEnabled = false
    )

    // The selection.

    @Test
    fun theCategoryAloneNeedsFirebaseDelivery() {
        assertTrue(altDexOnly.requiresFirebaseDelivery)
    }

    @Test
    fun disabledHasTheCategoryOff() {
        assertFalse(PcgProfileAlertSelection.DISABLED.altDexFormAlertsEnabled)
        assertFalse(PcgProfileAlertSelection.DISABLED.requiresFirebaseDelivery)
    }

    @Test
    fun theCategoryTakesPartInEquality() {
        assertNotEquals(dexOnly, dexOnly.copy(altDexFormAlertsEnabled = true))
    }

    // The confirmed baseline.

    @Test
    fun theBaselineHasTheCategoryOnlyWhenOnHereAndAcknowledgedOn() {
        val on = dexOnly.copy(altDexFormAlertsEnabled = true)

        assertEquals(on, on.confirmedBy(on))
        assertEquals(dexOnly, on.confirmedBy(dexOnly))
        assertEquals(dexOnly, on.confirmedBy(null))
        assertEquals(dexOnly, dexOnly.confirmedBy(on))
    }

    /** Only the alt-dex category is masked: the other categories show what this phone holds. */
    @Test
    fun theBaselineKeepsTheOtherCategoriesAsHeldHere() {
        val local = dexOnly.copy(mostWantedEnabled = true, altDexFormAlertsEnabled = true)

        assertEquals(local.copy(altDexFormAlertsEnabled = false), local.confirmedBy(null))
    }

    // The bell menu's plan (decision 15).

    @Test
    fun turningTheCategoryOnEndsWithTheUploadAfterDelivery() {
        assertEquals(
            listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY, PcgProfileAlertSyncStep.ALT_DEX_UPLOAD),
            PcgProfileAlertSyncPlanner.buildPlan(
                current = PcgProfileAlertSelection.DISABLED,
                requested = altDexOnly
            )
        )
        assertEquals(
            listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY, PcgProfileAlertSyncStep.ALT_DEX_UPLOAD),
            PcgProfileAlertSyncPlanner.buildPlan(
                current = dexOnly,
                requested = dexOnly.copy(altDexFormAlertsEnabled = true)
            )
        )
    }

    @Test
    fun withMostWantedTurnedOnTooTheUploadStillComesLast() {
        listOf(PcgProfileAlertSelection.DISABLED, dexOnly).forEach { current ->
            val plan = PcgProfileAlertSyncPlanner.buildPlan(
                current = current,
                requested = current.copy(mostWantedEnabled = true, altDexFormAlertsEnabled = true)
            )

            assertEquals(plan.toString(), PcgProfileAlertSyncStep.ALT_DEX_UPLOAD, plan.last())
            assertEquals(plan.toString(), 1, plan.count { it == PcgProfileAlertSyncStep.ALT_DEX_UPLOAD })
            assertTrue(plan.toString(), PcgProfileAlertSyncStep.FIREBASE_DELIVERY in plan)
            assertTrue(plan.toString(), PcgProfileAlertSyncStep.MOST_WANTED in plan)
        }
    }

    @Test
    fun withMostWantedTurnedOffTheUploadStillComesLast() {
        val current = dexOnly.copy(mostWantedEnabled = true)

        assertEquals(
            listOf(
                PcgProfileAlertSyncStep.MOST_WANTED,
                PcgProfileAlertSyncStep.FIREBASE_DELIVERY,
                PcgProfileAlertSyncStep.ALT_DEX_UPLOAD
            ),
            PcgProfileAlertSyncPlanner.buildPlan(
                current = current,
                requested = current.copy(mostWantedEnabled = false, altDexFormAlertsEnabled = true)
            )
        )
    }

    @Test
    fun turningTheCategoryOffSendsTheSelectionAndNoUpload() {
        assertEquals(
            listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY),
            PcgProfileAlertSyncPlanner.buildPlan(
                current = dexOnly.copy(altDexFormAlertsEnabled = true),
                requested = dexOnly
            )
        )
    }

    @Test
    fun leavingTheCategoryAsItWasPlansNoUpload() {
        val on = dexOnly.copy(altDexFormAlertsEnabled = true)

        assertEquals(emptyList<PcgProfileAlertSyncStep>(), PcgProfileAlertSyncPlanner.buildPlan(on, on))
        assertEquals(
            listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY),
            PcgProfileAlertSyncPlanner.buildPlan(
                current = on,
                requested = on.copy(
                    spawnSettings = PcgSpawnAlertSettings(PcgSpawnAlertMode.ALL_SPAWNS, false)
                )
            )
        )
    }

    /** The OK after a 409: the checkbox still shows on, the baseline does not, so OK resends. */
    @Test
    fun anUnconfirmedCategoryMakesTheNextOkSendTheSelectionThenTheUpload() {
        val local = dexOnly.copy(altDexFormAlertsEnabled = true)
        val baseline = local.confirmedBy(dexOnly)

        assertEquals(
            listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY, PcgProfileAlertSyncStep.ALT_DEX_UPLOAD),
            PcgProfileAlertSyncPlanner.buildPlan(current = baseline, requested = local)
        )
    }

    // Running the plan.

    @Test
    fun aFailedDeliveryStepMeansNoUploadIsSent() {
        val ran = mutableListOf<PcgProfileAlertSyncStep>()

        val allSucceeded = PcgProfileAlertSyncRunner.runInOrder(
            listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY, PcgProfileAlertSyncStep.ALT_DEX_UPLOAD)
        ) { step ->
            ran += step
            step != PcgProfileAlertSyncStep.FIREBASE_DELIVERY
        }

        assertFalse(allSucceeded)
        assertEquals(listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY), ran)
    }

    @Test
    fun everyStepRunsInOrderWhenEachSucceeds() {
        val plan = listOf(
            PcgProfileAlertSyncStep.FIREBASE_DELIVERY,
            PcgProfileAlertSyncStep.MOST_WANTED,
            PcgProfileAlertSyncStep.ALT_DEX_UPLOAD
        )
        val ran = mutableListOf<PcgProfileAlertSyncStep>()

        assertTrue(PcgProfileAlertSyncRunner.runInOrder(plan) { step -> ran += step; true })
        assertEquals(plan, ran)
    }

    // The boot pass, unchanged by the acknowledged category.

    @Test
    fun theBootPassRegistersAndRestoresTheCategoryWhateverItsAcknowledgement() {
        listOf(null, altDexOnly, PcgProfileAlertSelection.DISABLED).forEach { acknowledged ->
            assertEquals(
                acknowledged.toString(),
                listOf(
                    PcgProfileRegistrationSyncStep.REGISTER_TOKEN,
                    PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION
                ),
                PcgProfileRegistrationSyncPlanner.buildPlan(
                    selection = altDexOnly,
                    acknowledged = acknowledged
                )
            )
        }
    }

    @Test
    fun theBootPassSendsTheTurnOffOfAnAcknowledgedCategory() {
        assertEquals(
            listOf(PcgProfileRegistrationSyncStep.RESTORE_ALERT_SELECTION),
            PcgProfileRegistrationSyncPlanner.buildPlan(
                selection = PcgProfileAlertSelection.DISABLED,
                acknowledged = altDexOnly
            )
        )
    }

    // The owed-work policy, after a 409 lowered the acknowledgement.

    /** Other categories still acknowledged on keep a removed profile's disable owed. */
    @Test
    fun aRemovedProfileStillOwesItsDisableAfterARefusal() {
        // Acknowledged with ordinary alerts and the category on, then lowered by a 409.
        val afterRefusal = dexOnly

        assertEquals(
            OwedProfileDisableDecision.DISABLE,
            OwedProfileDisablePolicy.decide(
                acknowledged = afterRefusal,
                explicitlyOwed = false,
                local = PcgProfileLocalSelection.Removed
            )
        )
    }

    /**
     * An acknowledgement lowered to every category off owes no disable by itself. The 409 is
     * the server's word that the category is off on every device record, and the other
     * categories were acknowledged off. A failed disable is still owed through its marker.
     */
    @Test
    fun aRefusalOfTheOnlyCategoryLeavesTheMarkerAsTheOnlyDebt() {
        val afterRefusal = altDexOnly.copy(altDexFormAlertsEnabled = false)

        assertEquals(
            OwedProfileDisableDecision.NOTHING_OWED,
            OwedProfileDisablePolicy.decide(
                acknowledged = afterRefusal,
                explicitlyOwed = false,
                local = PcgProfileLocalSelection.Removed
            )
        )
        assertEquals(
            OwedProfileDisableDecision.DISABLE,
            OwedProfileDisablePolicy.decide(
                acknowledged = afterRefusal,
                explicitlyOwed = true,
                local = PcgProfileLocalSelection.Removed
            )
        )
    }

    @Test
    fun aProfileOnThisPhoneOwesNothingAfterARefusal() {
        assertEquals(
            OwedProfileDisableDecision.NOTHING_OWED,
            OwedProfileDisablePolicy.decide(
                acknowledged = altDexOnly.copy(altDexFormAlertsEnabled = false),
                explicitlyOwed = false,
                local = PcgProfileLocalSelection.Present(altDexOnly)
            )
        )
    }

    // The request.

    @Test
    fun theSelectionRequestCarriesTheCategory() {
        val on = FcmRegistrationUploader.alertSelectionPayload(
            deviceId = "device-a",
            deviceName = "device-name-a",
            token = "token-a",
            profileId = "profile-a",
            selection = altDexOnly
        )
        val off = FcmRegistrationUploader.alertSelectionPayload(
            deviceId = "device-a",
            deviceName = "device-name-a",
            token = "",
            profileId = "profile-a",
            selection = PcgProfileAlertSelection.DISABLED
        )

        assertEquals(true, on.get("alt_dex_form_alerts_enabled"))
        assertEquals(true, on.get("enabled"))
        assertEquals(false, off.get("alt_dex_form_alerts_enabled"))
        assertEquals(false, off.get("enabled"))
        assertEquals(
            setOf(
                "device_id", "device_name", "fcm_token", "profile_id", "spawn_alert_mode",
                "event_spawn_enabled", "alt_dex_form_alerts_enabled", "enabled"
            ),
            on.keys().asSequence().toSet()
        )
    }
}
