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

    /** Credentials could not be acquired; [failure] says why, and whether to try again. */
    data class Failed(val failure: BackendIrcTokenFailure) : BackendIrcTokenResult
}

/**
 * Why the token step failed, classified once, here.
 *
 * A transient failure is one a later identical request can be expected to clear: the
 * network, the server, a rate limit, an answer that was not a usable response. The chat
 * page keeps reconnecting after it. Every other failure is a definitive answer, or a
 * local state a retry would not change; the page stops and says so, as before #115.
 * HTTP 401/403 is not a failure here at all: it is [BackendIrcTokenResult.ReauthorizationRequired].
 */
sealed interface BackendIrcTokenFailure {

    /** True when the chat should stay in its reconnect cycle after this failure. */
    val isTransient: Boolean

    /** Shortest wait before the next attempt the server asked for, in milliseconds; null if none. */
    val minimumRetryDelayMs: Long?
        get() = null

    /** Log fields for this failure: cause and codes only, never a token, account or channel. */
    val logFields: String

    /** No connectivity, a name that does not resolve, a timeout, a reset, a failed TLS handshake. */
    data object Network : BackendIrcTokenFailure {
        override val isTransient = true
        override val logFields = "cause=network"
    }

    /** The server answered 5xx. */
    data class ServerError(val statusCode: Int) : BackendIrcTokenFailure {
        override val isTransient = true
        override val logFields = "cause=server_error status=$statusCode"
    }

    /** The server answered 429; [retryAfterSeconds] is its Retry-After, when valid. */
    data class RateLimited(val retryAfterSeconds: Long?) : BackendIrcTokenFailure {
        override val isTransient = true
        override val minimumRetryDelayMs: Long? = retryAfterSeconds?.times(1_000L)
        override val logFields = "cause=rate_limited retryAfterSec=${retryAfterSeconds ?: "none"}"
    }

    /** The answer was not a usable response; [reason] says how. */
    data class MalformedResponse(
        val reason: BackendIrcTokenMalformedReason
    ) : BackendIrcTokenFailure {
        override val isTransient = true
        override val logFields = "cause=malformed_response reason=${reason.logValue}"
    }

    /** The stored backend session exists but cannot be read. Retrying would read it again. */
    data object SessionUnavailable : BackendIrcTokenFailure {
        override val isTransient = false
        override val logFields = "cause=session_unavailable"
    }

    /** No backend session, and the locally stored Twitch credentials are incomplete. */
    data object LocalCredentialsMissing : BackendIrcTokenFailure {
        override val isTransient = false
        override val logFields = "cause=local_credentials_missing"
    }

    /** A 2xx whose JSON lacks the token or the username. */
    data object IncompleteCredentials : BackendIrcTokenFailure {
        override val isTransient = false
        override val logFields = "cause=incomplete_credentials"
    }

    /**
     * A definitive answer other than 401/403/429/5xx: 1xx, 3xx or another 4xx, such as
     * 404 `profile_not_found`, 409 `oauth_identity_mismatch` or 426 `upgrade_required`.
     */
    data class Rejected(val statusCode: Int) : BackendIrcTokenFailure {
        override val isTransient = false
        override val logFields = "cause=rejected status=$statusCode"
    }

    /** An exception that is not an input/output failure, such as a programming error. */
    data class UnexpectedException(val exceptionClass: String) : BackendIrcTokenFailure {
        override val isTransient = false
        override val logFields = "cause=unexpected_exception exception=$exceptionClass"
    }
}

/** One log line's fields for [this] result; no token, account or channel. */
val BackendIrcTokenResult.logFields: String
    get() = when (this) {
        is BackendIrcTokenResult.Success -> "result=success"
        BackendIrcTokenResult.ReauthorizationRequired -> "result=reauthorization_required"
        is BackendIrcTokenResult.Failed -> {
            val resultClass = if (failure.isTransient) "transient" else "not_transient"
            "result=$resultClass ${failure.logFields}"
        }
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

    /**
     * Returns credentials or a typed failure without retrying through another mode.
     *
     * This call never retries; the chat page decides whether to attempt again, from
     * [BackendIrcTokenFailure.isTransient].
     */
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

            BackendSessionAuthDecision.Unavailable ->
                return failed(BackendIrcTokenFailure.SessionUnavailable)
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
                    failed(BackendIrcTokenFailure.IncompleteCredentials)
                }
            }

            is BackendIrcTokenApiResult.HttpError -> classifyHttpError(response)

            BackendIrcTokenApiResult.NetworkError ->
                failed(BackendIrcTokenFailure.Network)

            is BackendIrcTokenApiResult.MalformedResponse ->
                failed(BackendIrcTokenFailure.MalformedResponse(response.reason))

            is BackendIrcTokenApiResult.UnexpectedError ->
                failed(BackendIrcTokenFailure.UnexpectedException(response.exceptionClass))
        }
    }

    /**
     * Classifies a completed non-2xx response. 401/403 explicitly reject the Bearer
     * session and are never downgraded to anything less strict.
     */
    private fun classifyHttpError(
        response: BackendIrcTokenApiResult.HttpError
    ): BackendIrcTokenResult {
        val status = response.statusCode

        return when {
            status in AUTHENTICATION_ERROR_CODES ->
                BackendIrcTokenResult.ReauthorizationRequired

            status == RATE_LIMITED ->
                failed(BackendIrcTokenFailure.RateLimited(response.retryAfterSeconds))

            status in SERVER_ERROR_CODES ->
                failed(BackendIrcTokenFailure.ServerError(status))

            status !in VALID_STATUS_CODES ->
                failed(
                    BackendIrcTokenFailure.MalformedResponse(
                        BackendIrcTokenMalformedReason.INVALID_STATUS
                    )
                )

            else -> failed(BackendIrcTokenFailure.Rejected(status))
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
            return failed(BackendIrcTokenFailure.LocalCredentialsMissing)
        }

        return BackendIrcTokenResult.Success(
            accessToken = token,
            username = username
        )
    }

    private fun failed(failure: BackendIrcTokenFailure): BackendIrcTokenResult =
        BackendIrcTokenResult.Failed(failure)

    private companion object {

        /** HTTP statuses that explicitly reject the supplied Bearer session. */
        val AUTHENTICATION_ERROR_CODES = setOf(401, 403)

        /** HTTP status of a rate limit. */
        const val RATE_LIMITED = 429

        /** HTTP statuses of a server error. */
        val SERVER_ERROR_CODES = 500..599

        /** HTTP statuses a valid status line can carry. */
        val VALID_STATUS_CODES = 100..599
    }
}
