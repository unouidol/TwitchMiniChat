package com.fs.twitchminichat

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import com.fs.twitchminichat.chat.ChatMessageRow
import com.fs.twitchminichat.chat.ChatRowText
import com.fs.twitchminichat.chat.ChatTimelineChange
import com.fs.twitchminichat.chat.ChatTimelineRow
import com.fs.twitchminichat.chat.PendingEchoRow
import com.fs.twitchminichat.chat.PendingEchoStatus
import com.fs.twitchminichat.chat.RebindableTimelineView
import com.fs.twitchminichat.chat.RowText
import com.fs.twitchminichat.chat.SystemLineRow

/**
 * Builds the view of each row of the chat timeline from the row's data, and binds a row to a view:
 * the only code that makes a row's view or writes what it shows, its tag included.
 *
 * One view type per row kind: a chat message gets the page's message view, a system line a plain
 * TextView, an echo a [PendingEchoView] around a message view. The bind functions are typed by
 * kind, so a row is only ever bound to a view of its own kind, and [create] picks the kind from the
 * row.
 *
 * Binding a view that already showed another row leaves nothing of that row behind. Everything
 * carried inside the text resets by construction: a message view has a movement method, and with
 * one TextView.setText copies the text it is given into a new Spannable, so the spans of the row
 * before - mention highlights, link spans, the name's colour - go with its text. Only what lives on
 * the view outside its text can leak, which is why this list has five entries and not fifty:
 *
 * 1. The mention row's background. The applier applies a look to every row, the plain look as no
 *    background. Watched by the parity harness's rebind proof, which mutant A01 (the plain look
 *    skipped) fails on every pair of a mention followed by a plain row.
 * 2. The mention row's padding. Removing a background does not restore padding, so the plain look
 *    sets the padding to zero itself. Watched by the same pairs and the same mutant.
 * 3. The echo's message view. It is given to the [PendingEchoView] when that is built, and a bind
 *    puts the message on it, so a second bind cannot add a second message view as setMessage did.
 *    Watched by the proof's echo pairs and mutant L3.
 * 4. The swipe target, made state of the bind by building views from rows: the closure the page
 *    used to set once per view is now turned on or off by every bind, from the row's message id.
 *    Watched by the pairs into and out of a message without one, and mutant S1.
 * 5. The echo's hidden status text. Hiding the status line leaves its text in place, so what an
 *    echo's view holds depends on the statuses it passed through, not only on the one it shows. An
 *    echo is therefore only ever bound as sending - [bindEcho] requires it - and every later status
 *    reaches its view as a rebind, as in production; the proof rebuilds an echo the same way.
 *
 * The tag, which the long-press and swipe actions read when they fire, is written by every bind and
 * every rebind, in the same call that writes what the view shows. Watched by the proof and mutant T1.
 *
 * Constraint for 5d, which will rebind views as they scroll: keep one view type per row kind. The
 * flags a movement method forces on a message view - focusable, clickable, long-clickable - are set
 * by its first bind only, because TextView.setMovementMethod does nothing when given the movement
 * method the view already has (TextView.java:2808, API 36). After that they are a property of the
 * view's history, not of the bind, and a view type shared across row kinds would carry them from a
 * message into a system line without anything failing.
 *
 * In 5a-2 production binds each view once, when its row is inserted; nothing binds a view to a
 * second row yet. That doing so is correct is what the rebind proof shows, and 5d, which turns it
 * on, must run that proof again rather than inherit this result.
 *
 * Emote sessions stay outside the list until 5c. TwitchEmoteImageLoader keeps each message's
 * emotes in a session that lasts until the view is detached, so a second bind of a view with emotes
 * would leave the first session running. Nothing binds such a view twice before 5c gives the loader
 * an image source the proof can drive.
 */
