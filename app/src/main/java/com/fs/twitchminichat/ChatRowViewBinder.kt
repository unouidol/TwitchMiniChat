package com.fs.twitchminichat

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import com.fs.twitchminichat.chat.ChatMessageRow
import com.fs.twitchminichat.chat.ChatRowText
import com.fs.twitchminichat.chat.ChatRowViewType
import com.fs.twitchminichat.chat.ChatTimelineChange
import com.fs.twitchminichat.chat.ChatTimelineRow
import com.fs.twitchminichat.chat.PendingEchoRow
import com.fs.twitchminichat.chat.PendingEchoStatus
import com.fs.twitchminichat.chat.RebindableTimelineView
import com.fs.twitchminichat.chat.RowText
import com.fs.twitchminichat.chat.SystemLineRow

/**
 * Builds the view of each row of the chat timeline, and binds a row to a view: the only code that
 * makes a row's view or writes what it shows, its tag included.
 *
 * One view type per row kind, as [ChatRowViewType] names them: a chat message gets the page's
 * message view, a system line a plain TextView, an echo a [PendingEchoView] around a message view.
 * [newView] builds an empty view of a type and [bind] binds a row to a view of its kind; the bind
 * functions are typed by kind, so a row is only ever bound to a view of its own kind. [create] is
 * the two in order, for a row's view built outside the chat list.
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
 *    echo's view holds depends on the statuses it passed through, not only on the one it shows. A
 *    full bind therefore shows the status line as sending first and then the row's own status, so a
 *    view back from the pool holds the text of no row before it. A status that changes while the
 *    echo is shown reaches its view as a rebind; the proof rebuilds an echo the same way. The two
 *    paths agree in pixels for every echo, and in state for all but one: an echo that was unconfirmed
 *    before it was confirmed keeps "unconfirmed" in its hidden line when rebound, and "sending" when
 *    bound at once. The harness's direct-bind check declares that one exception.
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
 * Since 5d the timeline is a RecyclerView (ChatTimelineAdapter), and a view is bound to another row
 * whenever its holder comes back from the recycled pool. That doing so is correct is what the rebind
 * proof shows, run over the adapter's own bind. How the list keeps what this binder needs, each the
 * answer to a constraint 5c wrote down for it:
 *
 * 1. One view type per row kind: [ChatRowViewType] is the only source of view types.
 * 2. [unbind] is called from onViewRecycled. A holder reaches onViewRecycled only on its way to the
 *    recycled pool; one kept in the cache does not, and is handed back for its position without a
 *    bind, which is why a detach keeps the session and only the binding may end it. A removed row's
 *    holder is never cached (RecyclerView 1.3.1, RecyclerView.java:7097-7101), so it reaches the
 *    pool, and its session ends at the layout pass that removes it, not at the call that removed it.
 * 3. An echo's change of status stays a partial bind: the payload [ChatTimelineChange.ECHO_STATUS],
 *    applied by [rebind]. A full bind would end its emote session and load its emotes again.
 * 4. The list has no item animator, so a change never shows two holders for one row: with no
 *    animator an updated holder is rebound in place (RecyclerView.java:5188-5190, :7208-7216).
 * 5. A detach decides whether a row's emotes run: a row scrolled away is detached, and its session
 *    stops its animated emotes until it is attached again. The animation gate keeps only whether
 *    the page is started and current (EmoteAnimationGate).
 * 6. The viewport reporting, its 100 ms sampling and the walk up to the container are gone: the list
 *    attaches exactly the rows it lays out.
 * 7. A hidden row is no view at all: the list holds only the shown rows. (A LinearLayout skips a
 *    GONE child when measuring and laying out, LinearLayout.java:841 and :1695, API 36, so a GONE
 *    row kept the frame it last had; that question ended with the container.)
 * 8. onDestroyView still releases every session (TwitchEmoteImageLoader.releaseAll): holders do not
 *    reach onViewRecycled when the page's view goes. LinearLayoutManager recycles its children on
 *    detach only when recycleChildrenOnDetach is set, false by default (LinearLayoutManager.java:128,
 *    :245-250), and RecyclerView's own detach recycles nothing (RecyclerView.java:3421-3443, :7542-7547).
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
    fun create(row: ChatTimelineRow): View = newView(ChatRowViewType.of(row)).also { view -> bind(view, row) }

    /**
     * Builds an empty view of [viewType], one of [ChatRowViewType]: a message view with the page's
     * actions, a system line's TextView, or an echo's view around its own message view.
     */
    fun newView(viewType: Int): View = when (viewType) {
        ChatRowViewType.MESSAGE -> page.newMessageView().also { view -> page.attachMessageActions(view) }
        ChatRowViewType.SYSTEM_LINE -> TextView(page.context())
        ChatRowViewType.PENDING_ECHO -> PendingEchoView(page.context(), page.newMessageView())
        else -> error(
            "A view of type $viewType was asked for. Invariant: ChatRowViewType is the only source of view " +
                "types, and it has three."
        )
    }

    /** Binds [row] to [view], a view [newView] built for [row]'s kind. */
    fun bind(view: View, row: ChatTimelineRow) {
        when (row) {
            is ChatMessageRow -> bindMessage(view as TextView, row)
            is SystemLineRow -> bindSystemLine(view as TextView, row)
            is PendingEchoRow -> bindEcho(view as PendingEchoView, row)
        }
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
     * Binds [row], an echo at any status, to [view]: its tag, the message on the echo's own message
     * view - which carries no tag and no actions - then the status line, shown as sending first and
     * then at the row's own status (item 5 above).
     *
     * Any status, not only sending. Before 5d an echo was bound only as it entered the timeline, so
     * requiring sending encoded an invariant of the callers there were, not of the echo. In a
     * RecyclerView an echo confirmed in place stays a row, and when its holder comes back from the
     * pool the list binds it at its status of the moment: that requirement would have crashed the app.
     */
    fun bindEcho(view: PendingEchoView, row: PendingEchoRow) {
        view.tag = row
        bindMessageText(view.messageView, row.user, row.messageText, row.emotesRaw, row.replyParentUserLogin)
        val statusTextColor = page.echoStatusTextColor()
        view.bindStatus(PendingEchoStatus.SENDING, statusTextColor)
        if (row.status != PendingEchoStatus.SENDING) view.bindStatus(row.status, statusTextColor)
    }

    /**
     * Ends what [view], a view [newView] built, shows that outlives its text: its message's emote
     * session. Called when the view's holder is recycled: its row left the list, or the holder was
     * evicted from the list's cache.
     */
    fun unbind(view: View) {
        when (view) {
            is PendingEchoView -> page.unbindEmotes(view.messageView)
            is TextView -> page.unbindEmotes(view)
            else -> error(
                "A ${view.javaClass.simpleName} was unbound. Invariant: newView builds three kinds of view - a " +
                    "message view and a system line's, both TextViews, and a PendingEchoView - and the chat " +
                    "list's adapter recycles only views newView built."
            )
        }
    }

    /** Applies what [change] names of [row] to [view] in place, and moves the tag to [row]. */
    fun rebind(view: View, row: ChatTimelineRow, change: ChatTimelineChange) {
        val rebindable = checkNotNull(view as? RebindableTimelineView) {
            "A ${view.javaClass.simpleName} was rebound in place, which it cannot be. Invariant: only an " +
                "echo is rebound in place - ChatTimelineListSync.applyEchoEvent is the only source of " +
                "ECHO_STATUS - and newView builds every echo's view as a PendingEchoView."
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
