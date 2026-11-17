package com.fs.twitchminichat.harness

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.fs.twitchminichat.AccountConfig
import com.fs.twitchminichat.AnimatedEmoteRowListener
import com.fs.twitchminichat.ChatFragment
import com.fs.twitchminichat.ChatRowViewBinder
import com.fs.twitchminichat.ChatTimelineController
import com.fs.twitchminichat.EmoteImageSource
import com.fs.twitchminichat.PendingOutgoingChatMessage
import com.fs.twitchminichat.R
import com.fs.twitchminichat.TwitchEmoteImageLoader
import com.fs.twitchminichat.chat.PendingEchoEvent
import java.util.IdentityHashMap

/**
 * The production side of the parity harness: rows built by ChatFragment's own producers -
 * appendChatLine, appendSystemLine, appendPendingOutgoingMessage - into a real
 * ChatTimelineController over a list laid out like chatList.
 *
 * This is the one part of the harness that follows production on purpose. When production changes
 * how the producers build rows, this adapter changes with them and the frozen copy does not.
 *
 * The fragment is attached without a view and held at CREATED, so onCreateView, onViewCreated,
 * onStart and onResume never run: no IRC connection, no history request, no account read. The
 * producers reach private members, so they are called by reflection, through [ReflectiveTargets]
 * resolved before anything runs; nothing in src/main was made visible for this. The only fields set
 * are the three the producers need and nothing else provides: the account they write as, the
 * timeline controller, and the list it fills, which is also where scrollToBottom posts. A list that
 * is never attached never runs what is posted to it; the passes that need the scroll attach theirs.
 * The one field read is the fragment's own row view binder, which every timeline here builds its
 * views with, as the page's own timeline does.
 *
 * Since 5d a row's view exists only once its list lays it out, so every list here is laid out by
 * [layOut], at the harness's width [widthPx], before anything reads its children; and after every
 * such layout [ChatListChecks] checks the list against its timeline.
 *
 * The emote loader is the fourth field, and only the emote passes set it. onViewCreated never runs
 * here, so a fragment has no loader until [installEmoteLoader] gives it one that draws from the
 * harness's own image source, which is the one difference between this page and the real one.
 *
 * Read on the way, unchanged: HiddenUsersStore, by appendChatLine's hidden-user check. A catalogue
 * user hidden on the device would produce no row, which the harness reports.
 */
