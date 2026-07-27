package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Unit tests for isolated, atomically persisted backend sessions. */
class BackendSessionStoreTest {

    /** Temporary app-private directory used as the no-backup store root. */
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** Profile identifiers are trimmed and normalized with locale-independent casing. */
    @Test
    fun profileIdIsNormalized() {
        val store = newStore()

        assertTrue(store.putSession("  Profile-A  ", "session-a"))
        assertEquals(
            BackendSessionLookup.Present("session-a"),
            store.lookup("PROFILE-A")
        )
    }

    /** Multiple profiles retain independent backend sessions. */
    @Test
    fun multipleProfilesKeepSeparateSessions() {
        val store = newStore()

        assertTrue(store.putSession("profile-a", "session-a"))
        assertTrue(store.putSession("profile-b", "session-b"))

        assertEquals(
            BackendSessionLookup.Present("session-a"),
            store.lookup("profile-a")
        )
        assertEquals(
            BackendSessionLookup.Present("session-b"),
            store.lookup("profile-b")
        )
    }

    /** Removing one profile does not remove another profile's session. */
    @Test
    fun removingOneProfilePreservesOtherSessions() {
        val store = newStore()
        assertTrue(store.putSession("profile-a", "session-a"))
        assertTrue(store.putSession("profile-b", "session-b"))

        assertTrue(store.removeProfile("profile-a"))

        assertEquals(BackendSessionLookup.Missing, store.lookup("profile-a"))
        assertEquals(
            BackendSessionLookup.Present("session-b"),
            store.lookup("profile-b")
        )
    }

    /** Complete local deletion removes every profile session. */
    @Test
    fun clearAllRemovesEverySession() {
        val store = newStore()
        assertTrue(store.putSession("profile-a", "session-a"))
        assertTrue(store.putSession("profile-b", "session-b"))

        assertTrue(store.clearAll())

        assertEquals(BackendSessionLookup.Missing, store.lookup("profile-a"))
        assertEquals(BackendSessionLookup.Missing, store.lookup("profile-b"))
    }

    /** A malformed file is rejected completely and never interpreted as a missing session. */
    @Test
    fun malformedSessionFileFailsClosed() {
        val directory = temporaryFolder.newFolder("malformed-store")
        val store = BackendSessionStore(directory)
        assertTrue(store.putSession("profile-a", "session-a"))

        val sessionFile = requireNotNull(directory.listFiles()).single()
        sessionFile.writeText("not-a-complete-session")

        assertEquals(
            BackendSessionLookup.Unavailable,
            store.lookup("profile-a")
        )
    }

    /** Creates a fresh file-backed store for one test. */
    private fun newStore(): BackendSessionStore {
        return BackendSessionStore(temporaryFolder.newFolder())
    }
}
