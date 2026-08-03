package com.fs.twitchminichat

/**
 * Pure policy for bottom padding when Android resizes the chat window for the keyboard.
 */
internal object ChatKeyboardInsetPolicy {

    /**
     * Returns navigation-bar padding only when a docked keyboard is not resizing the window.
     */
    fun rootBottomPadding(
        imeVisible: Boolean,
        imeBottom: Int,
        navigationBarBottom: Int
    ): Int {
        val safeImeBottom = imeBottom.coerceAtLeast(0)
        val safeNavigationBarBottom = navigationBarBottom.coerceAtLeast(0)
        val dockedImeVisible = imeVisible && safeImeBottom > safeNavigationBarBottom

        return if (dockedImeVisible) {
            0
        } else {
            safeNavigationBarBottom
        }
    }
}
