package com.fs.twitchminichat.welcome

import com.fs.twitchminichat.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins what each situation puts on the page: its title, its closing button, whether the
 * update notice appears, and whether the page says where to find it again.
 */
class WelcomePageTextTest {

    @Test
    fun firstLaunch() {
        assertEquals(
            WelcomePageText(
                title = R.string.welcome_title_first_launch,
                close = R.string.welcome_continue,
                noticeVersion = null,
                versionLine = null,
                showsWhereToFindAgain = true
            ),
            WelcomePageText.of(WelcomePage(WelcomeSituation.FIRST_LAUNCH, "5.6.0"))
        )
    }

    @Test
    fun updated() {
        assertEquals(
            WelcomePageText(
                title = R.string.welcome_title_updated,
                close = R.string.welcome_continue,
                noticeVersion = null,
                versionLine = null,
                showsWhereToFindAgain = true
            ),
            WelcomePageText.of(WelcomePage(WelcomeSituation.UPDATED, "5.6.0"))
        )
    }

    @Test
    fun updateAvailable_namesTheNewVersion() {
        assertEquals(
            WelcomePageText(
                title = R.string.welcome_title_update_available,
                close = R.string.welcome_not_now,
                noticeVersion = "5.6.1",
                versionLine = null,
                showsWhereToFindAgain = true
            ),
            WelcomePageText.of(
                WelcomePage(WelcomeSituation.UPDATE_AVAILABLE, "5.6.0", availableVersion = "5.6.1")
            )
        )
    }

    @Test
    fun menu_withANewerRelease_showsTheNoticeAndNoVersionLine() {
        assertEquals(
            WelcomePageText(
                title = R.string.about_tmc,
                close = R.string.welcome_close,
                noticeVersion = "5.6.1",
                versionLine = null,
                showsWhereToFindAgain = false
            ),
            WelcomePageText.of(
                WelcomePage(WelcomeSituation.MENU, "5.6.0", availableVersion = "5.6.1")
            )
        )
    }

    @Test
    fun menu_withoutANewerRelease_showsItsVersionLine() {
        val checked = WelcomePage(
            WelcomeSituation.MENU,
            "5.6.0",
            menuVersionLine = MenuVersionLine.NO_NEWER_AT_LAST_CHECK
        )
        val notChecked = checked.copy(menuVersionLine = MenuVersionLine.NOT_CHECKED)

        assertEquals(R.string.welcome_version_latest, WelcomePageText.of(checked).versionLine)
        assertEquals(R.string.welcome_version_installed, WelcomePageText.of(notChecked).versionLine)
        assertEquals(false, WelcomePageText.of(notChecked).showsWhereToFindAgain)
    }
}
