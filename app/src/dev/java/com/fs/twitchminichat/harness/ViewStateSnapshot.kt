package com.fs.twitchminichat.harness

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.fs.twitchminichat.ChatTimelinePosition
import com.fs.twitchminichat.chat.ChatMessageRow
import com.fs.twitchminichat.chat.PendingEchoRow
import com.fs.twitchminichat.chat.SystemLineRow

/**
 * Everything about a row's view that is not pixels, as plain values, so the two paths can be
 * compared field by field: the flags setMovementMethod forces, the spans and their ranges, the
 * padding and background a mention sets, whether a swipe target is set, the tag.
 *
 * Production and frozen classes of the same role are named by their role, so that a difference
 * of class name alone is not a difference; everything else is compared exactly.
 */
internal object ViewStateSnapshot {

    /** The state of [view] and, for a view group, of its children, in order. */
    fun of(view: View): Map<String, Any?> {
        val state = linkedMapOf<String, Any?>()
        state["kind"] = kindOf(view)
        state["visibility"] = view.visibility
        state["alpha"] = view.alpha
        state["translationX"] = view.translationX
        state["translationY"] = view.translationY
        state["padding"] = listOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
        state["paddingRelative"] = listOf(view.paddingStart, view.paddingEnd)
        state["background"] = describe(view.background)
        state["layoutParams"] = view.layoutParams?.let { params ->
            val margins = (params as? ViewGroup.MarginLayoutParams)
                ?.let { listOf(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin) }
            listOf(params.width, params.height, margins, (params as? LinearLayout.LayoutParams)?.weight)
        }
        state["focusable"] = view.focusable
        state["isFocusable"] = view.isFocusable
        state["focusableInTouchMode"] = view.isFocusableInTouchMode
        state["clickable"] = view.isClickable
        state["longClickable"] = view.isLongClickable
        state["hasOnClickListeners"] = view.hasOnClickListeners()
        state["enabled"] = view.isEnabled
        state["tag"] = describeTag(view.tag)
        state["swipeReplySet"] = swipeReplySet(view)

        if (view is TextView) {
            state["text"] = view.text.toString()
            state["spans"] = spansOf(view.text)
            state["textSizePx"] = view.textSize
            state["currentTextColor"] = view.currentTextColor
            state["textColorsDefault"] = view.textColors.defaultColor
            state["linksClickable"] = view.linksClickable
            state["movementMethod"] = view.movementMethod?.javaClass?.name
            state["typefaceStyle"] = view.typeface?.style
            state["gravity"] = view.gravity
            state["maxLines"] = view.maxLines
            state["lineSpacing"] = listOf(view.lineSpacingExtra, view.lineSpacingMultiplier)
            state["includeFontPadding"] = view.includeFontPadding
        }
        if (view is LinearLayout) {
            state["orientation"] = view.orientation
        }
        if (view is ViewGroup) {
            state["children"] = (0 until view.childCount).map { index -> of(view.getChildAt(index)) }
        }
        return state
    }

    /** Every path at which [production] and [frozen] differ, with both values. Empty when they match. */
    fun diff(production: Any?, frozen: Any?, path: String = ""): List<String> {
        if (production is Map<*, *> && frozen is Map<*, *>) {
            val keys = (production.keys + frozen.keys).map { it.toString() }.distinct()
            return keys.flatMap { key -> diff(production[key], frozen[key], "$path/$key") }
        }
        if (production is List<*> && frozen is List<*>) {
            if (production.size != frozen.size) {
                return listOf("$path: production has ${production.size}, frozen has ${frozen.size}")
            }
            return production.indices.flatMap { index -> diff(production[index], frozen[index], "$path[$index]") }
        }
        return if (production == frozen) emptyList() else listOf("$path: production=$production frozen=$frozen")
    }

    /** A fresh view's theme defaults, for the record: padding, background and the focus flags. */
    fun defaultsOf(view: View): Map<String, Any?> = linkedMapOf(
        "class" to view.javaClass.name,
        "padding" to listOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom),
        "background" to describe(view.background),
        "focusable" to view.focusable,
        "focusableInTouchMode" to view.isFocusableInTouchMode,
        "clickable" to view.isClickable,
        "longClickable" to view.isLongClickable
    )

    private fun kindOf(view: View): String {
        val name = view.javaClass.name
        return when {
            name == "com.fs.twitchminichat.ChatFragment\$SwipeReplyTextView" ||
                name.endsWith("\$FrozenSwipeReplyTextView") -> "SwipeReplyTextView"
            name == "com.fs.twitchminichat.PendingEchoView" ||
                name.endsWith("\$FrozenPendingEchoView") -> "PendingEchoView"
            else -> name
        }
    }

    private fun describe(drawable: Drawable?): String? = when (drawable) {
        null -> null
        is GradientDrawable -> "GradientDrawable(shape=${drawable.shape}, " +
            "radius=${drawable.cornerRadius}, color=${drawable.color?.defaultColor})"
        is ColorDrawable -> "ColorDrawable(${drawable.color})"
        else -> drawable.javaClass.name
    }

    private fun spansOf(text: CharSequence): List<String> {
        val spanned = text as? Spanned ?: return emptyList()
        return spanned.getSpans(0, spanned.length, Any::class.java).map { span ->
            val range = "${spanned.getSpanStart(span)}-${spanned.getSpanEnd(span)} flags=${spanned.getSpanFlags(span)}"
            val what = when (span) {
                is ForegroundColorSpan -> "ForegroundColorSpan(${span.foregroundColor})"
                is BackgroundColorSpan -> "BackgroundColorSpan(${span.backgroundColor})"
                is StyleSpan -> "StyleSpan(${span.style})"
                is ImageSpan -> "ImageSpan"
                is ClickableSpan -> "WebLinkSpan(${fieldValue(span, "url")})"
                else -> span.javaClass.name
            }
            "$what $range"
        }
    }

    /* The row the view is tagged with, without its position: the frozen rows are never placed. */
    private fun describeTag(tag: Any?): String? = when (tag) {
        null -> null
        is ChatMessageRow -> tag.copy(position = UNPLACED).toString()
        is SystemLineRow -> tag.copy(position = UNPLACED).toString()
        is PendingEchoRow -> tag.copy(position = UNPLACED).toString()
        else -> tag.javaClass.name
    }

    private fun swipeReplySet(view: View): Boolean? {
        val field = generateSequence<Class<*>>(view.javaClass) { it.superclass }
            .mapNotNull { type -> runCatching { type.getDeclaredField("onSwipeReply") }.getOrNull() }
            .firstOrNull() ?: return null
        field.isAccessible = true
        return field.get(view) != null
    }

    private fun fieldValue(target: Any, name: String): Any? {
        val field = generateSequence<Class<*>>(target.javaClass) { it.superclass }
            .mapNotNull { type -> runCatching { type.getDeclaredField(name) }.getOrNull() }
            .firstOrNull() ?: return "<no field $name>"
        field.isAccessible = true
        return field.get(target)
    }

    private val UNPLACED = ChatTimelinePosition(timestampMillis = 0L, sequence = 0L)
}
