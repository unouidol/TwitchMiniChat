package com.fs.twitchminichat

import android.graphics.drawable.Drawable

/** One emote image to fetch: the image at [url], in [format], decoded at [renderSizePx] square. */
internal data class EmoteImageRequest(
    val url: String,
    val format: TwitchEmoteFormat,
    val renderSizePx: Int
)

/**
 * Where the outcome of one load goes. Main thread.
 *
 * A load ends in [onReady] or [onFailed], at most once, unless it is cleared first. [onCleared]
 * follows a clear, whether or not the load had ended, and nothing follows it.
 */
internal interface EmoteImageSink {

    /** The image is ready. [drawable] belongs to the source, which may recycle it once the load is cleared. */
    fun onReady(drawable: Drawable)

    /**
     * The image could not be loaded. [missingAnimatedVariant] is true when the failure says the
     * emote has no animated version: Twitch answered a Hypertext Transfer Protocol (HTTP) 404.
     */
    fun onFailed(missingAnimatedVariant: Boolean)

    /** The load was cleared: a drawable it delivered must no longer be drawn. */
    fun onCleared()
}

/** One load an [EmoteImageSource] started, until it is cleared. */
internal fun interface EmoteImageLoad {

    /** Withdraws the load: it delivers nothing more, and its sink is told [EmoteImageSink.onCleared]. */
    fun clear()
}

/**
 * Fetches emote images for [TwitchEmoteImageLoader]: everything the loader needs from an image
 * library, and nothing about chat rows.
 *
 * Production uses [GlideEmoteImageSource], built where the chat page builds its loader. The dev
 * flavour's parity harness gives a loader one it controls, so that what an emote row shows depends
 * on nothing outside the harness.
 */
internal interface EmoteImageSource {

    /**
     * Starts loading [request], and reports its outcome to [sink]. The outcome may arrive before
     * this returns, as a memory-cache hit does in Glide.
     */
    fun load(request: EmoteImageRequest, sink: EmoteImageSink): EmoteImageLoad
}
