package com.fs.twitchminichat.welcome

import android.util.Log
import com.fs.twitchminichat.ProjectLinks
import java.net.HttpURLConnection
import java.net.URL

/**
 * Asks GitHub's releases API which release is the latest.
 *
 * The request is unauthenticated and carries only [REQUEST_HEADERS]. It has no account, no
 * token, no identifier of this installation and no query, and no cookies: the app installs
 * no `CookieHandler`, and `useCaches` is off. What GitHub learns is what every request
 * shows a server: the address it comes from and the time, plus the fixed [USER_AGENT].
 *
 * Every failure gives `null`:
 * - no network;
 * - a timeout;
 * - an HTTP error, including the 403 of the 60-an-hour unauthenticated rate limit;
 * - an oversized or unreadable answer.
 *
 * Nothing is retried. The caller runs this off the main thread.
 */
object LatestReleaseClient {

    private const val TAG = "TMC_WELCOME"

    /** The same limit the app's other clients use (`ProfileDeletionStateClient`). */
    private const val NETWORK_TIMEOUT_MS = 10_000

    /**
     * A fixed product name, and nothing about the phone.
     *
     * GitHub requires a User-Agent. Without this one, Android would send its default, which
     * names the Android version and the phone's model and build.
     */
    const val USER_AGENT = "TwitchMiniChat-Android"

    /** Every header this request sets: the only things it sends besides the address. */
    val REQUEST_HEADERS: Map<String, String> = linkedMapOf(
        "User-Agent" to USER_AGENT,
        "Accept" to "application/vnd.github+json"
    )

    /**
     * Fetches the latest release's tag, blocking the calling thread for at most twice
     * [NETWORK_TIMEOUT_MS].
     *
     * @return the tag, for example `v5.6.1`, or `null` on any failure.
     */
    fun fetchLatestTag(): String? {
        var connection: HttpURLConnection? = null

        return try {
            val url = URL(ProjectLinks.LATEST_RELEASE_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                useCaches = false
                REQUEST_HEADERS.forEach { (name, value) -> setRequestProperty(name, value) }
            }

            val status = connection.responseCode
            val body = if (status == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { LatestReleaseResponse.readBounded(it) }
            } else {
                null
            }

            val tag = LatestReleaseResponse.latestTag(status, body)
            // The status and whether a tag came back, never the body.
            Log.d(TAG, "update check status=$status tagFound=${tag != null}")
            tag
        } catch (error: Exception) {
            Log.d(TAG, "update check failed: ${error.javaClass.simpleName}")
            null
        } finally {
            connection?.disconnect()
        }
    }
}
