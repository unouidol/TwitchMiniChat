package com.fs.twitchminichat.harness

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        /*
         * A link at the very start of the message, behind a symbol the URL pattern accepts in a
         * host name but which is not a word character. Searched in the message alone, the link
         * starts on the symbol; searched with the "[user] " prefix before it, it would start one
         * character later. The only row whose links depend on where the search starts.
         *
         * FROZEN DEFECT. The linkifier currently includes the leading emoji in the URL
         * (http://👉example.com, the emoji underlined as part of the link). This row freezes that
         * behaviour deliberately, because the oracle preserves today's behaviour, defects included.
         * When the defect is fixed, THIS ROW'S EXPECTED RESULT CHANGES, and that is correct, not
         * a regression.
         */
        ParityRow.Message("msg.link.after-symbol-at-start.frozen-defect", "parity_alice", "👉example.com"),
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

    /**
     * The rows whose text holds emote markers. The parity pass shows them without images, as both of
     * its paths have no loader; the emote passes compare their images.
     */
    val emoteRows: List<String> = rows
        .filterIsInstance<ParityRow.Message>()
        .filter { row -> !row.emotesRaw.isNullOrBlank() }
        .map { row -> row.id }

    /** How many emotes the tag [emotesRaw] places: one per range, counted from the tag alone. */
    fun emoteCount(emotesRaw: String?): Int {
        if (emotesRaw.isNullOrBlank()) return 0
        return emotesRaw.split('/').sumOf { specification -> specification.substringAfter(':').split(',').size }
    }

    /**
     * Builds [row] with ChatFragment's producers into [list], laid out as frames would lay it out.
     *
     * A row's view exists only once the list lays it out, so the list is laid out where a frame
     * comes between two things in production: after the row arrives, after each of an echo's events
     * - each is its own message from Twitch, seconds apart - and after a user is hidden. An echo is
     * therefore built as sending and moved by rebinds, as on the phone; built after all its events
     * it would be bound at once at its last status, the path the direct-bind check compares.
     *
     * [hide] false leaves a hidden row's user shown: the row is the same, and only then has a view.
     */
    fun produce(row: ParityRow, index: Int, production: ProductionRowViews, list: RecyclerView, hide: Boolean = true) {
        val controller = production.newTimeline(list)
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
                production.layOut(list, "produce ${row.id}")
                if (row.hidden && hide) {
                    controller.setHiddenUsers(setOf(row.user.trim().lowercase()))
                    production.layOut(list, "produce ${row.id}, hidden")
                }
            }
            is ParityRow.System -> {
                production.systemLine(row.text)
                production.layOut(list, "produce ${row.id}")
            }
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
                production.layOut(list, "produce ${row.id}")
                row.events.forEach { event ->
                    production.applyEchoEvent(controller, localId, PendingEchoEvent.valueOf(event))
                    production.layOut(list, "produce ${row.id}, $event")
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
