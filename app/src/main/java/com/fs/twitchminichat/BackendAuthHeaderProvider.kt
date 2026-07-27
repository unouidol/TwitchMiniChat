package com.fs.twitchminichat

/**
 * Authentication mode selected before a backend request is opened.
 *
 * The mode cannot change in response to an HTTP error, which prevents a failed Bearer
 * request from being retried through the legacy profile-only path.
 */
sealed interface BackendSessionAuthDecision {

    /** No local backend session exists, so temporary legacy compatibility is allowed. */
    data object Legacy : BackendSessionAuthDecision

    /** A backend session exists and must be sent as this Authorization header. */
    data class Bearer(val authorizationHeader: String) : BackendSessionAuthDecision

    /** Local session state cannot be trusted, so no backend request should be attempted. */
    data object Unavailable : BackendSessionAuthDecision
}

/** Selects the backend authentication mode for one profile before network I/O begins. */
class BackendAuthHeaderProvider(
    private val sessionReader: BackendSessionReader
) {

    /** Resolves Bearer, legacy, or unavailable authentication for [profileId]. */
    fun resolve(profileId: String): BackendSessionAuthDecision {
        return when (val lookup = sessionReader.lookup(profileId)) {
            BackendSessionLookup.Missing -> BackendSessionAuthDecision.Legacy
            BackendSessionLookup.Unavailable -> BackendSessionAuthDecision.Unavailable
            is BackendSessionLookup.Present -> {
                val header = bearerHeader(lookup.token)
                    ?: return BackendSessionAuthDecision.Unavailable
                BackendSessionAuthDecision.Bearer(header)
            }
        }
    }

    companion object {

        /** Formats a non-blank backend session without exposing it to logs. */
        fun bearerHeader(sessionToken: String?): String? {
            val normalizedToken = sessionToken?.trim().orEmpty()
            if (normalizedToken.isBlank()) return null
            return "Bearer $normalizedToken"
        }
    }
}
