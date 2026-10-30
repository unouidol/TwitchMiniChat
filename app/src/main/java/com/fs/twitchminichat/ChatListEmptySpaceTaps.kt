package com.fs.twitchminichat

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Makes a tap on the chat list that no row takes a click on the list, so that the list's
 * OnClickListener fires as the old container's did.
 *
 * Needed because RecyclerView.onTouchEvent never calls performClick (RecyclerView 1.3.1,
 * RecyclerView.java:3750; the file has no call to it or to View.onTouchEvent), so a click listener
 * set on the list alone would never fire. Before 5d the listener that fired was the container's:
 * ScrollView.onTouchEvent does not call performClick either (ScrollView.java:782, API 36), and the
 * container, a LinearLayout, got the tap through View.onTouchEvent.
 *
 * As a touch listener on the list it sees exactly the events no row consumed: the touch listener
 * of a view group runs only for what its children did not take. A message row takes its own taps,
 * through its own listener; a system line and the space between and around rows do not, and their
 * taps become the list's click, as they became the container's. One difference, reasoned rather
 * than seen: the container ended at the last row, while the list fills the chat area, so a tap
 * below the last row of a short chat now clicks too.
 *
 * It returns false for every event, so the list still scrolls on everything a tap is not.
 */
internal class ChatListEmptySpaceTaps(list: RecyclerView) : View.OnTouchListener {

    private val taps = GestureDetector(list.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean = true
    })

    /* The click is performed here, on the up that ends a tap. */
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val tapped = taps.onTouchEvent(event)
        if (tapped && event.actionMasked == MotionEvent.ACTION_UP) {
            view.performClick()
        }
        return false
    }
}
