package com.fs.twitchminichat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/*
 * Seams for the owed-work paths (#117, F3, PR-1).
 *
 * Nothing here changes what the application does. Each type names a collaborator the
 * owed-work runner, the token deletion, the sign-in, the account removal and the erase
 * used to reach directly - Firebase, the network, the stores behind a Context, Logcat -
 * so that a local test, which has no Android runtime, can stand in for it. The
 * production implementation of each is the code those paths ran before.
 */

/**
 * Diagnostic lines of the owed-work paths, at the two levels they use.
 *
 * Injected because `android.util.Log` throws in a local test, and because the paths'
 * tests read what they logged.
 */
internal interface OwedWorkLog {

    /** One debug line. */
    fun debug(message: String)

    /** One warning line. */
    fun warn(message: String)
}

/** [OwedWorkLog] writing to Logcat under one tag. */
internal class AndroidOwedWorkLog(private val tag: String) : OwedWorkLog {

    override fun debug(message: String) {
        Log.d(tag, message)
    }

    override fun warn(message: String) {
        Log.w(tag, message)
    }
}

/**
 * One Firebase Cloud Messaging token deletion that has been started.
 *
 * A handle rather than a Boolean so that a caller can wait on it with a timeout it
 * chooses, and a test can decide when that wait ends.
 */
internal interface FirebaseTokenDeletionHandle {

    /**
     * Blocks until the deletion completes.
     *
     * Returns normally on success. Throws when the deletion fails, and when
     * [timeoutSeconds] pass first; a timeout does not cancel the deletion, which may
     * still complete later (as `Tasks.await` documents).
     */
    fun await(timeoutSeconds: Long)

    /**
     * Calls [listener] once the deletion completes, with whether it succeeded; at once if it
     * already has. The thread it runs on is the one the deletion completes on.
     *
     * Used after a wait that timed out, for a deletion that completes later (R1 on #117).
     */
    fun onCompletion(listener: (succeeded: Boolean) -> Unit)
}

/** The two Firebase Cloud Messaging token calls the application makes. */
internal interface FirebaseTokenOperations {

    /** Starts deleting this installation's token, and returns at once. */
    fun startTokenDeletion(): FirebaseTokenDeletionHandle

    /**
     * Asks Firebase for this installation's current token.
     *
     * [onComplete] receives whether the call succeeded, its result and its error,
     * exactly as the task's `isSuccessful`, `result` and `exception` gave them. It runs
     * on whichever thread Firebase completes on, as the listener it replaces did.
     */
    fun fetchToken(onComplete: (successful: Boolean, token: String?, error: Throwable?) -> Unit)
}

/** [FirebaseTokenOperations] through Google Play services, as before. */
internal object PlayServicesFirebaseTokens : FirebaseTokenOperations {

    override fun startTokenDeletion(): FirebaseTokenDeletionHandle {
        return TaskTokenDeletionHandle(FirebaseMessaging.getInstance().deleteToken())
    }

    override fun fetchToken(
        onComplete: (successful: Boolean, token: String?, error: Throwable?) -> Unit
    ) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true, task.result, null)
            } else {
                onComplete(false, null, task.exception)
            }
        }
    }

    /** A deletion backed by its Play services task. */
    private class TaskTokenDeletionHandle(
        private val task: Task<Void>
    ) : FirebaseTokenDeletionHandle {

        override fun await(timeoutSeconds: Long) {
            Tasks.await(task, timeoutSeconds, TimeUnit.SECONDS)
        }

        override fun onCompletion(listener: (succeeded: Boolean) -> Unit) {
            task.addOnCompleteListener { completed -> listener(completed.isSuccessful) }
        }
    }
}

/**
 * Deletes the Firebase Cloud Messaging token and its cached copy, blocking.
 *
 * The body of the two token deletions of `FcmRegistrationUploader`, with their collaborators
 * passed in. PR-3 adds [deleteBlocking]'s `onLateSuccess` (R1 on #117).
 */
internal object FirebaseTokenDeletion {

