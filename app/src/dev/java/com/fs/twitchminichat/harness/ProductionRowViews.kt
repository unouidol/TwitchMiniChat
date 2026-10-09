package com.fs.twitchminichat.harness

import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.fs.twitchminichat.AccountConfig
import com.fs.twitchminichat.ChatFragment
import com.fs.twitchminichat.ChatTimelineController
import com.fs.twitchminichat.PendingOutgoingChatMessage
import com.fs.twitchminichat.chat.PendingEchoEvent

/**
 * The production side of the parity harness: rows built by ChatFragment's own producers -
 * appendChatLine, appendSystemLine, appendPendingOutgoingMessage - into a real
 * ChatTimelineController over a container laid out like chatContainer.
 *
 * This is the one part of the harness that follows production on purpose. When 5a changes how
 * the producers build rows, this adapter changes with them and the frozen copy does not.
 *
 * The fragment is attached without a view and held at CREATED, so onCreateView, onViewCreated,
 * onStart and onResume never run: no IRC connection, no history request, no account read. The
 * producers reach private members, so they are called by reflection; nothing in src/main was made
 * visible for this. The only fields set are the three the producers need and nothing else
 * provides: the account they write as, the timeline controller, and the scroll view that
 * scrollToBottom posts to. That scroll view is never attached, so what it posts never runs.
 *
 * Read on the way, unchanged: HiddenUsersStore, by appendChatLine's hidden-user check. A catalogue
 * user hidden on the device would produce no row, which the harness reports.
 */
internal class ProductionRowViews(
    activity: FragmentActivity,
    signedInUsername: String
) {

    private val fragment = ChatFragment.newInstance("")

    init {
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, FRAGMENT_TAG)
            .setMaxLifecycle(fragment, Lifecycle.State.CREATED)
            .commitNow()

        setField(
            "cfg",
            AccountConfig(
                id = "parity-harness",
                username = signedInUsername,
                channel = "parity_harness",
                accessToken = "",
                profileId = ""
            )
        )
        setField("scrollChat", ScrollView(activity))
    }

    /** A fresh timeline over [container]; every row produced after this goes into it. */
    fun newTimeline(container: ViewGroup): ChatTimelineController {
        val controller = ChatTimelineController(container)
        setField("chatTimelineController", controller)
        return controller
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
        method(
            "appendChatLine",
            String::class.java, String::class.java, String::class.java, String::class.java,
            String::class.java, java.lang.Boolean.TYPE, java.lang.Double::class.java
        ).invoke(fragment, user, message, emotesRaw, dedupKey, replyParentUserLogin, false, messageTimestampSec)
    }

    /** appendSystemLine. */
    fun systemLine(text: String) {
        method("appendSystemLine", String::class.java).invoke(fragment, text)
    }

    /** appendPendingOutgoingMessage, as the send path calls it. */
    fun pendingEcho(pending: PendingOutgoingChatMessage, replyParentUserLogin: String?) {
        method(
            "appendPendingOutgoingMessage",
            PendingOutgoingChatMessage::class.java, String::class.java
        ).invoke(fragment, pending, replyParentUserLogin)
    }

    /** An echo event through the real timeline, as the page applies it. */
    fun applyEchoEvent(controller: ChatTimelineController, localId: String, event: PendingEchoEvent) {
        controller.applyEchoEvent(localId, event)
    }

    /** A message view as ChatFragment constructs it, before anything sets it up: its theme defaults. */
    fun freshMessageView(): View {
        val type = Class.forName("com.fs.twitchminichat.ChatFragment\$SwipeReplyTextView")
        val constructor = type.getDeclaredConstructor(ChatFragment::class.java, android.content.Context::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(fragment, fragment.requireContext()) as View
    }

    private fun setField(name: String, value: Any) {
        ChatFragment::class.java.getDeclaredField(name).apply { isAccessible = true }.set(fragment, value)
    }

    private fun method(name: String, vararg types: Class<*>) =
        ChatFragment::class.java.getDeclaredMethod(name, *types).apply { isAccessible = true }

    private companion object {
        const val FRAGMENT_TAG = "row-view-parity-harness"
    }
}
