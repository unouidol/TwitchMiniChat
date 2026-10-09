package com.fs.twitchminichat.harness

import android.view.View
import android.view.ViewGroup
import com.fs.twitchminichat.PendingOutgoingChatMessage
import com.fs.twitchminichat.chat.PendingEchoEvent

/**
 * One row state the parity harness renders through both paths. Fixed data only: nothing here
 * depends on the clock, the network, or the account on the device.
 */
internal sealed interface ParityRow {
    val id: String

    /** A chat message, as live chat or history delivers it; [hidden] hides its user afterwards. */
    data class Message(
        override val id: String,
        val user: String,
        val text: String,
        val emotesRaw: String? = null,
        val reply: String? = null,
        val stableId: Boolean = true,
        val hidden: Boolean = false
    ) : ParityRow

    /** A line the app writes itself. */
    data class System(override val id: String, val text: String) : ParityRow

    /** The signed-in user's pending echo, then [events] in order: TIMEOUT, USERSTATE or NOTICE. */
    data class Echo(
        override val id: String,
        val text: String,
        val reply: String? = null,
        val events: List<String> = emptyList()
    ) : ParityRow
}

/** The rows, and how each path turns one into the views of a container. */
internal object ParityCatalogue {

    /** The account both paths write as. Short on purpose, so that `x@me` reads as in the plan. */
    const val SIGNED_IN = "me"

    private const val LONG_TEXT =
        "This is a deliberately long chat message that has to wrap over several lines at the " +
            "width of a phone chat column, so that the harness compares line breaking, line " +
            "height and the spacing between wrapped lines as well as the first line of text, " +
            "and it keeps going a little further to be sure of at least three lines."

    val rows: List<ParityRow> = listOf(
        ParityRow.Message("msg.plain", "parity_alice", "hello there"),
        ParityRow.Message("msg.mention", "parity_alice", "@me hello"),
        ParityRow.Message("msg.mention.case", "parity_alice", "@ME hello"),
        ParityRow.Message("msg.mention.punctuation", "parity_alice", "hey, @me! are you there?"),
        ParityRow.Message("msg.mention.parentheses", "parity_alice", "(@me) twice @Me"),
        ParityRow.Message("msg.mention.inside-word.none", "parity_alice", "x@me must not highlight"),
        ParityRow.Message("msg.mention.longer-name.none", "parity_alice", "@meow and @me_too are not me"),
        ParityRow.Message("msg.reply", "parity_alice", "sure", reply = "parity_bob"),
        ParityRow.Message("msg.reply.mention", "parity_alice", "@me ok then", reply = "parity_bob"),
        ParityRow.Message("msg.self", SIGNED_IN, "my own line"),
        ParityRow.Message("msg.self.other-case", "ME", "my own line, other case"),
        ParityRow.Message("msg.bot.pcg", "pokemoncommunitygame", "A wild Pidgey appears!"),
        ParityRow.Message("msg.bot.elbierro", "elbierro", "bot colour table"),
        ParityRow.Message("msg.other-user", "parity_zed", "hashed colour"),
        ParityRow.Message("msg.link", "parity_alice", "see https://example.com/page now"),
        ParityRow.Message("msg.link.www", "parity_alice", "go to www.example.org please"),
        ParityRow.Message("msg.link.mention", "parity_alice", "@me look https://example.com/x"),
        ParityRow.Message("msg.emotes", "parity_alice", "Kappa hello Kappa", emotesRaw = "25:0-4,12-16"),
        ParityRow.Message(
            "msg.emotes.reply.mention", "parity_alice", "Kappa @me Kappa",
            emotesRaw = "25:0-4,10-14", reply = "parity_bob"
        ),
        ParityRow.Message("msg.long", "parity_alice", LONG_TEXT),
        ParityRow.Message("msg.empty", "parity_alice", ""),
        ParityRow.Message("msg.no-stable-id", "parity_alice", "no message id", stableId = false),
        ParityRow.Message("msg.hidden", "parity_hidden", "hidden after it arrived", hidden = true),
        ParityRow.System("system.line", "Connected to #parity_harness"),
        ParityRow.Echo("echo.sending", "sending now"),
        ParityRow.Echo("echo.sending-unconfirmed", "no answer yet", events = listOf("TIMEOUT")),
        ParityRow.Echo("echo.sending-confirmed", "confirmed quickly", events = listOf("USERSTATE")),
        ParityRow.Echo("echo.sending-rejected", "rejected quickly", events = listOf("NOTICE")),
        ParityRow.Echo("echo.unconfirmed-confirmed", "confirmed late", events = listOf("TIMEOUT", "USERSTATE")),
        ParityRow.Echo("echo.unconfirmed-rejected", "rejected late", events = listOf("TIMEOUT", "NOTICE")),
        ParityRow.Echo("echo.confirmed-then-notice", "a notice after confirming", events = listOf("USERSTATE", "NOTICE")),
        ParityRow.Echo("echo.mention", "@me note to self"),
        ParityRow.Echo("echo.reply", "answering", reply = "parity_alice")
    )

