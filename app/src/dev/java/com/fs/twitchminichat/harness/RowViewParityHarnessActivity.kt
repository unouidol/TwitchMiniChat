package com.fs.twitchminichat.harness

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.createBitmap
import androidx.core.view.doOnLayout
import com.fs.twitchminichat.BuildConfig
import com.fs.twitchminichat.PendingEchoView
import com.fs.twitchminichat.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Dev flavour only: renders every row of [ParityCatalogue] through ChatFragment's producers and
 * through [FrozenRowViews4804609], and compares the two, pixel for pixel and field by field.
 *
 * Started by adb, never from the app:
 * `adb shell am start -n com.fs.twitchminichat.dev/com.fs.twitchminichat.harness.RowViewParityHarnessActivity --es theme night`
 * (`day` when the extra is absent). Each row is laid out at this window's width, which is the chat
 * column's: nothing between the window and chatContainer has horizontal padding. The results go to
 * files/parity-harness/<theme>.json, with PNGs of both paths for every row whose pixels differ, and
 * one line is logged under [TAG] when it is done. Then the activity finishes.
 *
 * Three controls run with every pass, because a comparison that cannot fail proves nothing: two
 * rows a character apart must differ in pixels; a row whose focusable flag is cleared must differ in
 * state and not in pixels; one production row built twice must match itself.
 */
class RowViewParityHarnessActivity : AppCompatActivity() {

    private lateinit var themeName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        themeName = if (intent.getStringExtra(EXTRA_THEME) == "night") "night" else "day"
        delegate.localNightMode = if (themeName == "night") {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        setContentView(root)
        root.doOnLayout { run(width = root.width) }
    }

    private fun run(width: Int) {
        val output = File(filesDir, "parity-harness").apply { mkdirs() }
        val report = JSONObject()
        report.put("commit", BuildConfig.GIT_SHA)
        report.put("themeRequested", themeName)
        report.put("nightModeActive", isNightModeActive())
        report.put("widthPx", width)
        report.put("density", resources.displayMetrics.density.toDouble())
        report.put("fontScale", resources.configuration.fontScale.toDouble())
        report.put("sdk", Build.VERSION.SDK_INT)

        val frozen = FrozenRowViews4804609(this, ParityCatalogue.SIGNED_IN)
        val production = ProductionRowViews(this, ParityCatalogue.SIGNED_IN)

        val rows = JSONArray()
        var identical = 0
        ParityCatalogue.rows.forEachIndexed { index, row ->
            val result = compareRow(row.id, width, output) { side, container ->
                if (side == PRODUCTION) {
                    ParityCatalogue.produce(row, index, production, container)
                } else {
                    ParityCatalogue.freeze(row, index, frozen, container)
                }
            }
            if (result.optBoolean("bitmapIdentical") && result.optBoolean("stateIdentical")) identical++
            rows.put(result)
        }
        report.put("rows", rows)
        report.put("rowsIdentical", identical)
        report.put("rowsTotal", ParityCatalogue.rows.size)
        report.put("emoteRowsExcludedFromRebindUntil5c", JSONArray(ParityCatalogue.emoteRows))
        report.put("controls", controls(width, frozen, production))
        report.put("themeDefaults", themeDefaults(production))

        File(output, "$themeName.json").writeText(report.toString(2))
        Log.i(TAG, "done theme=$themeName rows=${ParityCatalogue.rows.size} identical=$identical width=$width")
        finish()
    }

    /* One row through both paths: child count, pixels of every child, and their state. */
    private fun compareRow(
        id: String,
        width: Int,
        output: File,
        build: (String, ViewGroup) -> Unit
    ): JSONObject {
        val result = JSONObject().put("id", id)
        try {
            val productionContainer = newContainer().also { build(PRODUCTION, it) }
            val frozenContainer = newContainer().also { build(FROZEN, it) }
            val productionBitmap = render(productionContainer, width)
            val frozenBitmap = render(frozenContainer, width)

            val pixels = PixelDiff.of(productionBitmap, frozenBitmap)
            result.put("childCount", "${productionContainer.childCount}/${frozenContainer.childCount}")
            result.put("bitmapIdentical", pixels.identical)
            result.put("bitmapDifference", pixels.describe())
            if (!pixels.identical) {
                save(productionBitmap, File(output, "$themeName-$id-production.png"))
                save(frozenBitmap, File(output, "$themeName-$id-frozen.png"))
            }

            val productionState = (0 until productionContainer.childCount)
                .map { ViewStateSnapshot.of(productionContainer.getChildAt(it)) }
            val frozenState = (0 until frozenContainer.childCount)
                .map { ViewStateSnapshot.of(frozenContainer.getChildAt(it)) }
            val stateDiff = ViewStateSnapshot.diff(productionState, frozenState)
            result.put("stateIdentical", stateDiff.isEmpty())
            result.put("stateDiff", JSONArray(stateDiff))
            result.put("productionState", JSONObject.wrap(productionState))
        } catch (failure: Exception) {
            val cause = (failure as? java.lang.reflect.InvocationTargetException)?.targetException ?: failure
            result.put("bitmapIdentical", false)
            result.put("stateIdentical", false)
            result.put("error", "${cause.javaClass.name}: ${cause.message}")
        }
        return result
    }

