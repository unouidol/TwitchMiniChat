package com.fs.twitchminichat

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat

/**
 * Presentation helper for Smart preset row highlighting.
 *
 * Timer Ball and Quick Ball are time-sensitive suggestions, so a subtle row
 * tint makes them easier to spot while a spawn is active.
 */
object QuickCatchSmartHighlight {

    fun buildBackground(
        context: Context,
        preset: CatchPreset
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.resources.displayMetrics.density * 10f
            setColor(resolveBackgroundColor(context, preset))
        }
    }

    @ColorInt
    private fun resolveBackgroundColor(
        context: Context,
        preset: CatchPreset
    ): Int {
        return when {
            preset.id.startsWith("smart_") && preset.ballId == "quick_ball" ->
                ContextCompat.getColor(context, R.color.quick_catch_quick_ball_smart_bg)

            preset.id.startsWith("smart_") && preset.ballId == "timer_ball" ->
                ContextCompat.getColor(context, R.color.quick_catch_timer_ball_smart_bg)

            else ->
                ContextCompat.getColor(context, R.color.quick_catch_normal_row_bg)
        }
    }
}