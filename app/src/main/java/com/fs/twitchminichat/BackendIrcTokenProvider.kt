package com.fs.twitchminichat

/**
 * Result of acquiring fresh Twitch Internet Relay Chat (IRC) credentials.
 */
sealed interface BackendIrcTokenResult {

    /** Complete credentials ready for a single IRC connection attempt. */
    data class Success(
        val accessToken: String,
        val username: String
    ) : BackendIrcTokenResult

    /** The stored backend session was rejected and requires a manual reauthorization. */
    data object ReauthorizationRequired : BackendIrcTokenResult

    /** Credentials could not be acquired safely. */
    data object Failed : BackendIrcTokenResult
}

/**
 * Acquires IRC credentials using one authentication decision and one backend request.
 *
 * A missing backend session may use already stored local Twitch credentials without
 * contacting the backend. Every backend request itself is Bearer-authenticated.
 */
class BackendIrcTokenProvider(
    sessionReader: BackendSessionReader,
    private val tokenApi: BackendIrcTokenApi = OAuthBackendApi
) {

    /** Header policy bound to the same session snapshot source. */
    private val authHeaderProvider = BackendAuthHeaderProvider(sessionReader)

    /** Returns credentials or a typed failure without retrying through another mode. */
    fun acquire(
        profileId: String,
        localAccessToken: String,
        localUsername: String
    ): BackendIrcTokenResult {
        val normalizedProfileId = BackendSessionStore.normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            return localCredentials(localAccessToken, localUsername)
        }

        val authorizationHeader = when (
            val authDecision = authHeaderProvider.resolve(normalizedProfileId)
        ) {
            is BackendSessionAuthDecision.Bearer -> authDecision.authorizationHeader
            BackendSessionAuthDecision.Missing ->
                return localCredentials(localAccessToken, localUsername)

            BackendSessionAuthDecision.Unavailable -> return BackendIrcTokenResult.Failed
        }

        return when (
            val response = tokenApi.tokenForIrc(
                profileId = normalizedProfileId,
                authorizationHeader = authorizationHeader
            )
        ) {
            is BackendIrcTokenApiResult.Success -> {
                val freshToken = response.value.accessToken.trim()
                val freshUsername = response.value.username.trim()

                if (freshToken.isNotBlank() && freshUsername.isNotBlank()) {
                    BackendIrcTokenResult.Success(
                        accessToken = freshToken,
                        username = freshUsername
                    )
                } else {
                    BackendIrcTokenResult.Failed
                }
            }

            is BackendIrcTokenApiResult.HttpError -> {
                if (
                    response.statusCode in AUTHENTICATION_ERROR_CODES
                ) {
                    BackendIrcTokenResult.ReauthorizationRequired
                } else {
                    BackendIrcTokenResult.Failed
                }
            }

            BackendIrcTokenApiResult.NetworkError -> BackendIrcTokenResult.Failed
        }
    }

    /** Builds a direct IRC result from already stored local Twitch credentials. */
    private fun localCredentials(
        localAccessToken: String,
        localUsername: String
    ): BackendIrcTokenResult {
        val token = localAccessToken.trim()
        val username = localUsername.trim()
        if (token.isBlank() || username.isBlank()) {
            return BackendIrcTokenResult.Failed
        }

        return BackendIrcTokenResult.Success(
            accessToken = token,
            username = username
        )
    }

    private companion object {

        /** HTTP statuses that explicitly reject the supplied Bearer session. */
        val AUTHENTICATION_ERROR_CODES = setOf(401, 403)
    }
}
