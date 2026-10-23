package com.fs.twitchminichat.harness

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import com.fs.twitchminichat.EmoteImageLoad
import com.fs.twitchminichat.EmoteImageRequest
import com.fs.twitchminichat.EmoteImageSink
import com.fs.twitchminichat.EmoteImageSource
import com.fs.twitchminichat.TwitchEmoteFormat
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * One emote image as the harness knows it: the emote id, format and theme its URL names, at the
 * scale the URL asks for. Read from the URL alone, so that a wrong id, format or theme in the
 * request shows in the image.
 */
internal data class FixedEmote(val id: String, val format: String, val theme: String, val scale: String) {

    /* Derived from the id, the format and the theme only, never from the size. */
    private val seed: Long = CRC32().run {
        update("$id/$format/$theme".toByteArray(Charsets.UTF_8))
        value
    }

    /** The colour that fills the image. */
    val background: Int = OPAQUE or (seed and 0xFFFFFF).toInt()

    /** The colour of the block in its top-left corner, the background's complement. */
    val mark: Int = OPAQUE or (background.inv() and 0xFFFFFF)

    /** The colour of the one-pixel frame on its right and bottom edges, which tells the formats apart. */
    val frame: Int = if (format == TwitchEmoteFormat.ANIMATED.pathSegment) ANIMATED_FRAME else STATIC_FRAME

    /** A drawable of this image whose intrinsic size is [sizePx], as Glide's override decodes it. */
    fun drawable(sizePx: Int): Drawable =
        if (format == TwitchEmoteFormat.ANIMATED.pathSegment) FixedAnimatedEmoteDrawable(this, sizePx) else FixedEmoteDrawable(this, sizePx)

    /** The first 16 hexadecimal digits of the SHA-256 of this image's pixels at [FINGERPRINT_SIZE_PX], for the report. */
    fun fingerprint(): String {
        val bitmap = createBitmap(FINGERPRINT_SIZE_PX, FINGERPRINT_SIZE_PX, Bitmap.Config.ARGB_8888)
        drawable(FINGERPRINT_SIZE_PX).apply {
            setBounds(0, 0, FINGERPRINT_SIZE_PX, FINGERPRINT_SIZE_PX)
            draw(Canvas(bitmap))
        }
        val pixels = IntArray(FINGERPRINT_SIZE_PX * FINGERPRINT_SIZE_PX)
        bitmap.getPixels(pixels, 0, FINGERPRINT_SIZE_PX, 0, 0, FINGERPRINT_SIZE_PX, FINGERPRINT_SIZE_PX)
        bitmap.recycle()
        val bytes = ByteBuffer.allocate(pixels.size * 4).apply { pixels.forEach(::putInt) }.array()
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }.take(16)
    }

    companion object {
        private const val OPAQUE = 0xFF000000.toInt()
        private const val ANIMATED_FRAME = 0xFFFF00FF.toInt()
        private const val STATIC_FRAME = 0xFF00FFFF.toInt()
        const val FINGERPRINT_SIZE_PX = 32

        private val URL = Regex("^https://static-cdn\\.jtvnw\\.net/emoticons/v2/([^/]+)/(animated|static)/(dark|light)/([0-9.]+)$")

        /** The image [request] names. A URL of any other shape stops the run: the harness does not guess. */
        fun of(request: EmoteImageRequest): FixedEmote {
            val match = checkNotNull(URL.matchEntire(request.url)) { "the fixed source cannot serve ${request.url}" }
            val (id, format, theme, scale) = match.destructured
            check(format == request.format.pathSegment) {
                "a request for ${request.format} named the $format image: ${request.url}"
            }
            return FixedEmote(id = id, format = format, theme = theme, scale = scale)
        }
    }
}

/**
 * A still image of a [FixedEmote], drawn in code into its bounds: a fill, an asymmetric block in the
 * top-left corner and a one-pixel frame on the right and bottom edges. Anti-aliasing is off, so
 * every pixel is decided by the bounds alone, and a bound one pixel larger or smaller moves the frame
 * onto other pixels: a wrong render size shows in what is drawn.
 */
internal open class FixedEmoteDrawable(val emote: FixedEmote, private val sizePx: Int) : Drawable() {

