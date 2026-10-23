package com.fs.twitchminichat

import android.graphics.drawable.Drawable
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition

/**
 * The [EmoteImageSource] production uses: Glide, through the chat page's [requestManager], so that
 * its loads follow the page's lifecycle.
 *
 * An animated request asks for a Graphics Interchange Format (GIF) image and plays it forever; a
 * 404 among the failure's root causes is reported as a missing animated version. A static request
 * asks for any drawable. Each load is one Glide target, and clearing the load clears that target.
 */
internal class GlideEmoteImageSource(
    private val requestManager: RequestManager
) : EmoteImageSource {

    override fun load(request: EmoteImageRequest, sink: EmoteImageSink): EmoteImageLoad = when (request.format) {
        TwitchEmoteFormat.ANIMATED -> loadAnimated(request, sink)
        TwitchEmoteFormat.STATIC -> loadStatic(request, sink)
    }

    /** Loads the animated GIF and plays it forever. */
    private fun loadAnimated(request: EmoteImageRequest, sink: EmoteImageSink): EmoteImageLoad {
        /* Set by the listener, which Glide calls before the target with the same failure. */
        var isMissingAnimatedVariant = false

        val target = object : CustomTarget<GifDrawable>(request.renderSizePx, request.renderSizePx) {
            override fun onResourceReady(
                resource: GifDrawable,
                transition: Transition<in GifDrawable>?
            ) {
                resource.setLoopCount(GifDrawable.LOOP_FOREVER)
                sink.onReady(resource)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                sink.onFailed(missingAnimatedVariant = isMissingAnimatedVariant)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                sink.onCleared()
            }
        }

        requestManager
            .asGif()
            .load(request.url)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    error: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    isMissingAnimatedVariant = error
                        ?.rootCauses
                        ?.filterIsInstance<HttpException>()
                        ?.any { cause -> cause.statusCode == 404 }
                        ?: false
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
            .override(request.renderSizePx, request.renderSizePx)
            .into(target)
        return EmoteImageLoad { requestManager.clear(target) }
    }

    /** Loads the static Portable Network Graphics (PNG) image. */
    private fun loadStatic(request: EmoteImageRequest, sink: EmoteImageSink): EmoteImageLoad {
        val target = object : CustomTarget<Drawable>(request.renderSizePx, request.renderSizePx) {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                sink.onReady(resource)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                sink.onFailed(missingAnimatedVariant = false)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                sink.onCleared()
            }
        }

        requestManager
            .asDrawable()
            .load(request.url)
            .override(request.renderSizePx, request.renderSizePx)
            .into(target)
        return EmoteImageLoad { requestManager.clear(target) }
    }
}
