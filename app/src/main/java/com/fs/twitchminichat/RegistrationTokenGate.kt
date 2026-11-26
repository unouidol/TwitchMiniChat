package com.fs.twitchminichat

import android.content.SharedPreferences
import androidx.core.content.edit

/*
 * The token gate and the late token deletion (#117, F3, PR-3: case (a), R1).
 *
 * Case (a): an owed-work pass decided to delete the Firebase Cloud Messaging token while no
 * account was signed in. While its deletion was in flight, a sign-in registered the token, and
 * the deletion then removed it: the new registration could receive no alert. PR-2 made the
 * sign-in void the debt under lock L and the pass claim the deletion; what is left is that a
 * registration must not obtain its token while that claimed deletion is still in flight.
 */

/** The token a request to the backend sends, obtained behind the token gate. */
internal object RegistrationTokenGate {

    /**
     * Calls [onToken] with the token a request for [selection] sends, or with null when none
     * could be obtained.
     *
     * - **A selection that needs no delivery** sends [callerToken] at once. It never waits: a
     *   disable must reach the backend whatever happens to the token (T13 on #117).
     * - **A selection that needs delivery** first waits, through
     *   [OwedWorkCoordinator.awaitTokenDeletionNotInFlight], for an owed token deletion in
     *   flight, at most [maxWaitNanos] from the moment it was claimed. It then ignores
     *   [callerToken], which may be the token that deletion removed, and takes the cached
     *   token, or asks Firebase for one and caches it (T3).
     *
     * Blocking while it waits: call it off the main thread, and not under L. [onToken] runs on
     * the calling thread, or on the thread Firebase answers on when it had to be asked.
     *
     * @param purpose what the token is for, named in the log lines.
     */
    fun obtain(
        selection: PcgProfileAlertSelection,
        callerToken: String,
        coordinator: OwedWorkCoordinator,
        tokens: FirebaseTokenOperations,
        tokenCache: SharedPreferences,
        cachedTokenKey: String,
        maxWaitNanos: Long,
        purpose: String,
        log: OwedWorkLog,
        onToken: (String?) -> Unit
    ) {
        if (!selection.requiresFirebaseDelivery) {
            onToken(callerToken)
            return
        }

        val gate = coordinator.awaitTokenDeletionNotInFlight(maxWaitNanos)
        if (gate != OwedWorkCoordinator.TokenGate.FREE) {
            log.debug("token gate passed for $purpose after a token deletion in flight outcome=$gate")
        }

        val cachedToken = tokenCache.getString(cachedTokenKey, null)?.trim().orEmpty()
        if (cachedToken.isNotEmpty()) {
            onToken(cachedToken)
            return
        }

        log.debug("No cached Firebase Cloud Messaging token; fetching one for $purpose")

        tokens.fetchToken { successful, token, error ->
            if (!successful) {
                log.warn(
                    "Unable to fetch a Firebase Cloud Messaging token for $purpose " +
                        "errorType=${DiagnosticError.typeOf(error)}"
                )
                onToken(null)
                return@fetchToken
            }

            val freshToken = token?.trim().orEmpty()
            if (freshToken.isEmpty()) {
                log.warn("Fresh Firebase Cloud Messaging token is blank for $purpose")
                onToken(null)
                return@fetchToken
            }

            tokenCache.edit {
                putString(cachedTokenKey, freshToken)
            }

            log.debug("Fetched a Firebase Cloud Messaging token for $purpose")
            onToken(freshToken)
        }
    }
}

/**
 * What follows a token deletion that timed out and then completed (R1 on #117).
 *
 * A wait on a deletion can time out while Firebase completes it later: the timeout does not
 * cancel the task. The gate has opened by then, so a registration made in between may hold the
 * token that the late deletion has just removed, and until the next start-up registration that
 * profile would receive nothing.
 */
internal object LateTokenDeletion {

    /**
     * Called when a deletion whose wait timed out has completed after all.
     *
     * Under L, it drops the cached token, which is now a deleted one, and reads the signed-in
     * profiles; outside L, it requests one registration for each through [requestRegistration],
     * which obtains a new token. With no account signed in there is nothing to register, and
     * nothing is requested.
     */
    fun completed(
        coordinator: OwedWorkCoordinator,
        accounts: AccountRepository,
        tokenCache: SharedPreferences,
        cachedTokenKey: String,
        requestRegistration: (profileId: String) -> Unit,
        log: OwedWorkLog
    ) {
        val profileIds = coordinator.locked("token.late-deletion") {
            tokenCache.edit(commit = true) {
                remove(cachedTokenKey)
            }

            accounts.loadAccounts()
                .map(AccountProfileIdResolver::resolve)
                .filter(String::isNotBlank)
                .distinct()
        }

        log.debug("delete_firebase_token completed late registrationsRequested=${profileIds.size}")

        profileIds.forEach(requestRegistration)
    }
}
