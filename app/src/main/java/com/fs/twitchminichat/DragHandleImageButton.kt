package com.fs.twitchminichat

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

/**
 * Small custom ImageButton used as a drag handle inside preset editor rows.
 *
 * RecyclerView drag starts from an explicit touch listener in the adapter.
 * Overriding performClick keeps the view accessible and satisfies Android lint:
 * touch-driven custom views must still expose a click path for accessibility
 * services and keyboard/assistive interactions.
 */
class DragHandleImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    /**
     * Provides the accessibility click path required when this custom view is
     * also used with an OnTouchListener.
     */
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}