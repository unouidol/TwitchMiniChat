package com.fs.twitchminichat

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max
import kotlin.math.min

/**
 * Keeps the chat UI above the soft keyboard by resizing the root content area.
 *
 * The chat screen root is a vertical LinearLayout:
 * - chat messages use layout_weight=1;
 * - reply bar and composer sit below it.
 *
 * For this layout, moving only the composer with translationY leaves a visual
 * gap because the original composer slot is still reserved by the LinearLayout.
 * Instead, this controller adds bottom padding to the root while the keyboard is
 * visible. That makes the weighted chat area shrink and keeps the composer
 * naturally attached to the chat.
 */
class ChatKeyboardLayoutController(
    private val root: View,
    private val composerBar: View,
    private val chatScroll: View,
    private val shouldLiftComposer: () -> Boolean,
    private val onKeyboardShown: () -> Unit
) {

    private val visibleFrame = Rect()

    private val originalRootPaddingLeft = root.paddingLeft
    private val originalRootPaddingTop = root.paddingTop
    private val originalRootPaddingRight = root.paddingRight
    private val originalRootPaddingBottom = root.paddingBottom

    private val originalChatPaddingLeft = chatScroll.paddingLeft
    private val originalChatPaddingTop = chatScroll.paddingTop
    private val originalChatPaddingRight = chatScroll.paddingRight
    private val originalChatPaddingBottom = chatScroll.paddingBottom

    private var lastAppliedBottomPadding = Int.MIN_VALUE
    private var lastKnownKeyboardHeight = 0
    private var keyboardOpenRequested = false
    private var started = false

    /**
     * True after this controller has lifted the composer for the current keyboard session.
     *
     * Android focus can briefly become unstable while the Input Method Editor is open.
     * Once the composer has been lifted, we keep it lifted until the keyboard is really
     * closed instead of trusting a single transient shouldLift=false frame.
     */
    private var composerLiftActiveForCurrentKeyboard = false

    /**
     * Main-thread handler used to refresh keyboard geometry while the Input Method
     * Editor animation is still settling.
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Delayed refresh callbacks posted during keyboard open animations.
     */
    private val pendingRefreshRunnables = mutableListOf<Runnable>()

    /**
     * Global layout listener used as the normal keyboard geometry source.
     */
    private val listener = ViewTreeObserver.OnGlobalLayoutListener {
        refreshKeyboardLayout()
    }

    /**
     * Starts listening to keyboard/layout changes.
     */
    fun start() {
        if (started) return
        started = true

        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        requestRefresh()
    }

    /**
     * Stops listening and restores the original layout.
     */
    fun stop() {
        clearPendingRefreshes()

        if (root.viewTreeObserver.isAlive) {
            root.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }

        started = false
        keyboardOpenRequested = false
        composerLiftActiveForCurrentKeyboard = false
        lastAppliedBottomPadding = Int.MIN_VALUE

        root.updatePadding(
            left = originalRootPaddingLeft,
            top = originalRootPaddingTop,
            right = originalRootPaddingRight,
            bottom = originalRootPaddingBottom
        )

        chatScroll.updatePadding(
            left = originalChatPaddingLeft,
            top = originalChatPaddingTop,
            right = originalChatPaddingRight,
            bottom = originalChatPaddingBottom
        )

        composerBar.translationY = 0f
    }

    /**
     * Requests one immediate keyboard layout refresh.
     *
     * This is useful when focus changes before Android dispatches stable keyboard
     * insets to the global layout listener.
     */
    fun requestRefresh() {
        root.post {
            refreshKeyboardLayout()
        }
    }

    /**
     * Prepares the layout for an expected keyboard opening.
     *
     * Android can report keyboard height too late when windowSoftInputMode is
     * adjustNothing. This method allows the chat composer to move above the expected
     * keyboard area immediately, before the final Input Method Editor height is known.
     */
    fun prepareForKeyboardOpen() {
        keyboardOpenRequested = true
        requestRefreshBurst()
    }

    /**
     * Requests multiple keyboard layout refreshes while the keyboard open animation settles.
     *
     * This covers the race where the user taps the composer and starts typing before
     * the keyboard has fully reported its final height.
     */
    fun requestRefreshBurst() {
        clearPendingRefreshes()

        val delaysMs = longArrayOf(0L, 40L, 80L, 140L, 220L, 340L, 520L, 760L)

        delaysMs.forEach { delayMs ->
            val runnable = Runnable {
                refreshKeyboardLayout()
            }

            pendingRefreshRunnables += runnable
            mainHandler.postDelayed(runnable, delayMs)
        }

        val cleanupRunnable = Runnable {
            /*
             * If the keyboard never became visible, drop the provisional state and
             * let the next refresh restore the normal bottom padding.
             */
            if (!isRealKeyboardKnown() && !shouldLiftComposer()) {
                keyboardOpenRequested = false
                refreshKeyboardLayout()
            }
        }

        pendingRefreshRunnables += cleanupRunnable
        mainHandler.postDelayed(cleanupRunnable, KEYBOARD_OPEN_CLEANUP_DELAY_MS)
    }

    /**
     * Cancels delayed keyboard refresh callbacks.
     */
    private fun clearPendingRefreshes() {
        pendingRefreshRunnables.forEach { runnable ->
            mainHandler.removeCallbacks(runnable)
        }

        pendingRefreshRunnables.clear()
    }

    /**
     * Recalculates the bottom padding needed to keep the chat composer above the keyboard.
     *
     * The calculation uses both WindowInsetsCompat and the visible display frame because
     * some devices report unreliable Input Method Editor insets when windowSoftInputMode
     * is set to adjustNothing.
     */
    private fun refreshKeyboardLayout() {
        val rootView = root.rootView

        val rootInsets = ViewCompat.getRootWindowInsets(root)

        val imeVisible = rootInsets?.isVisible(WindowInsetsCompat.Type.ime()) == true

        val imeBottom = rootInsets
            ?.getInsets(WindowInsetsCompat.Type.ime())
            ?.bottom
            ?: 0

        val navBottom = rootInsets
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom
            ?: 0

        val imeKeyboardHeight = if (imeVisible) {
            (imeBottom - navBottom).coerceAtLeast(0)
        } else {
            0
        }

        /*
         * Fallback source: visible display frame.
         *
         * With adjustNothing, some devices do not report useful Input Method Editor
         * insets to the fragment, but the visible display frame can still become shorter.
         */
        rootView.getWindowVisibleDisplayFrame(visibleFrame)

        val rootHeight = rootView.height
        val rawVisibleFrameKeyboardHeight =
            (rootHeight - visibleFrame.bottom).coerceAtLeast(0)

        val visibleFrameKeyboardHeight =
            if (rawVisibleFrameKeyboardHeight > minimumKeyboardHeightPx(rootHeight)) {
                rawVisibleFrameKeyboardHeight
            } else {
                0
            }

        val detectedKeyboardHeight = max(
            imeKeyboardHeight,
            visibleFrameKeyboardHeight
        )

        if (detectedKeyboardHeight > 0) {
            lastKnownKeyboardHeight = detectedKeyboardHeight
            keyboardOpenRequested = false
        }

        val shouldLift = shouldLiftComposer()

        val keyboardPadding = when {
            detectedKeyboardHeight > 0 && shouldLift -> {
                /*
                 * A real keyboard height is available and the composer currently wants to
                 * be lifted. Mark this keyboard session as owned by the composer.
                 */
                composerLiftActiveForCurrentKeyboard = true
                detectedKeyboardHeight
            }

            detectedKeyboardHeight > 0 && composerLiftActiveForCurrentKeyboard -> {
                /*
                 * The keyboard is still visible, and this controller already lifted the
                 * composer during this keyboard session.
                 *
                 * Do not collapse the layout just because shouldLiftComposer() returned false
                 * for one transient frame. That is the exact race that puts the input behind
                 * the keyboard during fast typing.
                 */
                detectedKeyboardHeight
            }

            /*
             * Critical fast-input path:
             *
             * The user tapped the composer, Android is opening the keyboard, but the real
             * keyboard height is still reported as zero. Use a provisional height so the
             * composer does not fall behind the keyboard while the animation is still settling.
             */
            keyboardOpenRequested && shouldLift -> {
                composerLiftActiveForCurrentKeyboard = true
                estimatedKeyboardHeight(rootHeight)
            }

            else -> {
                /*
                 * No real keyboard is visible and no provisional composer lift is needed.
                 * Reset the session lock so the next keyboard open starts cleanly.
                 */
                if (detectedKeyboardHeight == 0) {
                    composerLiftActiveForCurrentKeyboard = false
                }

                0
            }
        }

        /*
         * Keep navigation bar padding when the keyboard is closed, but when the
         * keyboard is open use the keyboard height as the dominant bottom inset.
         */
        val targetBottomPadding =
            originalRootPaddingBottom + max(navBottom, keyboardPadding)

        if (targetBottomPadding == lastAppliedBottomPadding) {
            if (keyboardPadding > 0) {
                root.post {
                    onKeyboardShown()
                }
            }
            return
        }

        lastAppliedBottomPadding = targetBottomPadding

        Log.d(
            "CHAT_IME",
            "keyboard controller rootPadding targetBottom=$targetBottomPadding " +
                    "imeVisible=$imeVisible " +
                    "imeBottom=$imeBottom " +
                    "navBottom=$navBottom " +
                    "imeKeyboardHeight=$imeKeyboardHeight " +
                    "visibleFrameKeyboardHeight=$visibleFrameKeyboardHeight " +
                    "detectedKeyboardHeight=$detectedKeyboardHeight " +
                    "keyboardPadding=$keyboardPadding " +
                    "keyboardOpenRequested=$keyboardOpenRequested " +
                    "composerLiftActive=$composerLiftActiveForCurrentKeyboard " +
                    "lastKnownKeyboardHeight=$lastKnownKeyboardHeight " +
                    "composerHeight=${composerBar.height} " +
                    "shouldLift=$shouldLift"
        )

        /*
         * Resize the whole vertical chat layout instead of translating only the
         * composer. This prevents a black/empty band between messages and input.
         */
        root.updatePadding(
            left = originalRootPaddingLeft,
            top = originalRootPaddingTop,
            right = originalRootPaddingRight,
            bottom = targetBottomPadding
        )

        /*
         * Do not add keyboard-sized padding to the ScrollView. The root padding
         * already shrinks the weighted chat area, so extra ScrollView padding would
         * create the same empty band we are trying to remove.
         */
        chatScroll.updatePadding(
            left = originalChatPaddingLeft,
            top = originalChatPaddingTop,
            right = originalChatPaddingRight,
            bottom = originalChatPaddingBottom
        )

        composerBar.translationY = 0f

        if (keyboardPadding > 0) {
            root.post {
                onKeyboardShown()
            }
        }
    }

    /**
     * Returns true when a real keyboard height has already been detected.
     */
    private fun isRealKeyboardKnown(): Boolean {
        return lastKnownKeyboardHeight > 0
    }

    /**
     * Returns the minimum visible-frame delta that should be treated as a real keyboard.
     *
     * This avoids mistaking small system-bar or cutout changes for keyboard visibility.
     */
    private fun minimumKeyboardHeightPx(rootHeight: Int): Int {
        val density = root.resources.displayMetrics.density
        val absoluteMinimum = (120 * density).toInt()
        val relativeMinimum = (rootHeight * 0.12f).toInt()

        return max(absoluteMinimum, relativeMinimum)
    }

    /**
     * Estimates keyboard height before Android reports the real one.
     *
     * The estimate is used only during the short keyboard opening race. Once real
     * insets or visible-frame height arrive, the real value replaces it.
     */
    private fun estimatedKeyboardHeight(rootHeight: Int): Int {
        if (lastKnownKeyboardHeight > 0) {
            return lastKnownKeyboardHeight
        }

        val density = root.resources.displayMetrics.density
        val minEstimate = (260 * density).toInt()
        val maxEstimate = (430 * density).toInt()
        val proportionalEstimate = (rootHeight * 0.38f).toInt()

        return proportionalEstimate
            .coerceAtLeast(minEstimate)
            .coerceAtMost(min(maxEstimate, (rootHeight * 0.55f).toInt()))
    }

    private companion object {

        /**
         * Delay after which a keyboard open request can be considered stale.
         */
        private const val KEYBOARD_OPEN_CLEANUP_DELAY_MS = 1_100L
    }
}