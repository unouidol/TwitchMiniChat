package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reason reported by the fake store when a write is refused. */
private const val FAILED_WRITE_REASON = "test_write_refused"

/** Reason reported by the fake store when the stored list cannot be read. */
private const val UNREADABLE_STORE_REASON = "test_unreadable"

/** Unit tests for account list behaviour, independent of how accounts are stored. */
class AccountRepositoryTest {

    /** In-memory stand-in for the encrypted production store. */
    private class FakeStore(
        private var lookup: AccountJsonLookup = AccountJsonLookup.Missing,
        private val writable: Boolean = true
    ) : AccountJsonStore {

        override fun read(): AccountJsonLookup = lookup

        override fun write(json: String): AccountWriteOutcome {
            if (!writable) return AccountWriteOutcome.Failure(FAILED_WRITE_REASON)
            lookup = AccountJsonLookup.Present(json)
            return AccountWriteOutcome.Success
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
        val repo = AccountRepository(
            FakeStore(AccountJsonLookup.Unavailable(UNREADABLE_STORE_REASON))
        )

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
    /**
     * Store whose reads can be made to fail while its content stays intact.
     *
     * This is the shape of a real transient failure: the encrypted file is still
     * there and still valid, but the Keystore refused to authenticate it once.
     */
    private class FlakyStore(
        initialJson: String? = null
    ) : AccountJsonStore {

        /** When true every read reports the list as unreadable. */
        var readsFail: Boolean = false

        /** Counts every replacement of the stored list. */
        var writeCount: Int = 0
            private set

        private var stored: String? = initialJson

        override fun read(): AccountJsonLookup {
            if (readsFail) {
                return AccountJsonLookup.Unavailable(UNREADABLE_STORE_REASON)
            }

            val current = stored ?: return AccountJsonLookup.Missing
            return AccountJsonLookup.Present(current)
        }

        override fun write(json: String): AccountWriteOutcome {
            stored = json
            writeCount += 1
            return AccountWriteOutcome.Success
        }

        override fun clear(): Boolean {
            stored = null
            return true
        }
    }

    /**
     * A read failure must never shrink the stored account list.
     *
     * Every mutation replaces the file in full, so one that runs while the list
     * cannot be read would write back only the accounts it managed to see. The
     * others would be gone: the file is in no-backup storage by design and the
     * access tokens exist nowhere else, so the user would silently lose every
     * account except the one just added.
     */
    @Test
    fun addAccountWhileStoreIsUnreadable_keepsExistingAccounts() {
        val store = FlakyStore()
        val repo = AccountRepository(store)

        repo.addAccount(account("a"))
        repo.addAccount(account("b"))

        store.readsFail = true
        repo.addAccount(account("c"))
        store.readsFail = false

        assertEquals(listOf("a", "b"), repo.loadAccounts().map { it.id })
    }

    /** A refused addition says so, so the caller can tell the user. */
    @Test
    fun addAccountWhileStoreIsUnreadable_reportsFailure() {
        val store = FlakyStore()
        val repo = AccountRepository(store)

        repo.addAccount(account("a"))
        store.readsFail = true

        assertFalse(repo.addAccount(account("b")))
    }

    /** A refused write is reported the same way as a refused read. */
    @Test
    fun addAccountWhenWriteIsRefused_reportsFailure() {
        val repo = AccountRepository(FakeStore(writable = false))

        assertFalse(repo.addAccount(account("a")))
    }

    /** Replacing the whole list reports whether it reached storage. */
    @Test
    fun saveAllReportsWhetherTheListWasStored() {
        assertTrue(AccountRepository(FakeStore()).saveAll(listOf(account("a"))))
        assertFalse(
            AccountRepository(FakeStore(writable = false)).saveAll(listOf(account("a")))
        )
    }

    /**
     * Content that is not a list blocks mutations exactly like an unreadable file.
     *
     * The bytes decrypted, so the Keystore is healthy and the file is intact: what is
     * damaged is its content. Overwriting it would still discard whatever it holds.
     */
    @Test
    fun unparseableStoredContent_blocksMutationsInsteadOfReplacingThem() {
        val store = FlakyStore(initialJson = "not json")
        val repo = AccountRepository(store)

        val writesBefore = store.writeCount

        assertFalse(repo.addAccount(account("a")))
        assertNull(repo.removeById("a"))
        assertEquals(writesBefore, store.writeCount)
    }

    /** No mutation writes anything while the stored list cannot be read. */
    @Test
    fun mutationsWhileStoreIsUnreadable_neverWrite() {
        val store = FlakyStore()
        val repo = AccountRepository(store)

        repo.addAccount(account("a"))
        repo.addAccount(account("b"))

        val writesBefore = store.writeCount
        store.readsFail = true

        repo.addAccount(account("c"))
        repo.updateChannel("a", "other")
        repo.reorderAccounts(listOf("b", "a"))
        repo.removeById("a")
        repo.updateCredentialsInPlace(
            accountId = "a",
            username = "user",
            accessToken = "token",
            profileId = "profile"
        )

        assertEquals(writesBefore, store.writeCount)
    }
}
