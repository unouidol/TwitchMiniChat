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
 * Legacy local-token fallback is available only when no backend session existed before
 * the request. A Bearer error never changes the decision or triggers another request.
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
        legacyAccessToken: String,
        legacyUsername: String
    ): BackendIrcTokenResult {
        val normalizedProfileId = BackendSessionStore.normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            return legacyCredentials(legacyAccessToken, legacyUsername)
        }

        val authDecision = authHeaderProvider.resolve(normalizedProfileId)
        if (authDecision == BackendSessionAuthDecision.Unavailable) {
            return BackendIrcTokenResult.Failed
        }

        val authorizationHeader = when (authDecision) {
            is BackendSessionAuthDecision.Bearer -> authDecision.authorizationHeader
            BackendSessionAuthDecision.Legacy -> null
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
                } else if (authDecision == BackendSessionAuthDecision.Legacy) {
                    legacyCredentials(legacyAccessToken, legacyUsername)
                } else {
                    BackendIrcTokenResult.Failed
                }
            }

            is BackendIrcTokenApiResult.HttpError -> {
                if (
                    authDecision is BackendSessionAuthDecision.Bearer &&
                    response.statusCode in AUTHENTICATION_ERROR_CODES
                ) {
                    BackendIrcTokenResult.ReauthorizationRequired
                } else if (authDecision == BackendSessionAuthDecision.Legacy) {
                    legacyCredentials(legacyAccessToken, legacyUsername)
                } else {
                    BackendIrcTokenResult.Failed
                }
            }

            BackendIrcTokenApiResult.NetworkError -> {
                if (authDecision == BackendSessionAuthDecision.Legacy) {
                    legacyCredentials(legacyAccessToken, legacyUsername)
                } else {
                    BackendIrcTokenResult.Failed
                }
            }
        }
    }

    /** Builds the temporary compatibility result from an existing local account. */
    private fun legacyCredentials(
        legacyAccessToken: String,
        legacyUsername: String
    ): BackendIrcTokenResult {
        val token = legacyAccessToken.trim()
        val username = legacyUsername.trim()
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
