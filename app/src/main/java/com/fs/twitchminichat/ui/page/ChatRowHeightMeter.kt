package com.fs.twitchminichat.ui.page

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.drawable.toDrawable
import com.fs.twitchminichat.chat.ChatRowText

/**
 * Measures the height of one chat row holding an emote, at the user's current font scale, so the
 * list's floor can be stated in rows rather than in dp.
 *
 * PROTOTYPE (branch `prototype/5d-layout-b`), not for merging.
 *
 * The row is rebuilt offscreen the way a plain message row is: a text view at
 * [ChatRowText.MESSAGE_TEXT_SIZE_SP] with no padding, holding an [ImageSpan] aligned to the bottom
 * and as large as the page draws an emote, 1.5 times the text size (`ChatFragment`'s
 * `targetEmoteRenderSizePx`). The emote is a transparent box: only its size matters.
 */
object ChatRowHeightMeter {

    /** The page's emote size, as a multiple of the text size. */
    private const val EMOTE_TO_TEXT_SIZE = 1.5f

    /** The character an image span replaces, U+FFFC. */
    private val OBJECT_REPLACEMENT = Char(0xFFFC)

    /** One emote row's height in pixels, measured with no width limit. */
    fun emoteRowHeightPx(context: Context): Int {
        val view = AppCompatTextView(context)
        view.textSize = ChatRowText.MESSAGE_TEXT_SIZE_SP
        view.setPadding(0, 0, 0, 0)

        val emotePx = (view.textSize * EMOTE_TO_TEXT_SIZE).toInt()
        val emote = Color.TRANSPARENT.toDrawable().apply { setBounds(0, 0, emotePx, emotePx) }
        val text = SpannableString("Ag $OBJECT_REPLACEMENT")
        text.setSpan(ImageSpan(emote, ImageSpan.ALIGN_BOTTOM), 3, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        view.text = text

        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(unspecified, unspecified)
        return view.measuredHeight
    }
}
