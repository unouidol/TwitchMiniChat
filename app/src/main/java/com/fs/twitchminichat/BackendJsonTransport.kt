package com.fs.twitchminichat

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** The status and body of one answered backend request. */
internal data class PostJsonResult(
    val responseCode: Int,
    val responseBody: String
)

/**
 * Sends the JavaScript Object Notation (JSON) requests of [FcmRegistrationUploader].
 *
 * An interface so that a local test can answer them instead of the network (#117, F3).
 * The application always uses [HttpBackendJsonTransport].
 */
internal interface BackendJsonTransport {

    /**
     * Posts [payload] to [urlString] with a mandatory, prevalidated Authorization header.
     *
     * Returns the answer, whatever its status, or null when no answer arrived.
     * [logLabel] names the request in diagnostics.
     */
    fun post(
        urlString: String,
        payload: JSONObject,
        logLabel: String,
        authorizationHeader: String
    ): PostJsonResult?
}

/**
 * [BackendJsonTransport] over `HttpURLConnection`.
 *
 * The body of `FcmRegistrationUploader.postJson` before #117's seams, unchanged, and
 * logging under the same `FCM_REGISTER` tag.
 */
internal object HttpBackendJsonTransport : BackendJsonTransport {

    private const val TAG = "FCM_REGISTER"

    override fun post(
        urlString: String,
        payload: JSONObject,
        logLabel: String,
        authorizationHeader: String
    ): PostJsonResult? {
        var conn: HttpURLConnection? = null

        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")

                setRequestProperty("Authorization", authorizationHeader)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseText = readStream(
                if (responseCode in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }
            )

            Log.d(TAG, "$logLabel responseCode=$responseCode")
            PostJsonResult(responseCode, responseText)
        } catch (t: Throwable) {
            Log.e(
                TAG,
                "$logLabel failed errorType=${DiagnosticError.typeOf(t)}"
            )
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}
