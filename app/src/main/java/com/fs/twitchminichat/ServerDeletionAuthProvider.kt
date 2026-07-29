package com.fs.twitchminichat

/**
 * Authentication outcome for the destructive server-data deletion request.
 *
 * This policy never permits legacy-key authentication.
 */
sealed interface ServerDeletionAuthDecision {

    /** A usable backend session was found for one local account profile. */
    data class Bearer(
        val profileId: String,
        val authorizationHeader: String
    ) : ServerDeletionAuthDecision

    /** None of the candidate profiles has a backend session yet. */
    data object SessionMissing : ServerDeletionAuthDecision

    /** At least one candidate session exists locally but cannot be trusted. */
    data object SessionUnavailable : ServerDeletionAuthDecision
}

/**
 * Selects one usable backend session for a destructive device-scoped request.
 *
 * Candidate profile identifiers are used only to inspect local session files.
 * They are never part of the deletion payload or its authoritative scope.
 */
class ServerDeletionAuthProvider(
    private val sessionReader: BackendSessionReader
) {

    /** Returns the first usable Bearer session without using a legacy key. */
    fun resolve(
        candidateProfileIds: Collection<String>
    ): ServerDeletionAuthDecision {
        var unavailableSessionFound = false

        candidateProfileIds
            .map(BackendSessionStore::normalizeProfileId)
            .filter(String::isNotBlank)
            .distinct()
            .forEach { profileId ->
                when (val lookup = sessionReader.lookup(profileId)) {
                    BackendSessionLookup.Missing -> Unit

                    BackendSessionLookup.Unavailable -> {
                        unavailableSessionFound = true
                    }

                    is BackendSessionLookup.Present -> {
                        val authorizationHeader =
                            BackendAuthHeaderProvider.bearerHeader(
                                lookup.token
                            )

                        if (authorizationHeader != null) {
                            return ServerDeletionAuthDecision.Bearer(
                                profileId = profileId,
                                authorizationHeader = authorizationHeader
                            )
                        }

                        unavailableSessionFound = true
                    }
                }
            }

        return if (unavailableSessionFound) {
            ServerDeletionAuthDecision.SessionUnavailable
        } else {
            ServerDeletionAuthDecision.SessionMissing
        }
    }
}