    /* Two rows a character apart; a cleared focusable flag; one production row built twice. */
    private fun controls(width: Int, frozen: FrozenRowViews4804609, production: ProductionRowViews): JSONObject {
        val controls = JSONObject()

        val one = newContainer().apply { addView(frozen.chatMessage("parity_alice", "hello there", null, "c1", null, 1.0)) }
        val other = newContainer().apply { addView(frozen.chatMessage("parity_alice", "hello therf", null, "c1", null, 1.0)) }
        val textPixels = PixelDiff.of(render(one, width), render(other, width))
        controls.put("oneCharacterApart_pixelsDiffer", !textPixels.identical)

        val plain = newContainer().apply { addView(frozen.chatMessage("parity_alice", "hello there", null, "c2", null, 1.0)) }
        val unfocusable = newContainer().apply {
            addView(frozen.chatMessage("parity_alice", "hello there", null, "c2", null, 1.0).apply { isFocusable = false })
        }
        val flagPixels = PixelDiff.of(render(plain, width), render(unfocusable, width))
        val flagState = ViewStateSnapshot.diff(
            ViewStateSnapshot.of(plain.getChildAt(0)),
            ViewStateSnapshot.of(unfocusable.getChildAt(0))
        )
        controls.put("focusableCleared_stateDiffers", flagState.isNotEmpty())
        controls.put("focusableCleared_pixelsIdentical", flagPixels.identical)

        val row = ParityCatalogue.rows.first { it.id == "msg.mention" }
        val first = newContainer().also { ParityCatalogue.produce(row, 0, production, it) }
        val second = newContainer().also { ParityCatalogue.produce(row, 0, production, it) }
        val repeatPixels = PixelDiff.of(render(first, width), render(second, width))
        val repeatState = ViewStateSnapshot.diff(
            ViewStateSnapshot.of(first.getChildAt(0)),
            ViewStateSnapshot.of(second.getChildAt(0))
        )
        controls.put("productionTwice_pixelsIdentical", repeatPixels.identical)
        controls.put("productionTwice_stateIdentical", repeatState.isEmpty())
        controls.put("productionTwice_stateDiff", JSONArray(repeatState))
        return controls
    }

    /* Each view kind as its constructor leaves it, before anything sets it up. */
    private fun themeDefaults(production: ProductionRowViews): JSONObject = JSONObject()
        .put("messageView (ChatFragment.SwipeReplyTextView)", JSONObject.wrap(ViewStateSnapshot.defaultsOf(production.freshMessageView())))
        .put("systemLine (TextView)", JSONObject.wrap(ViewStateSnapshot.defaultsOf(TextView(this))))
        .put("AppCompatTextView", JSONObject.wrap(ViewStateSnapshot.defaultsOf(AppCompatTextView(this))))
        .put("pendingEcho (PendingEchoView, after its init)", JSONObject.wrap(ViewStateSnapshot.defaultsOf(PendingEchoView(this))))
        .put("LinearLayout", JSONObject.wrap(ViewStateSnapshot.defaultsOf(LinearLayout(this))))

    private fun newContainer(): LinearLayout =
        layoutInflater.inflate(R.layout.parity_harness_container, FrameLayout(this), false) as LinearLayout

    /* Laid out at the chat column's width and drawn in software; the parent applies each child's alpha. */
    private fun render(container: ViewGroup, width: Int): Bitmap {
        container.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        container.layout(0, 0, container.measuredWidth, container.measuredHeight)
        val bitmap = createBitmap(width, container.measuredHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        container.draw(Canvas(bitmap))
        return bitmap
    }

    private fun save(bitmap: Bitmap, file: File) {
        file.outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
    }

    private fun isNightModeActive(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private companion object {
        const val TAG = "PARITY_HARNESS"
        const val EXTRA_THEME = "theme"
        const val PRODUCTION = "production"
        const val FROZEN = "frozen"
    }
}

/** Whether two bitmaps match exactly, and if not, how many pixels differ and where. */
internal class PixelDiff private constructor(
    val identical: Boolean,
    private val description: String
) {
    fun describe(): String = description

    companion object {
        fun of(a: Bitmap, b: Bitmap): PixelDiff {
            if (a.width != b.width || a.height != b.height) {
                return PixelDiff(false, "size ${a.width}x${a.height} vs ${b.width}x${b.height}")
            }
            val left = IntArray(a.width * a.height).also { a.getPixels(it, 0, a.width, 0, 0, a.width, a.height) }
            val right = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }
            var count = 0
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = -1
            var maxY = -1
            for (i in left.indices) {
                if (left[i] != right[i]) {
                    count++
                    val x = i % a.width
                    val y = i / a.width
                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                }
            }
            return if (count == 0) {
                PixelDiff(true, "identical ${a.width}x${a.height}")
            } else {
                PixelDiff(false, "$count pixels differ within x=$minX..$maxX y=$minY..$maxY of ${a.width}x${a.height}")
            }
        }
    }
}
