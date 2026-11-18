package com.fs.twitchminichat

import android.content.Context
import android.widget.Toast

/**
 * Tells the user what the single erase achieved, in messages that outlive the restart
 * that follows it.
 *
 * Toasts, because the erase restarts the app at once: a toast is queued and drawn by the
 * system, so it is still shown after the activity that raised it has gone, where a
 * dialog or a snackbar would go with it.
 *
 * One toast per fact, because Android 12 and later show at most two lines of a toast for
 * this app and cut the rest without a sign (#116). The outcome comes first, since it is
 * what the user asked about; the browser failure, when there is one, follows in a toast
 * of its own instead of being appended to the outcome, where it was the part that was
 * cut.
 */
object DeviceEraseNotice {

    /**
     * The messages to show, in the order they are shown.
     *
     * Each one is a string resource. Pure, so the choice and the order are tested
     * without a device.
     *
     * @param webDataCleared whether the built-in browser's website data was cleared.
     */
    fun messages(outcome: DeviceEraseOutcome, webDataCleared: Boolean): List<Int> {
        val outcomeMessage = when (outcome) {
            DeviceEraseOutcome.NOT_REGISTERED -> R.string.erase_device_not_registered
            DeviceEraseOutcome.REMOVED_FROM_SERVER -> R.string.erase_device_removed
            DeviceEraseOutcome.ALERTS_STOPPED -> R.string.erase_device_alerts_stopped
            DeviceEraseOutcome.NOTHING_REACHED -> R.string.erase_device_nothing_reached
        }

        return if (webDataCleared) {
            listOf(outcomeMessage)
        } else {
            listOf(outcomeMessage, R.string.erase_device_web_data_failed)
        }
    }

    /**
     * Queues the toasts for [messages]. Call it before the restart, not after: the
     * toasts are queued here, and the system shows them one after the other, each for
     * as long as [Toast.LENGTH_LONG] lasts.
     */
    fun show(context: Context, outcome: DeviceEraseOutcome, webDataCleared: Boolean) {
        messages(outcome, webDataCleared).forEach { messageRes ->
            Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
        }
    }
}
