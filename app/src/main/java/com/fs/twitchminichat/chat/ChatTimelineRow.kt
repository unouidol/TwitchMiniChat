package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition

/**
 * One row of the chat timeline, as data, at its chronological [position].
 *
 * Every view in the timeline has exactly one of these, in the same order, held by
 * [ChatTimeline]. The kinds differ in how much of the row they can describe without
 * its view.
 */
sealed interface ChatTimelineRow {

    /** Where the row sits in the timeline. */
    val position: ChatTimelinePosition
}

/**
 * One chat message, with everything its view was built from.
 *
 * [user], [messageText], [emotesRaw] and [replyParentUserLogin] are what the row's text
 * is rendered from; [messageId] and [messageTimestampSec] are what the long-press and
 * swipe-reply actions report.
 *
 * Rendering also reads state that belongs to the page rather than to the row - the
 * account's own username, which colours its name and marks mentions, the theme, and
 * the emote images - so a row rebuilt later is the same only while those are.
 */
data class ChatMessageRow(
    override val position: ChatTimelinePosition,
    /** The sender as received, with the casing the row shows. */
    val user: String,
    /** The sender trimmed and lowercased, as the hidden-user checks compare it. */
    val usernameLower: String,
    /** The Twitch message identifier, when the message came with a stable one. */
    val messageId: String?,
    val messageText: String,
    /**
     * The message's own timestamp in seconds, as the page received or substituted it.
     *
     * Not always what [position] was ordered by: a value that is not a positive finite
     * number is kept here as it came, while the position falls back to the local time.
     */
    val messageTimestampSec: Double,
    /** The raw Twitch emotes tag, or null when the message carried none. */
    val emotesRaw: String?,
    /** The login of the user being replied to, or null when the message is not a reply. */
    val replyParentUserLogin: String?
) : ChatTimelineRow

/** One line written by the app itself, such as a channel switch, with the [text] it reports. */
data class SystemLineRow(
    override val position: ChatTimelinePosition,
    val text: String
) : ChatTimelineRow

/**
 * A row whose content exists only in its views.
 *
 * Today that is the pending outgoing echo: its status line and its opacity are changed
 * in place on the views as the message is confirmed, left unconfirmed or rejected, and
 * nothing describes them as data yet. The timeline still needs its place in the order.
 */
data class ViewOnlyRow(
    override val position: ChatTimelinePosition
) : ChatTimelineRow

/**
 * The local echo of a message this account sent, shown at once while Twitch has not
 * confirmed it, with everything its view was built from and where its sending stands.
 *
 * The echo is found by [localId], the identifier OutgoingChatMessageTracker gave the
 * write. Only [status] changes during its life, and a change of status rebinds the
 * existing view instead of building a new one: the message part, with its emotes, is
 * never rebuilt. The echo ends when the canonical message replaces it, or with the
 * timeline.
 */
data class PendingEchoRow(
    override val position: ChatTimelinePosition,
    /** The write's identifier in OutgoingChatMessageTracker. */
    val localId: String,
    /** This account's username, as the row shows it. */
    val user: String,
    /** The message as the tracker recorded it. */
    val messageText: String,
    /** The emotes tag built from this account's emote catalog when the message was sent. */
    val emotesRaw: String?,
    /** The login of the user being replied to, or null when the message is not a reply. */
    val replyParentUserLogin: String?,
    /** When the write left, in seconds; what [position] was ordered by. */
    val sentAtSec: Double,
    val status: PendingEchoStatus
) : ChatTimelineRow
