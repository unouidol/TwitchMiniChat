package com.fs.twitchminichat.chat

/** What can happen to a sent message before its canonical copy arrives. */
enum class PendingEchoEvent {
    /** Ten seconds passed and Twitch has neither confirmed nor rejected the write. */
    TIMEOUT,

    /** Twitch's USERSTATE confirmed the write. */
    USERSTATE,

    /** A Twitch NOTICE rejected the write. */
    NOTICE
}

/** The line under an echo that says where its sending stands, when there is one. */
enum class PendingEchoStatusLine {
    SENDING,
    UNCONFIRMED,
    REJECTED
}

/** How an echo looks: its status line, or none, and the opacity of the whole row. */
data class PendingEchoPresentation(
    val statusLine: PendingEchoStatusLine?,
    val alpha: Float
)

/**
 * Where the sending of an echoed message stands, and how each event moves it.
 *
 * Which echo an event is about is decided by OutgoingChatMessageTracker, which only
 * offers a USERSTATE or a NOTICE to a write Twitch has not confirmed yet; the page
 * cancels an echo's timeout when Twitch answers. These transitions say the same thing
 * from the echo's side: once confirmed or rejected, an echo stays as it is.
 */
enum class PendingEchoStatus(val presentation: PendingEchoPresentation) {
    /** Just written to the socket. */
    SENDING(PendingEchoPresentation(PendingEchoStatusLine.SENDING, 0.72f)),

    /** Ten seconds without an answer from Twitch. */
    UNCONFIRMED(PendingEchoPresentation(PendingEchoStatusLine.UNCONFIRMED, 0.62f)),

    /** Confirmed by USERSTATE: the echo looks like any other message. */
    CONFIRMED(PendingEchoPresentation(null, 1f)),

    /** Rejected by a NOTICE. */
    REJECTED(PendingEchoPresentation(PendingEchoStatusLine.REJECTED, 0.5f));

    /** The status after [event]. */
    fun after(event: PendingEchoEvent): PendingEchoStatus {
        val awaiting = this == SENDING || this == UNCONFIRMED
        return when (event) {
            PendingEchoEvent.TIMEOUT -> if (this == SENDING) UNCONFIRMED else this
            PendingEchoEvent.USERSTATE -> if (awaiting) CONFIRMED else this
            PendingEchoEvent.NOTICE -> if (awaiting) REJECTED else this
        }
    }
}
