package com.fs.twitchminichat

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import kotlin.math.abs

class ChannelAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    var onChannelFieldTapped: ((wasDropdownOpenBeforeTap: Boolean) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var popupOpenOnDown = false
    private var tapCandidate = false

    override fun enoughToFilter(): Boolean {
        // Serve per permettere dropdown anche con testo vuoto.
        // AutoCompleteTextView normalmente non filtra sotto threshold 1.
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                popupOpenOnDown = isPopupShowing
                tapCandidate = true
            }

            MotionEvent.ACTION_MOVE -> {
                val moved =
                    abs(event.x - downX) > touchSlop ||
                            abs(event.y - downY) > touchSlop

                if (moved) {
                    tapCandidate = false
                }
            }

            MotionEvent.ACTION_UP -> {
                val handled = super.onTouchEvent(event)

                if (tapCandidate) {
                    super.performClick()
                    onChannelFieldTapped?.invoke(popupOpenOnDown)
                    tapCandidate = false
                    return true
                }

                tapCandidate = false
                return handled
            }

            MotionEvent.ACTION_CANCEL -> {
                tapCandidate = false
            }
        }

        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}