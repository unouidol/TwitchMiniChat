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
import com.fs.twitchminichat.chat.EmoteSessions
import java.util.Collections

/**
 * One chat row that holds animated emotes, as [TwitchEmoteImageLoader] reports it to
 * whoever decides when they run. Main thread.
 */
interface AnimatedEmoteRow {

    /** The row's view in the timeline. */
    val view: View

    /**
     * Lets the row's animated emotes run, or stops them on the frame they show.
     *
     * They run only while this is true and the row is attached to the window. A Glide
     * GIF that is stopped and run again continues from where it stopped: it is not
     * decoded again from its first frame.
     */
    fun setAnimationsWanted(wanted: Boolean)
}

/** Told when a row comes to hold animated emotes, and when that row is released. Main thread. */
interface AnimatedEmoteRowListener {

    /** [row] installed its first animated emote. Its emotes do not run until it is told to. */
    fun onAnimatedRowAdded(row: AnimatedEmoteRow)

    /** [row] was released, and its emotes stopped and cleared. */
    fun onAnimatedRowReleased(row: AnimatedEmoteRow)
}

/**
 * Loads inline Twitch emotes and owns the lifecycle of their images for rendered chat rows. The
 * images come from [images], which production backs with Glide.
 *
 * Each row's view holds at most one session: the loads and images of the row it shows. The binding
 * decides when a session ends, never the window. [bind] ends the session of the row the view showed
 * before, [release] ends the session of a view whose row leaves the timeline, and [releaseAll] ends
 * every session when the page's view goes. A view detached from the window keeps its session: its
 * animated emotes stop on the frame they show, and run again from it when the view is attached again.
 *
 * Animated media is attempted once per emote ID. When Twitch exposes only the static
 * Portable Network Graphics (PNG) version, a Hypertext Transfer Protocol (HTTP) 404
 * response is remembered by this loader, which the chat page builds in onViewCreated and
 * drops in onDestroyView, so for as long as the page's view lasts, and later rows load the
 * static resource directly.
 *
 * Whether an animated emote runs is not decided here: each row that holds one is
 * reported to [animatedRows], which tells it.
 */
