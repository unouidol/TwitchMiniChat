package com.fs.twitchminichat.harness

import android.content.Context
import com.fs.twitchminichat.ChatFragment
import com.fs.twitchminichat.PendingOutgoingChatMessage
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/** A member the harness reaches by reflection is not there: the run stops, naming it. */
internal class MissingReflectiveTarget(message: String, cause: Throwable) : Exception(message, cause)

/**
 * Every member the harness reaches by reflection, resolved once, before anything is rendered.
 *
 * The production members are private to ChatFragment and are reached this way so that nothing in
 * src/main had to be made visible. The price is that a rename breaks the harness when it runs, not
 * when it compiles, so [resolve] looks every one of them up first and stops the run with the class
 * and member that is missing. Nothing is skipped and nothing falls back to a default.
 *
 * The frozen classes' two fields are resolved the same way, so both sides are read alike.
 */
internal class ReflectiveTargets private constructor(
    val cfg: Field,
    val scrollChat: Field,
    val chatTimelineController: Field,
    val appendChatLine: Method,
    val appendSystemLine: Method,
    val appendPendingOutgoingMessage: Method,
    val swipeReplyTextView: Class<*>,
    val swipeReplyTextViewConstructor: Constructor<*>,
    val swipeReplyTextViewOnSwipeReply: Field,
    val externalWebLinkSpan: Class<*>,
    val externalWebLinkSpanUrl: Field,
    val frozenSwipeReplyTextView: Class<*>,
    val frozenSwipeReplyTextViewOnSwipeReply: Field,
    val frozenWebLinkSpan: Class<*>,
    val frozenWebLinkSpanUrl: Field
) {

    companion object {

        private const val CHAT_FRAGMENT = "com.fs.twitchminichat.ChatFragment"
        private const val SWIPE_REPLY_TEXT_VIEW = "$CHAT_FRAGMENT\$SwipeReplyTextView"
        private const val EXTERNAL_WEB_LINK_SPAN = "com.fs.twitchminichat.ChatMessageLinkifier\$ExternalWebLinkSpan"
        private const val FROZEN = "com.fs.twitchminichat.harness.FrozenRowViews4804609"
        private const val FROZEN_SWIPE_REPLY_TEXT_VIEW = "$FROZEN\$FrozenSwipeReplyTextView"
        private const val FROZEN_WEB_LINK_SPAN = "$FROZEN\$FrozenWebLinkSpan"

        /** Class, member and signature of every target, for the results file and the PR. */
        val descriptions: List<String> = listOf(
            "$CHAT_FRAGMENT: field cfg: AccountConfig?",
            "$CHAT_FRAGMENT: field scrollChat: ScrollView (lateinit)",
            "$CHAT_FRAGMENT: field chatTimelineController: ChatTimelineController (lateinit)",
            "$CHAT_FRAGMENT: method appendChatLine(String, String, String?, String, String?, boolean, Double?)",
            "$CHAT_FRAGMENT: method appendSystemLine(String)",
            "$CHAT_FRAGMENT: method appendPendingOutgoingMessage(PendingOutgoingChatMessage, String?)",
            "$SWIPE_REPLY_TEXT_VIEW: class",
            "$SWIPE_REPLY_TEXT_VIEW: constructor(ChatFragment, Context)",
            "$SWIPE_REPLY_TEXT_VIEW: field onSwipeReply: (() -> Unit)?",
            "$EXTERNAL_WEB_LINK_SPAN: class",
            "$EXTERNAL_WEB_LINK_SPAN: field url: String",
            "$FROZEN_SWIPE_REPLY_TEXT_VIEW: class (harness's own)",
            "$FROZEN_SWIPE_REPLY_TEXT_VIEW: field onSwipeReply: (() -> Unit)? (harness's own)",
            "$FROZEN_WEB_LINK_SPAN: class (harness's own)",
            "$FROZEN_WEB_LINK_SPAN: field url: String (harness's own)"
        )

        /** Looks up every target; throws [MissingReflectiveTarget] for the first one that is missing. */
        fun resolve(): ReflectiveTargets {
            val fragment = ChatFragment::class.java
            val swipe = type(SWIPE_REPLY_TEXT_VIEW)
            val link = type(EXTERNAL_WEB_LINK_SPAN)
            val frozenSwipe = type(FROZEN_SWIPE_REPLY_TEXT_VIEW)
            val frozenLink = type(FROZEN_WEB_LINK_SPAN)
            return ReflectiveTargets(
                cfg = field(fragment, "cfg"),
                scrollChat = field(fragment, "scrollChat"),
                chatTimelineController = field(fragment, "chatTimelineController"),
                appendChatLine = method(
                    fragment, "appendChatLine",
                    String::class.java, String::class.java, String::class.java, String::class.java,
                    String::class.java, java.lang.Boolean.TYPE, java.lang.Double::class.java
                ),
                appendSystemLine = method(fragment, "appendSystemLine", String::class.java),
                appendPendingOutgoingMessage = method(
                    fragment, "appendPendingOutgoingMessage",
                    PendingOutgoingChatMessage::class.java, String::class.java
                ),
                swipeReplyTextView = swipe,
                swipeReplyTextViewConstructor = constructor(swipe, fragment, Context::class.java),
                swipeReplyTextViewOnSwipeReply = field(swipe, "onSwipeReply"),
                externalWebLinkSpan = link,
                externalWebLinkSpanUrl = field(link, "url"),
                frozenSwipeReplyTextView = frozenSwipe,
                frozenSwipeReplyTextViewOnSwipeReply = field(frozenSwipe, "onSwipeReply"),
                frozenWebLinkSpan = frozenLink,
                frozenWebLinkSpanUrl = field(frozenLink, "url")
            )
        }

        private fun type(name: String): Class<*> = try {
            Class.forName(name)
        } catch (missing: ClassNotFoundException) {
            throw MissingReflectiveTarget("class not found: $name", missing)
        }

        private fun field(owner: Class<*>, name: String): Field = try {
            owner.getDeclaredField(name).apply { isAccessible = true }
        } catch (missing: NoSuchFieldException) {
            throw MissingReflectiveTarget("field not found: ${owner.name}.$name", missing)
        }

        private fun method(owner: Class<*>, name: String, vararg types: Class<*>): Method = try {
            owner.getDeclaredMethod(name, *types).apply { isAccessible = true }
        } catch (missing: NoSuchMethodException) {
            throw MissingReflectiveTarget(
                "method not found: ${owner.name}.$name(${types.joinToString { it.name }})", missing
            )
        }

        private fun constructor(owner: Class<*>, vararg types: Class<*>): Constructor<*> = try {
            owner.getDeclaredConstructor(*types).apply { isAccessible = true }
        } catch (missing: NoSuchMethodException) {
            throw MissingReflectiveTarget(
                "constructor not found: ${owner.name}(${types.joinToString { it.name }})", missing
            )
        }
    }
}
