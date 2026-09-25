package com.fs.twitchminichat

/** What should happen to an owed Firebase Cloud Messaging token deletion. */
enum class OwedTokenDeletionDecision {

    /** Nothing is owed; there is no work to do. */
    NOTHING_OWED,

    /** The token deletion is still owed and must be attempted. */
    RUN,

    /**
     * An account has been signed in again on this phone, so the owed deletion is void.
     */
    VOID_SIGNED_IN
}

/**
 * Decides whether an owed token deletion may still run.
 *
 * The rule that matters is [VOID_SIGNED_IN][OwedTokenDeletionDecision.VOID_SIGNED_IN].
 * An owed deletion is recorded on a phone with no accounts left, to stop alerts nobody
 * can switch off any more. If the user then signs in again, registration creates a new
 * Firebase Cloud Messaging token and the backend starts delivering to it. Running the
 * owed deletion at that point would silently cut the alerts of a registration the user
 * has just recreated, and nothing in the application would report it: the alerts would
 * simply stop arriving. So signing in does not postpone the owed work, it cancels it -
 * what the erase wanted to achieve, an account-less phone that receives nothing, has
 * been undone by the user on purpose.
 *
 * Pure so this can be tested without Firebase, a device or a worker.
 */
object OwedTokenDeletionPolicy {

    /**
     * @param tokenDeletionOwed what [OwedServerOperationStore] holds at this moment,
     *   re-read at each attempt rather than captured when the work was scheduled.
     * @param signedInAccountCount how many accounts are stored on this phone now.
     */
    fun decide(
        tokenDeletionOwed: Boolean,
        signedInAccountCount: Int
    ): OwedTokenDeletionDecision {
        if (!tokenDeletionOwed) {
            return OwedTokenDeletionDecision.NOTHING_OWED
        }

        return if (signedInAccountCount > 0) {
            OwedTokenDeletionDecision.VOID_SIGNED_IN
        } else {
            OwedTokenDeletionDecision.RUN
        }
    }
}
