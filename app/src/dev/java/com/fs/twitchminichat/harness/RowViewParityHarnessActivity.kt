package com.fs.twitchminichat.harness

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Process
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
import com.fs.twitchminichat.chat.ChatMessageRow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.reflect.InvocationTargetException

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
 *
 * Each pass then runs [RebindProof]: every pair of catalogue rows of the same kind, bound one after
 * the other to one view, against the second built fresh, with its own controls.
 *
 * The emote passes follow, on pages whose loader draws from a [FixedEmoteImageSource] instead of
 * Glide: [EmoteLayerPass], each emote row against the frozen view plus [FrozenEmoteLayer4804609],
 * once with every image available and once with the animated version answering 404; then
 * [EmoteLifecycleProof], on rows attached to this window. Before any of them, [RenderSizeMeasurement]
 * records what decides mutant A18. The parity pass itself runs with no loader, as before, and the run
 * checks that its page has none.
 *
 * Two optional extras: `--es passes emote` runs only the measurement and the emote passes, and
 * `--es passes measure` only the measurement; `--ef fontScale 1.3` runs at that font scale instead
 * of the phone's. The activity applies the font scale to itself by restarting once with it, and stops
 * if the scale it then runs at is not the one asked for.
 *
 * Only the adb shell may start it. An activity started by `am start` cannot learn its caller: the
 * referrer can be spoofed, the calling package exists only for startActivityForResult, and the
 * launching uid exists only from API 34 and only when the caller chooses to share it. So the system
 * checks the caller instead: the manifest entry requires [REQUIRED_PERMISSION], which the shell
 * holds and an ordinary app cannot be granted. The activity refuses to run if that requirement is
 * ever missing from its own manifest entry, and, where the platform reports a launching uid at all,
 * if that uid is neither the shell's nor root's.
 *
 * Every member reached by reflection is resolved before anything is rendered; a missing one stops
 * the run, and the results file names it.
 */
class RowViewParityHarnessActivity : AppCompatActivity() {

    private lateinit var themeName: String

