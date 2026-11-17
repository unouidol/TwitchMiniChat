package com.fs.twitchminichat

/**
 * The project's own public addresses, each defined here and nowhere else.
 *
 * Changing a link is a one-line change. The strings that show them take the value as a
 * format argument rather than repeating it. The values are public by intent, and are
 * Federico's, supplied on 2026-11-12.
 *
 * The Discord invite is compiled in as given. An invite can expire, stop after a number
 * of uses, or be revoked, and a link compiled into an APK cannot be corrected until the
 * next release. Federico confirmed on 2026-11-13 that this one is permanent, which closes
 * that risk; see `docs/proposal-welcome-page.md`, *The risk in a compiled invite*.
 */
object ProjectLinks {

    /** The TMC Discord server, for news, help and bug reports. */
    const val DISCORD_INVITE_URL = "https://discord.gg/E8yRUp2MBK"

    /** The Discord channel where bugs and problems are reported, without its `#`. */
    const val BUG_REPORT_CHANNEL = "tmc-ticket"

    /** The project's Ko-fi page, for voluntary support. */
    const val KOFI_URL = "https://ko-fi.com/tmc_app"

    /**
     * GitHub's answer to "which release is the latest", the same source the website reads.
     *
     * It never returns a draft or a pre-release, so a release published for testing never
     * reaches the update notice.
     */
    const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/unouidol/TwitchMiniChat/releases/latest"

    /**
     * The release asset names, a contract with the public website: both link to
     * `releases/latest/download/<name>`, which resolves only on an exact match.
     */
    const val APK_ASSET_ARM64 = "TwitchMiniChat-Android-arm64-v8a.apk"

    /** The 32-bit asset; see [APK_ASSET_ARM64]. */
    const val APK_ASSET_ARMV7 = "TwitchMiniChat-Android-armeabi-v7a.apk"

    private const val LATEST_DOWNLOAD_BASE =
        "https://github.com/unouidol/TwitchMiniChat/releases/latest/download"

    /**
     * The latest release's APK for the build that is running, which the update button opens.
     *
     * The direct file is Federico's choice, made because it costs the user the fewest taps.
     * The browser downloads it; the app never does. The two published APKs differ only in
     * their native code, so a 64-bit process can only be running the `arm64-v8a` one, and a
     * 32-bit process the `armeabi-v7a` one. The result is the file the user already has.
     *
     * @param is64BitProcess `android.os.Process.is64Bit()` of the running app.
     */
    fun latestApkUrl(is64BitProcess: Boolean): String {
        val asset = if (is64BitProcess) APK_ASSET_ARM64 else APK_ASSET_ARMV7
        return "$LATEST_DOWNLOAD_BASE/$asset"
    }
}
