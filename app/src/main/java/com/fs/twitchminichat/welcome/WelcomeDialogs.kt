package com.fs.twitchminichat.welcome

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.fs.twitchminichat.BuildConfig
import com.fs.twitchminichat.BuildIdentity
import com.fs.twitchminichat.ProjectLinks
import com.fs.twitchminichat.R

/**
 * The welcome page, drawn from a [WelcomePage] passed in its arguments.
 *
 * A dialog, not an activity, and that is the reason it exists in this form. An opaque
 * activity on top would stop `MainActivity`, and the chat page closes its IRC connection
 * when it stops (`ChatFragment.onStop`). A dialog changes no lifecycle, so a signed-in
 * user's chat keeps connecting underneath. As a `DialogFragment` it also comes back by
 * itself after a rotation, from its arguments.
 *
 * It records nothing. The start that shows it has already recorded it as seen
 * ([WelcomePolicy.recordAfterStart]).
 */
class WelcomeDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val page = pageFrom(requireArguments())
        val text = WelcomePageText.of(page)
        val context = requireContext()
        /*
         * The activity's inflater, which carries its theme. The fragment's own would build
         * this dialog again from inside onCreateDialog.
         */
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_welcome, null)

        bindNotice(view, text.noticeVersion)

        view.findViewById<TextView>(R.id.textWelcomeVersionLine).apply {
            val line = text.versionLine
            visibility = if (line == null) View.GONE else View.VISIBLE
            if (line != null) setText(getString(line, page.installedVersionName))
        }

        view.findViewById<TextView>(R.id.textWelcomeBugReport).text =
            getString(R.string.welcome_bug_report, ProjectLinks.BUG_REPORT_CHANNEL)

        view.findViewById<Button>(R.id.btnWelcomeDiscord).setOnClickListener {
            ProjectLinkOpener.open(context, ProjectLinks.DISCORD_INVITE_URL)
        }
        view.findViewById<Button>(R.id.btnWelcomeKofi).setOnClickListener {
            ProjectLinkOpener.open(context, ProjectLinks.KOFI_URL)
        }

        view.findViewById<TextView>(R.id.textWelcomeWhereAgain).visibility =
            if (text.showsWhereToFindAgain) View.VISIBLE else View.GONE

        view.findViewById<Button>(R.id.btnWelcomeClose).apply {
            setText(text.close)
            contentDescription = getString(text.close)
            setOnClickListener { dismiss() }
        }

        return AlertDialog.Builder(context)
            .setTitle(getString(text.title, page.installedVersionName))
            .setView(view)
            .create()
            .apply { setCanceledOnTouchOutside(false) }
    }

    /** The update notice: the new version's number and the button, and nothing else. */
    private fun bindNotice(view: View, version: String?) {
        view.findViewById<View>(R.id.welcomeNoticeBlock).visibility =
            if (version == null) View.GONE else View.VISIBLE
        if (version == null) return

        view.findViewById<TextView>(R.id.textWelcomeNotice).text =
            getString(R.string.welcome_update_notice, version)

        view.findViewById<Button>(R.id.btnWelcomeDownload).apply {
            contentDescription = getString(R.string.welcome_download_description, version)
            setOnClickListener {
                ProjectLinkOpener.open(
                    requireContext(),
                    ProjectLinks.latestApkUrl(android.os.Process.is64Bit())
                )
            }
        }
    }

    companion object {
        /** The fragment tag, one per screen it is shown on. */
        const val TAG = "welcome_page"

        private const val ARG_SITUATION = "situation"
        private const val ARG_INSTALLED_VERSION = "installed_version"
        private const val ARG_AVAILABLE_VERSION = "available_version"
        private const val ARG_MENU_VERSION_LINE = "menu_version_line"

        /** Shows [page] over the screen that owns [fragmentManager]. */
        fun show(fragmentManager: FragmentManager, page: WelcomePage) {
            WelcomeDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SITUATION, page.situation.name)
                    putString(ARG_INSTALLED_VERSION, page.installedVersionName)
                    putString(ARG_AVAILABLE_VERSION, page.availableVersion)
                    putString(ARG_MENU_VERSION_LINE, page.menuVersionLine?.name)
                }
            }.show(fragmentManager, TAG)
        }

        private fun pageFrom(args: Bundle) = WelcomePage(
            situation = WelcomeSituation.valueOf(args.getString(ARG_SITUATION).orEmpty()),
            installedVersionName = args.getString(ARG_INSTALLED_VERSION).orEmpty(),
            availableVersion = args.getString(ARG_AVAILABLE_VERSION),
            menuVersionLine = args.getString(ARG_MENU_VERSION_LINE)?.let(MenuVersionLine::valueOf)
        )
    }
}

/**
 * The Bug report page, opened from Safety & Privacy.
 *
 * It sends the user to the Discord channel and says a diagnostics file may be asked for
 * there. Following Federico's decision of 2026-11-12, it offers no export of the
 * diagnostics journal and does not reveal the hidden gesture. It shows the installed
 * version, selectable, so a reporter can copy it without leaving the page.
 */
class BugReportDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        // The activity's inflater, for the reason given in WelcomeDialogFragment.
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_bug_report, null)

        view.findViewById<TextView>(R.id.textBugReportWhere).text =
            getString(R.string.bug_report_where, ProjectLinks.BUG_REPORT_CHANNEL)

        view.findViewById<TextView>(R.id.textBugReportVersion).text = getString(
            R.string.app_version_label,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildIdentity.shortSha
        )

        view.findViewById<Button>(R.id.btnBugReportDiscord).setOnClickListener {
            ProjectLinkOpener.open(context, ProjectLinks.DISCORD_INVITE_URL)
        }
        view.findViewById<Button>(R.id.btnBugReportClose).setOnClickListener { dismiss() }

        return AlertDialog.Builder(context)
            .setTitle(R.string.bug_report)
            .setView(view)
            .create()
            .apply { setCanceledOnTouchOutside(false) }
    }

    companion object {
        /** The fragment tag. */
        const val TAG = "bug_report_page"
    }
}