internal class TwitchEmoteImageLoader(
    private val images: EmoteImageSource,
    private val animatedRows: AnimatedEmoteRowListener
) {
    private val sessions = EmoteSessions<TextView, LoadSession>(release = { session -> session.release() })
    private val staticOnlyEmoteIds = Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Gives [textView] the emotes of the row it has just been bound to: ends the session of the
     * row it showed before, then, when [markers] is not empty, installs [text] and loads each marker's
     * image into it.
     */
    fun bind(
        textView: TextView,
        text: SpannableStringBuilder,
        markers: List<TwitchEmoteMarker>,
        markerOffset: Int,
        renderSizePx: Int,
        darkTheme: Boolean
    ) {
        sessions.release(textView)
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
            text = liveText
        )

        sessions.put(textView, session)
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

    /** Ends the session of [textView], whose row leaves the timeline or whose holder is recycled. */
    fun release(textView: TextView) {
        sessions.release(textView)
    }

    /** Ends every session, stopping every animation and clearing every load: the page's view is going. */
    fun releaseAll() {
        sessions.releaseAll()
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

        val request = EmoteImageRequest(url = url, format = TwitchEmoteFormat.ANIMATED, renderSizePx = renderSizePx)
        session.track(images.load(request, object : EmoteImageSink {
            private var resource: Drawable? = null

            override fun onReady(drawable: Drawable) {
                if (session.isReleased) return

                resource = drawable
                session.installDrawable(
                    drawable = drawable,
                    markerIndex = markerIndex,
                    renderSizePx = renderSizePx
                )
            }

            override fun onFailed(missingAnimatedVariant: Boolean) {
                if (missingAnimatedVariant) {
                    staticOnlyEmoteIds += emoteId
                }
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

            override fun onCleared() {
                /*
                 * The source may recycle this GIF after clearing the load. Remove the
                 * matching ImageSpan first so the TextView never retains recycled frames.
                 */
                session.clearInstalledDrawable(
                    drawable = resource,
                    markerIndex = markerIndex
                )
                resource = null
            }
        }))
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

        val request = EmoteImageRequest(url = url, format = TwitchEmoteFormat.STATIC, renderSizePx = renderSizePx)
        session.track(images.load(request, object : EmoteImageSink {
            private var resource: Drawable? = null

            override fun onReady(drawable: Drawable) {
                if (session.isReleased) return

                resource = drawable
                session.installDrawable(
                    drawable = drawable,
                    markerIndex = markerIndex,
                    renderSizePx = renderSizePx
                )
            }

            /* A static image that cannot be loaded leaves its marker invisible, as it always did. */
            override fun onFailed(missingAnimatedVariant: Boolean) = Unit

            override fun onCleared() {
                /*
                 * Static resources follow the same ownership contract as GIFs.
                 */
                session.clearInstalledDrawable(
                    drawable = resource,
                    markerIndex = markerIndex
                )
                resource = null
            }
        }))
    }

    /** Owns all loads and drawable callbacks attached to one chat TextView. */
    private inner class LoadSession(
        private val textView: TextView,
        private val text: Spannable
    ) : View.OnAttachStateChangeListener, AnimatedEmoteRow {
        private val loads = mutableSetOf<EmoteImageLoad>()
        private val animatedDrawables = mutableSetOf<Animatable>()
        private var isAttachedToWindow = textView.isAttachedToWindow

        /* Set by whoever decides; false until it first says otherwise. */
        private var animationsWanted = false

        /* Whether this row has been reported to animatedRows, and so must be withdrawn. */
        private var reportedAsAnimated = false

        override val view: View
            get() = textView

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

        /**
         * Whether the session has ended. Every sink reads it before touching the session, and an
         * image or a failure that arrives after the release is dropped.
         *
         * A silent guard on purpose, not a precondition. As far as Glide 4.16.0 can be read, such
         * a delivery cannot happen: a release clears every load, a cleared request delivers
         * nothing (SingleRequest.clear cancels its load), and EngineJob delivers only to a
         * callback still registered (EngineJob.java:401, :425). But that is a library's internal
         * contract, which this code can read and cannot control, and a precondition here would
         * turn a future change in it into a crash in the user's app. The test in installDrawable
         * is the same guard, one call further down: only the sinks call it.
         */
        var isReleased: Boolean = false
            private set

        /** Keeps one load until the session is released. */
        fun track(load: EmoteImageLoad) {
            check(!isReleased) {
                "A load was started for a released emote session. Invariant: a load starts only inside its " +
                    "session's bind, or from a failed load's callback, which checks isReleased first."
            }
            loads += load
        }

        /**
         * Removes one source-owned drawable before its resource can be recycled.
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
                /* Visibility stays true: running and stopping go through start and stop. */
                drawable.setVisible(true, false)

                if (!reportedAsAnimated) {
                    reportedAsAnimated = true
                    animatedRows.onAnimatedRowAdded(this)
                }
                applyAnimationState()
            }
        }

        override fun setAnimationsWanted(wanted: Boolean) {
            /* A released row is no longer tracked, and its drawables are gone: this is never asked of one. */
            if (animationsWanted == wanted) return

            animationsWanted = wanted
            applyAnimationState()
        }

        /** Runs every animated emote of the row while it is wanted and attached, and stops them otherwise. */
        private fun applyAnimationState() {
            val run = animationsWanted && isAttachedToWindow

            animatedDrawables.forEach { drawable ->
                if (run) drawable.start() else drawable.stop()
            }
            if (run) {
                textView.postInvalidateOnAnimation()
            }
        }

        /*
         * Neither attach callback is ever called on a released session: release removes this
         * listener first.
         */

        /** Resumes the row's animations, if they are wanted, when it is attached again: ViewPager2's page, or a list's row. */
        override fun onViewAttachedToWindow(view: View) {
            isAttachedToWindow = true
            applyAnimationState()
        }

        /**
         * Pauses the row's animations while it is detached, and nothing else: its spans, its
         * drawables and its loads stay, so a row attached again without being bound again - a page
         * ViewPager2 brings back, a holder a RecyclerView hands back from its cache - shows its
         * emotes as it left them. The session ends only when the binding ends it.
         */
        override fun onViewDetachedFromWindow(view: View) {
            isAttachedToWindow = false
            applyAnimationState()
        }

        /** Clears its loads, which stops and removes their GIFs, and releases the row. Only [sessions] calls it. */
        fun release() {
            check(!isReleased) {
                "An emote session was released twice. Invariant: only EmoteSessions releases a session, and it " +
                    "removes the session in the same call."
            }
            isReleased = true

            textView.removeOnAttachStateChangeListener(this)
            animatedDrawables.forEach { drawable -> drawable.stop() }
            animatedDrawables.clear()
            loads.toList().forEach { load -> load.clear() }
            loads.clear()
            if (reportedAsAnimated) {
                animatedRows.onAnimatedRowReleased(this)
            }
        }
    }
}
