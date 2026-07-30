package com.fs.twitchminichat.pcg.mostwanted

import android.content.Context
import android.util.Log
import com.fs.twitchminichat.BackendAuthHeaderProvider
import com.fs.twitchminichat.BackendSessionAuthDecision
import com.fs.twitchminichat.BackendSessionStore
import com.fs.twitchminichat.BuildConfig
import com.fs.twitchminichat.DeviceCredentialStore
import com.fs.twitchminichat.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Stable failure categories for one user-triggered watchlist sync. */
enum class PcgMostWantedSyncError {
    AUTHENTICATION_UNAVAILABLE,
    NETWORK,
    SERVER_REJECTED,
    INVALID_RESPONSE
}

/** Result of one explicit Most Wanted server synchronization request. */
data class PcgMostWantedSyncResult(
    val ok: Boolean,
    val statusCode: Int? = null,
    val error: PcgMostWantedSyncError? = null
)

/**
 * Sends one profile-scoped PCG Most Wanted state to the backend.
 *
 * The client performs exactly one request for one explicit Save tap. It never
 * retries automatically and never sends chat or gameplay commands.
 */
class PcgMostWantedSyncClient(context: Context) {

    /** Application context used for credentials and resource URLs. */
    private val appContext = context.applicationContext

    /** Resolves the preselected backend authentication mode for one profile. */
    private val authHeaderProvider = BackendAuthHeaderProvider(
        sessionReader = BackendSessionStore(appContext)
    )

    /** Synchronizes one already validated local watchlist state. */
    fun sync(
        profileId: String,
        state: PcgMostWantedState
    ): PcgMostWantedSyncResult {
        val normalizedProfileId = profileId.trim().lowercase()
        if (normalizedProfileId.isBlank()) {
            return PcgMostWantedSyncResult(
                ok = false,
                error = PcgMostWantedSyncError.AUTHENTICATION_UNAVAILABLE
            )
        }

        val authDecision = authHeaderProvider.resolve(normalizedProfileId)
        val payload = PcgMostWantedSyncPayloadBuilder.build(
            deviceId = DeviceCredentialStore.getOrCreateDeviceId(appContext),
            profileId = normalizedProfileId,
            state = state
        )

        val authorizationHeader = when (authDecision) {
            BackendSessionAuthDecision.Legacy -> {
                payload.put("key", BuildConfig.HISTORY_SECRET_KEY)
                Log.d(TAG, "set_custom_watchlist authMode=legacy_key")
                null
            }

            is BackendSessionAuthDecision.Bearer -> {
                Log.d(TAG, "set_custom_watchlist authMode=backend_session")
                authDecision.authorizationHeader
            }

            BackendSessionAuthDecision.Unavailable -> {
                Log.w(
                    TAG,
                    "set_custom_watchlist skipped: backend session unavailable"
                )
                return PcgMostWantedSyncResult(
                    ok = false,
                    error =
                        PcgMostWantedSyncError.AUTHENTICATION_UNAVAILABLE
                )
            }
        }

        return postOnce(
            payload = payload,
            authorizationHeader = authorizationHeader
        )
    }

    /** Executes one JavaScript Object Notation request without retrying. */
    private fun postOnce(
        payload: JSONObject,
        authorizationHeader: String?
    ): PcgMostWantedSyncResult {
        var connection: HttpURLConnection? = null

        return try {
            val activeConnection = (
                URL(
                    appContext.getString(
                        R.string.pcg_most_wanted_sync_url
                    )
                ).openConnection() as HttpURLConnection
                ).apply {
                requestMethod = "POST"
                connectTimeout = NETWORK_TIMEOUT_MILLIS
                readTimeout = NETWORK_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                setRequestProperty("Accept", "application/json")
                authorizationHeader?.let { header ->
                    setRequestProperty("Authorization", header)
                }
            }
            connection = activeConnection

            OutputStreamWriter(
                activeConnection.outputStream,
                Charsets.UTF_8
            ).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val statusCode = activeConnection.responseCode
            val responseBody = readStream(
                if (statusCode in 200..299) {
                    activeConnection.inputStream
                } else {
                    activeConnection.errorStream
                }
            )
            val responseJson = runCatching {
                JSONObject(responseBody)
            }.getOrNull()

            val ok =
                statusCode in 200..299 &&
                    responseJson?.optBoolean("ok", false) == true

            Log.d(
                TAG,
                "set_custom_watchlist statusCode=$statusCode ok=$ok"
            )

            when {
                ok -> PcgMostWantedSyncResult(
                    ok = true,
                    statusCode = statusCode
                )

                statusCode !in 200..299 ->
                    PcgMostWantedSyncResult(
                        ok = false,
                        statusCode = statusCode,
                        error = PcgMostWantedSyncError.SERVER_REJECTED
                    )

                else -> PcgMostWantedSyncResult(
                    ok = false,
                    statusCode = statusCode,
                    error = PcgMostWantedSyncError.INVALID_RESPONSE
                )
            }
        } catch (error: Exception) {
            Log.w(TAG, "set_custom_watchlist network failure", error)
            PcgMostWantedSyncResult(
                ok = false,
                error = PcgMostWantedSyncError.NETWORK
            )
        } finally {
            connection?.disconnect()
        }
    }

    /** Reads one response stream as UTF-8 text. */
    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""

        return BufferedReader(
            InputStreamReader(stream, Charsets.UTF_8)
        ).use { reader ->
            reader.readText()
        }
    }

    companion object {
        /** Logcat tag for watchlist synchronization diagnostics. */
        private const val TAG = "PCG_MOST_WANTED_SYNC"

        /** Connection and response timeout for one explicit request. */
        private const val NETWORK_TIMEOUT_MILLIS = 10_000
    }
}