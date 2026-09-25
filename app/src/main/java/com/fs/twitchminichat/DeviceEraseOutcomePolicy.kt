package com.fs.twitchminichat

/** What the erase actually achieved beyond this phone. */
enum class DeviceEraseOutcome {

    /**
     * The backend confirmed that this device is gone, so nothing will be sent to it.
     */
    REMOVED_FROM_SERVER,

    /**
     * The backend was not reached, but the Firebase Cloud Messaging token is gone, so
     * no notification can be delivered and the backend drops the registration the next
     * time it tries to send to it.
     */
    ALERTS_STOPPED,

    /**
     * Neither step reached the network. The phone is erased, alerts can still arrive,
     * and the owed token deletion is what will eventually stop them.
     */
    NOTHING_REACHED
}

/**
 * Turns the two server-facing steps of the erase into one outcome and one owed record.
 *
 * The decision that is easy to get wrong: a failed token deletion does not matter when
 * the device removal succeeded. The backend no longer holds this device, so it will
 * never try to deliver to that token, and telling the user that alerts may still
 * arrive would be false. The token is still owed a deletion in that case - it is an
 * identifier the user asked to be rid of - but the message must not be downgraded.
 *
 * Pure so every combination can be tested without a device.
 */
object DeviceEraseOutcomePolicy {

    /** Chooses the outcome to report from the two steps that ran before the wipe. */
    fun decide(
        serverRemovalOk: Boolean,
        tokenDeletionOk: Boolean
    ): DeviceEraseOutcome {
        return when {
            serverRemovalOk -> DeviceEraseOutcome.REMOVED_FROM_SERVER
            tokenDeletionOk -> DeviceEraseOutcome.ALERTS_STOPPED
            else -> DeviceEraseOutcome.NOTHING_REACHED
        }
    }

    /**
     * Whether the token deletion has to be recorded as owed and finished later.
     *
     * Owed whenever it did not go through, including after a successful device
     * removal: the user asked for everything on this phone to be gone, and a live push
     * token is not nothing.
     */
    fun tokenDeletionOwed(tokenDeletionOk: Boolean): Boolean = !tokenDeletionOk
}
