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
     * The owed record is re-read here, at the moment of acting, rather than captured
     * when the work was scheduled: between the two, the user may have signed in again,
     * which voids an owed token deletion rather than postponing it. See
     * [OwedTokenDeletionPolicy].
     */
    fun runOwedWorkBlocking(context: Context): Attempt {
        val appContext = context.applicationContext

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
