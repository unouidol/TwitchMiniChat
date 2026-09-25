package com.fs.twitchminichat

import android.content.Context
import android.util.Log
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
 */
object OwedServerOperationRunner {

    /** Logcat tag shared with [OwedServerOperationWorker]. */
    private const val TAG = "OWED_SERVER_OP"

    /** How one pass over the owed work ended. */
    enum class Attempt {

        /** Nothing was owed, or what was owed became void. */
        NOTHING_TO_DO,

        /** Everything owed has now been done. */
        COMPLETED,

        /** Something owed did not go through and is still owed. */
        FAILED
    }

    /**
     * Runs whatever is still owed. Blocking: callers must be off the main thread.
     *
     * Every record is re-read here, at the moment of acting, rather than captured when
     * the work was scheduled. Between the two the user may have signed an account in
     * again, which voids owed work rather than postponing it - see
     * [OwedTokenDeletionPolicy] and [OwedProfileDisablePolicy].
     *
     * Both kinds are attempted even if the first fails: they are independent, and a phone
     * that cannot reach Firebase may still reach the backend.
     */
    fun runOwedWorkBlocking(context: Context): Attempt {
        val appContext = context.applicationContext

        val tokenAttempt = runOwedTokenDeletion(appContext)
        val disableAttempt = runOwedProfileDisables(appContext)

        return combine(tokenAttempt, disableAttempt)
    }

    /** Deletes the Firebase Cloud Messaging token an erase could not delete. */
    private fun runOwedTokenDeletion(appContext: Context): Attempt {
        val decision = OwedTokenDeletionPolicy.decide(
            tokenDeletionOwed = OwedServerOperationStore
                .isFirebaseTokenDeletionOwed(appContext),
            signedInAccountCount = AccountRepository(appContext).loadAccounts().size
        )

        Log.d(TAG, "owed token deletion decision=$decision")

        return when (decision) {
            OwedTokenDeletionDecision.NOTHING_OWED -> Attempt.NOTHING_TO_DO

            OwedTokenDeletionDecision.VOID_SIGNED_IN -> {
                OwedServerOperationStore.clearFirebaseTokenDeletion(appContext)
                Attempt.NOTHING_TO_DO
            }

            OwedTokenDeletionDecision.RUN -> {
                if (FcmRegistrationUploader.deleteFirebaseTokenBlocking(appContext)) {
                    OwedServerOperationStore.clearFirebaseTokenDeletion(appContext)
                    Attempt.COMPLETED
                } else {
                    Attempt.FAILED
                }
            }
        }
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
     */
    private fun runOwedProfileDisables(appContext: Context): Attempt {
        val candidateProfileIds =
            PcgProfileAlertAcknowledgementStore.knownProfileIds(appContext) +
                OwedServerOperationStore.owedProfileDisables(appContext)

        if (candidateProfileIds.isEmpty()) return Attempt.NOTHING_TO_DO

        val signedInProfileIds = AccountRepository(appContext)
            .loadAccounts()
            .map(AccountProfileIdResolver::resolve)
            .filter(String::isNotBlank)
            .toSet()

        var completed = false
        var failed = false

        for (profileId in candidateProfileIds) {
            val local = if (profileId in signedInProfileIds) {
                PcgProfileLocalSelection.Present(
                    PcgProfileAlertSelectionStore.read(appContext, profileId)
                )
            } else {
                PcgProfileLocalSelection.Removed
            }

            val decision = OwedProfileDisablePolicy.decide(
                acknowledged = PcgProfileAlertAcknowledgementStore
                    .read(appContext, profileId),
                explicitlyOwed = OwedServerOperationStore
                    .isProfileDisableOwed(appContext, profileId),
                local = local
            )

            Log.d(TAG, "owed profile disable decision=$decision")

            when (decision) {
                OwedProfileDisableDecision.NOTHING_OWED -> Unit

                OwedProfileDisableDecision.VOID_SIGNED_IN -> {
                    /*
                     * The acknowledged selection is deliberately left alone: the backend
                     * still holds whatever it holds, and the next registration pushes the
                     * recreated selection and records the new acknowledgement.
                     */
                    OwedServerOperationStore
                        .clearOwedProfileDisable(appContext, profileId)
                }

                OwedProfileDisableDecision.DISABLE -> {
                    val ok = FcmRegistrationUploader
                        .sendProfileAlertDisableBlocking(appContext, profileId)

                    Log.d(TAG, "owed profile disable sent ok=$ok")

                    if (ok) {
                        OwedServerOperationStore
                            .clearOwedProfileDisable(appContext, profileId)

                        /*
                         * Only now is the session removed. It is the credential the
                         * request authenticates with, so removing it at the account's
                         * removal - as this used to - made the retry impossible.
                         */
                        val sessionRemoved =
                            BackendSessionStore(appContext).removeProfile(profileId)

                        Log.d(
                            TAG,
                            "owed profile disable acknowledged " +
                                "backendSessionRemoved=$sessionRemoved"
                        )
                        completed = true
                    } else {
                        failed = true
                    }
                }
            }
        }

        return when {
            failed -> Attempt.FAILED
            completed -> Attempt.COMPLETED
            else -> Attempt.NOTHING_TO_DO
        }
    }

    /**
     * Reduces the two passes to one outcome.
     *
     * A failure anywhere makes the pass a failure, so the queue retries everything that
     * is still owed rather than only the kind that happened to fail last.
     */
    private fun combine(first: Attempt, second: Attempt): Attempt {
        return when {
            first == Attempt.FAILED || second == Attempt.FAILED -> Attempt.FAILED
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
     * reach the token finds the record already cleared, or deletes an already deleted
     * token, both of which are harmless.
     */
    fun attemptNowAndSchedule(context: Context) {
        val appContext = context.applicationContext

        OwedServerOperationWorker.enqueue(appContext)

        thread(start = true, name = "owed-server-op-startup") {
            runOwedWorkBlocking(appContext)
        }
    }
}
