package com.fs.twitchminichat.diagnostics

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.fs.twitchminichat.R

/**
 * Hidden gesture that hands the diagnostics journal to the system share sheet.
 *
 * The journal is written into `filesDir`, and a release build is not
 * debuggable, so on the test device the file cannot be reached by any means
 * outside the application itself. Without an in-application export the
 * evidence is written and then stranded. A long press on the version label is
 * the smallest surface that makes it retrievable: nothing is added to any
 * screen, nothing announces itself, and no one arrives at it by accident.
 *
 * The only text a user can meet is one of the two failure toasts, and only
 * after having performed the gesture deliberately. Success shows the system
 * chooser and nothing else.
 *
 * ## Confining this to the development flavor
 *
 * The whole gesture is reachable through [attach] and nothing else, so moving
 * it out of the shipped application is a `git mv` of this file into
 * `src/dev/java/...` plus a twin in `src/stable/java/...` declaring the same
 * object with an empty [attach]. The call site does not change. The two
 * strings it uses would move to `src/dev/res/values/` in the same step, or
 * lint reports them unused in the stable flavor.
 */
object DiagnosticsExportGesture {

    /**
     * Makes a long press on [view] export and share the journal.
     *
     * Replaces any long-press listener previously set on the view.
     */
    fun attach(view: View) {
        view.setOnLongClickListener { pressed ->
            share(pressed)
            true
        }
    }

    private fun share(view: View) {
        val context = view.context

        val export = runCatching {
            HistoryDiagnosticsLog.exportSnapshot(context)
        }.getOrNull()

        if (export == null) {
            Toast.makeText(
                context,
                R.string.diagnostics_export_empty,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                context.packageName + ".diagnostics",
                export
            )
        }.getOrNull()

        if (uri == null) {
            Toast.makeText(
                context,
                R.string.diagnostics_export_failed,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, export.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, null))
        }.onFailure {
            Toast.makeText(
                context,
                R.string.diagnostics_export_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
