package com.fs.twitchminichat

/**
 * Pure policy for keeping the chat above the Input Method Editor (IME).
 *
 * The policy uses the overlap that remains after Android has applied any native
 * `adjustResize` behaviour. This supports both legacy resized windows and enforced
 * edge-to-edge windows without adding the keyboard height twice.
 */
internal object ChatKeyboardInsetPolicy {

    /**
     * Treats a docked IME inset as visible even if the OEM visibility flag lags.
     */
    fun isImeConsideredVisible(
        imeVisible: Boolean,
        imeBottom: Int,
        navigationBarBottom: Int
    ): Boolean {
        return imeVisible ||
                imeBottom.coerceAtLeast(0) > navigationBarBottom.coerceAtLeast(0)
    }

    /**
     * Returns the bottom padding still required by the current window geometry.
     *
     * A docked IME is identified by an inset larger than the navigation bar. The
     * explicit visibility flag is intentionally not the sole source of truth because
     * some Original Equipment Manufacturer (OEM) builds update it one frame late.
     */
    fun rootBottomPadding(
        imeVisible: Boolean,
        imeBottom: Int,
        navigationBarBottom: Int,
        measuredImeOverlap: Int
    ): Int {
        val safeImeBottom = imeBottom.coerceAtLeast(0)
        val safeNavigationBarBottom = navigationBarBottom.coerceAtLeast(0)
        val safeMeasuredOverlap = measuredImeOverlap
            .coerceAtLeast(0)
            .coerceAtMost(safeImeBottom)

        val dockedImeReported = safeImeBottom > safeNavigationBarBottom

        return when {
            !dockedImeReported -> safeNavigationBarBottom
            safeMeasuredOverlap > 0 -> safeMeasuredOverlap
            imeVisible -> 0
            else -> safeNavigationBarBottom
        }
    }

    /**
     * Measures how much of a root still extends below the top edge of a docked IME.
     *
     * Both coordinates must use the same coordinate space: window coordinates on
     * Android 11 and newer, or screen coordinates for the legacy visible-frame fallback.
     */
    fun overlappingImeHeight(
        rootBottom: Int,
        imeTop: Int,
        imeBottom: Int
    ): Int {
        val safeImeBottom = imeBottom.coerceAtLeast(0)
        if (safeImeBottom == 0) return 0

        return (rootBottom.coerceAtLeast(0) - imeTop.coerceAtLeast(0))
            .coerceAtLeast(0)
            .coerceAtMost(safeImeBottom)
    }
}
