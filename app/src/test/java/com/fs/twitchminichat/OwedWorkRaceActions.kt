package com.fs.twitchminichat

/**
 * What the race tests of #117 do to the owed work, in one place.
 *
 * [OwedWorkRaceTest] reaches the production sections only through these, so that the same test
 * file can also be run on the base commit, with a version of this file written for the base's
 * signatures, to show each regression test red there (the plan on #117, section 3).
 */
internal object OwedWorkRaceActions {

    /** One pass of the owed work, as the worker and the start-up attempt run it. */
    fun pass(env: TestOwedWorkEnvironment): OwedServerOperationRunner.Attempt =
        OwedServerOperationRunner.runOwedWorkBlocking(env)

    /** Whether the worker retries after a pass that ended with [attempt]. */
    fun workerRetries(attempt: OwedServerOperationRunner.Attempt): Boolean =
        OwedServerOperationWorker.retries(attempt)

    /** A sign-in's local section for [profileId], saving [sessionToken] as its session. */
    fun signIn(
        env: TestOwedWorkEnvironment,
        profileId: String,
        sessionToken: String = "new-session-$profileId",
        persistSession: () -> Boolean = { env.sessions.putSession(profileId, sessionToken) }
    ): NewAccountLocalCommit.Outcome = NewAccountLocalCommit.commit(
        coordinator = env.coordinator,
        accounts = env.accounts,
        account = TestOwedWorkEnvironment.account(profileId),
        persistSession = persistSession,
        owedRecords = { env.owedRecords }
    )

    /** One account removal, between the account's removal and its disable's answer. */
    class Removal(internal val ticket: AccountRemovalDisableAnswer.Ticket)

    /** An account removal's removal of [profileId]'s account; its disable request is then out. */
    fun removeAccount(env: TestOwedWorkEnvironment, profileId: String): Removal {
        val removal = AccountRemovalDisableAnswer.removeAccount(env.coordinator, profileId) {
            env.accounts.removeById(TestOwedWorkEnvironment.accountId(profileId)) != null
        }
        check(removal.removedAccount)
        return Removal(removal.ticket)
    }

    /** The answer to [removal]'s disable; whether the disable is now owed. */
    fun removalAnswer(
        env: TestOwedWorkEnvironment,
        removal: Removal,
        backendOk: Boolean
    ): Boolean = AccountRemovalDisableAnswer.apply(
        coordinator = env.coordinator,
        ticket = removal.ticket,
        backendOk = backendOk,
        accounts = env.accounts,
        sessions = env.sessions,
        owedRecords = { env.owedRecords },
        enqueueOwedWork = {},
        requestRegistration = env::requestProfileRegistration,
        log = env.log
    )

    /** An erase: its deletion of the two files, then the record it leaves. */
    fun erase(env: TestOwedWorkEnvironment, tokenDeletionOk: Boolean) {
        DeviceEraseOwedWork.deletePreferences(env.coordinator, forgetOwedEntries = true) {
            env.owedRecords.edit().clear().commit()
            env.acknowledgements.edit().clear().commit()
        }
        DeviceEraseOwedWork.recordIfNeeded(
            coordinator = env.coordinator,
            tokenDeletionOk = tokenDeletionOk,
            owedRecords = { env.owedRecords },
            enqueueOwedWork = {}
        )
    }
}
