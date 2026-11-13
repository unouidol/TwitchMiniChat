package com.fs.twitchminichat.welcome

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.fs.twitchminichat.BuildConfig
import kotlin.concurrent.thread

/**
 * Connects [WelcomePolicy] to the screens, the preference file and the network.
 *
 * Every decision is made by the policy. This object reads the state, writes back what the
 * policy says to record, shows a page, and starts the version check.
 */
object WelcomeCoordinator {

    private const val TAG = "TMC_WELCOME"

    /**
     * The welcome decision of one start, run once the terms are accepted.
     *
     * The order matters:
     * 1. **Decide from what is stored.** A check started now can only affect a later start,
     *    so an answer arriving mid-session never raises the page.
     * 2. **Record before showing.** The page counts as seen once it is shown.
     * 3. **Start the check last,** off the main thread. Nothing here waits for it.
     *
     * Running only after the terms are accepted keeps the check, a request declared in the
     * privacy policy, from going out before that policy has been accepted.
     */
    fun runStartDecision(activity: FragmentActivity) {
        val prefs = WelcomePrefs(activity)
        val state = prefs.read()

        val page = WelcomePolicy.decideAtStart(
            state = state,
            installedVersionCode = BuildConfig.VERSION_CODE,
            installedVersionName = BuildConfig.VERSION_NAME,
            updateNoticeEnabled = BuildConfig.UPDATE_NOTICE_ENABLED
        )
        prefs.recordStart(WelcomePolicy.recordAfterStart(state, page, BuildConfig.VERSION_CODE))
        Log.d(TAG, "start decision=${page?.situation ?: "none"}")

        if (page != null) {
            WelcomeDialogFragment.show(activity.supportFragmentManager, page)
        }

        startCheckIfDue(activity.applicationContext, state.lastCheckEpochMs)
    }

    /** Opens the page from Safety & Privacy. It records nothing, and dismisses nothing. */
    fun showFromMenu(activity: FragmentActivity) {
        val page = WelcomePolicy.pageForMenu(
            state = WelcomePrefs(activity).read(),
            installedVersionName = BuildConfig.VERSION_NAME,
            updateNoticeEnabled = BuildConfig.UPDATE_NOTICE_ENABLED
        )
        WelcomeDialogFragment.show(activity.supportFragmentManager, page)
    }

    /**
     * Starts the daily version check if it is due. Dev builds never check
     * ([WelcomePolicy.isCheckDue]).
     *
     * The attempt is recorded before the request goes out, so a failure also waits a day.
     * That is what "at most once a day" means: an offline phone does not ask again at
     * every start.
     *
     * A check that is still in flight when a reset erases the file writes its tag
     * afterwards. That is accepted knowingly, as `LocalDataCleaner` accepts the alert
     * audio watcher's lines: the tag is a fact about GitHub, not about the user, and the
     * file it lands in still reads as a first launch.
     */
    private fun startCheckIfDue(context: Context, lastCheckEpochMs: Long?) {
        val now = System.currentTimeMillis()
        if (!WelcomePolicy.isCheckDue(BuildConfig.UPDATE_NOTICE_ENABLED, lastCheckEpochMs, now)) {
            return
        }

        val prefs = WelcomePrefs(context)
        prefs.recordCheckAttempt(now)

        thread(name = "tmc-update-check") {
            WelcomePolicy.storableTag(LatestReleaseClient.fetchLatestTag())
                ?.let(prefs::recordLatestTag)
        }
    }
}
