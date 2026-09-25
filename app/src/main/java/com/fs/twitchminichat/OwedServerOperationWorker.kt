package com.fs.twitchminichat

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Finishes, on its own, the server-facing work the user already asked for.
 *
 * Why this is not the automation the product rules forbid. That rule keeps gameplay
 * and alerts from happening without a deliberate tap: nothing may catch, send a chat
 * command, or raise a notification by itself. This worker does the opposite of acting
 * on the user's behalf - it *stops* something the user has asked to stop and the
 * network refused at the time. Leaving that half-done is the harm: a phone that has
 * been erased, or an account that has been removed, keeps receiving alerts until
 * somebody happens to open the application again. Nothing here can send a gameplay
 * command or show an alert.
 *
 * Retry shape: the work is unique, constrained to a connected network, and retried
 * with exponential backoff from 30 seconds. After [MAX_RUN_ATTEMPTS] failed attempts
 * it gives up *for this enqueue only* - the owed record is deliberately left in place,
 * so [OwedServerOperationRunner] re-enqueues it at the next application start rather
 * than the work retrying for ever inside one scheduling. That start-up attempt is not
 * redundancy: on the test device family background work is deferred or killed by the
 * vendor's battery management, so the worker is the normal path and the start-up
 * attempt is the backstop when the worker never gets to run.
 */
class OwedServerOperationWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker(context, parameters) {

    override fun doWork(): Result {
        if (runAttemptCount >= MAX_RUN_ATTEMPTS) {
            /*
             * Still owed on purpose. Dropping the record here would end the retries
             * permanently for a phone that is simply failing to reach Firebase.
             */
            Log.w(
                TAG,
                "giving up this enqueue runAttemptCount=$runAttemptCount; work stays owed"
            )
            return Result.failure()
        }

        return when (val attempt = OwedServerOperationRunner.runOwedWorkBlocking(applicationContext)) {
            OwedServerOperationRunner.Attempt.NOTHING_TO_DO,
            OwedServerOperationRunner.Attempt.COMPLETED -> {
                Log.d(TAG, "worker finished attempt=$attempt")
                Result.success()
            }

            OwedServerOperationRunner.Attempt.FAILED -> {
                Log.d(
                    TAG,
                    "worker attempt failed runAttemptCount=$runAttemptCount; retrying"
                )
                Result.retry()
            }
        }
    }

    companion object {

        /** Logcat tag shared with [OwedServerOperationRunner]. */
        private const val TAG = "OWED_SERVER_OP"

        /** One queue, so a second erase cannot stack a second chain of retries. */
        private const val UNIQUE_WORK_NAME = "owed_server_operations"

        /** First backoff interval; WorkManager doubles it up to its own five-hour cap. */
        private const val BACKOFF_SECONDS = 30L

        /**
         * Attempts per enqueue before the chain stops and the next start re-enqueues.
         */
        internal const val MAX_RUN_ATTEMPTS = 8

        /**
         * Queues the owed work, keeping an existing chain rather than restarting it.
         *
         * Never throws: an erase must finish even on an installation where WorkManager
         * cannot be reached, because the start-up attempt still covers that case.
         */
        fun enqueue(context: Context) {
            val appContext = context.applicationContext

            val request = OneTimeWorkRequestBuilder<OwedServerOperationWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()

            runCatching {
                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
            }.onFailure { error ->
                Log.w(
                    TAG,
                    "could not enqueue owed work " +
                        "errorType=${DiagnosticError.typeOf(error)}"
                )
            }.onSuccess {
                Log.d(TAG, "owed work enqueued")
            }
        }
    }
}
