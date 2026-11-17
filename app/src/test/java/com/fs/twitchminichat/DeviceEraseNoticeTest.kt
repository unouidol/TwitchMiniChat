package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterizes which toasts the single erase shows, and in what order (#116).
 *
 * Android 12 and later show at most two lines of a toast for this app, so the browser
 * failure has a toast of its own instead of a clause appended to the outcome, where it
 * was the part that was cut.
 */
class DeviceEraseNoticeTest {

    /** Each outcome has its own message, and a cleared browser adds nothing to it. */
    @Test
    fun `a cleared browser shows the outcome alone`() {
        val expected = mapOf(
            DeviceEraseOutcome.NOT_REGISTERED to R.string.erase_device_not_registered,
            DeviceEraseOutcome.REMOVED_FROM_SERVER to R.string.erase_device_removed,
            DeviceEraseOutcome.ALERTS_STOPPED to R.string.erase_device_alerts_stopped,
            DeviceEraseOutcome.NOTHING_REACHED to R.string.erase_device_nothing_reached
        )

        for (outcome in DeviceEraseOutcome.entries) {
            assertEquals(
                outcome.name,
                listOf(expected.getValue(outcome)),
                DeviceEraseNotice.messages(outcome, webDataCleared = true)
            )
        }
    }

    /**
     * The browser failure is a second message, after the outcome's: the outcome is what
     * the user asked about, so it must not wait behind the failure.
     */
    @Test
    fun `a failed browser clear adds a second message after the outcome`() {
        for (outcome in DeviceEraseOutcome.entries) {
            val messages = DeviceEraseNotice.messages(outcome, webDataCleared = false)

            assertEquals(outcome.name, 2, messages.size)
            assertEquals(
                outcome.name,
                DeviceEraseNotice.messages(outcome, webDataCleared = true).single(),
                messages[0]
            )
            assertEquals(outcome.name, R.string.erase_device_web_data_failed, messages[1])
        }
    }
}
