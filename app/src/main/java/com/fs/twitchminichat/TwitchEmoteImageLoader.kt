package com.fs.twitchminichat

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import java.util.Collections

/**
 * Loads inline Twitch emotes and owns their Glide lifecycle for rendered chat rows.
 *
 * Animated media is attempted once per emote ID. When Twitch exposes only the static
 * Portable Network Graphics (PNG) version, a Hypertext Transfer Protocol (HTTP) 404
 * response is remembered for the current process and later rows load the static
 * resource directly.
 */
class TwitchEmoteImageLoader(
    private val requestManager: RequestManager,
    private val chatPageView: View
) {
    private val sessions = mutableSetOf<LoadSession>()
    private val staticOnlyEmoteIds = Collections.synchronizedSet(mutableSetOf<String>())

    /** Loads all markers belonging to one chat row. */
    fun loadInto(
        textView: TextView,
        text: SpannableStringBuilder,
        markers: List<TwitchEmoteMarker>,
        markerOffset: Int,
        renderSizePx: Int,
        darkTheme: Boolean
    ) {
        if (markers.isEmpty() || renderSizePx <= 0) return

        /*
         * Keep one live Spannable buffer inside the TextView. Asynchronous Glide
         * completions must update this buffer instead of repeatedly installing
         * stale copies containing older ImageSpan instances.
         */
        textView.setText(text, TextView.BufferType.SPANNABLE)
        val liveText = textView.text as? Spannable ?: return

        val session = LoadSession(
            textView = textView,
            text = liveText,
            onReleased = { releasedSession ->
                sessions.remove(releasedSession)
            }
        )

        sessions += session
        textView.addOnAttachStateChangeListener(session)

        val theme = if (darkTheme) TwitchEmoteTheme.DARK else TwitchEmoteTheme.LIGHT
        val scale = TwitchEmoteUrlFactory.scaleForRenderSize(renderSizePx)

        for (marker in markers) {
            val absoluteMarkerIndex = markerOffset + marker.markerIndex
            if (absoluteMarkerIndex !in text.indices) continue
            if (text[absoluteMarkerIndex] != TwitchEmoteMessageFormatter.EMOTE_MARKER) continue

            if (marker.emoteId in staticOnlyEmoteIds) {
                loadStatic(
                    session = session,
                    emoteId = marker.emoteId,
                    markerIndex = absoluteMarkerIndex,
                    renderSizePx = renderSizePx,
                    theme = theme,
                    scale = scale
                )
            } else {
                loadAnimated(
                    session = session,
                    emoteId = marker.emoteId,
                    markerIndex = absoluteMarkerIndex,
                    renderSizePx = renderSizePx,
                    theme = theme,
                    scale = scale
                )
            }
        }
    }

    /** Stops animations and clears every request owned by the current chat view. */
    fun clearAll() {
        sessions.toList().forEach { session -> session.release() }
        sessions.clear()
    }

    /** Attempts the animated Graphics Interchange Format (GIF) before static PNG. */
    private fun loadAnimated(
        session: LoadSession,
        emoteId: String,
        markerIndex: Int,
        renderSizePx: Int,
        theme: TwitchEmoteTheme,
        scale: String
    ) {
        val url = TwitchEmoteUrlFactory.build(
            emoteId = emoteId,
            format = TwitchEmoteFormat.ANIMATED,
            theme = theme,
            scale = scale
        ) ?: return

        val target = object : CustomTarget<GifDrawable>(renderSizePx, renderSizePx) {
            private var resource: GifDrawable? = null

            override fun onResourceReady(
                resource: GifDrawable,
                transition: Transition<in GifDrawable>?
            ) {
                if (session.isReleased) return

                this.resource = resource
                resource.setLoopCount(GifDrawable.LOOP_FOREVER)
                session.installDrawable(
                    drawable = resource,
                    markerIndex = markerIndex,
                    renderSizePx = renderSizePx
                )
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                if (session.isReleased) return

                loadStatic(
                    session = session,
                    emoteId = emoteId,
                    markerIndex = markerIndex,
                    renderSizePx = renderSizePx,
                    theme = theme,
                    scale = scale
                )
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                /*
                 * Glide may recycle this GIF after clearing the target. Remove the
                 * matching ImageSpan first so the TextView never retains recycled frames.
                 */
                session.clearInstalledDrawable(
                    drawable = resource,
                    markerIndex = markerIndex
                )
                resource = null
            }
        }

        session.track(target)
        requestManager
            .asGif()
            .load(url)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    error: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    val isMissingAnimatedVariant = error
                        ?.rootCauses
                        ?.filterIsInstance<HttpException>()
                        ?.any { cause -> cause.statusCode == 404 }
                        ?: false

                    if (isMissingAnimatedVariant) {
                        staticOnlyEmoteIds += emoteId
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean = false
            })
            .override(renderSizePx, renderSizePx)
            .into(target)
    }

    /** Loads the static PNG used when the emote has no animated representation. */
    private fun loadStatic(
        session: LoadSession,
        emoteId: String,
        markerIndex: Int,
        renderSizePx: Int,
        theme: TwitchEmoteTheme,
        scale: String
    ) {
        val url = TwitchEmoteUrlFactory.build(
            emoteId = emoteId,
            format = TwitchEmoteFormat.STATIC,
            theme = theme,
            scale = scale
        ) ?: return

        val target = object : CustomTarget<Drawable>(renderSizePx, renderSizePx) {
            private var resource: Drawable? = null

            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                if (session.isReleased) return

                this.resource = resource
                session.installDrawable(
                    drawable = resource,
                    markerIndex = markerIndex,
                    renderSizePx = renderSizePx
                )
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                /*
                 * Static resources follow the same Glide ownership contract as GIFs.
                 */
                session.clearInstalledDrawable(
                    drawable = resource,
                    markerIndex = markerIndex
                )
                resource = null
            }
        }

        session.track(target)
        requestManager
            .asDrawable()
            .load(url)
            .override(renderSizePx, renderSizePx)
            .into(target)
    }

    /** Owns all requests and drawable callbacks attached to one chat TextView. */
    private inner class LoadSession(
        private val textView: TextView,
        private val text: Spannable,
        private val onReleased: (LoadSession) -> Unit
    ) : View.OnAttachStateChangeListener {
        private val targets = mutableSetOf<Target<*>>()
        private val animatedDrawables = mutableSetOf<Animatable>()
        private var isAttachedToWindow = textView.isAttachedToWindow

        /** Invalidates the TextView whenever an animated drawable advances a frame. */
        private val drawableCallback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                textView.postInvalidateOnAnimation()
            }

            override fun scheduleDrawable(
                who: Drawable,
                what: Runnable,
                whenMillis: Long
            ) {
                /*
                 * Drawable.Callback supplies an absolute uptime timestamp.
                 * Convert it to the relative delay expected by View.postDelayed().
                 */
                val delayMillis =
                    (whenMillis - SystemClock.uptimeMillis()).coerceAtLeast(0L)
                textView.postDelayed(what, delayMillis)
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                textView.removeCallbacks(what)
            }
        }

        var isReleased: Boolean = false
            private set

        /** Keeps one Glide target available until the chat view is destroyed. */
        fun track(target: Target<*>) {
            if (isReleased) {
                requestManager.clear(target)
                return
            }
            targets += target
        }

        /**
         * Removes one Glide-owned drawable before its resource can be recycled.
         *
         * The identity check prevents a late clear callback from removing a newer
         * drawable that has already been installed at the same marker.
         */
        fun clearInstalledDrawable(
            drawable: Drawable?,
            markerIndex: Int
        ) {
            if (drawable == null) return

            (drawable as? Animatable)?.let { animatable ->
                animatable.stop()
                animatedDrawables.remove(animatable)
            }

            if (markerIndex in text.indices) {
                text.getSpans(
                    markerIndex,
                    markerIndex + 1,
                    ImageSpan::class.java
                )
                    .filter { span -> span.drawable === drawable }
                    .forEach { span -> text.removeSpan(span) }
            }

            drawable.callback = null
            textView.requestLayout()
            textView.postInvalidateOnAnimation()
        }

        /** Replaces one invisible marker with its loaded drawable. */
        fun installDrawable(
            drawable: Drawable,
            markerIndex: Int,
            renderSizePx: Int
        ) {
            if (isReleased || markerIndex !in text.indices) return

            /*
             * A marker must own exactly one ImageSpan. Removing an older asynchronous
             * result prevents a late Glide completion from visually overlapping or
             * replacing another emote.
             */
            text.getSpans(
                markerIndex,
                markerIndex + 1,
                ImageSpan::class.java
            ).forEach { existingSpan ->
                val existingDrawable = existingSpan.drawable

                if (existingDrawable !== drawable) {
                    (existingDrawable as? Animatable)?.let { animatable ->
                        animatable.stop()
                        animatedDrawables.remove(animatable)
                    }
                    existingDrawable.callback = null
                }

                text.removeSpan(existingSpan)
            }

            drawable.setBounds(0, 0, renderSizePx, renderSizePx)
            drawable.callback = drawableCallback

            text.setSpan(
                ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
                markerIndex,
                markerIndex + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            textView.requestLayout()
            textView.postInvalidateOnAnimation()

            if (drawable is Animatable) {
                animatedDrawables += drawable

                if (isAttachedToWindow) {
                    drawable.setVisible(true, true)
                    drawable.start()
                    textView.postInvalidateOnAnimation()
                } else {
                    drawable.stop()
                }
            }
        }

        /** Restarts paused animations when ViewPager2 reattaches the account page. */
        override fun onViewAttachedToWindow(view: View) {
            if (isReleased) return

            isAttachedToWindow = true
            animatedDrawables.forEach { drawable ->
                (drawable as? Drawable)?.setVisible(true, false)
                drawable.start()
            }
            textView.postInvalidateOnAnimation()
        }

        /** Pauses animations without releasing rows that ViewPager2 may reattach. */
        override fun onViewDetachedFromWindow(view: View) {
            if (isReleased) return

            if (chatPageView.isAttachedToWindow) {
                release()
                return
            }

            isAttachedToWindow = false
            animatedDrawables.forEach { drawable -> drawable.stop() }
        }

        /** Clears requests, stops GIFs through their targets, and releases the row. */
        fun release() {
            if (isReleased) return
            isReleased = true

            textView.removeOnAttachStateChangeListener(this)
            animatedDrawables.forEach { drawable -> drawable.stop() }
            animatedDrawables.clear()
            targets.toList().forEach { target -> requestManager.clear(target) }
            targets.clear()
            onReleased(this)
        }
    }
}
