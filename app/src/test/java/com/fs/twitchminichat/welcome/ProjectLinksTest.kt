package com.fs.twitchminichat.welcome

import com.fs.twitchminichat.ProjectLinks
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the project's links exactly as Federico supplied them on 2026-11-12, the update
 * button's download address, and everything the version check sends.
 *
 * A changed link then fails here, so the change is visible in review.
 */
class ProjectLinksTest {

    @Test
    fun theLinksAreTheOnesSupplied() {
        assertEquals("https://discord.gg/E8yRUp2MBK", ProjectLinks.DISCORD_INVITE_URL)
        assertEquals("tmc-ticket", ProjectLinks.BUG_REPORT_CHANNEL)
        assertEquals("https://ko-fi.com/tmc_app", ProjectLinks.KOFI_URL)
    }

    @Test
    fun theCheckAsksTheSameEndpointAsTheWebsite() {
        assertEquals(
            "https://api.github.com/repos/unouidol/TwitchMiniChat/releases/latest",
            ProjectLinks.LATEST_RELEASE_API_URL
        )
    }

    /** The asset names are a contract with the website and with every past release. */
    @Test
    fun theDownloadIsTheInstalledBuildsApk() {
        assertEquals(
            "https://github.com/unouidol/TwitchMiniChat/releases/latest/download/" +
                "TwitchMiniChat-Android-arm64-v8a.apk",
            ProjectLinks.latestApkUrl(is64BitProcess = true)
        )
        assertEquals(
            "https://github.com/unouidol/TwitchMiniChat/releases/latest/download/" +
                "TwitchMiniChat-Android-armeabi-v7a.apk",
            ProjectLinks.latestApkUrl(is64BitProcess = false)
        )
    }

    /**
     * Everything the check sends besides the address: a fixed product name and the format
     * it asks for. Nothing names the phone, the user or the installation.
     */
    @Test
    fun theCheckSendsOnlyAFixedUserAgentAndAnAccept() {
        assertEquals(
            mapOf(
                "User-Agent" to "TwitchMiniChat-Android",
                "Accept" to "application/vnd.github+json"
            ),
            LatestReleaseClient.REQUEST_HEADERS
        )
    }
}
