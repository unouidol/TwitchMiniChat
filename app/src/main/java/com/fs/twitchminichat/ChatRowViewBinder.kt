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
 * the view outside its text can leak, which is why this list has six entries and not fifty:
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
 * 6. The emote session. The loader keeps a message's emote loads and images in a session held by
 *    the view, outside the text the applier replaces, and an image delivered to the session of the
 *    row before would land in a buffer the view no longer shows. So every bind of a message ends the
 *    view's session before anything loads, whether or not the new row has emotes, and [unbind] ends
 *    it when the row leaves. Watched by the proof's session check, which only the record of the loads
 *    can make because no pixel shows it, and mutant O1.
 *
 * The tag, which the long-press and swipe actions read when they fire, is written by every bind and
 * every rebind, in the same call that writes what the view shows. Watched by the proof and mutant T1.
 *
 * Production still binds each view once, when its row is inserted; nothing binds a view to a
 * second row yet. That doing so is correct is what the rebind proof shows, and 5d, which turns it
 * on, must run that proof again rather than inherit this result.
 *
 * Constraints for 5d, which will put the timeline in a RecyclerView and rebind views as they
 * scroll. Each is true of this code now and becomes false, or starts to matter, once rows recycle:
 *
 * 1. Keep one view type per row kind. The flags a movement method forces on a message view -
 *    focusable, clickable, long-clickable - are set by its first bind only, because
 *    TextView.setMovementMethod does nothing when given the movement method the view already has
 *    (TextView.java:2808, API 36), so a view type shared across kinds carries them from a message
 *    into a system line without anything failing.
 * 2. Call [unbind] from onViewRecycled. A holder reaches onViewRecycled only on its way to the
 *    recycled pool; one kept in the cache does not, and can be handed back for its position without
 *    a bind, which is why a detach keeps the session and only the binding may end it.
 * 3. An echo's change of status must stay a partial bind, a payload applied by [rebind]. A full bind
 *    of the echo would end its emote session and load its emotes again on every status change.
 * 4. A change animation can show two holders for one row at once, the old and the new, so one row
 *    would hold two sessions while it runs. Bind changes with payloads, or turn change animations
 *    off, rather than let a change rebuild the row.
 * 5. The animation gate assumes a row that scrolled away stays attached (EmoteAnimationGate). That is
 *    true while the timeline is a ScrollView and false in a RecyclerView, where a detach already
 *    stops a row's emotes; 5d decides whether the detach or the viewport rules.
 * 6. The gate's viewport is reported from the ScrollView's offset (reportEmoteAnimationViewport),
 *    sampled at most every 100 ms after a scroll or a layout (scheduleEmoteAnimationReconcile), and
 *    a row's place in it is found by walking the row's parents up to chatContainer
 *    (timelineSpanOf). None of the three exists in a RecyclerView, which reports positions itself.
 * 7. A hidden row is GONE but stays attached, so a detach never pauses it. Whether the gate counts a
 *    GONE row as visible depends on the position and height a GONE child keeps in its LinearLayout,
 *    which has NOT been read. In a list fed only the shown rows, a hidden row is no view at all.
 * 8. onDestroyView releases every session (TwitchEmoteImageLoader.releaseAll) because no removal
 *    reaches the views that go with the page. Whether every holder of a RecyclerView reaches
 *    onViewRecycled when the page's view is destroyed has NOT been read, so keep the call until it
 *    has.
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
         * Gives [view] the emotes of [row], whose text the applier has just set from [text], at
         * [textSizePx], the text size the applier set: ends the session of the row the view showed
         * before, then loads [row]'s emotes, if it has any. Only after the applier: the loader
         * installs its own copy of the text, which a later setText would replace.
         */
        fun bindEmotes(view: TextView, text: SpannableStringBuilder, row: RowText, textSizePx: Float)

        /** Ends the emote session of [view], whose row is leaving: what it loaded is cleared. */
        fun unbindEmotes(view: TextView)
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

    /**
     * Ends what [view], a view [create] built, shows that outlives its text: its message's emote
     * session. Called when the view's row leaves the timeline, and in 5d when its holder is recycled.
     */
    fun unbind(view: View) {
        when (view) {
            is PendingEchoView -> page.unbindEmotes(view.messageView)
            is TextView -> page.unbindEmotes(view)
            else -> error(
                "A ${view.javaClass.simpleName} was unbound. Invariant: create builds three kinds of view - a " +
                    "message view and a system line's, both TextViews, and a PendingEchoView - and " +
                    "ChatTimelineController is the only writer of the container."
            )
        }
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

    /*
     * A message's text as ChatRowText decides it, put on [view] by the applier, then its emotes, read
     * at the text size the applier has just set. Every bind gives the view its emotes, none included,
     * so that the session of the row it showed before ends here.
     */
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
        val textSizePx = view.textSize
        page.bindEmotes(view, text, row, textSizePx)
    }
}