    private val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }

    override fun draw(canvas: Canvas) {
        val area = bounds
        if (area.isEmpty) return
        paint.color = emote.background
        canvas.drawRect(area, paint)
        paint.color = emote.mark
        canvas.drawRect(
            area.left.toFloat(), area.top.toFloat(),
            (area.left + area.width() / 2).toFloat(), (area.top + area.height() / 3).toFloat(), paint
        )
        paint.color = emote.frame
        canvas.drawRect((area.right - 1).toFloat(), area.top.toFloat(), area.right.toFloat(), area.bottom.toFloat(), paint)
        canvas.drawRect(area.left.toFloat(), (area.bottom - 1).toFloat(), area.right.toFloat(), area.bottom.toFloat(), paint)
    }

    override fun getIntrinsicWidth(): Int = sizePx

    override fun getIntrinsicHeight(): Int = sizePx

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/**
 * The animated version of a [FixedEmote]. It records every start and stop, and draws the same pixels
 * running or stopped: it has no frames, so the harness compares where an emote is, never which frame
 * it shows, and nothing in it runs on a clock.
 */
internal class FixedAnimatedEmoteDrawable(emote: FixedEmote, sizePx: Int) : FixedEmoteDrawable(emote, sizePx), Animatable {

    private var running = false

    /** How many times it was started and stopped. */
    var starts = 0
        private set
    var stops = 0
        private set

    override fun start() {
        starts++
        running = true
    }

    override fun stop() {
        stops++
        running = false
    }

    override fun isRunning(): Boolean = running
}

/**
 * Dev flavour only: the [EmoteImageSource] the parity harness gives TwitchEmoteImageLoader, so that
 * what an emote row shows depends on nothing outside the harness.
 *
 * No network, no decoder, no executor and no clock. Every image is a [FixedEmote] drawn in code from
 * what its URL names. The animated version of an id in [staticOnly] fails as Twitch's 404 does, and
 * every other request succeeds.
 *
 * Delivery is on the caller's thread, the main one: inside [load], as a Glide memory-cache hit
 * delivers inside into(), or, while [holdDeliveries] is set, only when the harness calls
 * [deliverPending]. A cleared load delivers nothing afterwards and its sink is told it was cleared,
 * whether or not it had delivered: the contract Glide keeps (SingleRequest.clear calls
 * onLoadCleared, and EngineJob delivers only to a callback still registered).
 *
 * Every load is recorded with the [label] the harness set when it was requested, so a check can ask
 * which bind's loads are still live. A load requested while another is delivering - the static
 * image after a 404 - carries the label of the load that asked for it.
 */
internal class FixedEmoteImageSource(private val staticOnly: Set<String>) : EmoteImageSource {

    /** Whether loads wait for [deliverPending] instead of delivering inside [load]. */
    var holdDeliveries = false

    /** Recorded with every load requested from now on. */
    var label = "unlabelled"

    /** Every load requested of this source, in order. */
    val loads = mutableListOf<Load>()

    private var delivering: Load? = null

    /** One load, from its request to its clear. */
    inner class Load(
        val request: EmoteImageRequest,
        val image: FixedEmote,
        val label: String,
        private val sink: EmoteImageSink
    ) : EmoteImageLoad {

        /** The drawable it delivered, or null while it has delivered none. */
        var delivered: Drawable? = null
            private set

        /** Whether it failed: the animated version of an id declared static-only. */
        var failed = false
            private set

        /** Whether it was cleared. */
        var cleared = false
            private set

        /** Requested and neither delivered, failed nor cleared. */
        val pending: Boolean
            get() = !cleared && !failed && delivered == null

        /** Delivered and not cleared: its image may be in a view. */
        val live: Boolean
            get() = !cleared && delivered != null

        override fun clear() {
            if (cleared) return
            cleared = true
            sink.onCleared()
        }

        /* Delivers this pending load: the image, or the 404 its declaration gives it. */
        fun deliver() {
            check(pending) { "a load of ${request.url} was delivered twice, or after it was cleared" }
            val outer = delivering
            delivering = this
            try {
                if (request.format == TwitchEmoteFormat.ANIMATED && image.id in staticOnly) {
                    failed = true
                    sink.onFailed(missingAnimatedVariant = true)
                } else {
                    val drawable = image.drawable(request.renderSizePx)
                    delivered = drawable
                    sink.onReady(drawable)
                }
            } finally {
                delivering = outer
            }
        }
    }

    override fun load(request: EmoteImageRequest, sink: EmoteImageSink): EmoteImageLoad {
        val load = Load(request, FixedEmote.of(request), delivering?.label ?: label, sink)
        loads += load
        if (!holdDeliveries) load.deliver()
        return load
    }

    /** Delivers every pending load in the order requested, including any it requests on the way. */
    fun deliverPending() {
        while (true) {
            val next = loads.firstOrNull { load -> load.pending } ?: return
            next.deliver()
        }
    }

    /** The loads recorded with [label]. */
    fun loadsOf(label: String): List<Load> = loads.filter { load -> load.label == label }
}
