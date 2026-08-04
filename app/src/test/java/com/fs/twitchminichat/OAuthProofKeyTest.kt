package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the application-to-backend S256 proof. */
class OAuthProofKeyTest {

    /** S256 derivation matches the published RFC 7636 example. */
    @Test
    fun s256ChallengeMatchesRfcExample() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            OAuthProofKeyPolicy.deriveS256CodeChallenge(verifier)
        )
    }

    /** Exactly 256 random bits become one valid unpadded Base64 URL verifier. */
    @Test
    fun generatorProducesExpectedVerifierAndErasesSourceBytes() {
        lateinit var generatedBytes: ByteArray
        val generator = OAuthCodeVerifierGenerator { target ->
            generatedBytes = target
            target.indices.forEach { index -> target[index] = index.toByte() }
        }

        val verifier = generator.generate()

        assertEquals(
            "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8",
            verifier
        )
        assertTrue(generatedBytes.all { byte -> byte == 0.toByte() })
    }

    /** Invalid verifier syntax and entropy lengths are rejected before hashing. */
    @Test
    fun invalidVerifierShapesAreRejected() {
        val invalidValues = listOf(
            "",
            "a".repeat(42),
            "a".repeat(129),
            "a".repeat(42) + "=",
            "a".repeat(42) + "+",
            "a".repeat(42) + "/",
            "a".repeat(42) + " "
        )

        invalidValues.forEach { value ->
            assertFalse(OAuthProofKeyPolicy.isValidCodeVerifier(value))
            assertNull(OAuthProofKeyPolicy.deriveS256CodeChallenge(value))
        }
    }

    /** A failing entropy source prevents login instead of producing a fallback secret. */
    @Test
    fun generatorFailureHasNoFallbackVerifier() {
        val generator = OAuthCodeVerifierGenerator {
            throw IllegalStateException("simulated entropy failure")
        }

        assertNull(generator.generate())
    }
}
