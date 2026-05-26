package com.fs.twitchminichat.ui.insets

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import kotlin.math.max

/**
 * Applies Android system-bar Window Insets to views that are visually anchored
 * near screen edges.
 *
 * This helper keeps floating controls away from the Android status bar and
 * display cutout without changing the rest of the screen layout.
 */
object SystemBarsInsetHelper {

    /**
     * Adds the current status-bar or display-cutout top inset to the original top
     * margin of [view].
     *
     * Use this for small top-aligned control rows that must remain tappable and
     * visible when Android draws the app edge-to-edge.
     */
    fun keepBelowStatusBar(view: View) {
        val originalTopMargin = (view.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(view) { targetView, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val cutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val safeTopInset = max(statusBarInsets.top, cutoutInsets.top)

            val marginLayoutParams = targetView.layoutParams as? ViewGroup.MarginLayoutParams
                ?: return@setOnApplyWindowInsetsListener insets

            marginLayoutParams.topMargin = originalTopMargin + safeTopInset
            targetView.layoutParams = marginLayoutParams

            insets
        }

        requestApplyInsetsWhenAttached(view)
    }

    /**
     * Requests Window Insets immediately when "view" is already attached, or waits
     * for attachment when the view is still being inflated.
     */
    /**
     * Requests Window Insets immediately when [view] is already attached, or waits
     * for attachment when the view is still being inflated.
     */
    private fun requestApplyInsetsWhenAttached(view: View) {
        if (view.isAttachedToWindow) {
            ViewCompat.requestApplyInsets(view)
        } else {
            view.doOnAttach { attachedView ->
                ViewCompat.requestApplyInsets(attachedView)
            }
        }
    }
}