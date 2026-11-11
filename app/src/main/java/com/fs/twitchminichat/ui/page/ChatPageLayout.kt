package com.fs.twitchminichat.ui.page

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.fs.twitchminichat.R

/**
 * The chat page's root: a vertical [LinearLayout] that shares its height by [ChatPagePolicy] at
 * measure time, so the policy holds on every frame, the frame on which the keyboard arrives
 * included.
 *
 * PROTOTYPE (branch `prototype/5d-layout-b`), not for merging.
 *
 * Three children are named by attribute: the list's frame, the stream player and the emote
 * picker. Every other visible child never yields and is measured first. The player's and the
 * picker's own layout heights are their full heights; the picker's minimum is
 * `chatPagePickerMinHeight`. Without a list child the layout behaves as a plain [LinearLayout].
 *
 * Children above the list are laid out from the top, children below it from the bottom. When the
 * children that never yield do not fit on their own, the list is 0 and the bottom group, the
 * composer row last of all, is drawn over the top group: the composer stays on screen.
 *
 * The root's padding is honoured, including the bottom padding [com.fs.twitchminichat.ChatKeyboardLayoutController]
 * sets for the keyboard.
 */
class ChatPageLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val listId: Int
    private val playerId: Int
    private val pickerId: Int
    private val pickerMinPx: Int

    /**
     * The list height the picker must leave, in pixels. Set by the page from its row height at the
     * user's font scale.
     */
    var listFloorPx: Int = 0
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    /**
     * Called, posted after the measure pass, each time a panel's refusal starts or ends. Never
     * called from inside a measure pass.
     */
    var onRefusalChanged: ((ChatPageAllocation) -> Unit)? = null

    /** The last allocation applied, for the page's diagnostics. */
    var lastAllocation: ChatPageAllocation? = null
        private set

    private var allocatedThisPass = false

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ChatPageLayout)
        try {
            listId = a.getResourceId(R.styleable.ChatPageLayout_chatPageListId, View.NO_ID)
            playerId = a.getResourceId(R.styleable.ChatPageLayout_chatPagePlayerId, View.NO_ID)
            pickerId = a.getResourceId(R.styleable.ChatPageLayout_chatPagePickerId, View.NO_ID)
            pickerMinPx = a.getDimensionPixelSize(R.styleable.ChatPageLayout_chatPagePickerMinHeight, 0)
        } finally {
            a.recycle()
        }
    }

    private fun child(id: Int): View? = if (id == View.NO_ID) null else findViewById<View>(id)?.takeIf { it.parent === this }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val list = child(listId)
        if (orientation != VERTICAL || list == null || MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            allocatedThisPass = false
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val player = child(playerId)
        val picker = child(pickerId)

        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val inner = height - paddingTop - paddingBottom

        var fixed = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.isGone) continue
            val lp = c.layoutParams as MarginLayoutParams
            fixed += lp.topMargin + lp.bottomMargin
            if (c === list || c === player || c === picker) continue
            measureChildWithMargins(c, widthMeasureSpec, 0, heightMeasureSpec, 0)
            fixed += c.measuredHeight
        }

        val allocation = ChatPagePolicy.allocate(
            availablePx = inner - fixed,
            playerWanted = player?.isVisible == true,
            playerPx = player?.layoutParams?.height?.coerceAtLeast(0) ?: 0,
            pickerWanted = picker?.isVisible == true,
            pickerMaxPx = picker?.layoutParams?.height?.coerceAtLeast(0) ?: 0,
            pickerMinPx = pickerMinPx,
            listFloorPx = listFloorPx
        )

        if (player != null && !player.isGone) measureExactly(player, widthMeasureSpec, allocation.playerPx)
        if (picker != null && !picker.isGone) measureExactly(picker, widthMeasureSpec, allocation.pickerPx)
        measureExactly(list, widthMeasureSpec, allocation.listPx)

        setMeasuredDimension(width, height)
        allocatedThisPass = true
        report(allocation, inner - fixed)
    }

    private fun measureExactly(child: View, widthMeasureSpec: Int, heightPx: Int) {
        val lp = child.layoutParams as MarginLayoutParams
        val widthSpec = getChildMeasureSpec(
            widthMeasureSpec,
            paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin,
            lp.width
        )
        child.measure(widthSpec, MeasureSpec.makeMeasureSpec(heightPx.coerceAtLeast(0), MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val list = child(listId)
        if (!allocatedThisPass || list == null) {
            super.onLayout(changed, l, t, r, b)
            return
        }
        val listIndex = indexOfChild(list)

        var top = paddingTop
        for (i in 0 until listIndex) {
            val c = getChildAt(i)
            if (c.isGone) continue
            val lp = c.layoutParams as MarginLayoutParams
            top += lp.topMargin
            place(c, lp, top)
            top += c.measuredHeight + lp.bottomMargin
        }

        var bottom = (b - t) - paddingBottom
        for (i in childCount - 1 downTo listIndex + 1) {
            val c = getChildAt(i)
            if (c.isGone) continue
            val lp = c.layoutParams as MarginLayoutParams
            bottom -= lp.bottomMargin + c.measuredHeight
            place(c, lp, bottom)
            bottom -= lp.topMargin
        }

        val listLp = list.layoutParams as MarginLayoutParams
        place(list, listLp, top + listLp.topMargin)
    }

    private fun place(c: View, lp: MarginLayoutParams, top: Int) {
        val left = paddingLeft + lp.leftMargin
        c.layout(left, top, left + c.measuredWidth, top + c.measuredHeight)
    }

    private fun report(allocation: ChatPageAllocation, availablePx: Int) {
        val previous = lastAllocation
        lastAllocation = allocation
        if (previous != allocation) {
            Log.d(
                TAG,
                "allocation available=$availablePx floor=$listFloorPx " +
                        "player=${allocation.playerPx} playerRefused=${allocation.playerRefused} " +
                        "picker=${allocation.pickerPx} pickerRefused=${allocation.pickerRefused} " +
                        "list=${allocation.listPx}"
            )
        }
        val refusalChanged = previous == null && (allocation.playerRefused || allocation.pickerRefused) ||
                previous != null && (previous.playerRefused != allocation.playerRefused ||
                        previous.pickerRefused != allocation.pickerRefused)
        if (refusalChanged) {
            post { onRefusalChanged?.invoke(allocation) }
        }
    }

    private companion object {
        const val TAG = "CHAT_PAGE"
    }
}
