package com.fs.twitchminichat

import android.content.Context
import kotlin.concurrent.thread

/**
 * Carries out the server-facing work recorded in [OwedServerOperationStore].
 *
 * One implementation, two callers: [OwedServerOperationWorker], which is the durable
 * path across process death and reboot, and [attemptNowAndSchedule], the start-up
 * backstop for the case where the worker is never allowed to run.
 *
 * Two kinds of owed work, one mechanism: a Firebase Cloud Messaging token deletion owed
 * by an erase, and an alert disable owed by an account removal. They are attempted in
 * that order, both are attempted even when the other fails, and one failure anywhere
 * makes the whole pass a failure so the queue retries.
 *
 * Everything a pass reads and calls comes from one [OwedWorkEnvironment], so that a
 * local test can run the same pass against its own (#117, F3). The application always
 * passes [ProductionOwedWorkEnvironment], which logs under `OWED_SERVER_OP`, the tag
 * [OwedServerOperationWorker] shares.
 *
 * Each pass decides, claims and applies its answers under lock L, and makes its network
 * calls outside it: see [OwedWorkCoordinator] for the rules and why (#117, F3, PR-2).
 */
object OwedServerOperationRunner {


    /** How one pass over the owed work ended. */
    enum class Attempt {

        /** Nothing was owed, or what was owed became void. */
        NOTHING_TO_DO,

        /** Everything owed has now been done. */
        COMPLETED,

        /**
         * Something owed was left to the attempt that already had it in flight. That attempt
         * may still fail, so the worker tries again later instead of reporting success.
         */
        DEFERRED,

        /** Something owed did not go through and is still owed. */
        FAILED
    }

    /**
     * Runs whatever is still owed. Blocking: callers must be off the main thread.
     *
     * Every record is re-read here, under lock L ([OwedWorkCoordinator]), at the moment of
     * acting, rather than captured when the work was scheduled. Between the two the user may
     * have signed an account in again, which voids owed work rather than postponing it - see
     * [OwedTokenDeletionPolicy] and [OwedProfileDisablePolicy].
     *
     * Both kinds are attempted even if the first fails: they are independent, and a phone
     * that cannot reach Firebase may still reach the backend.
     */
    fun runOwedWorkBlocking(context: Context): Attempt {
        return runOwedWorkBlocking(ProductionOwedWorkEnvironment(context.applicationContext))
    }

    /** [runOwedWorkBlocking] against [env]; the application's own call passes the real one. */
    internal fun runOwedWorkBlocking(env: OwedWorkEnvironment): Attempt {
        val tokenAttempt = runOwedTokenDeletion(env)
        val disableAttempt = runOwedProfileDisables(env)

        return combine(tokenAttempt, disableAttempt)
    }

    /** What the token deletion's decision, taken under L, leaves the pass to do. */
    private sealed interface TokenStart {

        /** The pass ends with [attempt] and deletes nothing. */
        data class Done(val attempt: Attempt) : TokenStart

        /** Delete the token; [stamp] is the record the deletion is for. */
        data class Delete(val stamp: OwedWorkCoordinator.Stamp) : TokenStart
    }

    /**
     * Deletes the Firebase Cloud Messaging token an erase could not delete.
     *
     * Decided and claimed under L, deleted outside it, and cleared under L only if the record
     * is still the one the decision read: an erase that recorded a new deletion meanwhile keeps
     * its record (T8 on #117).
     */
    private fun runOwedTokenDeletion(env: OwedWorkEnvironment): Attempt {
        val coordinator = env.coordinator

        val start = coordinator.locked { startTokenDeletion(env) }
        val stamp = when (start) {
            is TokenStart.Done -> return start.attempt
            is TokenStart.Delete -> start.stamp
        }

        var deleted = false
        try {
            deleted = env.deleteFirebaseToken()
        } finally {
            coordinator.locked {
                coordinator.releaseToken()

                if (deleted &&
                    !coordinator.clearTokenDeletionIfUnchanged(env.owedRecords, stamp)
                ) {
                    env.log.debug("owed token deletion record changed while in flight; kept")
                }
            }
        }

        return if (deleted) Attempt.COMPLETED else Attempt.FAILED
    }

