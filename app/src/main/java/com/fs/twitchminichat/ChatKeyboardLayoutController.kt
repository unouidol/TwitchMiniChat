package com.fs.twitchminichat

import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Applies bottom system-bar padding around Android's native keyboard resize.
 *
 * `MainActivity` uses `adjustResize`, so the platform keeps the chat composer in
 * the area left above the Input Method Editor (IME). This controller deliberately
 * does not estimate keyboard height or move the composer. It only protects the
 * bottom edge from the navigation bar while the IME is not resizing the window.
 */
class ChatKeyboardLayoutController(
    private val root: View,
    private val onKeyboardShown: () -> Unit
) {
    private val originalRootPaddingLeft = root.paddingLeft
    private val originalRootPaddingTop = root.paddingTop
    private val originalRootPaddingRight = root.paddingRight
    private val originalRootPaddingBottom = root.paddingBottom

    private var lastAppliedBottomPadding = Int.MIN_VALUE

    /**
     * Applies the latest IME and navigation-bar insets to the chat root.
     *
     * A docked IME already reduces the available window through `adjustResize`, so
     * adding its height as padding would move the composer twice. A floating or
     * zero-height IME does not resize the bottom edge and therefore keeps the normal
     * navigation-bar protection.
     */
    fun applyWindowInsets(insets: WindowInsetsCompat) {
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val navigationBarBottom = insets
            .getInsets(WindowInsetsCompat.Type.navigationBars())
            .bottom

        val targetBottomPadding = originalRootPaddingBottom +
                ChatKeyboardInsetPolicy.rootBottomPadding(
                    imeVisible = imeVisible,
                    imeBottom = imeBottom,
                    navigationBarBottom = navigationBarBottom
                )

        if (targetBottomPadding == lastAppliedBottomPadding) {
            if (imeVisible) {
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

        if (imeVisible) {
            root.post {
                onKeyboardShown()
            }
        }
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
}
