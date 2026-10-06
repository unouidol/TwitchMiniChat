package com.fs.twitchminichat.chat

import com.fs.twitchminichat.chat.PendingEchoEvent.NOTICE
import com.fs.twitchminichat.chat.PendingEchoEvent.TIMEOUT
import com.fs.twitchminichat.chat.PendingEchoEvent.USERSTATE
import com.fs.twitchminichat.chat.PendingEchoStatusLine.REJECTED
import com.fs.twitchminichat.chat.PendingEchoStatusLine.SENDING
import com.fs.twitchminichat.chat.PendingEchoStatusLine.UNCONFIRMED
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterizes how a sent message's echo moves and looks, against ChatFragment at
 * 4f90dbd: appendPendingOutgoingMessage, its 10 s timeout, confirmOldestPendingOutgoing-
 * FromUserState and rejectNewestPendingOutgoing, which set the status line and the
 * row's alpha on the views.
 *
 * Each test sends a message, delivers events in order, and asserts what the echo looked
 * like after each one: its status line, or none, and its opacity.
 */
class PendingEchoStatusTest {

    @Test
    fun aSentMessage_showsSending_atSeventyTwoPercent() {
        assertEquals(listOf(look(SENDING, 0.72f)), looksAfter())
    }

    @Test
    fun tenSecondsWithoutAnAnswer_turnTheEchoUnconfirmed() {
        assertEquals(listOf(look(SENDING, 0.72f), look(UNCONFIRMED, 0.62f)), looksAfter(TIMEOUT))
    }

    @Test
    fun aUserStateConfirmation_removesTheStatusLine_andMakesTheEchoOpaque() {
        assertEquals(listOf(look(SENDING, 0.72f), look(null, 1f)), looksAfter(USERSTATE))
    }

    @Test
    fun aNoticeRejection_marksTheEchoRejected_atHalfOpacity() {
        assertEquals(listOf(look(SENDING, 0.72f), look(REJECTED, 0.5f)), looksAfter(NOTICE))
    }

    @Test
    fun anUnconfirmedEcho_canStillBeConfirmed() {
        assertEquals(
            listOf(look(SENDING, 0.72f), look(UNCONFIRMED, 0.62f), look(null, 1f)),
            looksAfter(TIMEOUT, USERSTATE)
        )
    }

    @Test
    fun anUnconfirmedEcho_canStillBeRejected() {
        assertEquals(
            listOf(look(SENDING, 0.72f), look(UNCONFIRMED, 0.62f), look(REJECTED, 0.5f)),
            looksAfter(TIMEOUT, NOTICE)
        )
    }

    @Test
    fun aSecondTimeout_changesNothing() {
        assertEquals(
            listOf(look(SENDING, 0.72f), look(UNCONFIRMED, 0.62f), look(UNCONFIRMED, 0.62f)),
            looksAfter(TIMEOUT, TIMEOUT)
        )
    }

    @Test
    fun aConfirmedEcho_staysConfirmed_whateverFollows() {
        assertEquals(
            List(4) { look(null, 1f) },
            looksAfter(USERSTATE, TIMEOUT, NOTICE, USERSTATE).drop(1)
        )
    }

    @Test
    fun aRejectedEcho_staysRejected_whateverFollows() {
        assertEquals(
            List(4) { look(REJECTED, 0.5f) },
            looksAfter(NOTICE, TIMEOUT, USERSTATE, NOTICE).drop(1)
        )
    }

    @Test
    fun everyStatusAndEvent_leadWhereTheFragmentWent() {
        val expected = mapOf(
            PendingEchoStatus.SENDING to listOf(PendingEchoStatus.UNCONFIRMED, PendingEchoStatus.CONFIRMED, PendingEchoStatus.REJECTED),
            PendingEchoStatus.UNCONFIRMED to listOf(PendingEchoStatus.UNCONFIRMED, PendingEchoStatus.CONFIRMED, PendingEchoStatus.REJECTED),
            PendingEchoStatus.CONFIRMED to listOf(PendingEchoStatus.CONFIRMED, PendingEchoStatus.CONFIRMED, PendingEchoStatus.CONFIRMED),
            PendingEchoStatus.REJECTED to listOf(PendingEchoStatus.REJECTED, PendingEchoStatus.REJECTED, PendingEchoStatus.REJECTED)
        )

        for ((status, next) in expected) {
            assertEquals("$status", next, listOf(TIMEOUT, USERSTATE, NOTICE).map { event -> status.after(event) })
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun look(line: PendingEchoStatusLine?, alpha: Float) = PendingEchoPresentation(line, alpha)

    /** How the echo looked when sent, and after each of [events]. */
    private fun looksAfter(vararg events: PendingEchoEvent): List<PendingEchoPresentation> {
        var status = PendingEchoStatus.SENDING
        val looks = mutableListOf(status.presentation)
        for (event in events) {
            status = status.after(event)
            looks += status.presentation
        }
        return looks
    }
}
