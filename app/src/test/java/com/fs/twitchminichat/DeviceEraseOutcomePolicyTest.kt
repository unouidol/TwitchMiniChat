package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterizes what the single erase is allowed to tell the user.
 *
 * The message is the only place the user learns whether alerts have stopped, so a
 * wrong mapping here is a false statement about their data, not a cosmetic defect.
 */
class DeviceEraseOutcomePolicyTest {

    /** Both steps through: the backend no longer holds this device. */
    @Test
    fun `server removal and token deletion report removal`() {
        assertEquals(
            DeviceEraseOutcome.REMOVED_FROM_SERVER,
            DeviceEraseOutcomePolicy.decide(
                serverRemovalOk = true,
                tokenDeletionOk = true
            )
        )
    }

    /**
     * Rejects an implementation that downgrades the message whenever any step failed.
     * With the device already removed, the backend will never try to deliver to that
     * token, so warning that alerts may still arrive would be false.
     */
    @Test
    fun `a failed token deletion does not weaken a successful server removal`() {
        assertEquals(
            DeviceEraseOutcome.REMOVED_FROM_SERVER,
            DeviceEraseOutcomePolicy.decide(
                serverRemovalOk = true,
                tokenDeletionOk = false
            )
        )
    }

    /**
     * Rejects an implementation that reports success whenever the token went, and one
     * that reports total failure whenever the server call did: the truthful statement
     * here is that alerts have stopped although the server was not told.
     */
    @Test
    fun `token deletion alone stops the alerts`() {
        assertEquals(
            DeviceEraseOutcome.ALERTS_STOPPED,
            DeviceEraseOutcomePolicy.decide(
                serverRemovalOk = false,
                tokenDeletionOk = true
            )
        )
    }

    /**
     * Rejects an implementation that claims anything reached the network on a phone
     * that was offline throughout. This is the only case where alerts can still arrive.
     */
    @Test
    fun `neither step reaching the network is reported as such`() {
        assertEquals(
            DeviceEraseOutcome.NOTHING_REACHED,
            DeviceEraseOutcomePolicy.decide(
                serverRemovalOk = false,
                tokenDeletionOk = false
            )
        )
    }

    /**
     * Rejects an implementation that owes nothing once the device was removed. The
     * user asked for everything on this phone to be gone, and a live push token is an
     * identifier that outlives the wipe unless it is deleted.
     */
    @Test
    fun `a failed token deletion is owed even after a successful server removal`() {
        assertTrue(DeviceEraseOutcomePolicy.tokenDeletionOwed(tokenDeletionOk = false))
    }

    /** Rejects an implementation that queues work it has already done. */
    @Test
    fun `a token deletion that went through is not owed`() {
        assertFalse(DeviceEraseOutcomePolicy.tokenDeletionOwed(tokenDeletionOk = true))
    }
}