    /* Applied before anything reads this activity's resources; set by the first start, read by the restart. */
    override fun attachBaseContext(newBase: Context) {
        val scale = fontScaleToApply
        super.attachBaseContext(
            if (scale == null) {
                newBase
            } else {
                newBase.createConfigurationContext(Configuration(newBase.resources.configuration).apply { fontScale = scale })
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        themeName = if (intent.getStringExtra(EXTRA_THEME) == "night") "night" else "day"
        delegate.localNightMode = if (themeName == "night") {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        super.onCreate(savedInstanceState)

        refusal()?.let { reason ->
            Log.w(TAG, "refused: $reason")
            writeResult(JSONObject().put("refused", reason))
            finish()
            return
        }

        val requestedScale = intent.getFloatExtra(EXTRA_FONT_SCALE, 0f).takeIf { it > 0f }
        if (requestedScale != null && fontScaleToApply != requestedScale) {
            fontScaleToApply = requestedScale
            /*
             * Posted, so that it runs once this start has resumed. Called from onCreate itself it
             * did nothing on the test phone (Android 12): the activity stayed resumed with no
             * content and nothing in the log said why.
             */
            window.decorView.post { recreate() }
            return
        }
        if (requestedScale != null && resources.configuration.fontScale != requestedScale) {
            val reason = "asked for font scale $requestedScale, running at ${resources.configuration.fontScale}"
            Log.e(TAG, "aborted: $reason")
            writeResult(JSONObject().put("aborted", reason))
            finish()
            return
        }

        val root = FrameLayout(this)
        setContentView(root)
        root.doOnLayout { run(width = root.width, root = root) }
    }

    /* Why this start must not run, or null when it may. */
    private fun refusal(): String? {
        val component = ComponentName(this, javaClass)
        val declared = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getActivityInfo(component, PackageManager.ComponentInfoFlags.of(0L)).permission
        } else {
            @Suppress("DEPRECATION")
            packageManager.getActivityInfo(component, 0).permission
        }
        if (declared != REQUIRED_PERMISSION) {
            return "its manifest entry requires $declared instead of $REQUIRED_PERMISSION, so any app could start it"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val uid = launchedFromUid
            if (uid != Process.INVALID_UID && uid != Process.SHELL_UID && uid != Process.ROOT_UID) {
                return "started by uid $uid, which is neither the adb shell nor root"
            }
        }
        return null
    }

    private fun run(width: Int, root: FrameLayout) {
        val targets = try {
            ReflectiveTargets.resolve()
        } catch (missing: MissingReflectiveTarget) {
            Log.e(TAG, "aborted: ${missing.message}")
            writeResult(
                JSONObject()
                    .put("aborted", missing.message)
                    .put("reflectiveTargets", JSONArray(ReflectiveTargets.descriptions))
            )
            finish()
            return
        }
        try {
            runResolved(width, root, targets)
        } catch (failure: Exception) {
            abort(failure)
        }
    }

    private fun abort(failure: Exception) {
        val cause = (failure as? InvocationTargetException)?.targetException ?: failure
        Log.e(TAG, "aborted: ${cause.javaClass.name}: ${cause.message}")
        writeResult(JSONObject().put("aborted", "${cause.javaClass.name}: ${cause.message}"))
        finish()
    }

    private fun runResolved(width: Int, root: FrameLayout, targets: ReflectiveTargets) {
        val snapshot = ViewStateSnapshot(targets)
        val passes = intent.getStringExtra(EXTRA_PASSES) ?: PASSES_ALL
        check(passes in setOf(PASSES_ALL, PASSES_EMOTE, PASSES_MEASURE)) { "unknown passes extra: $passes" }
        val report = JSONObject()
        report.put("commit", BuildConfig.GIT_SHA)
        report.put("themeRequested", themeName)
        report.put("nightModeActive", isNightModeActive())
        report.put("passes", passes)
        report.put("widthPx", width)
        report.put("density", resources.displayMetrics.density.toDouble())
        report.put("fontScale", resources.configuration.fontScale.toDouble())
        report.put("fontScaleRequested", intent.getFloatExtra(EXTRA_FONT_SCALE, 0f).takeIf { it > 0f }?.toDouble() ?: JSONObject.NULL)
        report.put("sdk", Build.VERSION.SDK_INT)
        report.put("reflectiveTargets", JSONArray(ReflectiveTargets.descriptions))

        report.put("renderSize", RenderSizeMeasurement(ProductionRowViews(this, ParityCatalogue.SIGNED_IN, targets)).measure(this))
        if (passes == PASSES_MEASURE) {
            finishWith(report, "renderSize measured")
            return
        }
        if (passes == PASSES_ALL) {
            textPasses(width, targets, snapshot, report)
        }
        emotePasses(width, root, targets, snapshot, report)
    }

    /* The parity pass, its controls, the theme defaults and the rebind proof. */
    private fun textPasses(width: Int, targets: ReflectiveTargets, snapshot: ViewStateSnapshot, report: JSONObject) {
        val output = File(filesDir, "parity-harness").apply { mkdirs() }
        val frozen = FrozenRowViews4804609(this, ParityCatalogue.SIGNED_IN)
        val production = ProductionRowViews(this, ParityCatalogue.SIGNED_IN, targets)

        val rows = JSONArray()
        var identical = 0
        ParityCatalogue.rows.forEachIndexed { index, row ->
            val result = compareRow(row.id, width, output, snapshot) { side, container ->
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
        /* The parity pass compares text alone: neither of its paths may have a loader. */
        report.put("parityPageHasNoEmoteLoader", production.emoteLoader == null)
        report.put("controls", controls(width, frozen, targets, snapshot))
        report.put("themeDefaults", themeDefaults(production, snapshot))

        /* On a fragment of its own, so that its producers accept every catalogue row once more. */
        val rebindPage = ProductionRowViews(this, ParityCatalogue.SIGNED_IN, targets)
        val rebindImages = FixedEmoteImageSource(staticOnly = emptySet())
        rebindPage.installEmoteLoader(rebindImages, EmoteRowRecorder())
        val rebind = RebindProof(
            production = rebindPage,
            images = rebindImages,
            snapshot = snapshot,
            newContainer = ::newContainer,
            render = { container -> render(container, width) }
        ).run()
        report.put("rebindProof", rebind)
    }

    /* The emote layer with and without a 404, its controls, then the lifecycle proof, which runs across loop turns. */
    private fun emotePasses(width: Int, root: FrameLayout, targets: ReflectiveTargets, snapshot: ViewStateSnapshot, report: JSONObject) {
        val render = { container: ViewGroup -> render(container, width) }
        val layer = EmoteLayerPass(this, targets, snapshot, isNightModeActive(), ::newContainer, render)
        report.put("emoteLayer", layer.run(staticOnly = emptySet()))
        report.put("emoteLayerWith404", layer.run(staticOnly = setOf(KAPPA)))
        report.put("emoteLayerControls", layer.controls())

        /* The rows' data as their producer tags them, from a page with no loader, so that nothing loads on the way. */
        val dataPage = ProductionRowViews(this, ParityCatalogue.SIGNED_IN, targets)
        val dataRows = (ParityCatalogue.emoteRows + EmoteLifecycleProof.PLAIN_ROW).associateWith { id ->
            val row = ParityCatalogue.rows.first { it.id == id }
            val container = newContainer().also { ParityCatalogue.produce(row, ParityCatalogue.rows.indexOf(row), dataPage, it) }
            checkNotNull(container.getChildAt(0)?.tag as? ChatMessageRow) { "the producer of $id left no message row" }
        }
        val frozen = FrozenRowViews4804609(this, ParityCatalogue.SIGNED_IN)
        val references = ParityCatalogue.emoteRows.associateWith { id ->
            val row = ParityCatalogue.rows.first { it.id == id } as ParityRow.Message
            val index = ParityCatalogue.rows.indexOf(row)
            val build: () -> LinearLayout = {
                layer.reference(row, index, frozen, FrozenEmoteLayer4804609(this, FixedEmoteImageSource(emptySet())))
            }
            build
        }

        val lifecyclePage = ProductionRowViews(this, ParityCatalogue.SIGNED_IN, targets)
        val lifecycleImages = FixedEmoteImageSource(staticOnly = emptySet()).apply { holdDeliveries = true }
        val recorder = EmoteRowRecorder()
        lifecyclePage.installEmoteLoader(lifecycleImages, recorder)
        EmoteLifecycleProof(
            root = root,
            page = lifecyclePage,
            images = lifecycleImages,
            recorder = recorder,
            rowData = dataRows,
            references = references,
            snapshot = snapshot,
            newContainer = ::newContainer,
            render = render
        ).start { lifecycle ->
            try {
                report.put("emoteLifecycle", lifecycle)
                finishWith(report, summaryOf(report))
            } catch (failure: Exception) {
                abort(failure)
            }
        }
    }

    /* Writes [report], logs the one line the stage script waits for, and finishes. */
    private fun finishWith(report: JSONObject, summary: String) {
        writeResult(report)
        Log.i(TAG, "done theme=$themeName $summary")
        finish()
    }

    private fun summaryOf(report: JSONObject): String {
        val parts = mutableListOf<String>()
        if (report.has("rowsTotal")) {
            parts += "rows=${report.getInt("rowsTotal")} identical=${report.getInt("rowsIdentical")}"
            val rebind = report.getJSONObject("rebindProof")
            val pairs = rebind.getJSONObject("pairs")
            parts += "rebindPairs=${pairs.getJSONObject("vacuity").getInt("pairs")} rebindIdentical=${pairs.getInt("identical")} " +
                "sessionsHeld=${pairs.getInt("sessionsHeld")} imagesHeld=${pairs.getInt("imagesHeld")} " +
                "rebindPassed=${rebind.getBoolean("passed")}"
        }
        val layer = report.getJSONObject("emoteLayer")
        val layer404 = report.getJSONObject("emoteLayerWith404")
        parts += "emoteLayer=${layer.getInt("rowsIdentical")}/${layer.getInt("rowsTotal")}"
        parts += "emoteLayerWith404=${layer404.getInt("rowsIdentical")}/${layer404.getInt("rowsTotal")}"
        val lifecycle = report.getJSONObject("emoteLifecycle")
        parts += "lifecycle=${lifecycle.getInt("passed")}/${lifecycle.getInt("expectedSteps")}"
        return parts.joinToString(" ")
    }

    private fun writeResult(result: JSONObject) {
        val output = File(filesDir, "parity-harness").apply { mkdirs() }
        File(output, "$themeName.json").writeText(result.toString(2))
    }

    /* One row through both paths: child count, pixels of every child, and their state. */
    private fun compareRow(
        id: String,
        width: Int,
        output: File,
        snapshot: ViewStateSnapshot,
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
                .map { snapshot.of(productionContainer.getChildAt(it)) }
            val frozenState = (0 until frozenContainer.childCount)
                .map { snapshot.of(frozenContainer.getChildAt(it)) }
            val stateDiff = snapshot.diff(productionState, frozenState)
            result.put("stateIdentical", stateDiff.isEmpty())
            result.put("stateDiff", JSONArray(stateDiff))
            result.put("productionState", JSONObject.wrap(productionState))
        } catch (thrown: InvocationTargetException) {
            /* A producer itself threw for this row: recorded as a failed row, never as a match. */
            val cause = thrown.targetException
            result.put("bitmapIdentical", false)
            result.put("stateIdentical", false)
            result.put("error", "${cause.javaClass.name}: ${cause.message}")
        }
        return result
    }

    /* Two rows a character apart; a cleared focusable flag; one production row built twice. */
    private fun controls(
        width: Int,
        frozen: FrozenRowViews4804609,
        targets: ReflectiveTargets,
        snapshot: ViewStateSnapshot
    ): JSONObject {
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
        val flagState = snapshot.diff(
            snapshot.of(plain.getChildAt(0)),
            snapshot.of(unfocusable.getChildAt(0))
        )
        controls.put("focusableCleared_stateDiffers", flagState.isNotEmpty())
        controls.put("focusableCleared_pixelsIdentical", flagPixels.identical)

        /*
         * Each build on a fragment of its own: one fragment's deduplicator keeps every stable id it
         * has accepted, so the same row built twice on it would be suppressed the second time.
         */
        val row = ParityCatalogue.rows.first { it.id == "msg.mention" }
        val first = newContainer().also {
            ParityCatalogue.produce(row, 0, ProductionRowViews(this, ParityCatalogue.SIGNED_IN, targets), it)
        }
        val second = newContainer().also {
            ParityCatalogue.produce(row, 0, ProductionRowViews(this, ParityCatalogue.SIGNED_IN, targets), it)
        }
        check(first.childCount == 1 && second.childCount == 1) {
            "control productionTwice: the builds produced ${first.childCount} and ${second.childCount} rows, not 1 each"
        }
        val repeatPixels = PixelDiff.of(render(first, width), render(second, width))
        val repeatState = snapshot.diff(
            snapshot.of(first.getChildAt(0)),
            snapshot.of(second.getChildAt(0))
        )
        controls.put("productionTwice_pixelsIdentical", repeatPixels.identical)
        controls.put("productionTwice_stateIdentical", repeatState.isEmpty())
        controls.put("productionTwice_stateDiff", JSONArray(repeatState))
        return controls
    }

    /* Each view kind as its constructor leaves it, before anything sets it up. */
    private fun themeDefaults(production: ProductionRowViews, snapshot: ViewStateSnapshot): JSONObject = JSONObject()
        .put("messageView (ChatFragment.SwipeReplyTextView)", JSONObject.wrap(snapshot.defaultsOf(production.freshMessageView())))
        .put("systemLine (TextView)", JSONObject.wrap(snapshot.defaultsOf(TextView(this))))
        .put("AppCompatTextView", JSONObject.wrap(snapshot.defaultsOf(AppCompatTextView(this))))
        .put("pendingEcho (PendingEchoView, after its init)", JSONObject.wrap(snapshot.defaultsOf(PendingEchoView(this, TextView(this)))))
        .put("LinearLayout", JSONObject.wrap(snapshot.defaultsOf(LinearLayout(this))))

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
        const val EXTRA_PASSES = "passes"
        const val EXTRA_FONT_SCALE = "fontScale"
        const val PASSES_ALL = "all"
        const val PASSES_EMOTE = "emote"
        const val PASSES_MEASURE = "measure"
        const val PRODUCTION = "production"
        const val FROZEN = "frozen"

        /* The emote id of the catalogue's emote rows, declared static-only for the pass with a 404. */
        const val KAPPA = "25"

        /* The font scale the restart applies; null until a start asks for one. */
        var fontScaleToApply: Float? = null

        /* Held by the adb shell; an ordinary app cannot be granted it. The manifest entry requires it. */
        const val REQUIRED_PERMISSION = Manifest.permission.DUMP
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
