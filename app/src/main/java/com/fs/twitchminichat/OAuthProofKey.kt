package com.fs.twitchminichat

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Validates and derives the application-to-backend Proof Key for Code Exchange (PKCE).
 *
 * The verifier is a short-lived secret retained only in the app-private pending-request
 * store. Only its Secure Hash Algorithm 256-bit (SHA-256) challenge enters the browser URL.
 */
internal object OAuthProofKeyPolicy {

    /** Challenge method accepted by the hardened backend contract. */
    const val CODE_CHALLENGE_METHOD = "S256"

    /** Returns true only for an RFC 7636 verifier accepted by the backend. */
    fun isValidCodeVerifier(value: String): Boolean {
        return CODE_VERIFIER_PATTERN.matches(value)
    }

    /** Derives one unpadded Base64 URL-encoded S256 challenge from a valid verifier. */
    fun deriveS256CodeChallenge(codeVerifier: String): String? {
        if (!isValidCodeVerifier(codeVerifier)) return null

        return runCatching {
            val digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
                .digest(codeVerifier.toByteArray(Charsets.US_ASCII))
            BASE64_URL_ENCODER.encodeToString(digest)
        }.getOrNull()?.takeIf { challenge ->
            CODE_CHALLENGE_PATTERN.matches(challenge)
        }
    }

    private const val SHA_256_ALGORITHM = "SHA-256"

    private val BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding()
    private val CODE_VERIFIER_PATTERN = Regex("^[A-Za-z0-9._~-]{43,128}$")
    private val CODE_CHALLENGE_PATTERN = Regex("^[A-Za-z0-9_-]{43}$")
}

/** Generates a fresh 256-bit OAuth code verifier for every browser login. */
internal class OAuthCodeVerifierGenerator(
    private val fillRandomBytes: (ByteArray) -> Unit = { target ->
        SECURE_RANDOM.nextBytes(target)
    }
) {

    /** Returns a valid verifier, or null if the secure source or encoding fails. */
    fun generate(): String? {
        val randomBytes = ByteArray(CODE_VERIFIER_BYTES)

        return try {
            fillRandomBytes(randomBytes)
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes)
                .takeIf(OAuthProofKeyPolicy::isValidCodeVerifier)
        } catch (_: Exception) {
            null
        } finally {
            randomBytes.fill(0)
        }
    }

    private companion object {
        /** Process-wide cryptographically strong random source. */
        private val SECURE_RANDOM = SecureRandom()

        /** Thirty-two random bytes produce the RFC 7636 minimum 43-character verifier. */
        private const val CODE_VERIFIER_BYTES = 32
    }
}
