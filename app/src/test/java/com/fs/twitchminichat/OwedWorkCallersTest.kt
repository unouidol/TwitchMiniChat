package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Characterization of the three local sections that write owed work outside the runner
 * (#117, F3, PR-1): the sign-in's, the account removal's answer, and the erase's record.
 *
 * Each was extracted from its Activity or Fragment unchanged. These tests record what
 * it does, so that PR-2 can put it under the coordinator's lock without changing it.
 */
class OwedWorkCallersTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var env: TestOwedWorkEnvironment

    @Before
    fun setUp() {
        env = TestOwedWorkEnvironment(temporaryFolder.newFolder("sessions"))
    }

    // --- The sign-in's local section ----------------------------------------------

    private fun signIn(
        profileId: String,
        persistSession: () -> Boolean = { env.sessions.putSession(profileId, "new-session") }
    ) = NewAccountLocalCommit.commit(
        accounts = env.accounts,
        account = TestOwedWorkEnvironment.account(profileId),
        persistSession = persistSession,
        owedRecords = env.owedRecords
    )

    @Test
    fun aCompletedSignInVoidsBothKindsOfOwedWork() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "pa")
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "pb")

        assertEquals(NewAccountLocalCommit.Outcome.ADDED, signIn("PA"))

        assertEquals(listOf("PA"), env.accounts.loadAccounts().map { it.profileId })
        assertTrue(env.hasSession("pa"))
        assertFalse(OwedServerOperationStore.isFirebaseTokenDeletionOwed(env.owedRecords))
        assertEquals(setOf("pb"), OwedServerOperationStore.owedProfileDisables(env.owedRecords))
    }

    @Test
    fun aSessionThatCannotBeSavedUndoesTheAccountAndVoidsNothing() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, "pa")

        assertEquals(
            NewAccountLocalCommit.Outcome.SESSION_NOT_SAVED,
            signIn("pa", persistSession = { false })
        )

        assertTrue(env.accounts.loadAccounts().isEmpty())
        assertTrue(OwedServerOperationStore.isFirebaseTokenDeletionOwed(env.owedRecords))
        assertTrue(OwedServerOperationStore.isProfileDisableOwed(env.owedRecords, "pa"))
    }

    @Test
    fun anAccountThatCannotBeSavedStopsBeforeTheSession() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.accountStore.writable = false
        var sessionAttempts = 0

        assertEquals(
            NewAccountLocalCommit.Outcome.ACCOUNT_NOT_SAVED,
            signIn("pa", persistSession = { sessionAttempts += 1; true })
        )

        assertEquals(0, sessionAttempts)
        assertTrue(env.accounts.loadAccounts().isEmpty())
        assertTrue(OwedServerOperationStore.isFirebaseTokenDeletionOwed(env.owedRecords))
    }

    // --- The account removal's answer ---------------------------------------------

    private var enqueued = 0

    private fun removalAnswer(profileId: String, backendOk: Boolean) =
        AccountRemovalDisableAnswer.apply(
            backendOk = backendOk,
            profileId = profileId,
            sessions = env.sessions,
            owedRecords = env.owedRecords,
            enqueueOwedWork = { enqueued += 1 },
            log = env.log
        )

    @Test
    fun anAcknowledgedRemovalDisableRemovesTheSessionAndOwesNothing() {
        env.signIn("pa")
        env.removeAccount("pa")

        assertFalse(removalAnswer("pa", backendOk = true))

        assertFalse(env.hasSession("pa"))
        assertFalse(OwedServerOperationStore.isProfileDisableOwed(env.owedRecords, "pa"))
        assertEquals(0, enqueued)
        assertEquals(
            listOf("D backend notification disable completed ok=true backendSessionRemoved=true"),
            env.log.lines
        )
    }

    @Test
    fun aFailedRemovalDisableIsRecordedQueuedAndKeepsTheSession() {
        env.signIn("pa")
        env.removeAccount("pa")

        assertTrue(removalAnswer("pa", backendOk = false))

        assertTrue(env.hasSession("pa"))
        assertTrue(OwedServerOperationStore.isProfileDisableOwed(env.owedRecords, "pa"))
        assertEquals(1, enqueued)
        assertTrue(env.log.lines.none { "pa" in it.split(' ', ';', '=') })
    }

    // --- The erase's owed record --------------------------------------------------

    @Test
    fun anEraseWhoseTokenDeletionFailedRecordsItAndQueuesTheWork() {
        assertTrue(
            DeviceEraseOwedWork.recordIfNeeded(
                tokenDeletionOk = false,
                owedRecords = env.owedRecords,
                enqueueOwedWork = { enqueued += 1 }
            )
        )

        assertTrue(OwedServerOperationStore.isFirebaseTokenDeletionOwed(env.owedRecords))
        assertEquals(1, enqueued)
    }

    @Test
    fun anEraseWhoseTokenDeletionSucceededRecordsNothing() {
        assertFalse(
            DeviceEraseOwedWork.recordIfNeeded(
                tokenDeletionOk = true,
                owedRecords = env.owedRecords,
                enqueueOwedWork = { enqueued += 1 }
            )
        )

        assertFalse(OwedServerOperationStore.isFirebaseTokenDeletionOwed(env.owedRecords))
        assertEquals(0, enqueued)
        assertTrue(env.owedRecords.all.isEmpty())
    }
}
