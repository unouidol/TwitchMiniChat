package com.fs.twitchminichat

import java.nio.file.Files
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lock L's state on its own, and where the application uses it (#117, F3, PR-2), with PR-3's
 * line per section and the token gate's bound.
 *
 * The races it closes are in [OwedWorkRaceTest]; these check the pieces those tests rely on, and,
 * by reading the sources, the writers no local test can run because they need a device.
 */
class OwedWorkCoordinatorTest {

    private val coordinator = testCoordinator()
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
        coordinator.locked("test") {
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
        coordinator.locked("test") {
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
        coordinator.locked("test") {
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
        coordinator.locked("test") {
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

    /**
     * Claims live in the coordinator's memory and nowhere else, so a process that dies holding
     * one - a crash during a request - leaves nothing behind: the next process starts with a new
     * coordinator, and every claim free. A claim held by one coordinator says nothing to another.
     */
    @Test
    fun aNewProcessStartsWithEveryClaimFree() {
        val crashed = testCoordinator()
        crashed.locked("test") {
            assertTrue(crashed.claimToken())
            assertTrue(crashed.claimProfile("pa"))
        }

        val next = testCoordinator()
        next.locked("test") {
            assertTrue(next.claimToken())
            assertTrue(next.claimProfile("pa"))
            assertTrue(next.claimProfile("pb"))
        }
        assertFalse(crashed.lock.isLocked)
        assertFalse(next.lock.isLocked)
    }

    /**
     * Nothing about a claim is stored: the claim functions touch only the coordinator's own
     * fields, and no part of the coordinator opens a file or preferences of its own.
     */
    @Test
    fun claimsAreNeverStored() {
        val source = checkNotNull(mainSources().toMap()["OwedWorkCoordinator.kt"])
        val claims = source
            .substringAfter("// --- Claims")
            .substringBefore("// --- The owed records")

        listOf("Store", "preferences", "edit", "put", "SharedPreferences", "File").forEach { word ->
            assertFalse(word, word in claims)
        }
        listOf("getSharedPreferences", "Context", "File(", "openFileOutput").forEach { word ->
            assertFalse(word, word in source)
        }
    }

    @Test
    fun anEraseDeletesItsFilesUnderTheLockAndForgetsTheEntries() {
        val before = coordinator.locked("test") { coordinator.tokenStamp() }
        val heldInside = DeviceEraseOwedWork.deletePreferences(coordinator, forgetOwedEntries = true) {
            coordinator.lock.isHeldByCurrentThread
        }

        assertTrue(heldInside)
        assertNotEquals(before, coordinator.locked("test") { coordinator.tokenStamp() })

        val kept = coordinator.locked("test") { coordinator.tokenStamp() }
        DeviceEraseOwedWork.deletePreferences(coordinator, forgetOwedEntries = false) {}
        assertEquals(kept, coordinator.locked("test") { coordinator.tokenStamp() })
    }

    // --- One line per section, and the token gate's bound (PR-3) ---------------------------

    /**
     * A section logs its name, how long it waited for L and how long it held it, in
     * microseconds, once, and only after L is free again (DV1 on #117).
     */
    @Test
    fun aSectionLogsItsWaitAndItsHoldOnceLIsFree() {
        val times = ArrayDeque(listOf(1_000L, 3_000L, 10_000L))
        lateinit var timed: OwedWorkCoordinator
        val heldWhileLogging = mutableListOf<Boolean>()
        val log = object : OwedWorkLog {
            val lines = mutableListOf<String>()
            override fun debug(message: String) {
                heldWhileLogging += timed.lock.isHeldByCurrentThread
                lines += message
            }
            override fun warn(message: String) = throw AssertionError(message)
        }
        timed = OwedWorkCoordinator(log = log, nanoTime = { times.removeFirst() })

        assertEquals("inside", timed.locked("pass.token.decide") { "inside" })

        assertEquals(listOf("lock L section=pass.token.decide waitedUs=2 heldUs=7"), log.lines)
        assertEquals(listOf(false), heldWhileLogging)
        assertTrue(times.isEmpty())
    }

    /** A section entered under L belongs to the one around it, which alone logs. */
    @Test
    fun aSectionInsideAnotherLogsOnlyTheOuterOne() {
        val log = RecordingOwedWorkLog()
        val nested = OwedWorkCoordinator(log = log, nanoTime = { 0L })

        nested.locked("outer") { nested.locked("inner") {} }

        assertEquals(listOf("D lock L section=outer waitedUs=0 heldUs=0"), log.lines)
    }

    /**
     * The gate is free with no deletion claimed, and with one claimed it gives up exactly when
     * the bound has passed since the claim, not since the wait began. The test's timeout is a
     * liveness guard only: a gate counting from the wait would otherwise block this test for good.
     */
    @Test(timeout = 60_000L)
    fun theTokenGateIsBoundedFromTheClaim() {
        val now = java.util.concurrent.atomic.AtomicLong(TimeUnit.SECONDS.toNanos(5))
        val gate = OwedWorkCoordinator(log = RecordingOwedWorkLog(), nanoTime = now::get)
        val bound = TimeUnit.SECONDS.toNanos(20)

        assertEquals(OwedWorkCoordinator.TokenGate.FREE, gate.awaitTokenDeletionNotInFlight(bound))

        gate.locked("test") { assertTrue(gate.claimToken()) }
        now.addAndGet(bound)
        assertEquals(OwedWorkCoordinator.TokenGate.DEADLINE, gate.awaitTokenDeletionNotInFlight(bound))

        gate.locked("test") { gate.releaseToken() }
        assertEquals(OwedWorkCoordinator.TokenGate.FREE, gate.awaitTokenDeletionNotInFlight(bound))
    }

    /** Waiting under L would hold up every section of the process: the gate refuses. */
    @Test
    fun theTokenGateIsNeverWaitedForUnderTheLock() {
        coordinator.locked("test") {
            assertThrows(IllegalStateException::class.java) {
                coordinator.awaitTokenDeletionNotInFlight(TimeUnit.SECONDS.toNanos(20))
            }
        }
    }

    // --- Where the application takes L (read from the sources) -------------------------

    /** One lock per process: no production code makes a coordinator but the process's own. */
    @Test
    fun theApplicationHasOneCoordinator() {
        val construction = Regex("""= OwedWorkCoordinator\(""")
        val constructions = mainSources().filter { (_, text) -> construction.containsMatchIn(text) }

        assertEquals(listOf("OwedWorkCoordinator.kt"), constructions.map { it.first })
        assertEquals(1, construction.findAll(constructions.single().second).count())
    }

    /**
     * Every section of the application is named, each name once, so that a device log tells them
     * apart (DV1 on #117).
     */
    @Test
    fun everySectionOfTheApplicationHasItsOwnName() {
        val names = mainSources()
            .flatMap { (_, text) -> Regex("""\.locked\("([^"]+)"\)""").findAll(text).map { it.groupValues[1] } }

        assertEquals(
            listOf(
                "alt-dex.confirmation",
                "delivery.acknowledgement",
                "erase.preferences",
                "erase.record",
                "pass.disable.answer",
                "pass.disable.candidates",
                "pass.disable.decide",
                "pass.token.answer",
                "pass.token.decide",
                "removal",
                "removal.answer",
                "sign-in",
                "token.late-deletion"
            ),
            names.sorted()
        )
        assertEquals(0, mainSources().sumOf { (_, text) -> Regex("""\.locked \{""").findAll(text).count() })
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
            .substringAfter("OwedWorkCoordinator.PROCESS.locked(\"delivery.acknowledgement\") {")
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
