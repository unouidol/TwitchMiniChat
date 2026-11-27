package com.fs.twitchminichat

import android.content.SharedPreferences

/*
 * The three local sections that write owed work outside OwedServerOperationRunner
 * (#117, F3).
 *
 * Each was extracted unchanged from the Activity or Fragment it ran in (PR-1), so that a
 * local test can run it; PR-2 puts each under lock L (OwedWorkCoordinator). Their callers
 * still decide what the user sees.
 *
 * Each takes the owed records as a function rather than as a file, and calls it under L: an
 * erase deletes the file under L, and a commit through an instance obtained before it would
 * write the whole old map back (R3).
 */

/** The local writes of a completed sign-in that adds a new account. */
internal object NewAccountLocalCommit {

    /** How the local commit ended. */
    enum class Outcome {

        /** The account and its session are saved, and the owed work they void is gone. */
        ADDED,

        /** The account list could not be read or replaced; nothing was written. */
        ACCOUNT_NOT_SAVED,

        /** The session could not be saved; the account was removed again. */
        SESSION_NOT_SAVED
    }

    /**
     * Saves [account], then its backend session through [persistSession], then voids the
     * owed work a new sign-in makes void.
     *
     * The body of `AuthCallbackActivity.finishNewAccount` between the account's creation
     * and the screen it opens, in the same order, all of it under lock L. A pass deciding in
     * the middle of it would see an account that a failed session save then removes again,
     * and void a token deletion that is still owed (case (e), T7 on #117). The section is
     * local reads and writes only; it runs on `"tmc-oauth-finalize"`, not on the main thread.
     *
     * @param owedRecords the file of [OwedServerOperationStore], obtained under L.
     */
    fun commit(
        coordinator: OwedWorkCoordinator,
        accounts: AccountRepository,
        account: AccountConfig,
        persistSession: () -> Boolean,
        owedRecords: () -> SharedPreferences
    ): Outcome = coordinator.locked("sign-in") {
        if (!accounts.addAccount(account)) {
            /*
             * The stored list could not be read or could not be replaced, so nothing
             * was written. Saying so is what keeps the other accounts recoverable:
             * the user retries instead of discovering later that they are gone.
             */
            return@locked Outcome.ACCOUNT_NOT_SAVED
        }

        if (!persistSession()) {
            accounts.removeById(account.id)
            return@locked Outcome.SESSION_NOT_SAVED
        }

        val records = owedRecords()

        /*
         * An erase that could not delete the Firebase Cloud Messaging token leaves that
         * deletion owed. Signing in again makes it void rather than pending: the next
         * registration creates a new token, and deleting it afterwards would cut the
         * alerts of a registration the user has just recreated, silently. See
         * OwedTokenDeletionPolicy.
         */
        coordinator.clearTokenDeletion(records)

        /*
         * Same reasoning for an alert disable owed by an earlier removal of this profile:
         * sending it now would switch off the registration this sign-in is about to
         * create. The queue re-checks as well, so this is the earlier of two guards, not
         * the only one. See OwedProfileDisablePolicy. Clearing it also changes the
         * profile's stamp, so a disable already in flight for it leaves the new session
         * alone when it answers (T4 on #117).
         */
        coordinator.clearProfileDisable(records, account.profileId)

        Outcome.ADDED
    }
}

/** What an account removal does around its alert disable, and with the backend's answer. */
internal object AccountRemovalDisableAnswer {

    /**
     * One removal's disable, from the account's removal to the backend's answer.
     *
     * Carries the profile's stamp at the removal, so the answer is applied only if nothing
     * happened to the profile since, and whether the removal claimed the profile's disable.
     */
    class Ticket internal constructor(
        val profileId: String,
        internal val stamp: OwedWorkCoordinator.Stamp,
        internal val claimed: Boolean
    )

    /** What [removeAccount] did. */
    class Removal internal constructor(
        /** Whether the account was in the list and is now gone from it. */
        val removedAccount: Boolean,
        /** What the disable request's answer is applied with. */
        val ticket: Ticket
    )

    /**
     * Removes the account of [profileId] from the visible list through [removeFromList],
     * under lock L, and claims the profile's disable for the request the caller sends next.
     *
     * While the claim is held, a pass that would send the same disable leaves it to this
     * request. If a pass already holds it - the profile was removed before, signed in, and is
     * being removed again - the request is sent anyway, unclaimed, and that pass's answer
     * leaves this removal's session and record alone, since the profile's stamp has changed
     * (R4 on #117).
     */
    fun removeAccount(
        coordinator: OwedWorkCoordinator,
        profileId: String,
        removeFromList: () -> Boolean
    ): Removal = coordinator.locked("removal") {
        val removedAccount = removeFromList()

        Removal(
            removedAccount = removedAccount,
            ticket = Ticket(
                profileId = profileId,
                stamp = coordinator.profileStamp(profileId),
                claimed = coordinator.claimProfile(profileId)
            )
        )
    }

