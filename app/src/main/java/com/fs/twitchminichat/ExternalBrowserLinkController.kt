package com.fs.twitchminichat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * Confirms chat links and opens them only in a user-selected external browser.
 *
 * A remembered browser is stored as an explicit Android component. No link is
 * opened inside GeckoView and every launch remains user-triggered.
 */
class ExternalBrowserLinkController(
    private val context: Context,
    private val preferences: ExternalLinkPreferences =
        ExternalLinkPreferences(context)
) {

    private var activeDialog: AlertDialog? = null

    /**
     * Starts the warning and browser-selection flow for one chat link.
     */
    fun openLink(rawUrl: String) {
        val normalizedUrl = ExternalWebLinkPolicy.normalize(rawUrl)

        if (normalizedUrl == null) {
            Toast.makeText(
                context,
                R.string.external_link_invalid,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val uri = Uri.parse(normalizedUrl)

        if (preferences.shouldShowExitWarning()) {
            showExitWarning(uri)
        } else {
            openWithSelectedBrowser(uri)
        }
    }

    /** Dismisses any link dialog owned by the current chat view. */
    fun release() {
        activeDialog?.dismiss()
        activeDialog = null
    }

    /** Shows the leave-app warning with an optional persistent dismissal. */
    private fun showExitWarning(uri: Uri) {
        release()

        val padding = dpToPx(24)
        val spacing = dpToPx(12)

        val warningContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, spacing, padding, spacing)
        }

        warningContent.addView(
            TextView(context).apply {
                text = context.getString(
                    R.string.external_link_warning_message,
                    uri.toString()
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val skipFutureWarnings = CheckBox(context).apply {
            setText(R.string.external_link_do_not_show_again)
        }

        warningContent.addView(
            skipFutureWarnings,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = spacing
            }
        )

        activeDialog = AlertDialog.Builder(context)
            .setTitle(R.string.external_link_warning_title)
            .setView(warningContent)
            .setPositiveButton(R.string.external_link_continue) { _, _ ->
                if (skipFutureWarnings.isChecked) {
                    preferences.setSkipExitWarning(true)
                }

                openWithSelectedBrowser(uri)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnDismissListener {
                    if (activeDialog === dialog) {
                        activeDialog = null
                    }
                }
                dialog.show()
            }
    }

    /** Uses a remembered browser or always asks before a new browser choice. */
    private fun openWithSelectedBrowser(uri: Uri) {
        val browsers = queryExternalBrowsers()

        if (browsers.isEmpty()) {
            Toast.makeText(
                context,
                R.string.external_link_no_browser,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rememberedComponent = preferences.rememberedBrowser()
        val rememberedBrowser = browsers.firstOrNull { browser ->
            browser.componentName == rememberedComponent
        }

        if (rememberedBrowser != null) {
            launchInBrowser(uri, rememberedBrowser)
            return
        }

        if (rememberedComponent != null) {
            preferences.clearRememberedBrowser()
        }

        /*
         * A single PackageManager result is not proof that only one browser is
         * installed. Android can filter web handlers according to current URL
         * associations, so TMC never opens that result automatically.
         */
        showBrowserChooser(
            uri = uri,
            browsers = browsers
        )
    }

    /** Displays all generic external browsers and an optional remember checkbox. */
    private fun showBrowserChooser(
        uri: Uri,
        browsers: List<BrowserOption>
    ) {
        release()

        val padding = dpToPx(24)
        val itemSpacing = dpToPx(8)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, itemSpacing, padding, padding)
        }

        content.addView(
            TextView(context).apply {
                setText(R.string.external_link_browser_message)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = itemSpacing
            }
        )

        val rememberChoice = CheckBox(context).apply {
            setText(R.string.external_link_remember_browser)
        }

        val labelCounts = browsers
            .groupingBy { browser -> browser.label }
            .eachCount()

        var chooserDialog: AlertDialog? = null

        browsers.forEach { browser ->
            val buttonLabel = if (
                labelCounts.getValue(browser.label) > 1
            ) {
                "${browser.label}\n${browser.componentName.packageName}"
            } else {
                browser.label
            }

            val browserButton = Button(context).apply {
                text = buttonLabel
                isAllCaps = false
                setOnClickListener {
                    if (rememberChoice.isChecked) {
                        preferences.setRememberedBrowser(
                            browser.componentName
                        )
                    }

                    chooserDialog?.dismiss()
                    launchInBrowser(uri, browser)
                }
            }

            content.addView(
                browserButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = itemSpacing
                }
            )
        }

        content.addView(
            rememberChoice,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = itemSpacing
            }
        )

        val scrollView = ScrollView(context).apply {
            addView(content)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.external_link_browser_title)
            .setView(scrollView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        chooserDialog = dialog
        activeDialog = dialog

        dialog.setOnDismissListener {
            if (activeDialog === dialog) {
                activeDialog = null
            }
        }

        dialog.show()
    }

    /**
     * Queries installed general-purpose browsers without trusting URL defaults.
     *
     * Android may filter a regular web-handler query according to the user's
     * current URL associations. The application-browser selector is therefore
     * the primary source, while the neutral HTTPS probe remains a compatibility
     * fallback for browsers that do not expose the selector category.
     */
    private fun queryExternalBrowsers(): List<BrowserOption> {
        val packageManager = context.packageManager
        val genericBrowserIntent = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN,
            Intent.CATEGORY_APP_BROWSER
        )
        val webProbeIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(BROWSER_PROBE_URL)
        ).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val genericBrowserPackages = queryActivities(genericBrowserIntent)
            .mapNotNull { resolveInfo ->
                resolveInfo.activityInfo?.packageName
            }
            .distinct()
        val resolvedActivities = buildList {
            addAll(queryActivities(webProbeIntent))

            /*
             * Resolve the real ACTION_VIEW activity inside every visible generic
             * browser package. The generic selector itself can point to a launcher
             * activity that is not the correct target for a web URL.
             */
            genericBrowserPackages.forEach { packageName ->
                addAll(
                    queryActivities(
                        Intent(webProbeIntent).setPackage(packageName)
                    )
                )
            }
        }

        return resolvedActivities
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                    ?: return@mapNotNull null

                if (activityInfo.packageName == context.packageName) {
                    return@mapNotNull null
                }

                val componentName = ComponentName(
                    activityInfo.packageName,
                    activityInfo.name
                )
                val label = resolveInfo
                    .loadLabel(packageManager)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                    .ifBlank {
                        activityInfo.packageName
                    }

                BrowserOption(
                    componentName = componentName,
                    label = label
                )
            }
            .distinctBy { browser ->
                browser.componentName.packageName
            }
            .sortedBy { browser ->
                browser.label.lowercase()
            }
            .toList()
    }

    /** Returns visible activities matching one browser-discovery intent. */
    @Suppress("DEPRECATION")
    private fun queryActivities(intent: Intent): List<ResolveInfo> {
        val packageManager = context.packageManager

        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(
                    PackageManager.MATCH_DEFAULT_ONLY.toLong()
                )
            )
        } else {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        }
    }

    /** Opens one allowed URL using an explicit external browser component. */
    private fun launchInBrowser(
        uri: Uri,
        browser: BrowserOption
    ) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            uri
        ).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            component = browser.componentName
        }

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            if (
                preferences.rememberedBrowser() ==
                browser.componentName
            ) {
                preferences.clearRememberedBrowser()
            }

            Toast.makeText(
                context,
                R.string.external_link_open_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Converts density-independent pixels into physical pixels. */
    private fun dpToPx(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    /** One installed external browser shown in the selection dialog. */
    private data class BrowserOption(
        val componentName: ComponentName,
        val label: String
    )

    companion object {
        /** Neutral address used only to discover general-purpose browsers. */
        private const val BROWSER_PROBE_URL = "https://example.com/"
    }
}