internal class ProductionRowViews(
    private val activity: FragmentActivity,
    signedInUsername: String,
    private val targets: ReflectiveTargets,
    private val widthPx: Int
) {

    private val fragment = ChatFragment.newInstance("")

    /* Each list's controller, for the checks after its layouts. */
    private val controllers = IdentityHashMap<RecyclerView, ChatTimelineController>()

    init {
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, "$FRAGMENT_TAG-${instances++}")
            .setMaxLifecycle(fragment, Lifecycle.State.CREATED)
            .commitNow()

        targets.cfg.set(
            fragment,
            AccountConfig(
                id = "parity-harness",
                username = signedInUsername,
                channel = "parity_harness",
                accessToken = "",
                profileId = ""
            )
        )
    }

    /** The fragment's binder: what builds and binds every row's view on this page. */
    val binder: ChatRowViewBinder
        get() = targets.rowViewBinder.get(fragment) as ChatRowViewBinder

    /** The fragment's emote loader: null unless [installEmoteLoader] gave it one. */
    val emoteLoader: TwitchEmoteImageLoader?
        get() = targets.emoteImageLoader.get(fragment) as TwitchEmoteImageLoader?

    /** Whether the page follows new rows, as its scroll listener and scrollToBottom leave it. */
    var stickToBottom: Boolean
        get() = targets.stickToBottom.getBoolean(fragment)
        set(value) = targets.stickToBottom.setBoolean(fragment, value)

    /** How many rows the page counts as unseen. */
    val unseenMessages: Int
        get() = targets.unseenMessages.getInt(fragment)

    /**
     * Gives the fragment an emote loader drawing from [images], where onViewCreated would give it one
     * drawing from Glide, reporting its animated rows to [rows].
     */
    fun installEmoteLoader(images: EmoteImageSource, rows: AnimatedEmoteRowListener): TwitchEmoteImageLoader {
        check(emoteLoader == null) { "this fragment already has an emote loader" }
        val loader = EmoteOwnership.newLoader(images, rows)
        targets.emoteImageLoader.set(fragment, loader)
        return loader
    }

    /** A list with chatList's attributes, which wraps its rows: one for a timeline to fill. */
    fun newList(): RecyclerView =
        activity.layoutInflater.inflate(R.layout.parity_harness_list, FrameLayout(activity), false) as RecyclerView

    /**
     * A fresh timeline over [list], with the fragment's binder: every row produced after this goes
     * into it, and the list becomes the page's chatList.
     */
    fun newTimeline(list: RecyclerView): ChatTimelineController {
        val controller = ChatTimelineController(list, binder)
        targets.chatTimelineController.set(fragment, controller)
        targets.chatList.set(fragment, list)
        controllers[list] = controller
        return controller
    }

    /**
     * Measures and lays out [list] at the harness's width, as a frame would: the rows it lays out
     * get their views now, bound through the adapter. With no [heightPx] every row is laid out; with
     * one, the list is that tall and lays out what fits. Then checks the list; [where] names the step.
     */
    fun layOut(list: RecyclerView, where: String, heightPx: Int? = null) {
        list.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            if (heightPx == null) {
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            } else {
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            }
        )
        list.layout(list.left, list.top, list.left + list.measuredWidth, list.top + list.measuredHeight)
        checkList(list, where)
    }

    /** Checks [list] against its timeline as it stands, after a layout or a scroll; [where] names the step. */
    fun checkList(list: RecyclerView, where: String) {
        ChatListChecks.check(where, list, checkNotNull(controllers[list]) { "a list no timeline of this page fills" }, targets)
    }

    /** Registers the page's own scroll listener on [list], as onViewCreated registers it on chatList. */
    fun followScrolls(list: RecyclerView) {
        list.addOnScrollListener(targets.chatScrollListener.get(fragment) as RecyclerView.OnScrollListener)
    }

    /** The page's onDestroyView, as the fragment manager calls it when the page's view goes. */
    fun destroyView() {
        fragment.onDestroyView()
    }

    /** appendChatLine, as live chat and history call it. */
    fun chatMessage(
        user: String,
        message: String,
        emotesRaw: String?,
        dedupKey: String,
        replyParentUserLogin: String?,
        messageTimestampSec: Double
    ) {
        targets.appendChatLine.invoke(
            fragment, user, message, emotesRaw, dedupKey, replyParentUserLogin, false, messageTimestampSec
        )
    }

    /** appendSystemLine. */
    fun systemLine(text: String) {
        targets.appendSystemLine.invoke(fragment, text)
    }

    /** appendPendingOutgoingMessage, as the send path calls it. */
    fun pendingEcho(pending: PendingOutgoingChatMessage, replyParentUserLogin: String?) {
        targets.appendPendingOutgoingMessage.invoke(fragment, pending, replyParentUserLogin)
    }

    /** An echo event through the real timeline, as the page applies it. */
    fun applyEchoEvent(controller: ChatTimelineController, localId: String, event: PendingEchoEvent) {
        controller.applyEchoEvent(localId, event)
    }

    /** A message view as ChatFragment constructs it, before anything sets it up: its theme defaults. */
    fun freshMessageView(): View = freshMessageView(fragment.requireContext())

    /** A message view as ChatFragment constructs it, in [context] instead of the page's. */
    fun freshMessageView(context: Context): TextView =
        targets.swipeReplyTextViewConstructor.newInstance(fragment, context) as TextView

    private companion object {
        const val FRAGMENT_TAG = "row-view-parity-harness"

        /* Each instance is its own fragment, with its own deduplicator and its own state. */
        var instances = 0
    }
}
