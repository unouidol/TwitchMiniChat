package com.fs.twitchminichat.harness

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.util.LinkifyCompat
import com.fs.twitchminichat.ChatTimelinePosition
import com.fs.twitchminichat.R
import com.fs.twitchminichat.chat.ChatMessageRow
import com.fs.twitchminichat.chat.PendingEchoRow
import com.fs.twitchminichat.chat.PendingEchoStatus
import com.fs.twitchminichat.chat.SystemLineRow

/**
 * FROZEN COPY of the chat row views as main-v5 built them at
 * 4804609de4ade00c75beff6618548f069b2e085d. NEVER "keep it in sync" with production.
 *
 * This is the reference the 5a parity harness compares production against. A copy that follows
 * production can no longer see a change in it, which is how the 4850a83 oracle came to run #75's
 * search instead of its own (#77). If production changes how a row looks, this file stays as it is
 * and the harness reports the difference; that report is the point.
 *
 * Copied, from that commit:
 * - ChatFragment.createMessageTextView, applyMentionHighlightSpans, applyMentionRowHighlight, the
 *   colour helpers, colorForUsername with its bot table, dp and normalizeChatUser;
 * - the listeners appendChatLine sets, and attachSwipeReplyAction;
 * - the view appendSystemLine builds, and the one appendPendingOutgoingMessage builds;
 * - SwipeReplyTextView's construction, PendingEchoView, PendingEchoStatus's looks and transitions;
 * - ChatMessageLinkifier and TwitchEmoteMessageFormatter;
 * - the tag ContainerTimelineViews sets before adding a view.
 *
 * It calls no production logic: only Android, AndroidX (LinkifyCompat, called as production calls
 * it) and production data - R resources, and the row data classes, built here only so that the tag
 * can be compared. Left out, on purpose, because no harness draw or state read can exercise them:
 * SwipeReplyTextView's touch handling and performClick; the long-press, click and link actions,
 * kept as listeners of the same kind that do nothing; and the emote loader, absent from both paths.
 */
