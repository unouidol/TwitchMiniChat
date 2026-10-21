package com.fs.twitchminichat.harness

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.fs.twitchminichat.AnimatedEmoteRow
import com.fs.twitchminichat.AnimatedEmoteRowListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stands in for the page's animation gate: it wants every row's animations from the moment the row
 * is reported, so that whether an emote runs depends only on whether its row is attached, and it
 * records which rows were reported and released.
 */
internal class EmoteRowRecorder : AnimatedEmoteRowListener {

    /** Every row reported as holding animated emotes, in order. */
    val added = mutableListOf<AnimatedEmoteRow>()

    /** Every row reported as released, in order. */
    val released = mutableListOf<AnimatedEmoteRow>()

    override fun onAnimatedRowAdded(row: AnimatedEmoteRow) {
        added += row
        row.setAnimationsWanted(true)
    }

    override fun onAnimatedRowReleased(row: AnimatedEmoteRow) {
        released += row
    }

    /** How many times a row whose view is [view] was released. */
    fun releasesOf(view: View): Int = released.count { row -> row.view === view }
}

/**
 * The emote-layer pass: each catalogue row with emotes, built by ChatFragment's producer on a page
 * whose loader draws from a [FixedEmoteImageSource], against the frozen view of that row plus
 * [FrozenEmoteLayer4804609] drawing from a source of its own with the same declaration. Pixels,
 * state - every image span with its drawable's bounds - and the requests each side made, in order.
 *
 * Its first run, on the commit that adds it and before 5c changes who owns a session, is the frozen
 * layer's calibration: production, given the same images, built the same views as the layer. That is
 * what licenses the layer as a faithful transcription of 4804609, and why it never changes after.
 *
 * Three controls run with it, because a comparison that cannot fail proves nothing: the reference with
 * its images must differ in pixels from the same frozen view without them; two emote ids must differ
 * in pixels; one production row built twice, on two pages with two sources, must match itself. A
 * fourth answers which instruments see a wrong render size: the reference against itself with every
 * image one pixel smaller.
 */
