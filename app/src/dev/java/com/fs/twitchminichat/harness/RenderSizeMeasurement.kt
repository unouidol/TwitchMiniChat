package com.fs.twitchminichat.harness

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import com.fs.twitchminichat.R
import com.fs.twitchminichat.TwitchEmoteUrlFactory
import org.json.JSONArray
import org.json.JSONObject

/**
 * What decides mutant A18, measured rather than reasoned: whether reading a message view's text size
 * before the applier sets it would give its emotes another render size.
 *
 * A18 reads the size of a view that has never been bound - the size its theme gives it - instead of
 * the 14 sp the applier sets. The two are computed differently: the theme's default is a dimension
 * resource, rounded to whole pixels when TextView reads it, and `textSize = 14f` is the exact
 * product of 14, the density and the font scale. So whether the render sizes, (size * 1.5)
 * truncated, differ depends on the density and the font scale, and is read here, never assumed.
 *
 * Read for the configuration the page runs in, then for each of [FONT_SCALES] through a context
 * that overrides only the font scale. A view bound a second time already carries 14 sp from its
 * first bind, so A18 can only change a view's first bind.
 */
internal class RenderSizeMeasurement(private val production: ProductionRowViews) {

    /** Every value item 5 of the 5c design names, at the page's configuration and at each override. */
    fun measure(context: Context): JSONObject {
        val out = JSONObject()
        out.put("atThisConfiguration", at(context, overridden = null))
        out.put("fontScaleOverrides", JSONArray(FONT_SCALES.map { scale -> at(withFontScale(context, scale), overridden = scale) }))
        return out
    }

    private fun at(context: Context, overridden: Float?): JSONObject {
        val metrics = context.resources.displayMetrics
        val configuration = context.resources.configuration
        val fresh = production.freshMessageView(context)
        val bound = production.freshMessageView(context).apply { textSize = MESSAGE_TEXT_SIZE_SP }
        val freshPx = fresh.textSize
        val boundPx = bound.textSize
        val fourteenSpPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE_SP, metrics)
        val renderFresh = (freshPx * 1.5f).toInt()
        val renderBound = (boundPx * 1.5f).toInt()
        return JSONObject()
            .put("fontScaleOverride", overridden?.toDouble() ?: JSONObject.NULL)
            .put("density", metrics.density.toDouble())
            .put("densityDpi", metrics.densityDpi)
            .put("fontScale", configuration.fontScale.toDouble())
            .put("scaledDensity", @Suppress("DEPRECATION") metrics.scaledDensity.toDouble())
            .put("nightMode", (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            .put("freshTextSizePx", freshPx.toDouble())
            .put("freshTextSizeUnit", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) unitName(fresh.textSizeUnit) else "unreadable below API 30")
            .put("fourteenSpPx", fourteenSpPx.toDouble())
            .put("boundTextSizePx", boundPx.toDouble())
            .put("renderSizePxFresh", renderFresh)
            .put("renderSizePxBound", renderBound)
            .put("urlScaleFresh", TwitchEmoteUrlFactory.scaleForRenderSize(renderFresh))
            .put("urlScaleBound", TwitchEmoteUrlFactory.scaleForRenderSize(renderBound))
            .put("renderSizesEqual", renderFresh == renderBound)
            .put("urlScalesEqual", TwitchEmoteUrlFactory.scaleForRenderSize(renderFresh) == TwitchEmoteUrlFactory.scaleForRenderSize(renderBound))
    }

    /* [base] with only its font scale replaced, and the app's theme over it, as the page's views are themed. */
    private fun withFontScale(base: Context, fontScale: Float): Context =
        ContextThemeWrapper(base, R.style.Theme_TwitchMiniChat).apply {
            applyOverrideConfiguration(Configuration(base.resources.configuration).apply { this.fontScale = fontScale })
        }

    private fun unitName(unit: Int): String = when (unit) {
        TypedValue.COMPLEX_UNIT_PX -> "px"
        TypedValue.COMPLEX_UNIT_DIP -> "dp"
        TypedValue.COMPLEX_UNIT_SP -> "sp"
        TypedValue.COMPLEX_UNIT_PT -> "pt"
        TypedValue.COMPLEX_UNIT_IN -> "in"
        TypedValue.COMPLEX_UNIT_MM -> "mm"
        else -> "unit $unit"
    }

    companion object {
        /** The font scales the measurement overrides, in the order the rule for A18 reads them. */
        val FONT_SCALES = listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)

        private const val MESSAGE_TEXT_SIZE_SP = 14f
    }
}