    /**
     * Removes the backend session after an acknowledged disable, or records the disable
     * as owed and queues it after a failed one.
     *
     * The body of the callback `AccountProfileRemovalController.removeAccountFromDevice`
     * passed to the disable request, under lock L, on the main thread. Returns whether the
     * disable is now owed, in which case the caller tells the user.
     *
     * An acknowledged disable removes the session only if the profile is as the removal left
     * it. If it was signed in again meanwhile, the session is the new sign-in's, and it stays;
     * and since the server's last word for the profile is then this disable, one registration
     * is requested for it through [requestRegistration] (R5, R6 on #117).
     *
     * @param owedRecords the file of [OwedServerOperationStore], obtained under L.
     * @param enqueueOwedWork queues [OwedServerOperationWorker]; called outside L.
     * @param requestRegistration starts one registration pass for a profile; called outside L.
     */
    fun apply(
        coordinator: OwedWorkCoordinator,
        ticket: Ticket,
        backendOk: Boolean,
        accounts: AccountRepository,
        sessions: BackendSessionStore,
        owedRecords: () -> SharedPreferences,
        enqueueOwedWork: () -> Unit,
        requestRegistration: (profileId: String) -> Unit,
        log: OwedWorkLog
    ): Boolean {
        val profileId = ticket.profileId

        val answer = coordinator.locked("removal.answer") {
            if (ticket.claimed) coordinator.releaseProfile(profileId)

            if (!backendOk) {
                /*
                 * The session stays. It is what the retry authenticates with, and there is
                 * nothing else on this phone that could: the account is already gone from
                 * the visible list, so no later start-up pass would iterate it.
                 *
                 * How long it stays, decided rather than left to happen: until the disable
                 * goes through, until that account is signed in again, or until a full erase.
                 * If the backend were never reachable again, it would stay indefinitely.
                 * That is the intended behaviour and not an oversight. The session is scoped
                 * to this one profile and grants nothing that profile did not already have,
                 * and it is the only thing left on the phone that can finish what the user
                 * asked for. Dropping it after some timeout would protect nobody: it would
                 * only guarantee that the orphaned alerts stay on for good, reachable
                 * afterwards by an email request and by nothing else.
                 *
                 * This is deliberately not symmetric with the erase, which refuses to keep
                 * any credential across its wipe (see OwedServerOperationStore). The two
                 * look contradictory read one at a time, and they are not: there the user
                 * asked for everything on the phone to go, so keeping a usable session would
                 * contradict what they were told; here they asked for one thing to stop, and
                 * the session is what stops it.
                 *
                 * This is also a deliberate exception to the rule that nothing is queued or
                 * retried automatically. That rule keeps gameplay and alerts from acting
                 * without the user. Here the opposite is at stake: data left active on the
                 * server after the user asked for it to stop. Finishing that is not acting
                 * on the user's behalf, and nothing in this path can send a gameplay command
                 * or raise a notification.
                 */
                coordinator.recordProfileDisable(owedRecords(), profileId)
                return@locked RemovalAnswer.OWED
            }

            if (coordinator.profileStamp(profileId) != ticket.stamp) {
                val signedIn = accounts.loadAccounts().any {
                    AccountProfileIdResolver.resolve(it) ==
                        AccountProfileIdResolver.normalize(profileId)
                }
                log.debug(
                    "backend notification disable completed ok=true for a changed record; " +
                        "backend session kept registrationRequested=$signedIn"
                )
                return@locked if (signedIn) RemovalAnswer.REGISTER else RemovalAnswer.DONE
            }

            val backendSessionRemoved = sessions.removeProfile(profileId)

            log.debug(
                "backend notification disable completed ok=true " +
                    "backendSessionRemoved=$backendSessionRemoved"
            )
            RemovalAnswer.DONE
        }

        return when (answer) {
            RemovalAnswer.DONE -> false

            RemovalAnswer.REGISTER -> {
                requestRegistration(profileId)
                false
            }

            RemovalAnswer.OWED -> {
                enqueueOwedWork()

                log.debug(
                    "backend notification disable completed ok=false; " +
                        "recorded as owed, backend session kept"
                )
                true
            }
        }
    }

    /** What [apply] left to do outside L. */
    private enum class RemovalAnswer {
        DONE,
        REGISTER,
        OWED
    }
}

/** The owed-work side of an erase: the deletion of its records, and the record it leaves. */
internal object DeviceEraseOwedWork {

    /**
     * Runs [deletePreferences], the erase's deletion of the preferences files, under lock L.
     *
     * When [forgetOwedEntries] is true - the owed records and the acknowledgements are among
     * the files deleted - every entry an attempt in flight is acting on is forgotten too, so
     * its answer clears nothing the erase did not already clear (R3 on #117). The keep-accounts
     * reset keeps both files and passes false.
     */
    fun <T> deletePreferences(
        coordinator: OwedWorkCoordinator,
        forgetOwedEntries: Boolean,
        deletePreferences: () -> T
    ): T = coordinator.locked("erase.preferences") {
        if (forgetOwedEntries) coordinator.erased()
        deletePreferences()
    }

    /**
     * Records the token deletion as owed and queues it, when [DeviceEraseOutcomePolicy]
     * says it is owed. Returns whether it recorded it.
     *
     * The body of `SafetyPrivacyFragment.recordOwedTokenDeletionIfNeeded`, without its
     * log line, which stays with the caller and its tag. The record is written under L,
     * so a deletion already in flight does not clear it when it answers (T8 on #117).
     *
     * @param owedRecords the file of [OwedServerOperationStore], obtained under L.
     * @param enqueueOwedWork queues [OwedServerOperationWorker]; called outside L.
     */
    fun recordIfNeeded(
        coordinator: OwedWorkCoordinator,
        tokenDeletionOk: Boolean,
        owedRecords: () -> SharedPreferences,
        enqueueOwedWork: () -> Unit
    ): Boolean {
        if (!DeviceEraseOutcomePolicy.tokenDeletionOwed(tokenDeletionOk)) return false

        coordinator.locked("erase.record") { coordinator.recordTokenDeletion(owedRecords()) }
        enqueueOwedWork()
        return true
    }
}
