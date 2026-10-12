package com.fs.twitchminichat.chat

import com.fs.twitchminichat.TwitchEmoteMarker
import com.fs.twitchminichat.TwitchEmoteMessageFormatter

/**
 * What a chat row shows, as plain values: its text, the spans over that text as descriptors, the
 * text's colour and size, and the look of the row around the text.
 *
 * Built by [ChatRowText] from the row and the one piece of page state it depends on, the
 * signed-in username, with no Android type, so every decision in it is testable on the JVM.
 * [ChatRowTextApplier] puts it on a view: it resolves each colour against the theme, creates the
 * Android spans, finds the web links (Linkify is called there, never reimplemented here) and
 * sets the text.
 *
 * Every field always has a value, so what a view shows after one is applied never depends on
 * what it showed before.
 */
data class RowText(
    /** The whole text the view shows. */
    val text: String,
    /** The spans over [text], in the order they are applied; their order is part of the result. */
    val spans: List<RowTextSpan>,
    /** The colour of the text outside any span. */
    val textColor: RowTextColor,
    /** The text size, in scaled pixels. */
    val textSizeSp: Float,
    /** Background and padding: the part of the row's look that lives outside its text. */
    val look: RowLook,
    /**
     * Where the search for web links starts in [text], or null for a row that holds no links.
     *
     * A row with a value also gets the link movement method, which makes its view focusable,
     * clickable and long-clickable whether or not a link is found.
     */
    val linksFrom: Int?,
    /** The emote markers in [text], each indexed from [emoteMarkerOffset]. */
    val emoteMarkers: List<TwitchEmoteMarker>,
    /** Where the message part of [text], which the emote markers index, starts. */
    val emoteMarkerOffset: Int
)

/** One span over a [RowText]'s text, from [start] inclusive to [end] exclusive. */
sealed interface RowTextSpan {
    val start: Int
    val end: Int

    /** Text in the theme's secondary colour: the reply header. */
    data class Secondary(override val start: Int, override val end: Int) : RowTextSpan

    /** The sender's name, in [color]. */
    data class Username(
        override val start: Int,
        override val end: Int,
        val color: UsernameColor
    ) : RowTextSpan

    /** The highlight behind a mention of the signed-in user. */
    data class MentionBackground(override val start: Int, override val end: Int) : RowTextSpan

    /** The bold weight of a mention of the signed-in user. */
    data class MentionBold(override val start: Int, override val end: Int) : RowTextSpan
}

/** How a sender's name is coloured. */
sealed interface UsernameColor {
    /** The signed-in user's own name: the theme's link colour. */
    data object SignedIn : UsernameColor

    /** A name in the fixed colour table, such as the Pokemon Community Game bot. */
    data class Fixed(val argb: Int) : UsernameColor

    /** Any other name: a colour from hue, saturation and value, the hue taken from the name. */
    data class Hashed(val hue: Float, val saturation: Float, val value: Float) : UsernameColor
}

/** The theme colour the text outside any span takes. */
enum class RowTextColor {
    /** The theme's primary text colour: chat messages. */
    PRIMARY,

    /** The theme's secondary text colour: the lines the app writes. */
    SECONDARY
}

/**
 * The look of a row outside its text: its background and its padding, both always given, so
 * that applying it leaves nothing from an earlier look behind.
 */
data class RowLook(
    /** What is drawn behind the row. */
    val background: RowBackground,
    /** The row's padding, relative to the layout direction, in density-independent pixels. */
    val padding: RowPadding
) {
    companion object {
        /** A row with nothing behind it and no padding. */
        val PLAIN = RowLook(
            background = RowBackground.None,
            padding = RowPadding(startDp = 0, topDp = 0, endDp = 0, bottomDp = 0)
        )

        /** A row that mentions the signed-in user. */
        val MENTION = RowLook(
            background = RowBackground.MentionHighlight(cornerRadiusDp = 10),
            padding = RowPadding(startDp = 8, topDp = 6, endDp = 8, bottomDp = 6)
        )
    }
}

/** What is drawn behind a row. */
sealed interface RowBackground {
    /** Nothing. */
    data object None : RowBackground

    /** A rectangle with rounded corners, in the theme's mention row highlight colour. */
    data class MentionHighlight(val cornerRadiusDp: Int) : RowBackground
}

/** A row's padding, relative to the layout direction, in density-independent pixels. */
data class RowPadding(
    val startDp: Int,
    val topDp: Int,
    val endDp: Int,
    val bottomDp: Int
)