    /** The rows the 5a-2 rebind proof cannot cover until 5c: their text holds emote markers. */
    val emoteRows: List<String> = rows
        .filterIsInstance<ParityRow.Message>()
        .filter { row -> !row.emotesRaw.isNullOrBlank() }
        .map { row -> row.id }

    /** Builds [row] with ChatFragment's producers into [container]. */
    fun produce(row: ParityRow, index: Int, production: ProductionRowViews, container: ViewGroup) {
        val controller = production.newTimeline(container)
        when (row) {
            is ParityRow.Message -> {
                production.chatMessage(
                    user = row.user,
                    message = row.text,
                    emotesRaw = row.emotesRaw,
                    dedupKey = dedupKey(row),
                    replyParentUserLogin = row.reply,
                    messageTimestampSec = timestampSec(index)
                )
                if (row.hidden) controller.setHiddenUsers(setOf(row.user.trim().lowercase()))
            }
            is ParityRow.System -> production.systemLine(row.text)
            is ParityRow.Echo -> {
                val localId = "parity-${row.id}"
                production.pendingEcho(
                    PendingOutgoingChatMessage(
                        localId = localId,
                        channel = "parity_harness",
                        username = SIGNED_IN,
                        message = row.text,
                        sentAtSec = timestampSec(index)
                    ),
                    row.reply
                )
                row.events.forEach { event ->
                    production.applyEchoEvent(controller, localId, PendingEchoEvent.valueOf(event))
                }
            }
        }
    }

    /** Builds [row] with the frozen copy into [container]. */
    fun freeze(row: ParityRow, index: Int, frozen: FrozenRowViews4804609, container: ViewGroup) {
        when (row) {
            is ParityRow.Message -> {
                val view = frozen.chatMessage(
                    user = row.user,
                    message = row.text,
                    emotesRaw = row.emotesRaw,
                    msgId = messageId(row),
                    replyParentUserLogin = row.reply,
                    messageTimestampSec = timestampSec(index)
                )
                container.addView(view)
                if (row.hidden) view.visibility = View.GONE
            }
            is ParityRow.System -> container.addView(frozen.systemLine(row.text))
            is ParityRow.Echo -> {
                val view = frozen.pendingEcho(
                    localId = "parity-${row.id}",
                    message = row.text,
                    replyParentUserLogin = row.reply,
                    sentAtSec = timestampSec(index)
                )
                container.addView(view)
                row.events.forEach { event ->
                    frozen.applyEchoEvent(view, FrozenRowViews4804609.FrozenEchoEvent.valueOf(event))
                }
            }
        }
    }

    /* The key appendChatLine is given: "id:" with a stable message id, otherwise a history key. */
    private fun dedupKey(row: ParityRow.Message): String =
        messageId(row)?.let { id -> "id:$id" } ?: "hist:parity:${row.id}"

    private fun messageId(row: ParityRow.Message): String? =
        if (row.stableId) "parity-${row.id}" else null

    /* Fixed, and in the past: nothing in a row's look depends on it. */
    private fun timestampSec(index: Int): Double = 1_791_000_000.0 + index
}
