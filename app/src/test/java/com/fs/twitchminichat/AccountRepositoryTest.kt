package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for account list behaviour, independent of how accounts are stored. */
class AccountRepositoryTest {

    /** In-memory stand-in for the encrypted production store. */
    private class FakeStore(
        private var lookup: AccountJsonLookup = AccountJsonLookup.Missing,
        private val writable: Boolean = true
    ) : AccountJsonStore {

        override fun read(): AccountJsonLookup = lookup

        override fun write(json: String): Boolean {
            if (!writable) return false
            lookup = AccountJsonLookup.Present(json)
            return true
        }

        override fun clear(): Boolean {
            lookup = AccountJsonLookup.Missing
            return true
        }
    }

    private fun account(
        id: String,
        username: String = "user-$id",
        channel: String = "chan-$id",
        accessToken: String = "token-$id",
        profileId: String = "profile-$id"
    ) = AccountConfig(
        id = id,
        username = username,
        channel = channel,
        accessToken = accessToken,
        profileId = profileId
    )

    /** A fresh installation reports no accounts. */
    @Test
    fun emptyStore_hasNoAccounts() {
        assertTrue(AccountRepository(FakeStore()).loadAccounts().isEmpty())
    }

    /** Added accounts are stored and returned in insertion order. */
    @Test
    fun addedAccountsAreReadBackInOrder() {
        val repo = AccountRepository(FakeStore())

        repo.addAccount(account("a"))
        repo.addAccount(account("b"))

        assertEquals(listOf("a", "b"), repo.loadAccounts().map { it.id })
        assertEquals("token-a", repo.getById("a")?.accessToken)
    }

    /**
     * An unreadable store reports no accounts.
     *
     * The application must ask the user to sign in again rather than continue with
     * credentials it could not authenticate.
     */
    @Test
    fun unavailableStore_reportsNoAccounts() {
        val repo = AccountRepository(FakeStore(AccountJsonLookup.Unavailable))

        assertTrue(repo.loadAccounts().isEmpty())
    }

    /** Entries without an identity or a credential are ignored instead of crashing. */
    @Test
    fun malformedEntriesAreSkipped() {
        val stored = """
            [
              {"id":"a","username":"user","channel":"chan","accessToken":"token","profileId":"p"},
              {"id":"","username":"user","channel":"chan","accessToken":"token","profileId":"p"},
              {"id":"c","username":"user","channel":"chan","accessToken":"","profileId":"p"},
              {"id":"d"}
            ]
        """.trimIndent()

        val repo = AccountRepository(FakeStore(AccountJsonLookup.Present(stored)))

        assertEquals(listOf("a"), repo.loadAccounts().map { it.id })
    }

    /** Unparseable content is treated as an empty list. */
    @Test
    fun unparseableContent_reportsNoAccounts() {
        val repo = AccountRepository(FakeStore(AccountJsonLookup.Present("not json")))

        assertTrue(repo.loadAccounts().isEmpty())
    }

    /** Removing an account returns it and takes it out of the list. */
    @Test
    fun removeByIdReturnsRemovedAccount() {
        val repo = AccountRepository(FakeStore())
        repo.addAccount(account("a"))
        repo.addAccount(account("b"))

        assertEquals("a", repo.removeById("a")?.id)
        assertEquals(listOf("b"), repo.loadAccounts().map { it.id })
        assertNull(repo.removeById("missing"))
    }

    /** Re-authorization replaces credentials without changing local identity. */
    @Test
    fun updateCredentialsKeepsIdentityAndPosition() {
        val repo = AccountRepository(FakeStore())
        repo.addAccount(account("a"))
        repo.addAccount(account("b"))

        assertTrue(
            repo.updateCredentialsInPlace(
                accountId = "a",
                username = "renamed",
                accessToken = "fresh-token",
                profileId = "profile-new"
            )
        )

        val updated = repo.loadAccounts().first()
        assertEquals("a", updated.id)
        assertEquals("renamed", updated.username)
        assertEquals("fresh-token", updated.accessToken)
        assertEquals("chan-a", updated.channel)
        assertEquals(listOf("a", "b"), repo.loadAccounts().map { it.id })
    }

    /** A blank profile identifier never erases the one already stored. */
    @Test
    fun updateCredentialsKeepsExistingProfileWhenBlank() {
        val repo = AccountRepository(FakeStore())
        repo.addAccount(account("a"))

        assertTrue(
            repo.updateCredentialsInPlace(
                accountId = "a",
                username = "user-a",
                accessToken = "fresh-token",
                profileId = "   "
            )
        )

        assertEquals("profile-a", repo.loadAccounts().first().profileId)
    }

    /** Updating an account that no longer exists reports failure. */
    @Test
    fun updateCredentialsOnMissingAccountFails() {
        val repo = AccountRepository(FakeStore())

        assertFalse(
            repo.updateCredentialsInPlace(
                accountId = "missing",
                username = "user",
                accessToken = "token",
                profileId = "profile"
            )
        )
    }

    /** Reordering follows the requested order and keeps unlisted accounts at the end. */
    @Test
    fun reorderAccountsAppendsUnlistedAccounts() {
        val repo = AccountRepository(FakeStore())
        repo.addAccount(account("a"))
        repo.addAccount(account("b"))
        repo.addAccount(account("c"))

        repo.reorderAccounts(listOf("c", "a"))

        assertEquals(listOf("c", "a", "b"), repo.loadAccounts().map { it.id })
    }

    /** Channels are normalized and an empty channel is ignored. */
    @Test
    fun updateChannelNormalizesAndRejectsBlank() {
        val repo = AccountRepository(FakeStore())
        repo.addAccount(account("a"))

        repo.updateChannel("a", "  #NewChannel  ")
        assertEquals("NewChannel", repo.loadAccounts().first().channel)

        repo.updateChannel("a", "   ")
        assertEquals("NewChannel", repo.loadAccounts().first().channel)
    }
}
