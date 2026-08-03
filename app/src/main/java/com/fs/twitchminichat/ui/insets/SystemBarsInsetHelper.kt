package com.fs.twitchminichat.ui.insets

import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import kotlin.math.max

/**
 * Applies Android system-bar Window Insets to views that are visually anchored
 * near screen edges.
 *
 * This helper keeps edge-anchored content away from Android system bars and
 * display cutouts without consuming insets needed by descendant views.
 */
object SystemBarsInsetHelper {

    /**
     * Enables backward-compatible edge-to-edge rendering and keeps [rootView]
     * outside every visible system bar and display cutout.
     */
    fun enableEdgeToEdgeWithSafePadding(
        window: Window,
        rootView: View
    ) {
        WindowCompat.enableEdgeToEdge(window)
        applySystemBarsPadding(rootView)
    }

    /**
     * Adds system-bar and display-cutout insets to the original padding of [view].
     *
     * The listener does not consume the insets, so child views can still react to
     * them when needed. Repeated inset dispatches are idempotent because every
     * update starts from the padding captured before the listener was installed.
     */
    fun applySystemBarsPadding(view: View) {
        val originalPaddingLeft = view.paddingLeft
        val originalPaddingTop = view.paddingTop
        val originalPaddingRight = view.paddingRight
        val originalPaddingBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { targetView, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())

            targetView.setPadding(
                originalPaddingLeft + max(systemBarInsets.left, cutoutInsets.left),
                originalPaddingTop + max(systemBarInsets.top, cutoutInsets.top),
                originalPaddingRight + max(systemBarInsets.right, cutoutInsets.right),
                originalPaddingBottom + max(systemBarInsets.bottom, cutoutInsets.bottom)
            )

            insets
        }

        requestApplyInsetsWhenAttached(view)
    }

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
