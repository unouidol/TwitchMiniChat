package com.fs.twitchminichat

/** What the erase actually achieved beyond this phone. */
enum class DeviceEraseOutcome {

    /**
     * This installation never registered, so there was nothing on the server to remove.
     *
     * Not a failure, and not a state a later attempt would improve. It takes precedence
     * over every other outcome: an installation the server holds no registration for
     * cannot be receiving alerts through it, so no message about alerts continuing or
     * about a removal still to come could be true of it.
     */
    NOT_REGISTERED,

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
 * The decision that was wrong until 5.5.2's device round found it: an installation that
 * never registered was reported as one whose removal had failed, so the user was told
 * their device could not be removed from the server *now* and would be removed at the next
 * attempt - when there had never been anything to remove and no attempt would find
 * anything.
 *
 * The other decision that is easy to get wrong: a failed token deletion does not matter
 * when the device removal succeeded. The backend no longer holds this device, so it will
 * never try to deliver to that token, and telling the user that alerts may still
 * arrive would be false. The token is still owed a deletion in that case - it is an
 * identifier the user asked to be rid of - but the message must not be downgraded.
 *
 * Pure so every combination can be tested without a device.
 */
object DeviceEraseOutcomePolicy {

    /**
     * Chooses the outcome to report from the steps that ran before the wipe.
     *
     * @param serverNotRegistered the request was never attempted because this
     *   installation has no device credential. First in the order, because the other two
     *   both describe a registration that exists.
     */
    fun decide(
        serverRemovalOk: Boolean,
        serverNotRegistered: Boolean,
        tokenDeletionOk: Boolean
    ): DeviceEraseOutcome {
        return when {
            serverNotRegistered -> DeviceEraseOutcome.NOT_REGISTERED
            serverRemovalOk -> DeviceEraseOutcome.REMOVED_FROM_SERVER
            tokenDeletionOk -> DeviceEraseOutcome.ALERTS_STOPPED
            else -> DeviceEraseOutcome.NOTHING_REACHED
        }
    }

    /**
     * Whether the token deletion has to be recorded as owed and finished later.
     *
     * Owed whenever it did not go through, including after a successful device
     * removal, and including on an installation that never registered: the user asked for
     * everything on this phone to be gone, and a live push token is not nothing. The token
     * and the registration are separate things, which is why the message for
     * [DeviceEraseOutcome.NOT_REGISTERED] says nothing about the token.
     */
    fun tokenDeletionOwed(tokenDeletionOk: Boolean): Boolean = !tokenDeletionOk
}
