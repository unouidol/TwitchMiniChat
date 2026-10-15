package com.fs.twitchminichat

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import com.fs.twitchminichat.chat.RowBackground
import com.fs.twitchminichat.chat.RowLook
import com.fs.twitchminichat.chat.RowText
import com.fs.twitchminichat.chat.RowTextColor
import com.fs.twitchminichat.chat.RowTextSpan
import com.fs.twitchminichat.chat.UsernameColor

/**
 * Puts a [RowText] on a chat row's view, always in the same order, deciding nothing of its own:
 * it resolves the row's colours against the page's [theme], turns each span descriptor into its
 * Android span, finds the web links with [ChatMessageLinkifier] and sets the text.
 *
 * The look is applied to every row, highlighted or not.
 */
internal class ChatRowTextApplier(
    private val theme: Theme,
    private val onLinkClick: (url: String) -> Unit
) {

    /** The page's theme colours and display density, read each time a row is applied. */
    interface Theme {
        /** The colour of text outside any span, for [role]. */
        fun textColor(role: RowTextColor): Int

        /** The colour of the signed-in user's own name. */
        fun signedInUsernameColor(): Int

        /** The colour behind a mention of the signed-in user. */
        fun mentionTextHighlightColor(): Int

        /** The colour behind a whole row that mentions the signed-in user. */
        fun mentionRowHighlightColor(): Int

        /** [dp] density-independent pixels, in pixels. */
        fun pxOf(dp: Int): Int
    }

    /**
     * Applies [row] to [view] and returns the text with its spans, the buffer an emote loader
     * fills afterwards. The view shows a copy of it when it has a movement method.
     */
    fun apply(view: TextView, row: RowText): SpannableStringBuilder {
        /* The size comes before anything that reads view.textSize, such as the emote render size. */
        view.setTextColor(theme.textColor(row.textColor))
        view.textSize = row.textSizeSp

        /*
         * A row that may hold links gets the link movement method. Setting it makes the view
         * focusable, clickable and long-clickable, as it always did.
         */
        if (row.linksFrom != null) {
            view.linksClickable = true
            view.movementMethod = LinkMovementMethod.getInstance()
        }

        val text = SpannableStringBuilder(row.text)
        for (span in row.spans) {
            text.setSpan(androidSpanOf(span), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        applyLook(view, row.look)

        row.linksFrom?.let { from ->
            ChatMessageLinkifier.addWebLinks(text = text, messageStartIndex = from, onLinkClick = onLinkClick)
        }

        /* The text comes last. A row with nothing to span shows its plain string, as it did. */
        view.text = if (row.spans.isEmpty() && row.linksFrom == null) row.text else text
        return text
    }

    private fun androidSpanOf(span: RowTextSpan): Any = when (span) {
        is RowTextSpan.Secondary -> ForegroundColorSpan(theme.textColor(RowTextColor.SECONDARY))
        is RowTextSpan.Username -> ForegroundColorSpan(colorOf(span.color))
        is RowTextSpan.MentionBackground -> BackgroundColorSpan(theme.mentionTextHighlightColor())
        is RowTextSpan.MentionBold -> StyleSpan(Typeface.BOLD)
    }

    private fun colorOf(color: UsernameColor): Int = when (color) {
        UsernameColor.SignedIn -> theme.signedInUsernameColor()
        is UsernameColor.Fixed -> color.argb
        is UsernameColor.Hashed -> Color.HSVToColor(floatArrayOf(color.hue, color.saturation, color.value))
    }

    private fun applyLook(view: TextView, look: RowLook) {
        view.background = when (val background = look.background) {
            RowBackground.None -> null
            is RowBackground.MentionHighlight -> GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = theme.pxOf(background.cornerRadiusDp).toFloat()
                setColor(theme.mentionRowHighlightColor())
            }
        }
        view.setPaddingRelative(
            theme.pxOf(look.padding.startDp),
            theme.pxOf(look.padding.topDp),
            theme.pxOf(look.padding.endDp),
            theme.pxOf(look.padding.bottomDp)
        )
    }
}
