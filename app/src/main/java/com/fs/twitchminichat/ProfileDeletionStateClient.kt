package com.fs.twitchminichat

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Reads remote deletion tombstones using a device-scoped credential.
 *
 * Profile identifiers are intentionally not supplied by Android. The backend
 * derives the permitted profiles exclusively from its registered-device record.
 */
class ProfileDeletionStateClient(
    context: Context
) {

    /** Result containing only fields required by the local deletion workflow. */
    data class Result(
        val ok: Boolean,
        val deletedProfileIds: List<String>,
        val responseCode: Int?,
        val error: String?
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Performs one deletion-state request and returns on the main thread.
     */
    fun fetch(onComplete: (Result) -> Unit) {
        thread(start = true, name = "profile-deletion-state") {
            val deviceSecret = runCatching {
                DeviceCredentialStore.getExistingDeviceSecret(appContext)
            }.getOrNull()

            if (deviceSecret.isNullOrBlank()) {
                dispatch(
                    onComplete,
                    Result(
                        ok = false,
                        deletedProfileIds = emptyList(),
                        responseCode = null,
                        error = "device_credential_missing"
                    )
                )
                return@thread
            }

            val result = postJson(
                payload = JSONObject().apply {
                    put(
                        "device_id",
                        DeviceCredentialStore.getOrCreateDeviceId(appContext)
                    )
                    put("device_secret", deviceSecret)
                }
            )

            val responseBody = result?.responseBody.orEmpty()
            val body = runCatching {
                if (responseBody.isBlank()) {
                    null
                } else {
                    JSONObject(responseBody)
                }
            }.getOrNull()

            val deletedProfiles = mutableListOf<String>()
            val deletedArray = body?.optJSONArray("deleted_profile_ids")

            if (deletedArray != null) {
                for (index in 0 until deletedArray.length()) {
                    val profileId = deletedArray
                        .optString(index)
                        .trim()
                        .lowercase()

                    if (profileId.isNotBlank()) {
                        deletedProfiles += profileId
                    }
                }
            }

            val ok =
                result?.responseCode in 200..299 &&
                    body?.optBoolean("ok", false) == true

            val error = when {
                result == null -> "network_error"
                ok -> null
                else -> body
                    ?.optString("error")
                    ?.takeIf { it.isNotBlank() }
                    ?: "deletion_state_failed"
            }

            Log.d(
                TAG,
                "completed responseCode=${result?.responseCode} " +
                    "ok=$ok deletedCount=${deletedProfiles.distinct().size}"
            )

            dispatch(
                onComplete,
                Result(
                    ok = ok,
                    deletedProfileIds = deletedProfiles.distinct(),
                    responseCode = result?.responseCode,
                    error = error
                )
            )
        }
    }

    /**
     * Sends the device-authenticated JavaScript Object Notation request.
     */
    private fun postJson(payload: JSONObject): PostJsonResult? {
        var connection: HttpURLConnection? = null

        return try {
            connection = (
                URL(
                    appContext.getString(
                        R.string.profile_deletion_state_url
                    )
                ).openConnection() as HttpURLConnection
            ).apply {
                requestMethod = "POST"
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(
                connection.outputStream,
                Charsets.UTF_8
            ).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = readStream(
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            )

            PostJsonResult(
                responseCode = responseCode,
                responseBody = responseBody
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Deletion-state request failed", error)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** Posts one result to the Android main thread. */
    private fun dispatch(
        callback: (Result) -> Unit,
        result: Result
    ) {
        mainHandler.post {
            callback(result)
        }
    }

    /** Reads a complete HTTP response stream without logging its contents. */
    private fun readStream(stream: InputStream?): String {
        if (stream == null) {
            return ""
        }

        return BufferedReader(
            InputStreamReader(stream, Charsets.UTF_8)
        ).use { reader ->
            reader.readText()
        }
    }

    /** Internal HTTP response used only for structured parsing. */
    private data class PostJsonResult(
        val responseCode: Int,
        val responseBody: String
    )

    private companion object {
        private const val TAG = "DELETE_STATE_CLIENT"
        private const val NETWORK_TIMEOUT_MS = 10_000
    }
}