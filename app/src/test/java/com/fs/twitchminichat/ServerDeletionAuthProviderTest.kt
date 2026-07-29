package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for destructive server-deletion authentication selection. */
class ServerDeletionAuthProviderTest {

    /** A later valid session can authorize when earlier profiles are missing. */
    @Test
    fun selectsFirstUsableBearerSession() {
        val provider = ServerDeletionAuthProvider { profileId ->
            when (profileId) {
                "profile-b" ->
                    BackendSessionLookup.Present(" session-b ")

                else -> BackendSessionLookup.Missing
            }
        }

        assertEquals(
            ServerDeletionAuthDecision.Bearer(
                profileId = "profile-b",
                authorizationHeader = "Bearer session-b"
            ),
            provider.resolve(
                listOf(" PROFILE-A ", "PROFILE-B")
            )
        )
    }

    /** An unreadable candidate does not hide a later usable session. */
    @Test
    fun usableSessionWinsOverEarlierUnavailableSession() {
        val provider = ServerDeletionAuthProvider { profileId ->
            when (profileId) {
                "profile-a" -> BackendSessionLookup.Unavailable

                "profile-b" ->
                    BackendSessionLookup.Present("session-b")

                else -> BackendSessionLookup.Missing
            }
        }

        assertEquals(
            ServerDeletionAuthDecision.Bearer(
                profileId = "profile-b",
                authorizationHeader = "Bearer session-b"
            ),
            provider.resolve(
                listOf("profile-a", "profile-b")
            )
        )
    }

    /** Missing sessions never enable legacy authentication for deletion. */
    @Test
    fun allMissingSessionsReturnSessionMissing() {
        val provider = ServerDeletionAuthProvider {
            BackendSessionLookup.Missing
        }

        assertEquals(
            ServerDeletionAuthDecision.SessionMissing,
            provider.resolve(
                listOf("profile-a", "profile-b")
            )
        )
    }

    /** Corrupt local session state blocks the destructive request. */
    @Test
    fun unavailableSessionBlocksDeletion() {
        val provider = ServerDeletionAuthProvider {
            BackendSessionLookup.Unavailable
        }

        assertEquals(
            ServerDeletionAuthDecision.SessionUnavailable,
            provider.resolve(listOf("profile-a"))
        )
    }
}
