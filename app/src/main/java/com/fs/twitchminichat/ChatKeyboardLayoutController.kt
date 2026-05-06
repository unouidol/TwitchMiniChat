package com.fs.twitchminichat

import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

/**
 * Keeps the chat UI above the soft keyboard by resizing the root content area.
 *
 * The chat screen root is a vertical LinearLayout:
 * - chat messages use layout_weight=1
 * - reply bar and composer sit below it
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

    private val listener = ViewTreeObserver.OnGlobalLayoutListener {
        val rootView = root.rootView ?: return@OnGlobalLayoutListener

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
         * With adjustNothing, some devices do not report useful IME insets to the
         * fragment, but the visible display frame still becomes shorter.
         */
        rootView.getWindowVisibleDisplayFrame(visibleFrame)

        val rootHeight = rootView.height
        val rawVisibleFrameKeyboardHeight =
            (rootHeight - visibleFrame.bottom).coerceAtLeast(0)

        val visibleFrameKeyboardHeight =
            if (rawVisibleFrameKeyboardHeight > rootHeight * 0.15f) {
                rawVisibleFrameKeyboardHeight
            } else {
                0
            }

        val detectedKeyboardHeight = max(
            imeKeyboardHeight,
            visibleFrameKeyboardHeight
        )

        val keyboardPadding = if (detectedKeyboardHeight > 0 && shouldLiftComposer()) {
            detectedKeyboardHeight
        } else {
            0
        }

        /*
         * Keep navigation bar padding when the keyboard is closed, but when the
         * keyboard is open use the keyboard height as the dominant bottom inset.
         */
        val targetBottomPadding =
            originalRootPaddingBottom + max(navBottom, keyboardPadding)

        if (targetBottomPadding == lastAppliedBottomPadding) {
            return@OnGlobalLayoutListener
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
                    "keyboardPadding=$keyboardPadding " +
                    "composerHeight=${composerBar.height} " +
                    "shouldLift=${shouldLiftComposer()}"
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
     * Starts listening to keyboard/layout changes.
     */
    fun start() {
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        root.post {
            listener.onGlobalLayout()
        }
    }

    /**
     * Stops listening and restores the original layout.
     */
    fun stop() {
        if (root.viewTreeObserver.isAlive) {
            root.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }

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
}