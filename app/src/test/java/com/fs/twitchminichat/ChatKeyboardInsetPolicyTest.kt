package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatKeyboardInsetPolicyTest {

    /** Keeps the composer above three-button navigation while the keyboard is closed. */
    @Test
    fun keyboardHidden_keepsNavigationBarPadding() {
        assertEquals(
            126,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = false,
                imeBottom = 0,
                navigationBarBottom = 126
            )
        )
    }

    /** Lets adjustResize own the bottom edge while a docked keyboard is visible. */
    @Test
    fun dockedKeyboardVisible_doesNotApplyKeyboardPaddingTwice() {
        assertEquals(
            0,
            ChatKeyboardInsetPolicy.rootBottomPadding(
                imeVisible = true,
                imeBottom = 972,
                navigationBarBottom = 126
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
                navigationBarBottom = 126
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
                navigationBarBottom = -20
            )
        )
    }
}
