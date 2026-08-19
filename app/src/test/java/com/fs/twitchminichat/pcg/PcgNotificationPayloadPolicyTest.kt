package com.fs.twitchminichat.pcg

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the explicit delayed-reminder payload contract. */
class PcgNotificationPayloadPolicyTest {

    @Test
    fun disabledReminder_dropsOnlyMarkedReminderPayload() {
        val data = mapOf(
            PcgNotificationPayloadPolicy.REMINDER_KEY to
                PcgNotificationPayloadPolicy.REMINDER_VALUE
        )

        assertFalse(
            PcgNotificationPayloadPolicy.shouldDisplay(
                data = data,
                reminderEnabled = false
            )
        )
    }

    @Test
    fun enabledReminder_displaysMarkedReminderPayload() {
        val data = mapOf(
            PcgNotificationPayloadPolicy.REMINDER_KEY to " 1 "
        )

        assertTrue(
            PcgNotificationPayloadPolicy.shouldDisplay(
                data = data,
                reminderEnabled = true
            )
        )
    }

    @Test
    fun disabledReminder_doesNotDropInitialOrLegacyAlerts() {
        assertTrue(
            PcgNotificationPayloadPolicy.shouldDisplay(
                data = mapOf("reminder" to "0"),
                reminderEnabled = false
            )
        )
        assertTrue(
            PcgNotificationPayloadPolicy.shouldDisplay(
                data = emptyMap(),
                reminderEnabled = false
            )
        )
    }
}