    /**
     * Returns whether the token is now gone.
     *
     * A wait that times out does not cancel the deletion. If it completes after all, the token a
     * registration obtained in between is gone, and [onLateSuccess] runs, on the thread the
     * deletion completes on. The owed pass's registers the signed-in profiles again (R1); the
     * erase's, made before its wipe, does nothing.
     *
     * @param tokenCache the preferences file holding [cachedTokenKey].
     */
    fun deleteBlocking(
        tokens: FirebaseTokenOperations,
        timeoutSeconds: Long,
        tokenCache: SharedPreferences,
        cachedTokenKey: String,
        log: OwedWorkLog,
        onLateSuccess: () -> Unit
    ): Boolean {
        val deleted = runCatching {
            val deletion = tokens.startTokenDeletion()
            try {
                deletion.await(timeoutSeconds)
            } catch (timeout: TimeoutException) {
                deletion.onCompletion { succeeded -> if (succeeded) onLateSuccess() }
                throw timeout
            }
            true
        }.getOrElse { error ->
            log.warn(
                "delete_firebase_token failed " +
                    "errorType=${DiagnosticError.typeOf(error)}"
            )
            false
        }

        if (deleted) {
            /*
             * The cached copy is what every other path reuses instead of asking
             * Firebase again, so leaving it behind would hand a deleted token to the
             * next registration.
             */
            tokenCache.edit(commit = true) {
                remove(cachedTokenKey)
            }

            log.debug("delete_firebase_token completed ok=true")
        }

        return deleted
    }
}

/**
 * Everything one owed-work pass reads and calls.
 *
 * [OwedServerOperationRunner] reached each of these directly; a local test supplies its
 * own. The production implementation is [ProductionOwedWorkEnvironment].
 */
internal interface OwedWorkEnvironment {

    /** The file of [OwedServerOperationStore]. */
    val owedRecords: SharedPreferences

    /** The file of [PcgProfileAlertAcknowledgementStore]. */
    val acknowledgements: SharedPreferences

    /** The signed-in accounts. A new repository per read, as before. */
    val accounts: AccountRepository

    /** The backend sessions. */
    val sessions: BackendSessionStore

    /** The local alert selection of one profile, as [PcgProfileAlertSelectionStore] reads it. */
    fun readSelection(profileId: String): PcgProfileAlertSelection

    /** Deletes the token and its cached copy; whether it is now gone. */
    fun deleteFirebaseToken(): Boolean

    /** Sends one profile's disabled selection; whether the backend acknowledged it. */
    fun sendProfileAlertDisable(profileId: String): Boolean

    /**
     * Starts one registration pass for [profileId], and returns at once.
     *
     * Requested when a disable was acknowledged for a profile that had signed in again
     * while the disable was in flight, so the server's last word for it is the disable (R5).
     */
    fun requestProfileRegistration(profileId: String)

    /** Lock L and the state it guards. */
    val coordinator: OwedWorkCoordinator

    /** The runner's diagnostic lines. */
    val log: OwedWorkLog
}

/** [OwedWorkEnvironment] over the application's real stores, Firebase and backend. */
internal class ProductionOwedWorkEnvironment(
    private val appContext: Context
) : OwedWorkEnvironment {

    override val owedRecords: SharedPreferences
        get() = OwedServerOperationStore.preferences(appContext)

    override val acknowledgements: SharedPreferences
        get() = PcgProfileAlertAcknowledgementStore.preferences(appContext)

    override val accounts: AccountRepository
        get() = AccountRepository(appContext)

    override val sessions: BackendSessionStore
        get() = BackendSessionStore(appContext)

    override fun readSelection(profileId: String): PcgProfileAlertSelection {
        return PcgProfileAlertSelectionStore.read(appContext, profileId)
    }

    override fun deleteFirebaseToken(): Boolean {
        return FcmRegistrationUploader.deleteOwedFirebaseTokenBlocking(appContext)
    }

    override fun sendProfileAlertDisable(profileId: String): Boolean {
        return FcmRegistrationUploader.sendProfileAlertDisableBlocking(appContext, profileId)
    }

    override fun requestProfileRegistration(profileId: String) {
        FcmRegistrationUploader.requestProfileRegistration(appContext, profileId)
    }

    override val coordinator: OwedWorkCoordinator = OwedWorkCoordinator.PROCESS

    override val log: OwedWorkLog = AndroidOwedWorkLog("OWED_SERVER_OP")
}
