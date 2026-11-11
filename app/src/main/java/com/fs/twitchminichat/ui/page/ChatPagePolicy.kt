package com.fs.twitchminichat.ui.page

/**
 * The heights one measure pass gives the yielding children, in pixels.
 *
 * A refused panel is wanted and gets no height. The list always gets the rest, which is never
 * negative.
 */
data class ChatPageAllocation(
    /** The player's height: the full height asked for, or 0. */
    val playerPx: Int,
    /** True when the player is wanted and its full height does not fit. */
    val playerRefused: Boolean,
    /** The picker's height, between its minimum and its maximum, or 0. */
    val pickerPx: Int,
    /** True when the picker is wanted and not even its minimum fits above the list's floor. */
    val pickerRefused: Boolean,
    /** What is left for the chat list. */
    val listPx: Int
)

/**
 * The chat page's height allocation, as one pure function, so that the whole policy can be tested
 * without a device. [ChatPageLayout] only applies it.
 *
 * PROTOTYPE (branch `prototype/5d-layout-b`), not for merging. The order is Federico's choice for
 * this prototype:
 * 1. the children that never yield are measured first, so the composer's text field is always
 *    entirely on screen: nothing below may take its room;
 * 2. the player gets its full height whenever that fits, keyboard up or not. The list's floor does
 *    not apply against the player, so the list may go below it, down to nothing;
 * 3. the picker gets its full height, or less down to its minimum, as long as the list keeps its
 *    floor. The floor applies against the picker only;
 * 4. the list takes what is left.
 *
 * What a refused panel does is not decided. Here it is held hidden at no height until the room
 * comes back, because that needs no state change from inside a measure pass.
 */
object ChatPagePolicy {

    /**
     * Shares [availablePx] between the player, the picker and the list. Takes plain values, so
     * that a measure pass allocates nothing but the result.
     *
     * @param availablePx the page's inner height less every child that never yields: the bars,
     *   the problem row, the reply bar when shown and the composer row. Negative when those alone
     *   do not fit.
     * @param playerWanted true while the stream player is shown.
     * @param playerPx the player's one height. It never shrinks: it is given whole, or refused.
     * @param pickerWanted true while the emote picker is open.
     * @param pickerMaxPx the picker's full height.
     * @param pickerMinPx the picker's smallest useful height. Below it the picker is refused.
     * @param listFloorPx the list height the picker must leave: a number of chat rows at the
     *   user's font scale.
     */
    fun allocate(
        availablePx: Int,
        playerWanted: Boolean,
        playerPx: Int,
        pickerWanted: Boolean,
        pickerMaxPx: Int,
        pickerMinPx: Int,
        listFloorPx: Int
    ): ChatPageAllocation {
        val room = availablePx.coerceAtLeast(0)

        val playerFits = playerWanted && playerPx in 0..room
        val player = if (playerFits) playerPx else 0
        val afterPlayer = room - player

        val pickerRoom = afterPlayer - listFloorPx.coerceAtLeast(0)
        val picker = if (pickerWanted) {
            val height = minOf(pickerMaxPx, pickerRoom)
            if (height >= pickerMinPx && height > 0) height else 0
        } else {
            0
        }

        return ChatPageAllocation(
            playerPx = player,
            playerRefused = playerWanted && !playerFits,
            pickerPx = picker,
            pickerRefused = pickerWanted && picker == 0,
            listPx = afterPlayer - picker
        )
    }

    /**
     * The list's floor: [rows] chat rows of [rowPx] each, plus the list's bottom padding, which
     * shows no row. Its top padding is not added: the list has `clipToPadding="false"`, so rows
     * scroll through it and are seen there.
     */
    fun listFloorPx(rows: Int, rowPx: Int, listBottomPaddingPx: Int): Int {
        return rows.coerceAtLeast(0) * rowPx.coerceAtLeast(0) + listBottomPaddingPx.coerceAtLeast(0)
    }
}