internal class FrozenRowViews4804609(
    private val context: Context,
    private val signedInUsername: String
) {

    private val botcolors = mapOf(
        "elbierro" to 0xFFFFD700.toInt(),
        "pokemoncommunitygame" to 0xFFFF5555.toInt()
    )

    private val userColorCache = HashMap<String, Int>()

    // ---------------------------------------------------------------------------------
    // The three row kinds, as their producers built them
    // ---------------------------------------------------------------------------------

    /** appendChatLine: the message view, its listeners and its swipe target, tagged with its row. */
    fun chatMessage(
        user: String,
        message: String,
        emotesRaw: String?,
        msgId: String?,
        replyParentUserLogin: String?,
        messageTimestampSec: Double
    ): View {
        val tv = createMessageTextView(
            user = user,
            rawMessage = message,
            emotesRaw = emotesRaw,
            replyParentUserLogin = replyParentUserLogin
        )

        tv.setOnLongClickListener {
            true
        }

        tv.setOnClickListener {
        }

        attachSwipeReplyAction(
            tv = tv,
            messageId = msgId
        )

        tv.tag = ChatMessageRow(
            position = UNPLACED,
            user = user,
            usernameLower = normalizeChatUser(user),
            messageId = msgId,
            messageText = message,
            messageTimestampSec = messageTimestampSec,
            emotesRaw = emotesRaw,
            replyParentUserLogin = replyParentUserLogin
        )
        return tv
    }

    /** appendSystemLine's view, tagged with its row. */
    fun systemLine(text: String): View {
        val tv = TextView(context)
        tv.text = context.getString(R.string.system_bullet, text)
        tv.setTextColor(colorOnSurfaceVariant())
        tv.textSize = 12f

        tv.tag = SystemLineRow(position = UNPLACED, text = text)
        return tv
    }

    /** appendPendingOutgoingMessage's view, tagged with its row, sending. */
    fun pendingEcho(
        localId: String,
        message: String,
        replyParentUserLogin: String?,
        sentAtSec: Double
    ): FrozenPendingEchoView {
        val user = signedInUsername
        val emotesRaw: String? = null

        val messageView = createMessageTextView(
            user = user,
            rawMessage = message,
            emotesRaw = emotesRaw,
            replyParentUserLogin = replyParentUserLogin
        )

        val echoView = FrozenPendingEchoView(context).apply {
            setMessage(messageView, statusTextColor = colorOnSurfaceVariant())
        }

        echoView.tag = PendingEchoRow(
            position = UNPLACED,
            localId = localId,
            user = user,
            messageText = message,
            emotesRaw = emotesRaw,
            replyParentUserLogin = replyParentUserLogin,
            sentAtSec = sentAtSec,
            status = PendingEchoStatus.SENDING
        )
        return echoView
    }

    /**
     * An event on a pending echo, as ChatTimelineViewSync.applyEchoEvent handled it: when the status
     * changes, the tag follows the row and the view shows the new status; otherwise nothing happens.
     */
    fun applyEchoEvent(view: FrozenPendingEchoView, event: FrozenEchoEvent) {
        val echo = view.tag as PendingEchoRow
        val status = FrozenEchoStatus.of(echo.status)
        val next = status.after(event)
        if (next == status) return

        view.tag = echo.copy(status = next.asRowStatus())
        view.showStatus(next)
    }

    // ---------------------------------------------------------------------------------
    // ChatFragment.createMessageTextView and what it called
    // ---------------------------------------------------------------------------------

    private fun createMessageTextView(
        user: String,
        rawMessage: String,
        emotesRaw: String?,
        replyParentUserLogin: String?
    ): FrozenSwipeReplyTextView {
        val tv = FrozenSwipeReplyTextView(context).apply {
            setTextColor(colorOnSurface())
            textSize = 14f
            linksClickable = true
            movementMethod =
                android.text.method.LinkMovementMethod.getInstance()
        }

        val emoteLayout = formatEmotes(
            rawMessage = rawMessage,
            emotesRaw = emotesRaw
        )
        val message = emoteLayout.text

        val lowerUser = user.lowercase()
        val lowerSelf = signedInUsername.lowercase()
        val nameColor = if (lowerUser == lowerSelf) colorPrimarySafe() else colorForUsername(user)

        val replyHeader = replyParentUserLogin
            ?.takeIf { it.isNotBlank() }
            ?.let { "↪ replying to @$it\n" }
            .orEmpty()

        val prefix = "[$user] "
        val fullPrefix = replyHeader + prefix

        if (emoteLayout.markers.isEmpty()) {
            val plain = SpannableStringBuilder(fullPrefix + message)

            if (replyHeader.isNotEmpty()) {
                plain.setSpan(
                    ForegroundColorSpan(colorOnSurfaceVariant()),
                    0,
                    replyHeader.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val open = plain.indexOf("[", replyHeader.length)
            val close = plain.indexOf("]", replyHeader.length)
            if (open != -1 && close > open) {
                plain.setSpan(
                    ForegroundColorSpan(nameColor),
                    open + 1,
                    close,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val hasMention = applyMentionHighlightSpans(plain)
            applyMentionRowHighlight(tv, hasMention)
            addWebLinks(
                text = plain,
                messageStartIndex = fullPrefix.length
            ) {
            }

            tv.text = plain
            return tv
        }

        val builder = SpannableStringBuilder(fullPrefix + message)

        if (replyHeader.isNotEmpty()) {
            builder.setSpan(
                ForegroundColorSpan(colorOnSurfaceVariant()),
                0,
                replyHeader.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val open = builder.indexOf("[", replyHeader.length)
        val close = builder.indexOf("]", replyHeader.length)
        if (open != -1 && close > open) {
            builder.setSpan(
                ForegroundColorSpan(nameColor),
                open + 1,
                close,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val hasMention = applyMentionHighlightSpans(builder)
        applyMentionRowHighlight(tv, hasMention)
        addWebLinks(
            text = builder,
            messageStartIndex = fullPrefix.length
        ) {
        }

        tv.text = builder

        /* Production started the emote loads here; the harness runs both paths without a loader. */

        return tv
    }

    private fun attachSwipeReplyAction(
        tv: FrozenSwipeReplyTextView,
        messageId: String?
    ) {
        tv.onSwipeReply = messageId
            ?.takeIf { it.isNotBlank() }
            ?.let {
                { }
            }
    }

    private fun applyMentionHighlightSpans(text: SpannableStringBuilder): Boolean {
        val username = signedInUsername.trim()
        if (username.isBlank()) return false

        val regex = Regex(
            pattern = "(?i)(?<![A-Za-z0-9_])@${Regex.escape(username)}(?![A-Za-z0-9_])"
        )

        val highlightColor = colorMentionHighlight()
        var found = false

        regex.findAll(text.toString()).forEach { match ->
            found = true

            text.setSpan(
                BackgroundColorSpan(highlightColor),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            text.setSpan(
                StyleSpan(Typeface.BOLD),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return found
    }

    private fun applyMentionRowHighlight(tv: TextView, hasMention: Boolean) {
        if (!hasMention) return

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(colorMentionRowHighlight())
        }

        tv.background = bg
        tv.setPaddingRelative(
            dp(8),
            dp(6),
            dp(8),
            dp(6)
        )
    }

    private fun colorForUsername(user: String): Int {
        val key = user.lowercase()
        botcolors[key]?.let { return it }

        return userColorCache.getOrPut(key) {
            val h = (key.hashCode() and 0x7fffffff) % 360
            Color.HSVToColor(floatArrayOf(h.toFloat(), 0.75f, 0.95f))
        }
    }

    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val typedValue = TypedValue()
        val ok = context.theme.resolveAttribute(attr, typedValue, true)
        if (!ok) return fallback

        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(context, typedValue.resourceId)
        } else {
            typedValue.data
        }
    }

    private fun colorOnSurface(): Int {
        return resolveThemeColor(android.R.attr.textColorPrimary, Color.WHITE)
    }

    private fun colorOnSurfaceVariant(): Int {
        return resolveThemeColor(android.R.attr.textColorSecondary, 0xFFAAAAAA.toInt())
    }

    private fun colorPrimarySafe(): Int {
        return resolveThemeColor(android.R.attr.textColorLink, 0xFF00FFAA.toInt())
    }

    private fun colorMentionHighlight(): Int {
        val base = resolveThemeColor(
            android.R.attr.textColorHighlight,
            0x66FFD54F
        )

        return Color.argb(
            110,
            Color.red(base),
            Color.green(base),
            Color.blue(base)
        )
    }

    private fun colorMentionRowHighlight(): Int {
        val base = resolveThemeColor(
            android.R.attr.textColorHighlight,
            0x66FFD54F
        )

        return Color.argb(
            54,
            Color.red(base),
            Color.green(base),
            Color.blue(base)
        )
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private fun normalizeChatUser(user: String?): String {
        return user?.trim()?.lowercase().orEmpty()
    }

    // ---------------------------------------------------------------------------------
    // ChatMessageLinkifier
    // ---------------------------------------------------------------------------------

    private fun addWebLinks(
        text: Spannable,
        messageStartIndex: Int,
        onLinkClick: (String) -> Unit
    ) {
        if (messageStartIndex < 0 || messageStartIndex >= text.length) {
            return
        }

        val messageText = SpannableString(
            text.subSequence(messageStartIndex, text.length)
        )

        LinkifyCompat.addLinks(
            messageText,
            Linkify.WEB_URLS
        )

        messageText
            .getSpans(
                0,
                messageText.length,
                URLSpan::class.java
            )
            .forEach { urlSpan ->
                val start = messageText.getSpanStart(urlSpan)
                val end = messageText.getSpanEnd(urlSpan)

                if (start < 0 || end <= start) {
                    return@forEach
                }

                text.setSpan(
                    FrozenWebLinkSpan(
                        url = urlSpan.url,
                        onLinkClick = onLinkClick
                    ),
                    messageStartIndex + start,
                    messageStartIndex + end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
    }

    /* ChatMessageLinkifier.ExternalWebLinkSpan; the harness reads url by its field name. */
    private class FrozenWebLinkSpan(
        private val url: String,
        private val onLinkClick: (String) -> Unit
    ) : ClickableSpan() {

        override fun onClick(widget: View) {
            onLinkClick(url)
        }
    }

    // ---------------------------------------------------------------------------------
    // TwitchEmoteMessageFormatter
    // ---------------------------------------------------------------------------------

    private class FrozenEmoteMarker(val markerIndex: Int)

    private class FrozenEmoteLayout(val text: String, val markers: List<FrozenEmoteMarker>)

    private class FrozenEmoteRange(val emoteId: String, val start: Int, val endInclusive: Int)

    private fun formatEmotes(
        rawMessage: String,
        emotesRaw: String?
    ): FrozenEmoteLayout {
        val ranges = parseRanges(emotesRaw)
        val normalizedMessage = normalizeMessage(
            rawMessage = rawMessage,
            hasEmotes = ranges.isNotEmpty()
        )

        if (ranges.isEmpty()) {
            return FrozenEmoteLayout(
                text = normalizedMessage,
                markers = emptyList()
            )
        }

        val validRanges = mutableListOf<FrozenEmoteRange>()
        var previousEndInclusive = -1

        for (range in ranges) {
            val endExclusive = range.endInclusive + 1
            val validRange = range.start in normalizedMessage.indices &&
                    endExclusive in (range.start + 1)..normalizedMessage.length &&
                    range.start > previousEndInclusive

            if (!validRange) continue
            validRanges += range
            previousEndInclusive = range.endInclusive
        }

        if (validRanges.isEmpty()) {
            return FrozenEmoteLayout(
                text = normalizedMessage,
                markers = emptyList()
            )
        }

        val renderedText = StringBuilder(normalizedMessage)

        for (range in validRanges.asReversed()) {
            val endExclusive = range.endInclusive + 1
            renderedText.replace(range.start, endExclusive, EMOTE_MARKER.toString())
        }

        var removedCharacterCount = 0
        val markers = validRanges.map { range ->
            val markerIndex = range.start - removedCharacterCount
            removedCharacterCount += range.endInclusive - range.start

            FrozenEmoteMarker(markerIndex = markerIndex)
        }

        return FrozenEmoteLayout(
            text = renderedText.toString(),
            markers = markers
        )
    }

    private fun parseRanges(emotesRaw: String?): List<FrozenEmoteRange> {
        if (emotesRaw.isNullOrBlank()) return emptyList()

        val ranges = mutableListOf<FrozenEmoteRange>()

        for (specification in emotesRaw.split('/')) {
            val separatorIndex = specification.indexOf(':')
            if (separatorIndex <= 0 || separatorIndex + 1 >= specification.length) {
                continue
            }

            val emoteId = specification.substring(0, separatorIndex).trim()
            if (!isValidEmoteId(emoteId)) continue

            val rawPositions = specification.substring(separatorIndex + 1)
            for (rawPosition in rawPositions.split(',')) {
                val dashIndex = rawPosition.indexOf('-')
                if (dashIndex <= 0 || dashIndex + 1 >= rawPosition.length) continue

                val start = rawPosition.substring(0, dashIndex).toIntOrNull() ?: continue
                val endInclusive = rawPosition.substring(dashIndex + 1).toIntOrNull() ?: continue
                if (start !in 0..endInclusive) continue

                ranges += FrozenEmoteRange(
                    emoteId = emoteId,
                    start = start,
                    endInclusive = endInclusive
                )
            }
        }

        return ranges.sortedWith(
            compareBy<FrozenEmoteRange> { range -> range.start }
                .thenBy { range -> range.endInclusive }
        )
    }

    private fun normalizeMessage(
        rawMessage: String,
        hasEmotes: Boolean
    ): String {
        if (
            rawMessage.startsWith(ACTION_PREFIX) &&
            rawMessage.endsWith(ACTION_SUFFIX) &&
            rawMessage.length > ACTION_PREFIX.length
        ) {
            return rawMessage.substring(
                ACTION_PREFIX.length,
                rawMessage.length - ACTION_SUFFIX.length
            )
        }

        return if (hasEmotes) rawMessage else rawMessage.replace(ACTION_SUFFIX, "")
    }

    private fun isValidEmoteId(emoteId: String): Boolean {
        return emoteId.isNotBlank() && emoteId.all { character ->
            character.isLetterOrDigit() || character == '-' || character == '_'
        }
    }

    // ---------------------------------------------------------------------------------
    // The views
    // ---------------------------------------------------------------------------------

    /* ChatFragment.SwipeReplyTextView's construction; the harness reads onSwipeReply by name. */
    private class FrozenSwipeReplyTextView(context: Context) : AppCompatTextView(context) {

        var onSwipeReply: (() -> Unit)? = null

        init {
            isClickable = true
            isLongClickable = true
            isFocusable = false
            isFocusableInTouchMode = false
        }
    }

    /** PendingEchoView: the message, built once, and a status line under it. */
    internal class FrozenPendingEchoView(context: Context) : LinearLayout(context) {

        private val statusView = TextView(context).apply {
            textSize = 10f
        }

        init {
            orientation = VERTICAL
            addView(statusView)
            showStatus(FrozenEchoStatus.SENDING)
        }

        fun setMessage(messageView: View, statusTextColor: Int) {
            addView(messageView, 0)
            statusView.setTextColor(statusTextColor)
        }

        fun showStatus(status: FrozenEchoStatus) {
            val line = status.line

            if (line == null) {
                statusView.visibility = View.GONE
            } else {
                statusView.text = context.getString(textOf(line))
                statusView.visibility = View.VISIBLE
            }
            alpha = status.alpha
        }

        private fun textOf(line: FrozenEchoLine): Int {
            return when (line) {
                FrozenEchoLine.SENDING -> R.string.chat_send_pending
                FrozenEchoLine.UNCONFIRMED -> R.string.chat_send_unconfirmed
                FrozenEchoLine.REJECTED -> R.string.chat_send_rejected
            }
        }
    }

    /** PendingEchoStatusLine. */
    internal enum class FrozenEchoLine { SENDING, UNCONFIRMED, REJECTED }

    /** PendingEchoEvent. */
    internal enum class FrozenEchoEvent { TIMEOUT, USERSTATE, NOTICE }

    /** PendingEchoStatus: each status's look, and PendingEchoStatus.after. */
    internal enum class FrozenEchoStatus(val line: FrozenEchoLine?, val alpha: Float) {
        SENDING(FrozenEchoLine.SENDING, 0.72f),
        UNCONFIRMED(FrozenEchoLine.UNCONFIRMED, 0.62f),
        CONFIRMED(null, 1f),
        REJECTED(FrozenEchoLine.REJECTED, 0.5f);

        fun after(event: FrozenEchoEvent): FrozenEchoStatus {
            val awaiting = this == SENDING || this == UNCONFIRMED
            return when (event) {
                FrozenEchoEvent.TIMEOUT -> if (this == SENDING) UNCONFIRMED else this
                FrozenEchoEvent.USERSTATE -> if (awaiting) CONFIRMED else this
                FrozenEchoEvent.NOTICE -> if (awaiting) REJECTED else this
            }
        }

        /* The row data's status of the same name, for the tag. */
        fun asRowStatus(): PendingEchoStatus = when (this) {
            SENDING -> PendingEchoStatus.SENDING
            UNCONFIRMED -> PendingEchoStatus.UNCONFIRMED
            CONFIRMED -> PendingEchoStatus.CONFIRMED
            REJECTED -> PendingEchoStatus.REJECTED
        }

        companion object {
            fun of(status: PendingEchoStatus): FrozenEchoStatus = when (status) {
                PendingEchoStatus.SENDING -> SENDING
                PendingEchoStatus.UNCONFIRMED -> UNCONFIRMED
                PendingEchoStatus.CONFIRMED -> CONFIRMED
                PendingEchoStatus.REJECTED -> REJECTED
            }
        }
    }

    private companion object {
        const val EMOTE_MARKER: Char = '⁣'
        const val ACTION_PREFIX = "\u0001ACTION "
        const val ACTION_SUFFIX = "\u0001"

        /* The frozen rows are never placed in a timeline; the harness compares tags without position. */
        val UNPLACED = ChatTimelinePosition(timestampMillis = 0L, sequence = 0L)
    }
}
