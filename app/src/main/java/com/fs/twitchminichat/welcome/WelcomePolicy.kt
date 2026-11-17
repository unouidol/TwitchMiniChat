package com.fs.twitchminichat.welcome

/**
 * What the welcome page remembers between starts, as stored in [WelcomePrefs].
 *
 * @property lastSeenVersionCode the `versionCode` of the last start that ran the decision.
 *     `null` means no start ever has, which is what "first launch" means. One value serves
 *     as both "first launch seen" and "last version seen": two flags could disagree, and
 *     one cannot.
 * @property latestReleaseTag the tag from the last successful version check.
 * @property dismissedReleaseTag the tag whose update notice was last shown.
 * @property lastCheckEpochMs when the last version check was attempted, successful or not.
 */
data class WelcomeState(
    val lastSeenVersionCode: Int? = null,
    val latestReleaseTag: String? = null,
    val dismissedReleaseTag: String? = null,
    val lastCheckEpochMs: Long? = null
)

/**
 * What a start writes back: the version it ran, and the tag whose notice has been shown.
 * The tag is `null` only when no notice has ever been shown.
 */
data class StartRecord(
    val lastSeenVersionCode: Int,
    val dismissedReleaseTag: String?
)

/** Which page is on screen, and why. */
enum class WelcomeSituation {
    /** No start has ever run the decision: a fresh install, or the first start after a reset. */
    FIRST_LAUNCH,

    /** The installed `versionCode` is higher than at the last start. */
    UPDATED,

    /** A newer release is known and its notice has not been shown yet. */
    UPDATE_AVAILABLE,

    /** Opened from Safety & Privacy, on demand. */
    MENU
}

/** What the menu page says about versions when no newer release is known. */
enum class MenuVersionLine {
    /** A check has succeeded, and the release it found is not newer. */
    NO_NEWER_AT_LAST_CHECK,

    /** No check has succeeded yet, or this build never checks (dev). */
    NOT_CHECKED
}

/**
 * The content of one appearance of the page, decided before it is drawn.
 *
 * @property availableVersion the newer release to name, as `5.6.1`, or `null` for no notice.
 * @property menuVersionLine shown only on the menu page without a notice.
 */
data class WelcomePage(
    val situation: WelcomeSituation,
    val installedVersionName: String,
    val availableVersion: String? = null,
    val menuVersionLine: MenuVersionLine? = null
)

/**
 * Every decision the welcome page and its version check make, as pure functions.
 *
 * Nothing here touches Android, so each row of the table of situations in
 * `docs/proposal-welcome-page.md` is a unit test of this object.
 */
object WelcomePolicy {

    /** At most one version check per this interval: "at most once a day". */
    const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L

    /**
     * Whether this start runs the welcome decision.
     *
     * @param savedPending the pending flag saved when the activity was recreated, or
     *     `null` on a fresh creation. A rotation while the terms gate is up would
     *     otherwise lose the decision for that start.
     * @param openedToAnAccount true when the start opened a chat directly: a tapped alert,
     *     or a sign-in that just finished. That user came for the chat, so the page waits
     *     for the next ordinary start.
     */
    fun decidesAtThisStart(savedPending: Boolean?, openedToAnAccount: Boolean): Boolean =
        savedPending ?: !openedToAnAccount

    /**
     * The page to show at a start, or `null` for none.
     *
     * The order is the priority:
     * 1. **First launch.** Nothing is recorded yet. It wins over a known update. They can
     *    meet only when a check that was in flight during a reset writes its tag into the
     *    fresh file, and the first-launch page is the one owed after a reset.
     * 2. **A newer release, not yet shown.** It comes before "updated", so that a user who
     *    installs one version while a later one is already known sees one page, not two.
     * 3. **Updated** since the last start.
     */
    fun decideAtStart(
        state: WelcomeState,
        installedVersionCode: Int,
        installedVersionName: String,
        updateNoticeEnabled: Boolean
    ): WelcomePage? {
        val lastSeen = state.lastSeenVersionCode
            ?: return WelcomePage(WelcomeSituation.FIRST_LAUNCH, installedVersionName)

        val available = availableUpdate(state, installedVersionName, updateNoticeEnabled)
        if (available != null && state.latestReleaseTag != state.dismissedReleaseTag) {
            return WelcomePage(
                WelcomeSituation.UPDATE_AVAILABLE,
                installedVersionName,
                availableVersion = available
            )
        }

        if (installedVersionCode > lastSeen) {
            return WelcomePage(WelcomeSituation.UPDATED, installedVersionName)
        }

        return null
    }

