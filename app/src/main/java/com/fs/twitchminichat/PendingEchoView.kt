package com.fs.twitchminichat

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fs.twitchminichat.chat.ChatTimelineChange
import com.fs.twitchminichat.chat.ChatTimelineRow
import com.fs.twitchminichat.chat.PendingEchoRow
import com.fs.twitchminichat.chat.PendingEchoStatus
import com.fs.twitchminichat.chat.PendingEchoStatusLine
import com.fs.twitchminichat.chat.RebindableTimelineView

/**
 * The view of a pending outgoing echo: the message, built once, and a status line under it.
 *
 * A change of status is applied in place by [rebind]: it sets the status line and the
 * opacity, and nothing else. The message view - and the emotes it loaded - is never
 * touched again, which is why this view exists instead of the echo being rebuilt.
 */
class PendingEchoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs), RebindableTimelineView {

    private val statusView = TextView(context).apply {
        textSize = 10f
    }

    init {
        orientation = VERTICAL
        addView(statusView)
        showStatus(PendingEchoStatus.SENDING)
    }

    /** Puts [messageView], the echo's message as it was built, above the status line. Called once. */
    fun setMessage(messageView: View, statusTextColor: Int) {
        addView(messageView, 0)
        statusView.setTextColor(statusTextColor)
    }

    override fun rebind(row: ChatTimelineRow, change: ChatTimelineChange) {
        if (change != ChatTimelineChange.ECHO_STATUS) return
        val echo = row as? PendingEchoRow ?: return
        showStatus(echo.status)
    }

    private fun showStatus(status: PendingEchoStatus) {
        val presentation = status.presentation
        val line = presentation.statusLine

        if (line == null) {
            statusView.visibility = View.GONE
        } else {
            statusView.text = context.getString(textOf(line))
            statusView.visibility = View.VISIBLE
        }
        alpha = presentation.alpha
    }

    private fun textOf(line: PendingEchoStatusLine): Int {
        return when (line) {
            PendingEchoStatusLine.SENDING -> R.string.chat_send_pending
            PendingEchoStatusLine.UNCONFIRMED -> R.string.chat_send_unconfirmed
            PendingEchoStatusLine.REJECTED -> R.string.chat_send_rejected
        }
    }
}
