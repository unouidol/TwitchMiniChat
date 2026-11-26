package com.fs.twitchminichat

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lock L's state on its own, and where the application uses it (#117, F3, PR-2).
 *
 * The races it closes are in [OwedWorkRaceTest]; these check the pieces those tests rely on, and,
 * by reading the sources, the writers no local test can run because they need a device.
 */
class OwedWorkCoordinatorTest {

    private val coordinator = OwedWorkCoordinator()
    private val owedRecords = ThreadSafeTestSharedPreferences()

    @Test
    fun theStateCannotBeUsedOutsideTheLock() {
        assertThrows(IllegalStateException::class.java) { coordinator.tokenStamp() }
        assertThrows(IllegalStateException::class.java) { coordinator.claimProfile("pa") }
        assertThrows(IllegalStateException::class.java) {
            coordinator.recordTokenDeletion(owedRecords)
        }
        assertTrue(owedRecords.all.isEmpty())
    }

    @Test
    fun everyWriteOfAnEntryChangesItsStampAndNoOtherProfiles() {
        coordinator.locked {
            val token = coordinator.tokenStamp()
            val pa = coordinator.profileStamp("pa")
            val pb = coordinator.profileStamp("pb")

            coordinator.recordProfileDisable(owedRecords, "PA")
            val paRecorded = coordinator.profileStamp("pa")
            assertNotEquals(pa, paRecorded)
            assertEquals(pb, coordinator.profileStamp("pb"))
            assertEquals(token, coordinator.tokenStamp())

            coordinator.clearProfileDisable(owedRecords, "pa")
            assertNotEquals(paRecorded, coordinator.profileStamp("pa"))

            coordinator.recordTokenDeletion(owedRecords)
            val tokenRecorded = coordinator.tokenStamp()
            assertNotEquals(token, tokenRecorded)
            coordinator.clearTokenDeletion(owedRecords)
            assertNotEquals(tokenRecorded, coordinator.tokenStamp())
        }
    }

    @Test
    fun anEraseChangesEveryStampEvenOfProfilesNeverWritten() {
        coordinator.locked {
            val token = coordinator.tokenStamp()
            val untouched = coordinator.profileStamp("pz")
            coordinator.recordProfileDisable(owedRecords, "pa")
            val pa = coordinator.profileStamp("pa")

            coordinator.erased()

            assertNotEquals(token, coordinator.tokenStamp())
            assertNotEquals(untouched, coordinator.profileStamp("pz"))
            assertNotEquals(pa, coordinator.profileStamp("pa"))
        }
    }

    @Test
    fun aClearMatchesOnlyTheEntryItWasDecidedOn() {
        coordinator.locked {
            coordinator.recordTokenDeletion(owedRecords)
            val decided = coordinator.tokenStamp()
            coordinator.recordTokenDeletion(owedRecords)

            assertFalse(coordinator.clearTokenDeletionIfUnchanged(owedRecords, decided))
            assertTrue(OwedServerOperationStore.isFirebaseTokenDeletionOwed(owedRecords))
            assertTrue(
                coordinator.clearTokenDeletionIfUnchanged(owedRecords, coordinator.tokenStamp())
            )
            assertFalse(OwedServerOperationStore.isFirebaseTokenDeletionOwed(owedRecords))
        }
    }

    @Test
    fun aClaimIsHeldByOneAttemptUntilReleased() {
        coordinator.locked {
            assertTrue(coordinator.claimToken())
            assertFalse(coordinator.claimToken())
            coordinator.releaseToken()
            assertTrue(coordinator.claimToken())

            assertTrue(coordinator.claimProfile("pa"))
            assertFalse(coordinator.claimProfile("PA"))
            assertTrue(coordinator.claimProfile("pb"))
            coordinator.releaseProfile("pa")
            assertTrue(coordinator.claimProfile("pa"))
        }
    }

    @Test
    fun anEraseDeletesItsFilesUnderTheLockAndForgetsTheEntries() {
        val before = coordinator.locked { coordinator.tokenStamp() }
        val heldInside = DeviceEraseOwedWork.deletePreferences(coordinator, forgetOwedEntries = true) {
            coordinator.lock.isHeldByCurrentThread
        }

        assertTrue(heldInside)
        assertNotEquals(before, coordinator.locked { coordinator.tokenStamp() })

        val kept = coordinator.locked { coordinator.tokenStamp() }
        DeviceEraseOwedWork.deletePreferences(coordinator, forgetOwedEntries = false) {}
        assertEquals(kept, coordinator.locked { coordinator.tokenStamp() })
    }

    // --- Where the application takes L (read from the sources) -------------------------

    /** One lock per process: no production code makes a coordinator but the process's own. */
    @Test
    fun theApplicationHasOneCoordinator() {
        val constructions = mainSources().filter { (_, text) -> "OwedWorkCoordinator()" in text }

        assertEquals(listOf("OwedWorkCoordinator.kt"), constructions.map { it.first })
        assertEquals(1, Regex("""OwedWorkCoordinator\(\)""").findAll(constructions.single().second).count())
    }

    /** Each production caller of a section passes the process's coordinator. */
    @Test
    fun theSectionsAreCalledWithTheProcessCoordinator() {
        val sources = mainSources().toMap()

        mapOf(
            "AuthCallbackActivity.kt" to 1,
            "AccountProfileRemovalController.kt" to 2,
            "SafetyPrivacyFragment.kt" to 1,
            "LocalDataCleaner.kt" to 1
        ).forEach { (file, calls) ->
            assertEquals(
                file,
                calls,
                Regex("""coordinator = OwedWorkCoordinator\.PROCESS""")
                    .findAll(checkNotNull(sources[file])).count()
            )
        }
    }

    /** R3: the acknowledgement written after a 2xx is written under L, its file obtained inside. */
    @Test
    fun theAcknowledgementAfterADeliveryIsWrittenUnderTheLock() {
        val uploader = checkNotNull(mainSources().toMap()["FcmRegistrationUploader.kt"])
        val section = uploader
            .substringAfter("OwedWorkCoordinator.PROCESS.locked {")
            .substringBefore("\n            }\n")

        assertTrue(section.contains("PcgProfileAlertAcknowledgementStore.record(\n                    context = context,"))
        assertEquals(1, Regex("""PcgProfileAlertAcknowledgementStore\.record\(""").findAll(uploader).count())
    }

    /** R3: the erase deletes the preferences files inside the section that takes L. */
    @Test
    fun theEraseDeletesThePreferencesUnderTheLock() {
        val cleaner = checkNotNull(mainSources().toMap()["LocalDataCleaner.kt"])
        val section = cleaner.substringAfter("DeviceEraseOwedWork.deletePreferences(")

        assertTrue(section.startsWith("\n            coordinator = OwedWorkCoordinator.PROCESS,"))
        assertTrue(
            section.substringBefore("var clearedCacheDirs")
                .contains("appContext.deleteSharedPreferences(name)")
        )
        assertEquals(1, Regex("""deleteSharedPreferences\(""").findAll(cleaner).count())
    }

    /** Every Kotlin file of the application package, by name, with line ends as LF. */
    private fun mainSources(): List<Pair<String, String>> {
        val directory = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/java/com/fs/twitchminichat")
        return Files.walk(directory).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }
                .map { path ->
                    path.fileName.toString() to
                        String(Files.readAllBytes(path), Charsets.UTF_8).replace("\r\n", "\n")
                }
                .iterator().asSequence().toList()
        }
    }
}