internal class EmoteLayerPass(
    private val activity: FragmentActivity,
    private val targets: ReflectiveTargets,
    private val snapshot: ViewStateSnapshot,
    private val pageView: View,
    private val darkTheme: Boolean,
    private val newContainer: () -> LinearLayout,
    private val render: (ViewGroup) -> Bitmap
) {

    /** The rows of [ParityCatalogue] this pass covers, with their index in the catalogue. */
    private val emoteRows: List<Pair<Int, ParityRow.Message>> = ParityCatalogue.rows
        .withIndex()
        .filter { (_, row) -> row.id in ParityCatalogue.emoteRows }
        .map { (index, row) -> index to row as ParityRow.Message }

    /** Production against the frozen layer for every emote row, the animated version of [staticOnly] ids failing as a 404. */
    fun run(staticOnly: Set<String>): JSONObject {
        val productionImages = FixedEmoteImageSource(staticOnly)
        val production = ProductionRowViews(activity, ParityCatalogue.SIGNED_IN, targets)
        production.installEmoteLoader(productionImages, pageView, EmoteRowRecorder())
        val frozen = FrozenRowViews4804609(activity, ParityCatalogue.SIGNED_IN)
        val frozenImages = FixedEmoteImageSource(staticOnly)
        val layer = FrozenEmoteLayer4804609(activity, frozenImages)

        val out = JSONObject()
        out.put("staticOnly", JSONArray(staticOnly.sorted()))
        out.put("renderSizePxFrozen", layer.renderSizePx())
        out.put("loaderPresent", production.emoteLoader != null)
        val rows = JSONArray()
        var identical = 0
        for ((index, row) in emoteRows) {
            val result = JSONObject().put("id", row.id)
            productionImages.label = row.id
            val productionContainer = newContainer().also { ParityCatalogue.produce(row, index, production, it) }
            frozenImages.label = row.id
            val frozenContainer = reference(row, index, frozen, layer)

            val pixels = compare(productionContainer, frozenContainer)
            val productionState = snapshot.of(productionContainer.getChildAt(0))
            val stateDiff = snapshot.diff(productionState, snapshot.of(frozenContainer.getChildAt(0)))
            val productionRequests = requestsOf(productionImages, row.id)
            val frozenRequests = requestsOf(frozenImages, row.id)
            val emotes = ParityCatalogue.emoteCount(row.emotesRaw)
            val productionShown = productionImages.loadsOf(row.id).count { load -> load.live }
            val frozenShown = frozenImages.loadsOf(row.id).count { load -> load.live }

            result.put("childCount", "${productionContainer.childCount}/${frozenContainer.childCount}")
            result.put("bitmapIdentical", pixels.identical)
            result.put("bitmapDifference", pixels.describe())
            result.put("stateIdentical", stateDiff.isEmpty())
            result.put("stateDiff", JSONArray(stateDiff.take(12)))
            result.put("requestsIdentical", productionRequests == frozenRequests)
            result.put("productionRequests", JSONArray(productionRequests))
            result.put("frozenRequests", JSONArray(frozenRequests))
            result.put("emotes", emotes)
            result.put("imagesDeliveredProductionFrozen", "$productionShown/$frozenShown")
            result.put("imageSpans", JSONArray((productionState["spans"] as List<*>).filter { it.toString().startsWith("ImageSpan") }))
            val rowPassed = pixels.identical && stateDiff.isEmpty() && productionRequests == frozenRequests &&
                productionShown == emotes && frozenShown == emotes && productionContainer.childCount == 1 &&
                frozenContainer.childCount == 1
            result.put("passed", rowPassed)
            if (rowPassed) identical++
            rows.put(result)
        }
        out.put("rows", rows)
        out.put("rowsTotal", emoteRows.size)
        out.put("rowsIdentical", identical)
        out.put("imageFingerprints", fingerprints(productionImages))
        out.put("passed", emoteRows.size == EXPECTED_EMOTE_ROWS && identical == emoteRows.size)
        return out
    }

    /** The controls, on the first emote row, with images that never fail. */
    fun controls(): JSONObject {
        val (index, row) = emoteRows.first()
        val out = JSONObject()
        val frozen = FrozenRowViews4804609(activity, ParityCatalogue.SIGNED_IN)

        val withImages = reference(row, index, frozen, FrozenEmoteLayer4804609(activity, FixedEmoteImageSource(emptySet())))
        val withoutImages = newContainer().also { ParityCatalogue.freeze(row, index, frozen, it) }
        out.put("imagesShown_pixelsDiffer", !compare(withImages, withoutImages).identical)

        val otherId = row.copy(emotesRaw = row.emotesRaw!!.replace(Regex("^[^:]+"), OTHER_EMOTE_ID))
        val withOtherId = reference(otherId, index, frozen, FrozenEmoteLayer4804609(activity, FixedEmoteImageSource(emptySet())))
        out.put("otherEmoteId_pixelsDiffer", !compare(withImages, withOtherId).identical)

        val first = builtOnItsOwnPage(row, index)
        val second = builtOnItsOwnPage(row, index)
        val twicePixels = compare(first, second)
        val twiceState = snapshot.diff(snapshot.of(first.getChildAt(0)), snapshot.of(second.getChildAt(0)))
        out.put("productionTwice_pixelsIdentical", twicePixels.identical)
        out.put("productionTwice_stateIdentical", twiceState.isEmpty())
        out.put("productionTwice_stateDiff", JSONArray(twiceState.take(8)))

        /* A1: which instruments see a render size one pixel off. */
        val smallerImages = FixedEmoteImageSource(emptySet())
        val smallerLayer = FrozenEmoteLayer4804609(activity, smallerImages)
        val size = smallerLayer.renderSizePx()
        val referenceImages = FixedEmoteImageSource(emptySet())
        val reference = newContainer().also { container ->
            ParityCatalogue.freeze(row, index, frozen, container)
            FrozenEmoteLayer4804609(activity, referenceImages).apply(container.getChildAt(0) as TextView, row.emotesRaw!!, darkTheme)
        }
        val smaller = newContainer().also { container ->
            ParityCatalogue.freeze(row, index, frozen, container)
            smallerLayer.apply(container.getChildAt(0) as TextView, row.emotesRaw!!, darkTheme, renderSizePx = size - 1)
        }
        val sizeState = snapshot.diff(snapshot.of(reference.getChildAt(0)), snapshot.of(smaller.getChildAt(0)))
        out.put("onePixelSmaller_renderSizesPx", "$size/${size - 1}")
        out.put("onePixelSmaller_pixelsDiffer", !compare(reference, smaller).identical)
        out.put("onePixelSmaller_stateDiffers", sizeState.isNotEmpty())
        out.put("onePixelSmaller_stateDiff", JSONArray(sizeState.take(4)))
        out.put("onePixelSmaller_requestsDiffer", requestsOf(referenceImages, null) != requestsOf(smallerImages, null))

        out.put(
            "behaved",
            out.getBoolean("imagesShown_pixelsDiffer") && out.getBoolean("otherEmoteId_pixelsDiffer") &&
                twicePixels.identical && twiceState.isEmpty() && out.getBoolean("onePixelSmaller_pixelsDiffer") &&
                out.getBoolean("onePixelSmaller_stateDiffers") && out.getBoolean("onePixelSmaller_requestsDiffer")
        )
        return out
    }

    /** The frozen view of [row] with the frozen layer's emotes, in a container of its own. */
    fun reference(row: ParityRow.Message, index: Int, frozen: FrozenRowViews4804609, layer: FrozenEmoteLayer4804609): LinearLayout =
        newContainer().also { container ->
            ParityCatalogue.freeze(row, index, frozen, container)
            layer.apply(container.getChildAt(0) as TextView, row.emotesRaw!!, darkTheme)
        }

    /* [row] through its producer on a page of its own, whose loader draws from a source of its own. */
    private fun builtOnItsOwnPage(row: ParityRow.Message, index: Int): LinearLayout {
        val page = ProductionRowViews(activity, ParityCatalogue.SIGNED_IN, targets)
        page.installEmoteLoader(FixedEmoteImageSource(emptySet()), pageView, EmoteRowRecorder())
        return newContainer().also { ParityCatalogue.produce(row, index, page, it) }
    }

    /* Every request [images] recorded with [label], or every request when [label] is null: format, size and URL. */
    private fun requestsOf(images: FixedEmoteImageSource, label: String?): List<String> =
        (if (label == null) images.loads else images.loadsOf(label))
            .map { load -> "${load.request.format} ${load.request.renderSizePx} ${load.request.url}" }

    private fun fingerprints(images: FixedEmoteImageSource): JSONObject {
        val out = JSONObject()
        images.loads.map { load -> load.image }.distinct().forEach { image ->
            out.put("${image.id}/${image.format}/${image.theme}", image.fingerprint())
        }
        return out
    }

    private fun compare(a: ViewGroup, b: ViewGroup): PixelDiff {
        val left = render(a)
        val right = render(b)
        return PixelDiff.of(left, right).also {
            left.recycle()
            right.recycle()
        }
    }

    companion object {
        /** msg.emotes and msg.emotes.reply.mention. A change to the catalogue changes it, on purpose. */
        const val EXPECTED_EMOTE_ROWS = 2

        /** An emote id the catalogue never uses, for the control that two ids differ. */
        const val OTHER_EMOTE_ID = "1902"
    }
}
