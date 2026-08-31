package com.fs.twitchminichat

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Applies only the bottom inset that remains after Android's native keyboard resize.
 *
 * `MainActivity` requests `adjustResize`, but enforced edge-to-edge windows and
 * Original Equipment Manufacturer (OEM) implementations do not always reduce the
 * Fragment root. The controller therefore measures the real overlap between the
 * root and Input Method Editor (IME), avoiding both keyboard coverage and double
 * padding on devices where native resize already succeeded.
 */
class ChatKeyboardLayoutController(
    private val root: View,
    private val onKeyboardShown: () -> Unit
) {
    private val originalRootPaddingLeft = root.paddingLeft
    private val originalRootPaddingTop = root.paddingTop
    private val originalRootPaddingRight = root.paddingRight
    private val originalRootPaddingBottom = root.paddingBottom

    private val rootLocation = IntArray(2)
    private val visibleWindowFrame = Rect()

    private var lastAppliedBottomPadding = Int.MIN_VALUE

    /**
     * Applies the latest IME and navigation-bar insets to the chat root.
     *
     * The measured overlap is zero when `adjustResize` already ends the root above
     * the keyboard. In an edge-to-edge window it equals the portion of the IME that
     * still covers the root. A floating or zero-height IME keeps normal navigation-
     * bar protection.
     */
    fun applyWindowInsets(insets: WindowInsetsCompat) {
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val navigationBarBottom = insets
            .getInsets(WindowInsetsCompat.Type.navigationBars())
            .bottom

        if (imeBottom > navigationBarBottom && root.height <= 0) {
            /* Initial inset dispatch can precede the first measurable layout pass. */
            root.post {
                ViewCompat.requestApplyInsets(root)
            }
        }

        val overlapMeasurement = measureImeOverlap(imeBottom)
        val requiredInset = ChatKeyboardInsetPolicy.rootBottomPadding(
            imeVisible = imeVisible,
            imeBottom = imeBottom,
            navigationBarBottom = navigationBarBottom,
            measuredImeOverlap = overlapMeasurement.overlap
        )

        val targetBottomPadding = originalRootPaddingBottom +
                requiredInset

        val imeConsideredVisible = ChatKeyboardInsetPolicy.isImeConsideredVisible(
            imeVisible = imeVisible,
            imeBottom = imeBottom,
            navigationBarBottom = navigationBarBottom
        )

        Log.d(
            TAG,
            "layout imeVisible=$imeVisible " +
                    "imeBottom=$imeBottom " +
                    "navigationBottom=$navigationBarBottom " +
                    "rootBottom=${overlapMeasurement.rootBottom} " +
                    "imeTop=${overlapMeasurement.imeTop} " +
                    "overlap=${overlapMeasurement.overlap} " +
                    "targetBottomPadding=$targetBottomPadding"
        )

        if (targetBottomPadding == lastAppliedBottomPadding) {
            if (imeConsideredVisible) {
                root.post {
                    onKeyboardShown()
                }
            }
            return
        }

        lastAppliedBottomPadding = targetBottomPadding

        root.updatePadding(
            left = originalRootPaddingLeft,
            top = originalRootPaddingTop,
            right = originalRootPaddingRight,
            bottom = targetBottomPadding
        )

        if (imeConsideredVisible) {
            root.post {
                onKeyboardShown()
            }
        }
    }

    /**
     * Measures root/IME overlap without assuming whether `adjustResize` succeeded.
     *
     * Android 11 and newer expose window bounds that include system-bar areas. On
     * older releases the visible window frame is used in the matching screen
     * coordinate space, which preserves the existing `adjustResize` behaviour.
     */
    private fun measureImeOverlap(imeBottom: Int): ImeOverlapMeasurement {
        if (imeBottom <= 0 || root.height <= 0) {
            return ImeOverlapMeasurement.EMPTY
        }

        val rootBottom: Int
        val imeTop: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.getLocationInWindow(rootLocation)
            rootBottom = rootLocation[1] + root.height

            val windowManager = root.context.getSystemService(WindowManager::class.java)
            val windowHeight = windowManager
                ?.currentWindowMetrics
                ?.bounds
                ?.height()
                ?: root.rootView.height
            imeTop = (windowHeight - imeBottom).coerceAtLeast(0)
        } else {
            root.getLocationOnScreen(rootLocation)
            rootBottom = rootLocation[1] + root.height

            root.getWindowVisibleDisplayFrame(visibleWindowFrame)
            imeTop = visibleWindowFrame.bottom.coerceAtLeast(0)
        }

        return ImeOverlapMeasurement(
            rootBottom = rootBottom,
            imeTop = imeTop,
            overlap = ChatKeyboardInsetPolicy.overlappingImeHeight(
                rootBottom = rootBottom,
                imeTop = imeTop,
                imeBottom = imeBottom
            )
        )
    }

    /**
     * Restores the XML padding before the Fragment view is released.
     */
    fun stop() {
        lastAppliedBottomPadding = Int.MIN_VALUE

        root.updatePadding(
            left = originalRootPaddingLeft,
            top = originalRootPaddingTop,
            right = originalRootPaddingRight,
            bottom = originalRootPaddingBottom
        )
    }

    /** Immutable geometry snapshot used for safe diagnostics and padding policy. */
    private data class ImeOverlapMeasurement(
        val rootBottom: Int,
        val imeTop: Int,
        val overlap: Int
    ) {
        companion object {
            /** Empty measurement used before the root has a measurable size. */
            val EMPTY = ImeOverlapMeasurement(
                rootBottom = 0,
                imeTop = 0,
                overlap = 0
            )
        }
    }

    private companion object {
        /** Privacy-safe Logcat tag for keyboard geometry diagnostics. */
        private const val TAG = "CHAT_IME"
    }
}
