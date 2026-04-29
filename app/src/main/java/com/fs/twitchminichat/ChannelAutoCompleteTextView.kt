package com.fs.twitchminichat

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import kotlin.math.abs

/**
 * AutoCompleteTextView used for the Twitch channel field.
 *
 * The default AutoCompleteTextView behavior is not ideal for our channel picker:
 * it normally requires at least one typed character before showing suggestions,
 * and tapping the field while the dropdown is already open can immediately close
 * and reopen it in confusing ways.
 *
 * This custom view keeps the dropdown available even with an empty field and
 * reports clean tap events to the owner, including whether the dropdown was
 * already open before the tap started.
 */
class ChannelAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    /**
     * Called when the channel field receives a real click/tap.
     *
     * The Boolean tells the caller whether the dropdown was already open before
     * the interaction started. For accessibility/programmatic clicks, the current
     * dropdown state is used instead.
     */
    var onChannelFieldTapped: ((wasDropdownOpenBeforeTap: Boolean) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var popupOpenOnDown = false
    private var hasTouchPopupState = false
    private var tapCandidate = false

    /**
     * Allows the dropdown to be shown even when the channel field is empty.
     *
     * AutoCompleteTextView normally refuses to filter below its threshold, which
     * means an empty channel field would not show recent/saved channel suggestions.
     */
    override fun enoughToFilter(): Boolean {
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                /*
                 * Remember the initial touch position and whether the popup was
                 * already visible before this tap started.
                 */
                downX = event.x
                downY = event.y
                popupOpenOnDown = isPopupShowing
                hasTouchPopupState = true
                tapCandidate = true
            }

            MotionEvent.ACTION_MOVE -> {
                /*
                 * If the finger moves farther than the platform touch slop, this
                 * is probably a drag/scroll gesture, not a clean tap.
                 */
                val moved =
                    abs(event.x - downX) > touchSlop ||
                            abs(event.y - downY) > touchSlop

                if (moved) {
                    tapCandidate = false
                }
            }

            MotionEvent.ACTION_UP -> {
                val handledBySuper = super.onTouchEvent(event)

                if (tapCandidate) {
                    /*
                     * Route the tap through performClick(), so accessibility and
                     * normal touch input share the same click path.
                     */
                    performClick()
                    tapCandidate = false
                    return true
                }

                tapCandidate = false
                hasTouchPopupState = false
                return handledBySuper
            }

            MotionEvent.ACTION_CANCEL -> {
                tapCandidate = false
                hasTouchPopupState = false
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Handles click behavior for both touch and accessibility/programmatic clicks.
     *
     * This is intentionally not a redundant override: it forwards the click to
     * AppCompatAutoCompleteTextView and then notifies the channel-field owner.
     */
    override fun performClick(): Boolean {
        val wasDropdownOpenBeforeClick =
            if (hasTouchPopupState) {
                popupOpenOnDown
            } else {
                isPopupShowing
            }

        val handledBySuper = super.performClick()

        onChannelFieldTapped?.invoke(wasDropdownOpenBeforeClick)

        hasTouchPopupState = false

        return handledBySuper || onChannelFieldTapped != null
    }
}