    /**
     * What the start records once it has decided, whether or not a page is shown.
     *
     * The page counts as seen as soon as it is shown. Its close is not waited for, because
     * not every way a page leaves the screen can be observed: the task swiped away, or the
     * process killed in the background. Whatever happens next, it appears once.
     */
    fun recordAfterStart(
        state: WelcomeState,
        shown: WelcomePage?,
        installedVersionCode: Int
    ): StartRecord = StartRecord(
        lastSeenVersionCode = installedVersionCode,
        dismissedReleaseTag = if (shown?.situation == WelcomeSituation.UPDATE_AVAILABLE) {
            state.latestReleaseTag
        } else {
            state.dismissedReleaseTag
        }
    )

    /** The page Safety & Privacy opens. It names a newer release whenever one is known. */
    fun pageForMenu(
        state: WelcomeState,
        installedVersionName: String,
        updateNoticeEnabled: Boolean
    ): WelcomePage {
        val available = availableUpdate(state, installedVersionName, updateNoticeEnabled)
        val line = when {
            available != null -> null
            updateNoticeEnabled && state.latestReleaseTag != null ->
                MenuVersionLine.NO_NEWER_AT_LAST_CHECK
            else -> MenuVersionLine.NOT_CHECKED
        }
        return WelcomePage(
            WelcomeSituation.MENU,
            installedVersionName,
            availableVersion = available,
            menuVersionLine = line
        )
    }

    /**
     * Whether a version check is due.
     *
     * A last attempt dated in the future counts as due. That happens when the clock was
     * set back, and without this a wrong clock could silence the check until the date
     * came round again.
     */
    fun isCheckDue(
        updateNoticeEnabled: Boolean,
        lastCheckEpochMs: Long?,
        nowEpochMs: Long
    ): Boolean {
        if (!updateNoticeEnabled) return false
        val last = lastCheckEpochMs ?: return true
        if (last > nowEpochMs) return true
        return nowEpochMs - last >= CHECK_INTERVAL_MS
    }

    /**
     * The tag a finished check stores, or `null` to store nothing and keep what was there.
     * A tag that does not parse is never stored.
     */
    fun storableTag(fetchedTag: String?): String? =
        fetchedTag?.takeIf { ReleaseVersion.parseTag(it) != null }

    private fun availableUpdate(
        state: WelcomeState,
        installedVersionName: String,
        updateNoticeEnabled: Boolean
    ): String? {
        if (!updateNoticeEnabled) return null
        val tag = state.latestReleaseTag
        if (!ReleaseVersion.isNewer(tag, installedVersionName)) return null
        return ReleaseVersion.parseTag(tag)?.toString()
    }
}

/**
 * Lets one start run the welcome decision once, however often the activity resumes.
 *
 * `MainActivity` resumes after every screen it opens, and after the terms gate's policy
 * pages. Only the first resume of a start decides; the later ones find nothing pending.
 * The flag is saved with the activity, so a rotation before the terms are accepted keeps
 * the decision owed.
 */
class WelcomeStartGate(pending: Boolean) {

    /** Whether the decision is still owed. This is what the activity saves. */
    var pending: Boolean = pending
        private set

    /** Returns true exactly once, the first time it is called while pending. */
    fun claim(): Boolean {
        if (!pending) return false
        pending = false
        return true
    }
}
