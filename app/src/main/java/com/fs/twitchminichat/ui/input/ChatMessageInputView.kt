package com.fs.twitchminichat.ui.input

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView
import kotlin.math.abs

/**
 * Message composer input used by the chat screen.
 *
 * This view exposes ACTION_DOWN without requiring an external OnTouchListener.
 * Keeping touch handling inside the custom view avoids accessibility lint warnings
 * and lets ChatFragment prepare input focus before the normal click/focus
 * pipeline runs.
 */
class ChatMessageInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.autoCompleteTextViewStyle
) : AppCompatMultiAutoCompleteTextView(context, attrs, defStyleAttr) {

    /**
     * Called as soon as the user touches the input field.
     *
     * The callback is intentionally fired before super.onTouchEvent(), because the
     * keyboard-opening race happens before regular click callbacks are dispatched.
     */
    var onComposerTouchDown: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var clickCandidate = false

    /**
     * Handles touch events while preserving the platform text input behavior.
     *
     * ACTION_DOWN is forwarded to ChatFragment early so it can prepare input focus
     * before the Input Method Editor animation settles.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                clickCandidate = true

                onComposerTouchDown?.invoke()
            }

            MotionEvent.ACTION_MOVE -> {
                val movedTooFar =
                    abs(event.x - downX) > touchSlop ||
                            abs(event.y - downY) > touchSlop

                if (movedTooFar) {
                    clickCandidate = false
                }
            }

            MotionEvent.ACTION_UP -> {
                if (clickCandidate) {
                    performClick()
                }

                clickCandidate = false
            }

            MotionEvent.ACTION_CANCEL -> {
                clickCandidate = false
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Preserves the accessibility click path for this custom input view.
     *
     * The method intentionally delegates to the superclass. It is still declared
     * because custom views that handle touch events must expose performClick() for
     * accessibility services and keyboard/controller click dispatch.
     */
    @Suppress("RedundantOverride")
    override fun performClick(): Boolean {
        return super.performClick()
    }
}