    /** The token deletion's decision, and its claim when it is to run. Under L. */
    private fun startTokenDeletion(env: OwedWorkEnvironment): TokenStart {
        val coordinator = env.coordinator

        val decision = OwedTokenDeletionPolicy.decide(
            tokenDeletionOwed = OwedServerOperationStore
                .isFirebaseTokenDeletionOwed(env.owedRecords),
            signedInAccountCount = env.accounts.loadAccounts().size
        )

        env.log.debug("owed token deletion decision=$decision")

        return when (decision) {
            OwedTokenDeletionDecision.NOTHING_OWED -> TokenStart.Done(Attempt.NOTHING_TO_DO)

            OwedTokenDeletionDecision.VOID_SIGNED_IN -> {
                coordinator.clearTokenDeletion(env.owedRecords)
                TokenStart.Done(Attempt.NOTHING_TO_DO)
            }

            OwedTokenDeletionDecision.RUN -> {
                if (coordinator.claimToken()) {
                    TokenStart.Delete(coordinator.tokenStamp())
                } else {
                    env.log.debug("owed token deletion deferred: in flight elsewhere")
                    TokenStart.Done(Attempt.DEFERRED)
                }
            }
        }
    }

    /** What one profile's decision, taken under L, leaves the pass to do. */
    private sealed interface ProfileStart {

        /** Nothing to send for this profile. */
        data object Done : ProfileStart

        /** Another attempt has this profile's disable in flight. */
        data object Deferred : ProfileStart

        /** Send the disable; [stamp] is the entry it is sent for. */
        data class Send(val stamp: OwedWorkCoordinator.Stamp) : ProfileStart
    }

    /**
     * Tells the backend about profiles whose alerts an account removal could not switch
     * off.
     *
     * Two records are consulted, because either can name a profile the other does not:
     * the profiles the backend has acknowledged something for, and the profiles whose
     * disable attempt has already failed. Whether the account is back on this phone is
     * read from [AccountRepository], never from the profile-scoped alert stores: an
     * account removal clears those, and reading them afterwards returns an *active*
     * default mode, so a removed profile would look as if it wanted alerts.
     *
     * The accounts are read again for each profile, under L, immediately before deciding:
     * a single read before the loop let a profile signed in again meanwhile look removed
     * (T10 on #117). The answer is applied only if the profile's entry is unchanged; if the
     * profile came back while the disable was in flight, the server may now hold a disable for
     * an account on this phone, so one registration is requested for it, after the answer (R5).
     */
    private fun runOwedProfileDisables(env: OwedWorkEnvironment): Attempt {
        val coordinator = env.coordinator

        val candidateProfileIds = coordinator.locked {
            PcgProfileAlertAcknowledgementStore.knownProfileIds(env.acknowledgements) +
                OwedServerOperationStore.owedProfileDisables(env.owedRecords)
        }

        if (candidateProfileIds.isEmpty()) return Attempt.NOTHING_TO_DO

        var completed = false
        var deferred = false
        var failed = false

        for (profileId in candidateProfileIds) {
            val start = coordinator.locked { startProfileDisable(env, profileId) }

            val stamp = when (start) {
                ProfileStart.Done -> continue
                ProfileStart.Deferred -> {
                    deferred = true
                    continue
                }
                is ProfileStart.Send -> start.stamp
            }

            var ok = false
            var registrationOwed = false
            try {
                ok = env.sendProfileAlertDisable(profileId)
                env.log.debug("owed profile disable sent ok=$ok")
            } finally {
                registrationOwed = coordinator.locked {
                    applyProfileDisableAnswer(env, profileId, stamp, ok)
                }
            }

            if (registrationOwed) env.requestProfileRegistration(profileId)

            if (ok) completed = true else failed = true
        }

        return when {
            failed -> Attempt.FAILED
            deferred -> Attempt.DEFERRED
            completed -> Attempt.COMPLETED
            else -> Attempt.NOTHING_TO_DO
        }
    }

