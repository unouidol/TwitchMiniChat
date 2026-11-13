package com.fs.twitchminichat.welcome

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.fs.twitchminichat.R

/**
 * Opens one of the project's own links outside the app, after a deliberate tap.
 *
 * It uses a plain `ACTION_VIEW`, as the policy pages' web button does. A Discord link may
 * open the Discord app; the APK link opens the browser, which downloads it. Chat links
 * go through `ExternalBrowserLinkController` instead, which warns about where a link
 * leads. These links are the project's own and need no warning.
 */
object ProjectLinkOpener {

    /** Opens [url], or says that nothing on the phone can open it. */
    fun open(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.project_link_no_app, Toast.LENGTH_SHORT).show()
        }
    }
}
