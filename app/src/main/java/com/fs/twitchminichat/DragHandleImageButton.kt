package com.fs.twitchminichat

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

/**
 * ImageButton used as a drag handle in TMC layouts.
 *
 * This class exists so XML layouts can reference a dedicated drag-handle view
 * type, while keeping room for future drag-specific behavior without mixing it
 * into unrelated UI code.
 */
class DragHandleImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageButton(context, attrs, defStyleAttr)