package com.fs.twitchminichat

import android.content.SharedPreferences

/*
 * The three local sections that write owed work outside OwedServerOperationRunner
 * (#117, F3, PR-1).
 *
 * Each was extracted unchanged from the Activity or Fragment it ran in, so that a local
 * test can run it, and so that the coordinator of PR-2 has one place to put each under
 * its lock. Their callers still decide what the user sees.
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
     * and the screen it opens, in the same order.
     *
     * @param owedRecords the file of [OwedServerOperationStore].
     */
    fun commit(
        accounts: AccountRepository,
        account: AccountConfig,
        persistSession: () -> Boolean,
        owedRecords: SharedPreferences
    ): Outcome {
        if (!accounts.addAccount(account)) {
            /*
             * The stored list could not be read or could not be replaced, so nothing
             * was written. Saying so is what keeps the other accounts recoverable:
             * the user retries instead of discovering later that they are gone.
             */
            return Outcome.ACCOUNT_NOT_SAVED
        }

        if (!persistSession()) {
            accounts.removeById(account.id)
            return Outcome.SESSION_NOT_SAVED
        }

        /*
         * An erase that could not delete the Firebase Cloud Messaging token leaves that
         * deletion owed. Signing in again makes it void rather than pending: the next
         * registration creates a new token, and deleting it afterwards would cut the
         * alerts of a registration the user has just recreated, silently. See
         * OwedTokenDeletionPolicy.
         */
        OwedServerOperationStore.clearFirebaseTokenDeletion(owedRecords)

        /*
         * Same reasoning for an alert disable owed by an earlier removal of this profile:
         * sending it now would switch off the registration this sign-in is about to
         * create. The queue re-checks as well, so this is the earlier of two guards, not
         * the only one. See OwedProfileDisablePolicy.
         */
        OwedServerOperationStore.clearOwedProfileDisable(
            preferences = owedRecords,
            profileId = account.profileId
        )

        return Outcome.ADDED
    }
}

/** What an account removal does with the backend's answer to its alert disable. */
internal object AccountRemovalDisableAnswer {

    /**
     * Removes the backend session after an acknowledged disable, or records the disable
     * as owed and queues it after a failed one.
     *
     * The body of the callback `AccountProfileRemovalController.removeAccountFromDevice`
     * passed to the disable request. Returns whether the disable is now owed, in which
     * case the caller tells the user.
     *
     * @param owedRecords the file of [OwedServerOperationStore].
     * @param enqueueOwedWork queues [OwedServerOperationWorker].
     */
    fun apply(
        backendOk: Boolean,
        profileId: String,
        sessions: BackendSessionStore,
        owedRecords: SharedPreferences,
        enqueueOwedWork: () -> Unit,
        log: OwedWorkLog
    ): Boolean {
        if (backendOk) {
            val backendSessionRemoved = sessions.removeProfile(profileId)

            log.debug(
                "backend notification disable completed ok=true " +
                    "backendSessionRemoved=$backendSessionRemoved"
            )
            return false
        }

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
        OwedServerOperationStore.recordOwedProfileDisable(owedRecords, profileId)
        enqueueOwedWork()

        log.debug(
            "backend notification disable completed ok=false; " +
                "recorded as owed, backend session kept"
        )
        return true
    }
}

/** The owed record an erase leaves when its token deletion failed. */
internal object DeviceEraseOwedWork {

    /**
     * Records the token deletion as owed and queues it, when [DeviceEraseOutcomePolicy]
     * says it is owed. Returns whether it recorded it.
     *
     * The body of `SafetyPrivacyFragment.recordOwedTokenDeletionIfNeeded`, without its
     * log line, which stays with the caller and its tag.
     *
     * @param owedRecords the file of [OwedServerOperationStore].
     * @param enqueueOwedWork queues [OwedServerOperationWorker].
     */
    fun recordIfNeeded(
        tokenDeletionOk: Boolean,
        owedRecords: SharedPreferences,
        enqueueOwedWork: () -> Unit
    ): Boolean {
        if (!DeviceEraseOutcomePolicy.tokenDeletionOwed(tokenDeletionOk)) return false

        OwedServerOperationStore.recordFirebaseTokenDeletion(owedRecords)
        enqueueOwedWork()
        return true
    }
}
