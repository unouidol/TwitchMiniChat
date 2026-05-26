package com.fs.twitchminichat.ui.input

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2

/**
 * Dismisses keyboard-related UI when the user changes pages.
 *
 * This controller is intentionally kept outside MainActivity and ChatFragment because
 * it handles cross-page user interface clean-up rather than chat or account logic.
 */
class PagerKeyboardDismissController(
    private val rootView: View
) {

    /**
     * Registers keyboard dismissal callbacks on the provided ViewPager2.
     *
     * The returned callback must be unregistered by the caller, usually from the
     * Activity onDestroy(), to avoid keeping stale references.
     */
    fun installOn(viewPager: ViewPager2): ViewPager2.OnPageChangeCallback {
        val callback = object : ViewPager2.OnPageChangeCallback() {

            /**
             * Dismisses input UI as soon as the user starts dragging between pages.
             */
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    dismissInputUi()
                }
            }

            /**
             * Dismisses input UI again when the selected page changes.
             *
             * This second pass also covers fast swipes and programmatic page changes.
             */
            override fun onPageSelected(position: Int) {
                dismissInputUi()
            }
        }

        viewPager.registerOnPageChangeCallback(callback)
        return callback
    }

    /**
     * Dismisses autocomplete popups, clears the focused view, and hides the keyboard.
     */
    fun dismissInputUi() {
        dismissAutoCompletePopups(rootView)

        val focusedView = rootView.findFocus()
        val windowToken = focusedView?.windowToken ?: rootView.windowToken

        /*
         * Capture the window token before clearing focus. Some Android builds are more
         * reliable when hideSoftInputFromWindow() receives the token that owned focus.
         */
        focusedView?.clearFocus()

        val inputMethodManager = rootView.context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

        if (windowToken != null) {
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        }

        /*
         * Ask the existing insets-based layout code to recalculate after the focus and
         * keyboard state changed.
         */
        ViewCompat.requestApplyInsets(rootView)
    }

    /**
     * Recursively dismisses any AutoCompleteTextView popup found under rootView.
     *
     * MultiAutoCompleteTextView extends AutoCompleteTextView, so this covers both
     * the chat mention field and the channel autocomplete field.
     */
    private fun dismissAutoCompletePopups(view: View) {
        if (view is AutoCompleteTextView) {
            view.dismissDropDown()
        }

        if (view !is ViewGroup) return

        for (index in 0 until view.childCount) {
            dismissAutoCompletePopups(view.getChildAt(index))
        }
    }
}