/**
 * Builds a row's [RowText], making the decisions ChatFragment's createMessageTextView and
 * appendSystemLine made: the reply header, the sender's name and its colour, the mentions of the
 * signed-in user, the look they give the row, the emote markers and where links may be.
 */
object ChatRowText {

    /** Names with a fixed colour, by lowercased name. */
    private val FIXED_COLORS = mapOf(
        "elbierro" to 0xFFFFD700.toInt(),
        "pokemoncommunitygame" to 0xFFFF5555.toInt()
    )

    private const val HASHED_SATURATION = 0.75f
    private const val HASHED_VALUE = 0.95f

    private const val MESSAGE_TEXT_SIZE_SP = 14f
    private const val SYSTEM_LINE_TEXT_SIZE_SP = 12f

    /**
     * A chat message's row: an optional reply header, the sender's name in brackets, then the
     * message, its emotes replaced by markers. [signedInUsername] decides which name takes the
     * signed-in colour and which mentions are highlighted; null or blank highlights none.
     */
    fun message(
        user: String,
        rawMessage: String,
        emotesRaw: String?,
        replyParentUserLogin: String?,
        signedInUsername: String?
    ): RowText {
        val emoteLayout = TwitchEmoteMessageFormatter.format(
            rawMessage = rawMessage,
            emotesRaw = emotesRaw
        )

        val replyHeader = replyParentUserLogin
            ?.takeIf { it.isNotBlank() }
            ?.let { "↪ replying to @$it\n" }
            .orEmpty()
        val fullPrefix = "$replyHeader[$user] "
        val text = fullPrefix + emoteLayout.text

        val spans = mutableListOf<RowTextSpan>()
        if (replyHeader.isNotEmpty()) {
            spans += RowTextSpan.Secondary(start = 0, end = replyHeader.length)
        }

        val open = text.indexOf("[", replyHeader.length)
        val close = text.indexOf("]", replyHeader.length)
        if (open != -1 && close > open) {
            spans += RowTextSpan.Username(
                start = open + 1,
                end = close,
                color = usernameColor(user, signedInUsername)
            )
        }

        /* Searched over the whole text, so a reply to the signed-in user highlights its header. */
        val mentions = mentionRanges(text, signedInUsername)
        for (mention in mentions) {
            spans += RowTextSpan.MentionBackground(start = mention.first, end = mention.last + 1)
            spans += RowTextSpan.MentionBold(start = mention.first, end = mention.last + 1)
        }

        return RowText(
            text = text,
            spans = spans,
            textColor = RowTextColor.PRIMARY,
            textSizeSp = MESSAGE_TEXT_SIZE_SP,
            look = if (mentions.isNotEmpty()) RowLook.MENTION else RowLook.PLAIN,
            linksFrom = fullPrefix.length,
            emoteMarkers = emoteLayout.markers,
            emoteMarkerOffset = fullPrefix.length
        )
    }

    /** A line the app writes, [formattedText] as the page formatted it: plain, secondary, smaller. */
    fun systemLine(formattedText: String): RowText = RowText(
        text = formattedText,
        spans = emptyList(),
        textColor = RowTextColor.SECONDARY,
        textSizeSp = SYSTEM_LINE_TEXT_SIZE_SP,
        look = RowLook.PLAIN,
        linksFrom = null,
        emoteMarkers = emptyList(),
        emoteMarkerOffset = 0
    )

    /*
     * The signed-in name is compared lowercased and untrimmed, while the mention search trims it:
     * both as the page did.
     */
    private fun usernameColor(user: String, signedInUsername: String?): UsernameColor {
        val key = user.lowercase()
        if (key == signedInUsername?.lowercase()) return UsernameColor.SignedIn
        FIXED_COLORS[key]?.let { return UsernameColor.Fixed(it) }

        val hue = (key.hashCode() and 0x7fffffff) % 360
        return UsernameColor.Hashed(
            hue = hue.toFloat(),
            saturation = HASHED_SATURATION,
            value = HASHED_VALUE
        )
    }

    /* Every "@name" of the signed-in user, in any case, not inside a longer word or name. */
    private fun mentionRanges(text: String, signedInUsername: String?): List<IntRange> {
        val username = signedInUsername?.trim().orEmpty()
        if (username.isBlank()) return emptyList()

        val regex = Regex(
            pattern = "(?i)(?<![A-Za-z0-9_])@${Regex.escape(username)}(?![A-Za-z0-9_])"
        )
        return regex.findAll(text).map { match -> match.range }.toList()
    }
}
