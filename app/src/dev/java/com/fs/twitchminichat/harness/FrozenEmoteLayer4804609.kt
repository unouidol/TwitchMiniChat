package com.fs.twitchminichat.harness

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.TypedValue
import android.widget.TextView
import com.fs.twitchminichat.EmoteImageRequest
import com.fs.twitchminichat.EmoteImageSink
import com.fs.twitchminichat.TwitchEmoteFormat

/**
 * FROZEN COPY of what main-v5 did to a chat message's view after setting its text, at
 * 4804609de4ade00c75beff6618548f069b2e085d: TwitchEmoteImageLoader.loadInto, as
 * ChatFragment.createMessageTextView called it. NEVER "keep it in sync" with production.
 *
 * [FrozenRowViews4804609] builds the view without its emotes, as both paths of the parity pass do.
 * This layer adds them from outside, so that the frozen file itself does not change: the emote-layer
 * pass compares production, loading through its own loader, with a frozen view plus this layer.
 *
 * A frozen artefact is trusted for one reason: it was calibrated against real production and never
 * moved afterwards. This one is licensed by the harness's run on the commit that adds it, before 5c
 * changes who owns an emote session, where the production loader, given the same images, must
 * build the same views as this layer; the run and its result are recorded with 5c's device plan in
 * docs/. From then on this file does not change.
 *
 * Restated from that commit, with where each step came from there:
 * - the render size, from the constant and never from the view: the message view's text size was
 *   set as `textSize = 14f` (ChatFragment.kt:4100), and the render size was that text size in pixels
 *   times 1.5, truncated (`targetEmoteRenderSizePx`, :3754, called at :4198). Reading it from the
 *   constant keeps this reference independent of what production reads;
 * - the text: `setText(text, BufferType.SPANNABLE)`, then the view's own copy is the buffer every
 *   image goes into (TwitchEmoteImageLoader.kt:87-88). The text it was given was the builder the
 *   view had just been set with (ChatFragment.kt:4191). The view's text is a copy of that builder
 *   plus the view's change watcher, which setText removes before adding its own (TextView.java:7483,
 *   API 36), so giving setText the view's text gives it that builder's spans;
 * - the URL: the Twitch content delivery network path with the id, `animated` or `static`, `dark` or
 *   `light`, and the scale, 3.0 above 56 pixels, 2.0 above 28, 1.0 otherwise
 *   (TwitchEmoteUrlFactory.kt:19-41, unchanged at that commit);
 * - the order: the animated image first, unless this layer has seen that emote's animated version
 *   answer 404, then the static image after such a 404 (TwitchEmoteImageLoader.kt:109-127,
 *   :171-182, :208-216);
 * - each image: any image span already at the marker removed, the drawable's bounds set to 0..size,
 *   and an `ImageSpan(drawable, ALIGN_BOTTOM)` over the marker alone, exclusive at both ends
 *   (:386-412); an animated drawable made visible (:420).
 *
 * Left out, on purpose, because no harness draw or state read can see them: the drawable callback
 * that invalidates the view, the layout and invalidation requests, and whether an animated emote is
 * reported to the animation gate and runs.
 *
 * Its images come from a [FixedEmoteImageSource] of its own, with the same declaration as the
 * production side's: the harness's fixture, not production code.
 */
internal class FrozenEmoteLayer4804609(
    private val context: Context,
    private val images: FixedEmoteImageSource
) {

    /* The emote ids whose animated version answered 404 to this layer. */
    private val staticOnlyEmoteIds = mutableSetOf<String>()

    /** The render size 4804609 gave every emote of a message: 14 sp in pixels, times 1.5, truncated. */
    fun renderSizePx(): Int =
        (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE_SP, context.resources.displayMetrics) * 1.5f)
            .toInt()

    /**
     * Adds the emotes [emotesRaw] names to [view], a message view the frozen copy has just built, at
     * [renderSizePx]: the frozen size, unless the harness's control asks for a wrong one on purpose.
     */
    fun apply(view: TextView, emotesRaw: String, darkTheme: Boolean, renderSizePx: Int = renderSizePx()) {
        val ids = emoteIdsInOrder(emotesRaw)
        if (ids.isEmpty() || renderSizePx <= 0) return

        view.setText(view.text, TextView.BufferType.SPANNABLE)
        val liveText = checkNotNull(view.text as? Spannable) { "the frozen view's text is not Spannable after setText" }

        val markers = liveText.indices.filter { index -> liveText[index] == EMOTE_MARKER }
        check(markers.size == ids.size) {
            "the frozen emote layer found ${markers.size} markers for ${ids.size} emotes in $emotesRaw: it restates only " +
                "rows whose name and reply header hold no marker"
        }

        val theme = if (darkTheme) "dark" else "light"
        val scale = when {
            renderSizePx > 56 -> "3.0"
            renderSizePx > 28 -> "2.0"
            else -> "1.0"
        }
        markers.zip(ids).forEach { (markerIndex, emoteId) ->
            if (emoteId in staticOnlyEmoteIds) {
                load(liveText, emoteId, markerIndex, renderSizePx, TwitchEmoteFormat.STATIC, theme, scale)
            } else {
                load(liveText, emoteId, markerIndex, renderSizePx, TwitchEmoteFormat.ANIMATED, theme, scale)
            }
        }
    }

    private fun load(
        text: Spannable,
        emoteId: String,
        markerIndex: Int,
        renderSizePx: Int,
        format: TwitchEmoteFormat,
        theme: String,
        scale: String
    ) {
        val url = "https://static-cdn.jtvnw.net/emoticons/v2/$emoteId/${format.pathSegment}/$theme/$scale"
        images.load(EmoteImageRequest(url = url, format = format, renderSizePx = renderSizePx), object : EmoteImageSink {
            override fun onReady(drawable: Drawable) {
                install(text, drawable, markerIndex, renderSizePx)
            }

            override fun onFailed(missingAnimatedVariant: Boolean) {
                if (format != TwitchEmoteFormat.ANIMATED) return
                if (missingAnimatedVariant) staticOnlyEmoteIds += emoteId
                load(text, emoteId, markerIndex, renderSizePx, TwitchEmoteFormat.STATIC, theme, scale)
            }

            /* The reference is never cleared: it is built, drawn and compared. */
            override fun onCleared() = Unit
        })
    }

    private fun install(text: Spannable, drawable: Drawable, markerIndex: Int, renderSizePx: Int) {
        if (markerIndex !in text.indices) return
        text.getSpans(markerIndex, markerIndex + 1, ImageSpan::class.java).forEach { existing -> text.removeSpan(existing) }
        drawable.setBounds(0, 0, renderSizePx, renderSizePx)
        text.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM), markerIndex, markerIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (drawable is Animatable) drawable.setVisible(true, false)
    }

    /* The emote id of each range in [emotesRaw], in the order of their start offsets, as the formatter sorted them. */
    private fun emoteIdsInOrder(emotesRaw: String): List<String> = emotesRaw.split('/')
        .flatMap { specification ->
            val id = specification.substringBefore(':')
            check(id.isNotEmpty() && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
                "the frozen emote layer restates only well-formed emote tags, not $emotesRaw"
            }
            specification.substringAfter(':').split(',').map { range -> range.substringBefore('-').toInt() to id }
        }
        .sortedBy { (start, _) -> start }
        .map { (_, id) -> id }

    private companion object {
        const val MESSAGE_TEXT_SIZE_SP = 14f
        const val EMOTE_MARKER = '⁣'
    }
}