    /** One profile's decision, and its claim when it is to be sent. Under L. */
    private fun startProfileDisable(env: OwedWorkEnvironment, profileId: String): ProfileStart {
        val coordinator = env.coordinator

        val local = if (isSignedIn(env, profileId)) {
            PcgProfileLocalSelection.Present(env.readSelection(profileId))
        } else {
            PcgProfileLocalSelection.Removed
        }

        val decision = OwedProfileDisablePolicy.decide(
            acknowledged = PcgProfileAlertAcknowledgementStore
                .read(env.acknowledgements, profileId),
            explicitlyOwed = OwedServerOperationStore
                .isProfileDisableOwed(env.owedRecords, profileId),
            local = local
        )

        env.log.debug("owed profile disable decision=$decision")

        return when (decision) {
            OwedProfileDisableDecision.NOTHING_OWED -> ProfileStart.Done

            OwedProfileDisableDecision.VOID_SIGNED_IN -> {
                /*
                 * The acknowledged selection is deliberately left alone: the backend
                 * still holds whatever it holds, and the next registration pushes the
                 * recreated selection and records the new acknowledgement.
                 */
                coordinator.clearProfileDisable(env.owedRecords, profileId)
                ProfileStart.Done
            }

            OwedProfileDisableDecision.DISABLE -> {
                if (coordinator.claimProfile(profileId)) {
                    ProfileStart.Send(coordinator.profileStamp(profileId))
                } else {
                    env.log.debug("owed profile disable deferred: in flight elsewhere")
                    ProfileStart.Deferred
                }
            }
        }
    }

    /**
     * Applies the backend's answer to [profileId]'s disable, sent for the entry [stamp].
     * Returns whether a registration is owed to the profile (R5). Under L.
     */
    private fun applyProfileDisableAnswer(
        env: OwedWorkEnvironment,
        profileId: String,
        stamp: OwedWorkCoordinator.Stamp,
        ok: Boolean
    ): Boolean {
        val coordinator = env.coordinator
        coordinator.releaseProfile(profileId)

        if (coordinator.profileStamp(profileId) != stamp) {
            /*
             * Signed in again, removed again, or erased while the request was out: the
             * session and the record now belong to whatever happened since, and are not
             * this answer's to remove (R4).
             */
            val registrationOwed = ok && isSignedIn(env, profileId)
            env.log.debug(
                "owed profile disable answered for a changed record; kept " +
                    "registrationRequested=$registrationOwed"
            )
            return registrationOwed
        }

        if (!ok) return false

        coordinator.clearProfileDisable(env.owedRecords, profileId)

        /*
         * Only now is the session removed. It is the credential the request
         * authenticates with, so removing it at the account's removal - as this
         * used to - made the retry impossible.
         */
        val sessionRemoved = env.sessions.removeProfile(profileId)

        env.log.debug(
            "owed profile disable acknowledged " +
                "backendSessionRemoved=$sessionRemoved"
        )
        return false
    }

    /** Whether [profileId]'s account is on this phone now. */
    private fun isSignedIn(env: OwedWorkEnvironment, profileId: String): Boolean {
        val normalized = AccountProfileIdResolver.normalize(profileId)
        return env.accounts
            .loadAccounts()
            .any { AccountProfileIdResolver.resolve(it) == normalized }
    }

    /**
     * Reduces the two passes to one outcome.
     *
     * A failure anywhere makes the pass a failure, so the queue retries everything that
     * is still owed rather than only the kind that happened to fail last. Work left to
     * another attempt comes next, because that attempt may still fail.
     */
    private fun combine(first: Attempt, second: Attempt): Attempt {
        return when {
            first == Attempt.FAILED || second == Attempt.FAILED -> Attempt.FAILED
            first == Attempt.DEFERRED || second == Attempt.DEFERRED -> Attempt.DEFERRED
            first == Attempt.COMPLETED || second == Attempt.COMPLETED -> Attempt.COMPLETED
            else -> Attempt.NOTHING_TO_DO
        }
    }

    /**
     * Schedules the owed work and attempts it now, from application start.
     *
     * Both, not one: the worker survives the process and the reboot but can be held
     * back indefinitely by the vendor's battery management, while the attempt made
     * here runs only while the application is open. If they overlap, the second one to
     * reach an entry finds it claimed and leaves it to the first, or finds it already
     * cleared ([OwedWorkCoordinator]).
     *
     * Nothing a skip leaves behind is stranded. This attempt never retries, so an entry the
     * worker skips because this attempt holds it is retried by the worker itself, which
     * answers [Attempt.DEFERRED] with a retry. An entry this attempt skips is held by the
     * worker, which retries on its own failure, or by an account removal, which enqueues the
     * worker on its own failure.
     */
    fun attemptNowAndSchedule(context: Context) {
        val appContext = context.applicationContext

        OwedServerOperationWorker.enqueue(appContext)

        thread(start = true, name = "owed-server-op-startup") {
            runOwedWorkBlocking(appContext)
        }
    }
}
