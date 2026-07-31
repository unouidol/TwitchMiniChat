package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for canonical saved-account profile resolution. */
class AccountProfileIdResolverTest {

    /** A backend profile identifier always wins over the legacy username. */
    @Test
    fun explicitBackendProfileIdHasPriority() {
        val account = account(
            username = "Legacy_User",
            profileId = " P_ABC123 "
        )

        assertEquals(
            "p_abc123",
            AccountProfileIdResolver.resolve(account)
        )
    }

    /** Older accounts without a backend profile retain the username fallback. */
    @Test
    fun missingBackendProfileIdUsesLegacyUsername() {
        val account = account(
            username = " Legacy_User ",
            profileId = " "
        )

        assertEquals(
            "legacy_user",
            AccountProfileIdResolver.resolve(account)
        )
    }

    /** An account without either identity produces no profile identifier. */
    @Test
    fun missingBackendProfileAndUsernameProducesBlankId() {
        val account = account(
            username = " ",
            profileId = ""
        )

        assertEquals(
            "",
            AccountProfileIdResolver.resolve(account)
        )
    }

    /** Builds one minimal account fixture for resolver tests. */
    private fun account(
        username: String,
        profileId: String
    ): AccountConfig {
        return AccountConfig(
            id = "account-id",
            username = username,
            channel = "channel",
            accessToken = "token",
            profileId = profileId
        )
    }
}