package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for backend Authorization header selection. */
class BackendAuthHeaderProviderTest {

    /** Null or blank session values never create an Authorization header. */
    @Test
    fun nullOrBlankSessionProducesNoAuthorizationHeader() {
        assertNull(BackendAuthHeaderProvider.bearerHeader(null))
        assertNull(BackendAuthHeaderProvider.bearerHeader(""))
        assertNull(BackendAuthHeaderProvider.bearerHeader("   "))
    }

    /** A stored session is formatted as one Bearer Authorization header. */
    @Test
    fun presentSessionProducesBearerAuthorizationHeader() {
        val provider = BackendAuthHeaderProvider(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Present("backend-session")
            }
        )

        assertEquals(
            BackendSessionAuthDecision.Bearer("Bearer backend-session"),
            provider.resolve("profile-a")
        )
    }

    /** A missing session keeps temporary legacy compatibility available. */
    @Test
    fun missingSessionSelectsLegacyAuthentication() {
        val provider = BackendAuthHeaderProvider(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Missing
            }
        )

        assertEquals(
            BackendSessionAuthDecision.Legacy,
            provider.resolve("profile-a")
        )
    }

    /** Untrusted local session state never falls back to legacy authentication. */
    @Test
    fun unavailableSessionBlocksBackendRequest() {
        val provider = BackendAuthHeaderProvider(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Unavailable
            }
        )

        assertEquals(
            BackendSessionAuthDecision.Unavailable,
            provider.resolve("profile-a")
        )
    }
}
