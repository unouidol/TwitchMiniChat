package com.fs.twitchminichat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for safe in-place OAuth account matching. */
class OAuthReauthorizationIdentityPolicyTest {

    /** Matching profile identifiers allow a username case or display change. */
    @Test
    fun matchingProfileIdIsAuthoritative() {
        assertTrue(
            OAuthReauthorizationIdentityPolicy.matches(
                expectedUsername = "old_name",
                expectedProfileId = "profile-123",
                actualUsername = "New_Name",
                actualProfileId = "PROFILE-123"
            )
        )
    }

    /** Different profile identifiers must never overwrite the local account. */
    @Test
    fun mismatchingProfileIdIsRejected() {
        assertFalse(
            OAuthReauthorizationIdentityPolicy.matches(
                expectedUsername = "unouidol",
                expectedProfileId = "profile-a",
                actualUsername = "unouidol",
                actualProfileId = "profile-b"
            )
        )
    }

    /** Username matching supports legacy accounts that have no profile identifier. */
    @Test
    fun usernameFallbackIsCaseInsensitive() {
        assertTrue(
            OAuthReauthorizationIdentityPolicy.matches(
                expectedUsername = "Unouidol",
                expectedProfileId = "",
                actualUsername = "unouidol",
                actualProfileId = ""
            )
        )
    }

    /** Blank or different fallback identities are rejected. */
    @Test
    fun invalidUsernameFallbackIsRejected() {
        assertFalse(
            OAuthReauthorizationIdentityPolicy.matches(
                expectedUsername = "unouidol",
                expectedProfileId = "",
                actualUsername = "another_user",
                actualProfileId = ""
            )
        )
        assertFalse(
            OAuthReauthorizationIdentityPolicy.matches(
                expectedUsername = "",
                expectedProfileId = "",
                actualUsername = "",
                actualProfileId = ""
            )
        )
    }
}
