package com.fs.twitchminichat.welcome

import androidx.annotation.StringRes
import com.fs.twitchminichat.R

/**
 * Which texts and blocks one appearance of the welcome page shows.
 *
 * It is decided here, from the [WelcomePage] alone, so that the content of every situation
 * is a unit test rather than something only a phone can show.
 *
 * @property title the title. It is formatted with the installed version, which only
 *     `welcome_title_updated` has a place for.
 * @property close the closing button's label.
 * @property noticeVersion the release the update notice names, or `null` for no notice.
 * @property versionLine the menu page's line about versions, or `null` for none.
 * @property showsWhereToFindAgain the one line that says where the page can be reopened.
 *     The menu page leaves it out, because its reader is already there.
 */
data class WelcomePageText(
    @param:StringRes val title: Int,
    @param:StringRes val close: Int,
    val noticeVersion: String?,
    @param:StringRes val versionLine: Int?,
    val showsWhereToFindAgain: Boolean
) {
    companion object {

        /** The texts for [page]. */
        fun of(page: WelcomePage): WelcomePageText = when (page.situation) {
            WelcomeSituation.FIRST_LAUNCH -> WelcomePageText(
                title = R.string.welcome_title_first_launch,
                close = R.string.welcome_continue,
                noticeVersion = null,
                versionLine = null,
                showsWhereToFindAgain = true
            )

            WelcomeSituation.UPDATED -> WelcomePageText(
                title = R.string.welcome_title_updated,
                close = R.string.welcome_continue,
                noticeVersion = null,
                versionLine = null,
                showsWhereToFindAgain = true
            )

            WelcomeSituation.UPDATE_AVAILABLE -> WelcomePageText(
                title = R.string.welcome_title_update_available,
                close = R.string.welcome_not_now,
                noticeVersion = page.availableVersion,
                versionLine = null,
                showsWhereToFindAgain = true
            )

            WelcomeSituation.MENU -> WelcomePageText(
                title = R.string.about_tmc,
                close = R.string.welcome_close,
                noticeVersion = page.availableVersion,
                versionLine = when (page.menuVersionLine) {
                    MenuVersionLine.NO_NEWER_AT_LAST_CHECK -> R.string.welcome_version_latest
                    MenuVersionLine.NOT_CHECKED -> R.string.welcome_version_installed
                    null -> null
                },
                showsWhereToFindAgain = false
            )
        }
    }
}
