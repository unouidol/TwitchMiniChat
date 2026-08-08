package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatKeyboardInsetPolicyTest {

    /** Accepts a docked inset when the OEM visibility flag has not caught up yet. */
    @Test
    fun imeVisibility_dockedInsetOverridesDelayedFlag() {
        assertEquals(
            true,
            ChatKeyboardInsetPolicy.isImeConsideredVisible(
                imeVisible = false,
                imeBottom = 972,
                navigationBarBottom = 126
            )
        )
    }

    /** Does not treat the navigation bar alone as an open keyboard. */
    @Test
    fun imeVisibility_navigationInsetAloneRemainsHidden() {
        assertEquals(
            false,
            ChatKeyboardInsetPolicy.isImeConsideredVisible(
                imeVisible = false,
                imeBottom = 126,
                navigationBarBottom = 126
            )
        )
    }

    /** Keeps the composer above three-button navigation while the keyboard is closed. */
    @Test
    fun keyboardHidden_keepsNavigationBarPadding() {
        assertEquals(
            126,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = false,
                imeBottom = 0,
                navigationBarBottom = 126,
                measuredImeOverlap = 0
            )
        )
    }

    /** Lets adjustResize own the bottom edge after it removes the full overlap. */
    @Test
    fun fullyResizedWindow_doesNotApplyKeyboardPaddingTwice() {
        assertEquals(
            0,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = true,
                imeBottom = 972,
                navigationBarBottom = 126,
                measuredImeOverlap = 0
            )
        )
    }

    /** Applies the complete remaining overlap in an edge-to-edge window. */
    @Test
    fun edgeToEdgeWindow_appliesCompleteImeOverlap() {
        assertEquals(
            972,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = true,
                imeBottom = 972,
                navigationBarBottom = 126,
                measuredImeOverlap = 972
            )
        )
    }

    /** Applies only the portion that native resize did not remove. */
    @Test
    fun partiallyResizedWindow_appliesResidualImeOverlap() {
        assertEquals(
            280,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = true,
                imeBottom = 972,
                navigationBarBottom = 126,
                measuredImeOverlap = 280
            )
        )
    }

    /** Uses a docked IME inset even if an OEM visibility flag arrives late. */
    @Test
    fun delayedVisibilityFlag_usesMeasuredDockedImeOverlap() {
        assertEquals(
            972,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = false,
                imeBottom = 972,
                navigationBarBottom = 126,
                measuredImeOverlap = 972
            )
        )
    }

    /** Keeps navigation protection if a stale OEM inset has no measured overlap. */
    @Test
    fun staleInvisibleImeWithoutOverlap_keepsNavigationBarPadding() {
        assertEquals(
            126,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = false,
                imeBottom = 972,
                navigationBarBottom = 126,
                measuredImeOverlap = 0
            )
        )
    }

    /** Preserves navigation-bar protection for a floating zero-height keyboard. */
    @Test
    fun floatingKeyboardVisible_keepsNavigationBarPadding() {
        assertEquals(
            126,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = true,
                imeBottom = 0,
                navigationBarBottom = 126,
                measuredImeOverlap = 0
            )
        )
    }

    /** Treats malformed negative inset values as zero. */
    @Test
    fun negativeInsets_areClampedToZero() {
        assertEquals(
            0,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = false,
                imeBottom = -10,
                navigationBarBottom = -20,
                measuredImeOverlap = -30
            )
        )
    }

    /** Calculates complete overlap when the root still reaches the window bottom. */
    @Test
    fun overlapMeasurement_fullHeightRootReturnsImeHeight() {
        assertEquals(
            972,
            ChatKeyboardInsetPolicy.overlappingImeHeight(
                rootBottom = 2_400,
                imeTop = 1_428,
                imeBottom = 972
            )
        )
    }

    /** Calculates no overlap when native resize ends the root at the IME top. */
    @Test
    fun overlapMeasurement_resizedRootReturnsZero() {
        assertEquals(
            0,
            ChatKeyboardInsetPolicy.overlappingImeHeight(
                rootBottom = 1_428,
                imeTop = 1_428,
                imeBottom = 972
            )
        )
    }

    /** Calculates only the residual overlap after a partial native resize. */
    @Test
    fun overlapMeasurement_partiallyResizedRootReturnsResidualHeight() {
        assertEquals(
            280,
            ChatKeyboardInsetPolicy.overlappingImeHeight(
                rootBottom = 1_708,
                imeTop = 1_428,
                imeBottom = 972
            )
        )
    }

    /** Clamps inconsistent geometry to the reported IME height. */
    @Test
    fun overlapMeasurement_inconsistentGeometryIsClamped() {
        assertEquals(
            972,
            ChatKeyboardInsetPolicy.overlappingImeHeight(
                rootBottom = 4_000,
                imeTop = 1_428,
                imeBottom = 972
            )
        )
    }
}
