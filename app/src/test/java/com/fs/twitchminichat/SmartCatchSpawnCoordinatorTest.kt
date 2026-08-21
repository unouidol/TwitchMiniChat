package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the lifecycle-independent IRC/FCM Smart Catch spawn pipeline. */
class SmartCatchSpawnCoordinatorTest {

    @Test
    fun ircSpawnStoresResolvedMetadataWithoutUiState() {
        val harness = Harness(nowMs = 1_000_000L)

        val result = harness.coordinator.ingestIrcMessage(
            user = "PokemonCommunityGame",
            message = "A wild Cacnea appears!",
            messageTimestampSec = 999.0
        )

        assertEquals(SmartCatchSpawnIngestionOutcome.STORED, result.outcome)
        assertTrue(result.snapshotChanged)
        assertEquals(SmartCatchSpawnSource.IRC, result.source)
        assertEquals("Cacnea", harness.stored?.displayName)
        assertEquals("Grass", harness.stored?.type1)
        assertEquals(999_000L, harness.stored?.seenAtMs)
    }

    @Test
    fun unrelatedChatMessageDoesNotChangeCurrentSpawn() {
        val harness = Harness(nowMs = 1_000_000L)

        val result = harness.coordinator.ingestIrcMessage(
            user = "viewer",
            message = "A wild Cacnea appears!",
            messageTimestampSec = 999.0
        )

        assertEquals(
            SmartCatchSpawnIngestionOutcome.IGNORED_NOT_SPAWN,
            result.outcome
        )
        assertNull(harness.stored)
        assertEquals(0, harness.writeCount)
    }

    @Test
    fun initialFcmPayloadStoresSpawnWhenChatUiDidNotObserveIt() {
        val harness = Harness(nowMs = 1_000_000L)

        val result = harness.coordinator.ingestFcmPayload(
            data = mapOf(
                "pokemon" to "Cacnea",
                "reminder" to "0"
            ),
            messageSentAtMs = 999_500L
        )

        assertEquals(SmartCatchSpawnIngestionOutcome.STORED, result.outcome)
        assertEquals(SmartCatchSpawnSource.FCM_INITIAL, result.source)
        assertEquals(999_500L, harness.stored?.seenAtMs)
    }

    @Test
    fun unknownCatalogEntryStillKeepsNameAndTimerAvailable() {
        val harness = Harness(nowMs = 1_000_000L)

        val result = harness.coordinator.ingestFcmPayload(
            data = mapOf("pokemon" to "Unmapped Form"),
            messageSentAtMs = 999_000L
        )

        assertEquals(SmartCatchSpawnIngestionOutcome.STORED, result.outcome)
        assertEquals("Unmapped Form", harness.stored?.displayName)
        assertNull(harness.stored?.type1)
        assertEquals(999_000L, harness.stored?.seenAtMs)
    }

    @Test
    fun reminderForCurrentSpawnDoesNotRestartNinetySecondTimer() {
        val harness = Harness(nowMs = 1_000_000L)

        harness.coordinator.ingestFcmPayload(
            data = mapOf(
                "pokemon" to "Cacnea",
                "reminder" to "0"
            ),
            messageSentAtMs = 1_000_000L
        )

        harness.nowMs = 1_045_000L
        val reminder = harness.coordinator.ingestFcmPayload(
            data = mapOf(
                "pokemon" to "Cacnea",
                "reminder" to "1"
            ),
            messageSentAtMs = 1_045_000L
        )

        assertEquals(
            SmartCatchSpawnIngestionOutcome.UNCHANGED,
            reminder.outcome
        )
        assertEquals(SmartCatchSpawnSource.FCM_REMINDER, reminder.source)
        assertEquals(1_000_000L, harness.stored?.seenAtMs)
        assertEquals(1, harness.writeCount)
    }

    @Test
    fun reminderWithoutInitialAlertUsesExpectedFortyFiveSecondOffset() {
        val harness = Harness(nowMs = 1_045_000L)

        val result = harness.coordinator.ingestFcmPayload(
            data = mapOf(
                "pokemon" to "Cacnea",
                "reminder" to "1"
            ),
            messageSentAtMs = 1_045_000L
        )

        assertEquals(SmartCatchSpawnIngestionOutcome.STORED, result.outcome)
        assertEquals(1_000_000L, harness.stored?.seenAtMs)
    }

    @Test
    fun explicitBackendStartTimestampOutranksReminderDeliveryTime() {
        val harness = Harness(nowMs = 2_060_000L)

        harness.coordinator.ingestFcmPayload(
            data = mapOf(
                "pokemon" to "Cacnea",
                "reminder" to "1",
                "spawn_started_at_ms" to "2000000"
            ),
            messageSentAtMs = 2_055_000L
        )

        assertEquals(2_000_000L, harness.stored?.seenAtMs)
    }

