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
}
