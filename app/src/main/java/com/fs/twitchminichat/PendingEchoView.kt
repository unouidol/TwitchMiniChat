package com.fs.twitchminichat

import android.annotation.SuppressLint
import android.content.Context
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
 * The view of a pending outgoing echo: [messageView], the echo's message, and a status line under it.
 *
 * The message view is given when this view is built and is the only one it ever holds, so binding
 * the echo again puts the message on that same view instead of adding a second one.
 *
 * A change of status is applied in place by [rebind]: it sets the status line and the
 * opacity, and nothing else. The message view - and the emotes it loaded - is never
 * touched again, which is why this view exists instead of the echo being rebuilt.
 */
/*
 * Never inflated from a layout, so the constructors layout tools call are not needed: only code
 * builds this view, and always with the message view it shows.
 */
@SuppressLint("ViewConstructor")
class PendingEchoView(
    context: Context,
    /** The echo's message, above the status line. */
    val messageView: TextView
) : LinearLayout(context), RebindableTimelineView {

    private val statusView = TextView(context).apply {
        textSize = 10f
    }

    init {
        orientation = VERTICAL
        addView(statusView)
        addView(messageView, 0)
        showStatus(PendingEchoStatus.SENDING)
    }

    /** Shows [status] on the status line, in [statusTextColor]: the status part of binding the echo. */
    fun bindStatus(status: PendingEchoStatus, statusTextColor: Int) {
        statusView.setTextColor(statusTextColor)
        showStatus(status)
    }

    override fun rebind(row: ChatTimelineRow, change: ChatTimelineChange) {
        when (change) {
            ChatTimelineChange.ECHO_STATUS -> {
                val echo = checkNotNull(row as? PendingEchoRow) {
                    "An echo's status was rebound with a row that is not an echo: ${row.javaClass.simpleName}. " +
                        "Invariant: ChatTimelineListSync.applyEchoEvent is the only source of ECHO_STATUS, and it " +
                        "passes the PendingEchoRow it moved."
                }
                showStatus(echo.status)
            }
        }
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