internal class ChatRowViewBinder(
    private val applier: ChatRowTextApplier,
    private val page: Page
) {

    /** What the binder needs from the page that owns the views. Main thread. */
    interface Page {
        /** The context the views are built in. */
        fun context(): Context

        /** A message view as the page constructs it, with nothing bound to it yet. */
        fun newMessageView(): TextView

        /**
         * Gives a chat message's view the page's long-press and click listeners. They are the
         * page's own, the same for every message, and read the message from the view's tag when
         * they fire.
         */
        fun attachMessageActions(view: TextView)

        /**
         * Turns swipe-to-reply on [view], a view from [newMessageView], on or off. When on, a
         * completed swipe replies to the message the view's tag holds at that moment.
         */
        fun setSwipeReply(view: TextView, enabled: Boolean)

        /** The signed-in username, which colours its own name and marks mentions; read at each bind. */
        fun signedInUsername(): String?

        /** The text of a line the app writes, as the page formats it. */
        fun systemLineText(text: String): String

        /** The colour of an echo's status line. */
        fun echoStatusTextColor(): Int

        /**
         * Loads the emotes of [row] into [view], whose text the applier has just set from [text].
         * Only after the applier: the loader installs its own copy of the text, which a later
         * setText would replace, and reads the render size from the text size the applier set.
         */
        fun loadEmotes(view: TextView, text: SpannableStringBuilder, row: RowText)
    }

    /** Builds the view of [row], of the type its kind takes, bound to it. */
    fun create(row: ChatTimelineRow): View = when (row) {
        is ChatMessageRow -> page.newMessageView().also { view ->
            page.attachMessageActions(view)
            bindMessage(view, row)
        }
        is SystemLineRow -> TextView(page.context()).also { view -> bindSystemLine(view, row) }
        is PendingEchoRow -> PendingEchoView(page.context(), page.newMessageView()).also { view -> bindEcho(view, row) }
    }

    /** Binds [row] to [view], a message view: its tag, its swipe target, then its text and emotes. */
    fun bindMessage(view: TextView, row: ChatMessageRow) {
        view.tag = row
        page.setSwipeReply(view, enabled = !row.messageId.isNullOrBlank())
        bindMessageText(view, row.user, row.messageText, row.emotesRaw, row.replyParentUserLogin)
    }

    /** Binds [row] to [view], a system line's view. */
    fun bindSystemLine(view: TextView, row: SystemLineRow) {
        view.tag = row
        applier.apply(view, ChatRowText.systemLine(page.systemLineText(row.text)))
    }

    /**
     * Binds [row], an echo that is sending, to [view]: its tag, the message on the echo's own message
     * view - which carries no tag and no actions - then the status line.
     */
    fun bindEcho(view: PendingEchoView, row: PendingEchoRow) {
        check(row.status == PendingEchoStatus.SENDING) {
            "An echo was bound as ${row.status}. Invariant: an echo is bound as it enters the timeline, " +
                "from appendPendingOutgoingMessage, which builds it SENDING, and every later status reaches " +
                "its view as an ECHO_STATUS rebind."
        }
        view.tag = row
        bindMessageText(view.messageView, row.user, row.messageText, row.emotesRaw, row.replyParentUserLogin)
        view.bindStatus(row.status, page.echoStatusTextColor())
    }

    /** Applies what [change] names of [row] to [view] in place, and moves the tag to [row]. */
    fun rebind(view: View, row: ChatTimelineRow, change: ChatTimelineChange) {
        val rebindable = checkNotNull(view as? RebindableTimelineView) {
            "A ${view.javaClass.simpleName} was rebound in place, which it cannot be. Invariant: only an " +
                "echo is rebound in place - ChatTimelineViewSync.applyEchoEvent is the only caller - and " +
                "create builds every echo's view as a PendingEchoView."
        }
        view.tag = row
        rebindable.rebind(row, change)
    }

    /* A message's text as ChatRowText decides it, put on [view] by the applier, then its emotes. */
    private fun bindMessageText(
        view: TextView,
        user: String,
        message: String,
        emotesRaw: String?,
        replyParentUserLogin: String?
    ) {
        val row = ChatRowText.message(
            user = user,
            rawMessage = message,
            emotesRaw = emotesRaw,
            replyParentUserLogin = replyParentUserLogin,
            signedInUsername = page.signedInUsername()
        )
        val text = applier.apply(view, row)
        if (row.emoteMarkers.isNotEmpty()) page.loadEmotes(view, text, row)
    }
}
