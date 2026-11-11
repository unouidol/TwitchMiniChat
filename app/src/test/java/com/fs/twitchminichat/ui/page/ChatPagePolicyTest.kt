package com.fs.twitchminichat.ui.page

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterizes [ChatPagePolicy], the prototype's height allocation, in plain units. The figures
 * are tenths of a dp from the 5d plan's variant B model, rounded, so each case can be checked
 * against the prediction it stands for.
 */
class ChatPagePolicyTest {

    private val player = 3000
    private val pickerMax = 2400
    private val pickerMin = 1520
    private val floor2 = 690
    private val floor3 = 974
    private val floor4 = 1259

    private fun request(
        available: Int,
        playerWanted: Boolean = false,
        pickerWanted: Boolean = false,
        floor: Int = floor3
    ) = ChatPagePolicy.allocate(
        availablePx = available,
        playerWanted = playerWanted,
        playerPx = player,
        pickerWanted = pickerWanted,
        pickerMaxPx = pickerMax,
        pickerMinPx = pickerMin,
        listFloorPx = floor
    )

    @Test
    fun nothingOpen_listTakesAllTheRoom() {
        val a = request(available = 7258)

        assertEquals(ChatPageAllocation(0, false, 0, false, 7258), a)
    }

    @Test
    fun playerFits_getsItsFullHeight() {
        val a = request(available = 7258, playerWanted = true)

        assertEquals(player, a.playerPx)
        assertFalse(a.playerRefused)
        assertEquals(4258, a.listPx)
    }

    @Test
    fun keyboardUp_playerStays_listGoesBelowTheFloor() {
        // The typical phone, stream on, keyboard up: 46.6 dp of list, below every floor.
        val a = request(available = 3466, playerWanted = true, floor = floor4)

        assertEquals(player, a.playerPx)
        assertFalse(a.playerRefused)
        assertEquals(466, a.listPx)
    }

    @Test
    fun playerDoesNotFit_isRefused_andTheListKeepsTheRoom() {
        // The reference phone in landscape: 216.9 dp of room for a 300 dp player.
        val a = request(available = 2169, playerWanted = true)

        assertEquals(0, a.playerPx)
        assertTrue(a.playerRefused)
        assertEquals(2169, a.listPx)
    }

    @Test
    fun playerExactlyFits_isNotRefused() {
        val a = request(available = player, playerWanted = true)

        assertEquals(player, a.playerPx)
        assertFalse(a.playerRefused)
        assertEquals(0, a.listPx)
    }

    @Test
    fun pickerWithRoom_getsItsMaximum() {
        val a = request(available = 7258, pickerWanted = true)

        assertEquals(pickerMax, a.pickerPx)
        assertFalse(a.pickerRefused)
        assertEquals(4858, a.listPx)
    }

    @Test
    fun pickerShrinksToKeepTheFloor() {
        // Density 450, stream on, picker open: 279.9 dp after the player.
        val atFloor2 = request(5799, playerWanted = true, pickerWanted = true, floor = floor2)
        val atFloor3 = request(5799, playerWanted = true, pickerWanted = true, floor = floor3)
        val atFloor4 = request(5799, playerWanted = true, pickerWanted = true, floor = floor4)

        assertEquals(2109, atFloor2.pickerPx)
        assertEquals(floor2, atFloor2.listPx)
        assertEquals(1825, atFloor3.pickerPx)
        assertEquals(floor3, atFloor3.listPx)
        assertEquals(1540, atFloor4.pickerPx)
        assertEquals(floor4, atFloor4.listPx)
    }

    @Test
    fun pickerAtExactlyItsMinimum_opens() {
        val a = request(available = pickerMin + floor3, pickerWanted = true)

        assertEquals(pickerMin, a.pickerPx)
        assertFalse(a.pickerRefused)
        assertEquals(floor3, a.listPx)
    }

    @Test
    fun pickerBelowItsMinimum_isRefused_andTheListKeepsTheRoom() {
        val available = pickerMin + floor3 - 1
        val a = request(available = available, pickerWanted = true)

        assertEquals(0, a.pickerPx)
        assertTrue(a.pickerRefused)
        assertEquals(available, a.listPx)
    }

    @Test
    fun keyboardUp_withPlayer_refusesThePicker_notThePlayer() {
        val a = request(available = 3466, playerWanted = true, pickerWanted = true)

        assertEquals(player, a.playerPx)
        assertTrue(a.pickerRefused)
        assertEquals(466, a.listPx)
    }

    @Test
    fun barsAloneOverflow_everythingRefused_listIsZero() {
        val a = request(available = -120, playerWanted = true, pickerWanted = true)

        assertEquals(ChatPageAllocation(0, true, 0, true, 0), a)
    }

    @Test
    fun theListIsNeverNegative_andTheHeightsAddUpToTheRoom() {
        for (available in listOf(0, 500, 1520, 2494, 3000, 3466, 4000, 5799, 7258, 9000)) {
            for (floor in listOf(0, floor2, floor3, floor4)) {
                for (playerWanted in listOf(false, true)) {
                    for (pickerWanted in listOf(false, true)) {
                        val a = request(available, playerWanted, pickerWanted, floor)
                        assertTrue(a.listPx >= 0)
                        assertEquals(available, a.playerPx + a.pickerPx + a.listPx)
                        if (a.pickerPx > 0) assertTrue(a.listPx >= floor)
                    }
                }
            }
        }
    }

    @Test
    fun listFloor_isRowsTimesRowPlusTheBottomPadding() {
        // 68 px rows and 12 dp of padding at 2.3875 px/dp, the reference phone.
        assertEquals(2 * 68 + 29, ChatPagePolicy.listFloorPx(rows = 2, rowPx = 68, listBottomPaddingPx = 29))
        assertEquals(29, ChatPagePolicy.listFloorPx(rows = 0, rowPx = 68, listBottomPaddingPx = 29))
        assertEquals(0, ChatPagePolicy.listFloorPx(rows = -1, rowPx = -5, listBottomPaddingPx = -3))
    }
}
