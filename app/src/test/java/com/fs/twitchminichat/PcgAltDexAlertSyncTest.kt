package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * The alt-dex uploads and the acknowledged alt-dex category around them
 * (`docs/design-alt-dex-alert.md`, *a*, *f* and *j*; the owner's handling of a 409, 2026-11-25).
 */
class PcgAltDexAlertSyncTest {

    private val profileId = "profile-a"

    private val acknowledgements = ThreadSafeTestSharedPreferences()
    private val localCategory = mutableMapOf(profileId to true)
    private var reading: PcgAltDexSnapshot? = PcgAltDexSnapshot(
        obtained = emptyList(),
        missing = listOf(PcgAltDexForm(key = "venusaur f", name = "Venusaur (F)", number = 10003)),
        readAtMs = 1_795_421_560_000L
    )

    private var answer: PcgAltDexAlertUploadResult = PcgAltDexAlertUploadResult.Stored(count = 1)
    private val sent = mutableListOf<PcgAltDexAlertUpload>()

    private val sync = PcgAltDexAlertSync(
        acknowledgements = acknowledgements,
        categoryEnabled = { localCategory[it] == true },
        loadReading = { reading },
        sender = { upload -> sent += upload; answer }
    )

    /** The alt-dex-only selection this phone holds: the category on, everything else off. */
    private val local = PcgProfileAlertSelection.DISABLED.copy(altDexFormAlertsEnabled = true)

    private fun acknowledge(selection: PcgProfileAlertSelection) {
        PcgProfileAlertAcknowledgementStore.record(acknowledgements, profileId, selection)
    }

    /** What a successful delivery step records, then the turn-on's stored upload confirms. */
    private fun confirmTheCategory() {
        acknowledge(local)
        answer = PcgAltDexAlertUploadResult.Stored(count = 1)
        assertEquals(PcgAltDexAlertSyncOutcome.STORED, sync.uploadAfterTurnOn(profileId, "label-a"))
        sent.clear()
    }

    private fun acknowledged() = PcgProfileAlertAcknowledgementStore.read(acknowledgements, profileId)

    private fun baseline() = local.confirmedBy(acknowledged())

    // After an accepted "Update Alt Dex".

    @Test
    fun afterAReadingWithTheCategoryOffNothingIsSent() {
        localCategory[profileId] = false
        confirmTheCategory()

        assertEquals(PcgAltDexAfterReadingDecision.CATEGORY_OFF, sync.decideAfterReading(profileId))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun afterAReadingWithTheCategoryOnAndConfirmedTheReadingIsSent() {
        confirmTheCategory()

        assertEquals(PcgAltDexAfterReadingDecision.UPLOAD, sync.decideAfterReading(profileId))
    }

    /** The unconfirmed baseline skips the upload, with no acknowledgement or with a lowered one. */
    @Test
    fun afterAReadingWithAnUnconfirmedCategoryNothingIsSent() {
        assertEquals(PcgAltDexAfterReadingDecision.NOT_CONFIRMED, sync.decideAfterReading(profileId))

        acknowledge(local)
        assertEquals(PcgAltDexAfterReadingDecision.NOT_CONFIRMED, sync.decideAfterReading(profileId))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun aStoredUploadAfterAReadingLeavesTheConfirmationAsItWas() {
        confirmTheCategory()
        val before = acknowledgements.all

        val upload = PcgAltDexAlertUpload.of(profileId, "label-a", reading!!)
        assertEquals(PcgAltDexAlertSyncOutcome.STORED, sync.uploadAfterReading(upload))

        assertEquals(listOf(upload), sent)
        assertEquals(before, acknowledgements.all)
    }

    @Test
    fun aRefusedUploadAfterAReadingUnconfirmsTheCategoryAndKeepsTheRest() {
        val active = local.copy(
            spawnSettings = PcgSpawnAlertSettings(PcgSpawnAlertMode.DEX_ONLY, eventSpawnsEnabled = true)
        )
        acknowledge(active)
        assertTrue(
            PcgProfileAlertAcknowledgementStore.setAltDexFormAlertsConfirmed(
                acknowledgements,
                profileId,
                confirmed = true
            )
        )
        answer = PcgAltDexAlertUploadResult.AlertsOff

        val outcome = sync.uploadAfterReading(PcgAltDexAlertUpload.of(profileId, "label-a", reading!!))

        assertEquals(PcgAltDexAlertSyncOutcome.ALERTS_OFF, outcome)
        assertEquals(active.copy(altDexFormAlertsEnabled = false), acknowledged())
        assertEquals(1, sent.size)
        assertTrue("the local category is not touched", localCategory[profileId] == true)
    }

    @Test
    fun aFailedUploadAfterAReadingChangesNothing() {
        confirmTheCategory()
        val before = acknowledgements.all

        listOf(PcgAltDexAlertUploadResult.Failed, PcgAltDexAlertUploadResult.NotSent).forEach {
            answer = it
            val outcome =
                sync.uploadAfterReading(PcgAltDexAlertUpload.of(profileId, "label-a", reading!!))
            assertEquals(PcgAltDexAlertSyncOutcome.FAILED, outcome)
        }
        assertEquals(before, acknowledgements.all)
    }

    // Required test (a).

    /**
     * A 409 for a profile with no acknowledgement writes nothing, and the baseline is already
     * unconfirmed, so the next OK plans the delivery step and then the upload again.
     */
    @Test
    fun aRefusalForAProfileWithNoConfirmationWritesNothingAndTheNextOkResends() {
        answer = PcgAltDexAlertUploadResult.AlertsOff

        assertEquals(
            PcgAltDexAlertSyncOutcome.ALERTS_OFF,
            sync.uploadAfterTurnOn(profileId, "label-a")
        )
        assertEquals(
            PcgAltDexAlertSyncOutcome.ALERTS_OFF,
            sync.uploadAfterReading(PcgAltDexAlertUpload.of(profileId, "label-a", reading!!))
        )

        assertTrue(acknowledgements.all.toString(), acknowledgements.all.isEmpty())
        assertNull(acknowledged())
        assertFalse(baseline().altDexFormAlertsEnabled)
        assertEquals(
            listOf(PcgProfileAlertSyncStep.FIREBASE_DELIVERY, PcgProfileAlertSyncStep.ALT_DEX_UPLOAD),
            PcgProfileAlertSyncPlanner.buildPlan(current = baseline(), requested = local)
        )
    }

    // Required test (b).

    /**
     * After a 409 the category is confirmed again only by a stored turn-on upload. The delivery
     * step's acknowledgement alone does not do it, and neither does a failed upload.
     */
    @Test
    fun afterARefusalOnlyAStoredUploadConfirmsTheCategoryAgain() {
        confirmTheCategory()
        answer = PcgAltDexAlertUploadResult.AlertsOff
        sync.uploadAfterReading(PcgAltDexAlertUpload.of(profileId, "label-a", reading!!))
        assertFalse(baseline().altDexFormAlertsEnabled)

        // The next OK: the delivery step succeeds, and records what it sent.
        acknowledge(local)
        assertEquals(false, acknowledged()?.altDexFormAlertsEnabled)
        assertEquals(
            PcgAltDexAfterReadingDecision.NOT_CONFIRMED,
            sync.decideAfterReading(profileId)
        )

        answer = PcgAltDexAlertUploadResult.Failed
        assertEquals(PcgAltDexAlertSyncOutcome.FAILED, sync.uploadAfterTurnOn(profileId, "label-a"))
        assertEquals(false, acknowledged()?.altDexFormAlertsEnabled)

        answer = PcgAltDexAlertUploadResult.Stored(count = 1)
        assertEquals(PcgAltDexAlertSyncOutcome.STORED, sync.uploadAfterTurnOn(profileId, "label-a"))
        assertEquals(true, acknowledged()?.altDexFormAlertsEnabled)
        assertEquals(local, baseline())
    }

    // The bell menu's turn-on.

    @Test
    fun aTurnOnWithNoReadingSendsNothingAndConfirmsTheCategory() {
        reading = null
        acknowledge(local)

        assertEquals(
            PcgAltDexAlertSyncOutcome.NO_READING,
            sync.uploadAfterTurnOn(profileId, "label-a")
        )

        assertTrue(sent.isEmpty())
        assertEquals(local, baseline())
        assertTrue(PcgAltDexAlertSyncOutcome.NO_READING.completesTurnOn)
    }

    @Test
    fun aTurnOnWithAReadingSendsItOnce() {
        acknowledge(local)

        sync.uploadAfterTurnOn(profileId, "label-a")

        assertEquals(listOf(PcgAltDexAlertUpload.of(profileId, "label-a", reading!!)), sent)
    }

    @Test
    fun onlyAStoredListOrNoReadingCompletesTheTurnOn() {
        assertEquals(
            setOf(PcgAltDexAlertSyncOutcome.STORED, PcgAltDexAlertSyncOutcome.NO_READING),
            PcgAltDexAlertSyncOutcome.entries.filter { it.completesTurnOn }.toSet()
        )
    }

    // What the user is told.

    @Test
    fun aRefusalAndAnUnconfirmedCategoryShareTheirOwnMessage() {
        val refusal = PcgAltDexAlertSync.messageFor(PcgAltDexAlertSyncOutcome.ALERTS_OFF)

        assertEquals(R.string.pcg_alt_dex_alert_upload_alerts_off, refusal)
        assertEquals(refusal, PcgAltDexAlertSync.notConfirmedMessage)
        assertNotEquals(refusal, PcgAltDexAlertSync.messageFor(PcgAltDexAlertSyncOutcome.FAILED))
        assertNotEquals(refusal, R.string.pcg_alt_dex_updated)
        assertNotEquals(refusal, R.string.pcg_alert_settings_save_failed)
        assertEquals(
            R.string.pcg_alt_dex_updated_and_sent,
            PcgAltDexAlertSync.messageFor(PcgAltDexAlertSyncOutcome.STORED)
        )
    }

    /**
     * Nothing about an upload is queued or retried: the file that holds the uploads never names
     * the owed-work store or its worker.
     */
    @Test
    fun theUploadsQueueNothing() {
        val source = String(
            Files.readAllBytes(
                BundledPcgCatalogFixture.findAppModuleDirectory()
                    .resolve("src/main/java/com/fs/twitchminichat/PcgAltDexAlertUpload.kt")
            ),
            Charsets.UTF_8
        )

        listOf("OwedServerOperation", "WorkManager", "enqueue").forEach { name ->
            assertFalse(name, name in source)
        }
    }
}