    @Test
    fun exactIrcTimestampCanCorrectLaterFcmObservationOfSameSpawn() {
        val harness = Harness(nowMs = 1_010_000L)

        harness.coordinator.ingestFcmPayload(
            data = mapOf("pokemon" to "Cacnea"),
            messageSentAtMs = 1_010_000L
        )

        val ircResult = harness.coordinator.ingestIrcMessage(
            user = "pokemoncommunitygame",
            message = "A wild Cacnea appears!",
            messageTimestampSec = 1_000.0
        )

        assertEquals(SmartCatchSpawnIngestionOutcome.STORED, ircResult.outcome)
        assertEquals(1_000_000L, harness.stored?.seenAtMs)
        assertEquals(2, harness.writeCount)
    }

    @Test
    fun olderHistorySpawnCannotReplaceNewerDifferentSpawn() {
        val harness = Harness(nowMs = 1_050_000L)
        harness.stored = snapshot(
            rawName = "Pikachu",
            seenAtMs = 1_040_000L
        )

        val result = harness.coordinator.ingestIrcMessage(
            user = "pokemoncommunitygame",
            message = "A wild Cacnea appears!",
            messageTimestampSec = 1_020.0
        )

        assertEquals(
            SmartCatchSpawnIngestionOutcome.IGNORED_OLDER,
            result.outcome
        )
        assertEquals("Pikachu", harness.stored?.rawName)
        assertEquals(0, harness.writeCount)
    }

    @Test
    fun expiredHistorySpawnCannotReviveSmartCatch() {
        val harness = Harness(nowMs = 1_100_001L)

        val result = harness.coordinator.ingestIrcMessage(
            user = "pokemoncommunitygame",
            message = "A wild Cacnea appears!",
            messageTimestampSec = 1_000.0
        )

        assertEquals(
            SmartCatchSpawnIngestionOutcome.IGNORED_EXPIRED,
            result.outcome
        )
        assertNull(harness.stored)
    }

    @Test
    fun largeFutureTimestampIsRejectedButSmallClockSkewIsClamped() {
        val rejectedHarness = Harness(nowMs = 1_000_000L)

        val rejected = rejectedHarness.coordinator.ingestIrcMessage(
            user = "pokemoncommunitygame",
            message = "A wild Cacnea appears!",
            messageTimestampSec = 1_006.0
        )

        assertEquals(
            SmartCatchSpawnIngestionOutcome.IGNORED_FUTURE,
            rejected.outcome
        )
        assertNull(rejectedHarness.stored)

        val acceptedHarness = Harness(nowMs = 1_000_000L)
        val accepted = acceptedHarness.coordinator.ingestIrcMessage(
            user = "pokemoncommunitygame",
            message = "A wild Cacnea appears!",
            messageTimestampSec = 1_003.0
        )

        assertEquals(SmartCatchSpawnIngestionOutcome.STORED, accepted.outcome)
        assertEquals(1_000_000L, acceptedHarness.stored?.seenAtMs)
    }

    private class Harness(
        var nowMs: Long
    ) {
        var stored: SpawnSnapshot? = null
        var writeCount: Int = 0

        val coordinator = SmartCatchSpawnCoordinator(
            resolvePokemon = { rawName ->
                if (rawName.equals("Cacnea", ignoreCase = true)) {
                    PokemonTypeEntry(
                        sourceKey = "cacnea",
                        displayName = "Cacnea",
                        pcgName = "Cacnea",
                        type1 = "Grass",
                        type2 = null,
                        weightKg = 51.3,
                        baseSpeed = 35,
                        baseHp = 50,
                        evolvesTwice = false,
                        aliases = listOf("Cacnea"),
                        mappingKind = "exact",
                        locked = false,
                        featured = false
                    )
                } else {
                    null
                }
            },
            loadCurrentSpawn = { stored },
            saveCurrentSpawn = { snapshot ->
                stored = snapshot
                writeCount += 1
            },
            nowMs = { nowMs }
        )
    }

    private companion object {
        fun snapshot(
            rawName: String,
            seenAtMs: Long
        ): SpawnSnapshot {
            return SpawnSnapshot(
                rawName = rawName,
                dexKey = null,
                displayName = rawName,
                type1 = null,
                type2 = null,
                weightKg = null,
                baseSpeed = null,
                baseHp = null,
                evolvesTwice = null,
                seenAtMs = seenAtMs,
                isAlreadyCaught = null
            )
        }
    }
}
