package com.fs.twitchminichat.pcg

/**
 * Interprets non-sensitive PCG routing metadata from a Firebase data payload.
 *
 * The live backend marks the delayed message with `reminder=1` and the initial
 * spawn alert with `reminder=0`.
 */
object PcgNotificationPayloadPolicy {

    const val REMINDER_KEY = "reminder"
    const val REMINDER_VALUE = "1"

    /** Returns true only for the explicit reminder marker used by the live. */
    fun isSpawnReminder(data: Map<String, String>): Boolean {
        return data[REMINDER_KEY]
            ?.trim()
            ?.equals(REMINDER_VALUE, ignoreCase = true) == true
    }

    /** Applies the local reminder preference without affecting initial alerts. */
    fun shouldDisplay(
        data: Map<String, String>,
        reminderEnabled: Boolean
    ): Boolean {
        return reminderEnabled || !isSpawnReminder(data)
    }
}
