package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Characterization of [OwedServerOperationRunner], one pass at a time (#117, F3, PR-1).
 *
 * These record what the runner does today, single-threaded, so that PR-2 and PR-3 can
 * change how it is coordinated without changing what one uncontended pass decides. Every
 * test here passes on the code before PR-1's seams and after them. The races of #117 are
 * not here: they are PR-2's and PR-3's regression tests.
 */
class OwedServerOperationRunnerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var env: TestOwedWorkEnvironment

    @Before
    fun setUp() {
        env = TestOwedWorkEnvironment(temporaryFolder.newFolder("sessions"))
        env.tokenCache.edit().putString(TestOwedWorkEnvironment.CACHED_TOKEN_KEY, "tok-1").commit()
    }

    private fun pass() = OwedServerOperationRunner.runOwedWorkBlocking(env)

    private fun tokenDeletionOwed() =
        OwedServerOperationStore.isFirebaseTokenDeletionOwed(env.owedRecords)

    private fun disableOwed(profileId: String) =
        OwedServerOperationStore.isProfileDisableOwed(env.owedRecords, profileId)

    private fun cachedToken() =
        env.tokenCache.getString(TestOwedWorkEnvironment.CACHED_TOKEN_KEY, null)

    // --- Nothing owed -------------------------------------------------------------

    @Test
    fun nothingOwedDoesNothing() {
        env.signIn("pa")

        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass())
        assertEquals(0, env.firebase.startedDeletions)
        assertTrue(env.sentDisables.isEmpty())
        assertEquals("tok-1", cachedToken())
    }

    // --- The owed token deletion --------------------------------------------------

    @Test
    fun owedTokenDeletionWithNoAccountRunsAndClears() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)

        assertEquals(OwedServerOperationRunner.Attempt.COMPLETED, pass())
        assertEquals(1, env.firebase.startedDeletions)
        assertEquals(listOf(TestOwedWorkEnvironment.TOKEN_DELETION_TIMEOUT_SECONDS), env.firebase.awaitedTimeouts)
        assertFalse(tokenDeletionOwed())
        assertNull(cachedToken())
    }

    @Test
    fun failedTokenDeletionStaysOwedAndKeepsTheCachedToken() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.firebase.deletionOutcomes += FakeTokenDeletionOutcome.FAILS

        assertEquals(OwedServerOperationRunner.Attempt.FAILED, pass())
        assertTrue(tokenDeletionOwed())
        assertEquals("tok-1", cachedToken())
        assertTrue(env.log.lines.any { it.startsWith("W delete_firebase_token failed") })
    }

    @Test
    fun timedOutTokenDeletionCountsAsFailed() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.firebase.deletionOutcomes += FakeTokenDeletionOutcome.TIMES_OUT

        assertEquals(OwedServerOperationRunner.Attempt.FAILED, pass())
        assertTrue(tokenDeletionOwed())
        assertEquals("tok-1", cachedToken())
    }

    @Test
    fun signedInAccountVoidsTheOwedTokenDeletion() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.signIn("pa")

        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass())
        assertEquals(0, env.firebase.startedDeletions)
        assertFalse(tokenDeletionOwed())
        assertEquals("tok-1", cachedToken())
    }

    @Test
    fun aRetriedPassAfterAFailureCompletes() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.firebase.deletionOutcomes += FakeTokenDeletionOutcome.FAILS

        assertEquals(OwedServerOperationRunner.Attempt.FAILED, pass())
        assertEquals(OwedServerOperationRunner.Attempt.COMPLETED, pass())
        assertEquals(2, env.firebase.startedDeletions)
        assertFalse(tokenDeletionOwed())
    }

    // --- Owed profile disables ----------------------------------------------------

    @Test
    fun explicitlyOwedDisableIsSentThenClearedAndTheSessionRemoved() {
        env.signIn("pa")
        env.removeAccount("pa")
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "pa")

        assertEquals(OwedServerOperationRunner.Attempt.COMPLETED, pass())
        assertEquals(listOf("pa"), env.sentDisables)
        assertFalse(disableOwed("pa"))
        assertFalse(env.hasSession("pa"))
        assertEquals(
            PcgProfileAlertSelection.DISABLED,
            PcgProfileAlertAcknowledgementStore.read(env.acknowledgements, "pa")
        )
    }

    @Test
    fun aRejectedDisableStaysOwedAndKeepsTheSession() {
        env.signIn("pa")
        env.removeAccount("pa")
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "pa")
        env.backendAcknowledges = { false }

        assertEquals(OwedServerOperationRunner.Attempt.FAILED, pass())
        assertEquals(listOf("pa"), env.sentDisables)
        assertTrue(disableOwed("pa"))
        assertTrue(env.hasSession("pa"))
    }

    @Test
    fun signingInAgainVoidsTheOwedDisableAndKeepsTheAcknowledgement() {
        env.signIn("pa")
        env.acknowledge("pa", TestOwedWorkEnvironment.ACTIVE_SELECTION)
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "pa")

        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass())
        assertTrue(env.sentDisables.isEmpty())
        assertFalse(disableOwed("pa"))
        assertTrue(env.hasSession("pa"))
        assertEquals(
            TestOwedWorkEnvironment.ACTIVE_SELECTION,
            PcgProfileAlertAcknowledgementStore.read(env.acknowledgements, "pa")
        )
    }

    /**
     * T5b of the plan on #117: a removed profile with an active acknowledged selection
     * and no marker is still covered, by its acknowledgement alone. A guard: green on
     * the base and after the fix.
     */
    @Test
    fun t5bRemovedProfileIsCoveredByItsAcknowledgedSelection() {
        env.signIn("pb")
        env.acknowledge("pb", TestOwedWorkEnvironment.ACTIVE_SELECTION)
        env.removeAccount("pb")

        assertFalse(disableOwed("pb"))
        assertEquals(OwedServerOperationRunner.Attempt.COMPLETED, pass())
        assertEquals(listOf("pb"), env.sentDisables)
        assertFalse(env.hasSession("pb"))
        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass())
        assertEquals(listOf("pb"), env.sentDisables)
    }

    @Test
    fun removedProfileWithADisabledAcknowledgementOwesNothing() {
        env.signIn("pc")
        env.acknowledge("pc", PcgProfileAlertSelection.DISABLED)
        env.removeAccount("pc")

        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass())
        assertTrue(env.sentDisables.isEmpty())
        assertTrue(env.hasSession("pc"))
    }

    @Test
    fun removedProfileWithNoRecordAtAllOwesNothing() {
        env.signIn("pd")
        env.removeAccount("pd")

        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass())
        assertTrue(env.sentDisables.isEmpty())
    }

    @Test
    fun signedInProfileWithAnAcknowledgementOwesNothing() {
        env.signIn("pe")
        env.acknowledge("pe", TestOwedWorkEnvironment.ACTIVE_SELECTION)

        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass())
        assertTrue(env.sentDisables.isEmpty())
    }

    @Test
    fun oneFailedDisableFailsThePassButTheOthersAreStillSent() {
        for (profileId in listOf("pa", "pb")) {
            env.signIn(profileId)
            env.removeAccount(profileId)
            OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, profileId)
        }
        env.backendAcknowledges = { it == "pb" }

        assertEquals(OwedServerOperationRunner.Attempt.FAILED, pass())
        assertEquals(setOf("pa", "pb"), env.sentDisables.toSet())
        assertTrue(disableOwed("pa"))
        assertFalse(disableOwed("pb"))
        assertTrue(env.hasSession("pa"))
        assertFalse(env.hasSession("pb"))
    }

    // --- Both kinds in one pass ---------------------------------------------------

    @Test
    fun bothKindsAreAttemptedAndOneFailureFailsThePass() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.firebase.deletionOutcomes += FakeTokenDeletionOutcome.FAILS
        env.signIn("pa")
        env.removeAccount("pa")
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "pa")

        assertEquals(OwedServerOperationRunner.Attempt.FAILED, pass())
        assertEquals(1, env.firebase.startedDeletions)
        assertEquals(listOf("pa"), env.sentDisables)
        assertTrue(tokenDeletionOwed())
        assertFalse(disableOwed("pa"))
    }

    @Test
    fun theTokenDeletionComesBeforeTheDisables() {
        val order = mutableListOf<String>()
        env.firebase.awaitHook = { order += "token" }
        env.backendAcknowledges = { order += "disable:$it"; true }
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.signIn("pa")
        env.removeAccount("pa")
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "pa")

        assertEquals(OwedServerOperationRunner.Attempt.COMPLETED, pass())
        assertEquals(listOf("token", "disable:pa"), order)
    }

    @Test
    fun theDecisionsAreLoggedWithoutProfileIdentifiers() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.signIn("secretprofile")
        env.removeAccount("secretprofile")
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "secretprofile")

        pass()

        assertTrue(env.log.lines.contains("D owed token deletion decision=RUN"))
        assertTrue(env.log.lines.contains("D owed profile disable decision=DISABLE"))
        assertTrue(env.log.lines.none { "secretprofile" in it })
    }
}
