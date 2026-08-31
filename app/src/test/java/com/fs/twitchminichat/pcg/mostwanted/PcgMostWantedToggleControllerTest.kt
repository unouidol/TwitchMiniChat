package com.fs.twitchminichat.pcg.mostwanted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for user-confirmed Most Wanted enabled-state synchronization. */
class PcgMostWantedToggleControllerTest {

    /** Synchronizes the complete requested state before persisting the flag. */
    @Test
    fun setEnabled_syncsBeforePersistingLocalState() {
        val operations = mutableListOf<String>()
        val initialState = state(enabled = false)
        val controller = PcgMostWantedToggleController(
            stateReader = {
                operations += "read"
                Result.success(initialState)
            },
            enabledPersister = { _, enabled ->
                operations += "persist:$enabled"
            },
            stateSynchronizer = { _, requestedState ->
                operations += "sync:${requestedState.enabled}"
                assertEquals(
                    initialState.selectedDisplayNames,
                    requestedState.selectedDisplayNames
                )
                PcgMostWantedSyncResult(ok = true, statusCode = 200)
            }
        )

        val result = controller.setEnabled(
            profileId = "unouidol",
            enabled = true
        )

        assertTrue(result.ok)
        assertEquals(true, result.effectiveEnabled)
        assertNull(result.error)
        assertEquals(
            listOf("read", "sync:true", "persist:true"),
            operations
        )
    }

    /** Keeps the previous local state when the backend rejects the request. */
    @Test
    fun setEnabled_serverFailureDoesNotPersistRequestedState() {
        var persistCalls = 0
        val controller = PcgMostWantedToggleController(
            stateReader = { Result.success(state(enabled = false)) },
            enabledPersister = { _, _ -> persistCalls += 1 },
            stateSynchronizer = { _, _ ->
                PcgMostWantedSyncResult(
                    ok = false,
                    statusCode = 503,
                    error = PcgMostWantedSyncError.SERVER_REJECTED
                )
            }
        )

        val result = controller.setEnabled(
            profileId = "unouidol",
            enabled = true
        )

        assertFalse(result.ok)
        assertEquals(false, result.effectiveEnabled)
        assertEquals(
            PcgMostWantedToggleError.SERVER_SYNC_FAILED,
            result.error
        )
        assertEquals(0, persistCalls)
    }

    /** Stops before network access when the complete local state cannot load. */
    @Test
    fun setEnabled_localReadFailureSkipsSyncAndPersistence() {
        var syncCalls = 0
        var persistCalls = 0
        val controller = PcgMostWantedToggleController(
            stateReader = {
                Result.failure(IllegalStateException("catalog unavailable"))
            },
            enabledPersister = { _, _ -> persistCalls += 1 },
            stateSynchronizer = { _, _ ->
                syncCalls += 1
                PcgMostWantedSyncResult(ok = true, statusCode = 200)
            }
        )

        val result = controller.setEnabled(
            profileId = "unouidol",
            enabled = true
        )

        assertFalse(result.ok)
        assertNull(result.effectiveEnabled)
        assertEquals(
            PcgMostWantedToggleError.LOCAL_STATE_UNAVAILABLE,
            result.error
        )
        assertEquals(0, syncCalls)
        assertEquals(0, persistCalls)
    }

    /** Reports a rare local failure after a server-confirmed state change. */
    @Test
    fun setEnabled_localPersistenceFailureIsVisible() {
        val controller = PcgMostWantedToggleController(
            stateReader = { Result.success(state(enabled = false)) },
            enabledPersister = { _, _ ->
                throw IllegalStateException("preferences unavailable")
            },
            stateSynchronizer = { _, _ ->
                PcgMostWantedSyncResult(ok = true, statusCode = 200)
            }
        )

        val result = controller.setEnabled(
            profileId = "unouidol",
            enabled = true
        )

        assertFalse(result.ok)
        assertEquals(false, result.effectiveEnabled)
        assertEquals(
            PcgMostWantedToggleError.LOCAL_PERSISTENCE_FAILED,
            result.error
        )
    }

    /** Builds a representative profile watchlist state. */
    private fun state(enabled: Boolean): PcgMostWantedState {
        return PcgMostWantedState(
            enabled = enabled,
            selectedDisplayNames = linkedSetOf(
                "Pal Wooper",
                "Pikachu"
            )
        )
    }
}
