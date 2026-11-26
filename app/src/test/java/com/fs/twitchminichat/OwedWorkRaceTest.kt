package com.fs.twitchminichat

import com.fs.twitchminichat.OwedWorkRaceActions.erase
import com.fs.twitchminichat.OwedWorkRaceActions.pass
import com.fs.twitchminichat.OwedWorkRaceActions.removalAnswer
import com.fs.twitchminichat.OwedWorkRaceActions.removeAccount
import com.fs.twitchminichat.OwedWorkRaceActions.signIn
import com.fs.twitchminichat.OwedWorkRaceActions.workerRetries
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The regression tests of #117's races, cases (b)-(e) and R4-R6 (F3, PR-2; the plan's
 * section 4).
 *
 * Each test holds one party at a chosen point - a request in flight, or a store between its
 * read and its write - and runs another against it on its own thread. The harness rule of the
 * plan applies: a party that, after the fix, waits for lock L is let run until its thread has
 * **finished**, which is what happens without L, or is **parked**, which after the fix means on
 * L; the test then releases the held party. Nothing sleeps and nothing reads the time: every
 * wait is on a latch or on a thread's state, with a liveness guard that fails a broken test
 * instead of hanging it. The guard is never asserted.
 *
 * The production sections are reached only through [OwedWorkRaceActions], so this file runs
 * unchanged on the base commit with a base version of that file, where each test here but T15
 * fails.
 */
class OwedWorkRaceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var env: TestOwedWorkEnvironment

    @Before
    fun setUp() {
        env = TestOwedWorkEnvironment(temporaryFolder.newFolder("sessions"))
        env.tokenCache.edit().putString(TestOwedWorkEnvironment.CACHED_TOKEN_KEY, "tok-1").commit()
    }

    /** T15's rule holds in every test: no modelled network call is made under L. */
    @After
    fun noNetworkCallWasMadeUnderLockL() {
        assertEquals(0, env.networkCallsUnderLock.get())
    }

    private fun tokenDeletionOwed() =
        OwedServerOperationStore.isFirebaseTokenDeletionOwed(env.owedRecords)

    private fun owedDisables() = OwedServerOperationStore.owedProfileDisables(env.owedRecords)

    private fun recordOwedDisable(profileId: String) =
        OwedServerOperationStore.recordOwedProfileDisable(env.owedRecords, profileId)

    /** An account that was signed in, with [acknowledged] on the server, then removed. */
    private fun removedProfile(profileId: String, acknowledged: PcgProfileAlertSelection? = null) {
        env.signIn(profileId)
        if (acknowledged != null) env.acknowledge(profileId, acknowledged)
        env.removeAccount(profileId)
    }

    /** Holds the thread named [threadName] in its disable request until [Hold.release]. */
    private fun holdDisableRequestOf(threadName: String): Hold {
        val hold = Hold()
        env.beforeDisableAnswered = {
            if (Thread.currentThread().name == threadName) hold.reachedAndWait()
        }
        return hold
    }

    /** Holds the first commit of [threadName] to the owed records until [Hold.release]. */
    private fun holdFirstOwedCommitOf(threadName: String): Hold {
        val hold = Hold()
        env.owedRecords.beforeCommit = {
            if (Thread.currentThread().name == threadName) hold.reachedAndWaitOnce()
        }
        return hold
    }

    // --- Claims: one attempt per entry --------------------------------------------------

    /** T1: one token deletion; the second pass leaves it to the first, and the worker retries. */
    @Test
    fun t1TheOwedTokenIsDeletedOnceAndTheSecondPassIsRetried() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        val hold = Hold()
        env.firebase.awaitHook = {
            if (Thread.currentThread().name == "pass-1") hold.reachedAndWait()
        }

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        val second = Racer("pass-2") { pass(env) }.join()
        hold.release()

        assertEquals(OwedServerOperationRunner.Attempt.COMPLETED, pass1.join())
        assertEquals(1, env.firebase.startedDeletions)
        assertTrue(workerRetries(second))
        assertFalse(tokenDeletionOwed())
    }

    /** T2a: one disable, while a second pass runs during the first's request. */
    @Test
    fun t2aTheOwedDisableIsSentOnce() {
        removedProfile("pa")
        recordOwedDisable("pa")
        val hold = holdDisableRequestOf("pass-1")

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        val second = Racer("pass-2") { pass(env) }.join()
        hold.release()
        pass1.join()

        assertEquals(listOf("pa"), env.sentDisables)
        assertTrue(workerRetries(second))
    }

    /** T2b: a second pass that decided during the first's request does not fail after it. */
    @Test
    fun t2bTheLateSecondPassSendsNothingAndDoesNotFail() {
        removedProfile("pa")
        recordOwedDisable("pa")
        val hold1 = Hold()
        val hold2 = Hold()
        env.beforeDisableAnswered = {
            when (Thread.currentThread().name) {
                "pass-1" -> hold1.reachedAndWait()
                "pass-2" -> hold2.reachedAndWait()
            }
        }

        val pass1 = Racer("pass-1") { pass(env) }
        hold1.awaitReached()
        val pass2 = Racer("pass-2") { pass(env) }
        pass2.awaitFinishedOrParked()
        hold1.release()
        assertEquals(OwedServerOperationRunner.Attempt.COMPLETED, pass1.join())
        hold2.release()

        assertNotEquals(OwedServerOperationRunner.Attempt.FAILED, pass2.join())
        assertEquals(listOf("pa"), env.sentDisables)
    }

    /**
     * Not in the plan's table: an account removal claims its own disable, so a pass that runs
     * while the removal's request is out leaves the profile to it, rather than sending it again
     * and removing the session the removal's request may still need.
     */
    @Test
    fun t17APassDuringARemovalsOwnRequestLeavesItToTheRemoval() {
        env.signIn("pb")
        env.acknowledge("pb", TestOwedWorkEnvironment.ACTIVE_SELECTION)

        val removal = removeAccount(env, "pb")
        val during = pass(env)

        assertTrue(env.sentDisables.isEmpty())
        assertTrue(workerRetries(during))

        // The removal's request succeeds; its 2xx records the disabled selection.
        env.acknowledge("pb", PcgProfileAlertSelection.DISABLED)
        assertFalse(removalAnswer(env, removal, backendOk = true))

        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass(env))
        assertTrue(env.sentDisables.isEmpty())
        assertFalse(env.hasSession("pb"))
    }

    // --- Case (b): a profile signed in again ------------------------------------------

    /** T4: the disable in flight when the profile signs in again leaves the new session. */
    @Test
    fun t4ADisableInFlightKeepsTheNewSessionAndAsksForARegistration() {
        removedProfile("pp", TestOwedWorkEnvironment.ACTIVE_SELECTION)
        val hold = holdDisableRequestOf("pass-1")

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        assertEquals(
            NewAccountLocalCommit.Outcome.ADDED,
            Racer("sign-in") { signIn(env, "pp", "session-new") }.join()
        )
        hold.release()
        pass1.join()

        assertEquals("session-new", env.sessionToken("pp"))
        assertEquals(listOf("pp"), env.requestedRegistrations)
    }

    /**
     * T9, with the plan's assertions (3), (4) and (5) unchanged, and (1) and (2) replaced by "no
     * commit made after the sign-in's clear names pp".
     */
    @Test
    fun t9AMarkerClearedBySignInNeverComesBack() {
        removedProfile("pa", TestOwedWorkEnvironment.ACTIVE_SELECTION)
        recordOwedDisable("pa")
        removedProfile("pp")
        recordOwedDisable("pp")
        val hold = holdFirstOwedCommitOf("pass-1")

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        val signIn = Racer("sign-in") { signIn(env, "pp", "session-new") }
        signIn.awaitFinishedOrParked()
        hold.release()
        pass1.join()
        assertEquals(NewAccountLocalCommit.Outcome.ADDED, signIn.join())

        assertTrue(env.sentDisables.none { it == "pp" }) // (3)
        assertTrue(owedDisables().isEmpty()) // (4)
        assertEquals("session-new", env.sessionToken("pp")) // (5)

        val commits = env.owedRecords.commits.toList()
        val signInClear = commits.indexOfFirst { commit ->
            commit.thread == "sign-in" && commit.changes.values.any { it is Set<*> }
        }
        assertTrue(signInClear >= 0)
        assertTrue(
            commits.drop(signInClear + 1).none { commit ->
                commit.changes.values.any { it is Set<*> && "pp" in it }
            }
        )
    }

    /** T10: a profile signed in again during another profile's disable is read afresh. */
    @Test
    fun t10AProfileSignedInAgainDuringThePassIsNotDisabled() {
        removedProfile("pa", TestOwedWorkEnvironment.ACTIVE_SELECTION)
        removedProfile("pp", TestOwedWorkEnvironment.ACTIVE_SELECTION)
        val hold = holdDisableRequestOf("pass-1")

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        assertEquals(listOf("pa"), env.sentDisables)
        assertEquals(
            NewAccountLocalCommit.Outcome.ADDED,
            Racer("sign-in") { signIn(env, "pp", "session-new") }.join()
        )
        hold.release()
        pass1.join()

        assertEquals(listOf("pa"), env.sentDisables)
        assertEquals("session-new", env.sessionToken("pp"))
    }

    /** T11 (R4): removed, signed in, removed again: the second removal's debt and session stay. */
    @Test
    fun t11TheFirstDisablesAnswerLeavesTheSecondRemovalsDebt() {
        removedProfile("pp", TestOwedWorkEnvironment.ACTIVE_SELECTION)
        val hold = holdDisableRequestOf("pass-1")

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        assertEquals(
            NewAccountLocalCommit.Outcome.ADDED,
            Racer("sign-in") { signIn(env, "pp", "session-new") }.join()
        )
        val secondRemoval = Racer("removal") { removeAccount(env, "pp") }.join()
        assertTrue(Racer("removal") { removalAnswer(env, secondRemoval, backendOk = false) }.join())
        hold.release()
        pass1.join()

        assertEquals("session-new", env.sessionToken("pp"))
        assertTrue(OwedServerOperationStore.isProfileDisableOwed(env.owedRecords, "pp"))
        assertTrue(env.requestedRegistrations.isEmpty())
    }

    /** T14 (R6): the removal's own success, after the profile signed in again, keeps the session. */
    @Test
    fun t14TheRemovalsSuccessAfterASignInKeepsTheNewSession() {
        env.signIn("pp")
        val removal = removeAccount(env, "pp")
        assertEquals(NewAccountLocalCommit.Outcome.ADDED, signIn(env, "pp", "session-new"))

        assertFalse(removalAnswer(env, removal, backendOk = true))

        assertEquals("session-new", env.sessionToken("pp"))
        assertEquals(listOf("pp"), env.requestedRegistrations)
    }

    // --- Cases (c) and (d): store read-modify-writes ----------------------------------

    /** T5a, case (c): a removal's record written during a pass's clear survives it. */
    @Test
    fun t5aARemovalsRecordSurvivesAConcurrentClear() {
        removedProfile("pa")
        recordOwedDisable("pa")
        env.signIn("pb")
        val removalOfB = removeAccount(env, "pb")
        val hold = holdFirstOwedCommitOf("pass-1")

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        val answerOfB = Racer("removal-b") { removalAnswer(env, removalOfB, backendOk = false) }
        answerOfB.awaitFinishedOrParked()
        hold.release()
        pass1.join()
        assertTrue(answerOfB.join())

        assertEquals(setOf("pb"), owedDisables())
        pass(env)
        assertTrue("pb" in env.sentDisables)
    }

    /** T6, case (d): two passes, two profiles; no completed debt comes back. */
    @Test
    fun t6ACompletedDebtDoesNotComeBack() {
        for (profileId in listOf("pc", "pd")) {
            removedProfile(profileId)
            recordOwedDisable(profileId)
        }
        val hold = holdFirstOwedCommitOf("pass-1")

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        val pass2 = Racer("pass-2") { pass(env) }
        pass2.awaitFinishedOrParked()
        hold.release()
        pass1.join()
        pass2.join()

        assertTrue(owedDisables().isEmpty())
        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass(env))
        assertEquals(OwedServerOperationRunner.Attempt.NOTHING_TO_DO, pass(env))
    }

    // --- Case (e) and the erase's record ------------------------------------------------

    /** T7, case (e): a sign-in that fails does not void the owed token deletion. */
    @Test
    fun t7AFailedSignInLeavesTheTokenDeletionOwed() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        env.firebase.deletionOutcomes += FakeTokenDeletionOutcome.FAILS
        val hold = Hold()

        val signIn = Racer("sign-in") {
            signIn(env, "pa", persistSession = { hold.reachedAndWait(); false })
        }
        hold.awaitReached()
        val pass1 = Racer("pass-1") { pass(env) }
        pass1.awaitFinishedOrParked()
        hold.release()
        assertEquals(NewAccountLocalCommit.Outcome.SESSION_NOT_SAVED, signIn.join())
        pass1.join()

        assertTrue(tokenDeletionOwed())
        assertTrue(env.accounts.loadAccounts().isEmpty())
    }

    /** T8: a new erase's record, written while a deletion is in flight, survives its answer. */
    @Test
    fun t8ANewErasesRecordSurvivesTheDeletionInFlight() {
        OwedServerOperationStore.recordFirebaseTokenDeletion(env.owedRecords)
        val hold = Hold()
        env.firebase.awaitHook = {
            if (Thread.currentThread().name == "pass-1") hold.reachedAndWait()
        }

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        Racer("erase") { erase(env, tokenDeletionOk = false) }.join()
        hold.release()

        assertEquals(OwedServerOperationRunner.Attempt.COMPLETED, pass1.join())
        assertTrue(tokenDeletionOwed())
    }

    // --- No network under L -------------------------------------------------------------

    /** T15: a removal and an erase complete while a pass's request is blocked. */
    @Test
    fun t15RemovalAndEraseCompleteWhileARequestIsBlocked() {
        removedProfile("pa")
        recordOwedDisable("pa")
        env.signIn("pb")
        val hold = holdDisableRequestOf("pass-1")

        val pass1 = Racer("pass-1") { pass(env) }
        hold.awaitReached()
        assertTrue(
            Racer("removal") { removalAnswer(env, removeAccount(env, "pb"), backendOk = false) }
                .join()
        )
        Racer("erase") { erase(env, tokenDeletionOk = false) }.join()
        hold.release()
        pass1.join()

        assertEquals(0, env.networkCallsUnderLock.get())
    }

    // --- Synchronization helpers --------------------------------------------------------

    /** A point where one thread stops until the test lets it go. */
    private class Hold {
        private val reached = CountDownLatch(1)
        private val released = CountDownLatch(1)
        @Volatile
        private var used = false

        /** Signals that the point was reached, then waits to be released. */
        fun reachedAndWait() {
            reached.countDown()
            guard(released.await(GUARD_SECONDS, TimeUnit.SECONDS), "hold never released")
        }

        /** As [reachedAndWait] the first time; later calls pass straight through. */
        fun reachedAndWaitOnce() {
            if (used) return
            used = true
            reachedAndWait()
        }

        fun awaitReached() {
            guard(reached.await(GUARD_SECONDS, TimeUnit.SECONDS), "hold never reached")
        }

        fun release() = released.countDown()
    }

    /** [block] on a thread of its own named [name]; [join] returns its result or rethrows. */
    private class Racer<T>(name: String, block: () -> T) {
        @Volatile
        private var result: Result<T>? = null

        private val thread = Thread({ result = runCatching(block) }, name).apply { start() }

        /**
         * Waits until the thread has finished, or is parked: what it does without lock L, and
         * what it does after the fix while L is held. Spins on the thread's state, yielding,
         * with the liveness guard as its only bound.
         *
         * Parked is `WAITING`, which is how a thread waits for L, or `TIMED_WAITING`, which is
         * how it waits at a [Hold], whose wait carries the guard.
         */
        fun awaitFinishedOrParked() {
            val guardEnd = System.nanoTime() + TimeUnit.SECONDS.toNanos(GUARD_SECONDS)
            while (true) {
                when (thread.state) {
                    Thread.State.TERMINATED,
                    Thread.State.WAITING,
                    Thread.State.TIMED_WAITING -> return
                    else -> guard(System.nanoTime() < guardEnd, "${thread.name} never settled")
                }
                Thread.yield()
            }
        }

        fun join(): T {
            thread.join(TimeUnit.SECONDS.toMillis(GUARD_SECONDS))
            guard(!thread.isAlive, "${thread.name} never finished")
            return checkNotNull(result).getOrThrow()
        }
    }

    private companion object {

        /** The liveness guard of every wait: wide, and never asserted on its own. */
        const val GUARD_SECONDS = 60L

        fun guard(ok: Boolean, what: String) {
            if (!ok) throw AssertionError("liveness guard: $what")
        }
    }
}
