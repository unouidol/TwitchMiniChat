package com.fs.twitchminichat

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging

object FcmRegistrationUploader {

    private const val TAG = "FCM_REGISTER"

    /** UI-facing deletion outcome that deliberately excludes raw backend metadata. */
    data class DeleteServerDataResult(
        val ok: Boolean,
        val message: String
    )

    /** UI-facing report outcome that deliberately excludes the reported content. */
    data class ReportMessageResult(
        val ok: Boolean
    )

    fun uploadToken(context: Context, token: String, profileId: String) {
        val appContext = context.applicationContext
        val trimmedToken = token.trim()
        if (trimmedToken.isEmpty()) {
            Log.w(TAG, "Empty token, skip upload")
            return
        }

        thread(start = true, name = "fcm-register-upload") {
            uploadTokenBlocking(appContext, trimmedToken, profileId)
        }
    }


    /**
     * Sends the selected Pokémon Community Game spawn settings to the backend.
     *
     * This is the new 4-state preference used by the bell menu:
     *
     * 0 = Dex + Tier A
     * 1 = Dex only
     * 2 = All spawns
     * 3 = No spawns
     *
     * Event spawns are independent from the four ordinary modes. The legacy
     * "enabled" field remains true whenever either category needs delivery.
     */
    fun setProfileSpawnAlertMode(
        context: Context,
        profileId: String,
        settings: PcgSpawnAlertSettings,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("fcm_registration", Context.MODE_PRIVATE)
        val normalizedProfileId = normalizeProfileId(profileId)

        fun finish(ok: Boolean) {
            Handler(Looper.getMainLooper()).post {
                onComplete(ok)
            }
        }

        if (normalizedProfileId.isBlank()) {
            Log.w(TAG, "Cannot set spawn alert mode: blank profileId")
            finish(false)
            return
        }

        fun sendRequest(token: String) {
            thread(start = true, name = "spawn-alert-mode") {
                val authDecision = BackendAuthHeaderProvider(
                    sessionReader = BackendSessionStore(appContext)
                ).resolve(normalizedProfileId)

                val payload = JSONObject().apply {
                    put("device_id", DeviceCredentialStore.getOrCreateDeviceId(appContext))
                    put("device_name", buildDeviceName(appContext))
                    put("fcm_token", token)
                    put("profile_id", normalizedProfileId)
                    put("spawn_alert_mode", settings.regularMode.id)
                    put("event_spawn_enabled", settings.eventSpawnsEnabled)

                    /*
                     * Compatibility bridge for the old registration model.
                     *
                     * The server can keep profile_ids aligned while also storing
                     * the more precise profile_spawn_alert_modes map.
                     */
                    put("enabled", settings.isAnyAlertEnabled)
                }

                val authorizationHeader = when (authDecision) {
                    BackendSessionAuthDecision.Missing -> {
                        Log.w(TAG, "set_spawn_alert_mode skipped: backend session missing")
                        finish(false)
                        return@thread
                    }

                    is BackendSessionAuthDecision.Bearer -> {
                        Log.d(TAG, "set_spawn_alert_mode authMode=backend_session")
                        authDecision.authorizationHeader
                    }

                    BackendSessionAuthDecision.Unavailable -> {
                        /*
                         * An unreadable or invalid local session must not be downgraded
                         * to profile-only legacy authentication.
                         */
                        Log.w(TAG, "set_spawn_alert_mode skipped: backend session unavailable")
                        finish(false)
                        return@thread
                    }
                }

                val result = postJson(
                    urlString = appContext.getString(R.string.fcm_set_spawn_alert_mode_url),
                    payload = payload,
                    logLabel = "set_spawn_alert_mode",
                    authorizationHeader = authorizationHeader
                )

                val ok = result?.responseCode in 200..299
                finish(ok)
            }
        }

        val cachedToken = prefs.getString("latest_fcm_token", null).orEmpty()

        /*
         * When both categories are disabled, the request can be sent without
         * forcing a fresh token fetch. Any enabled category requires a usable
         * token because the backend must be able to deliver its notification.
         */
        if (!settings.isAnyAlertEnabled) {
            sendRequest(cachedToken)
            return
        }

        if (cachedToken.isNotBlank()) {
            sendRequest(cachedToken)
            return
        }

        Log.d(TAG, "No cached Firebase Cloud Messaging token; fetching one for spawn mode")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    TAG,
                    "Unable to fetch a Firebase Cloud Messaging token for spawn mode " +
                        "errorType=${DiagnosticError.typeOf(task.exception)}"
                )
                finish(false)
                return@addOnCompleteListener
            }

            val freshToken = task.result?.trim().orEmpty()
            if (freshToken.isBlank()) {
                Log.w(TAG, "Fresh Firebase Cloud Messaging token is blank for spawn mode")
                finish(false)
                return@addOnCompleteListener
            }

            prefs.edit {
                putString("latest_fcm_token", freshToken)
            }

            Log.d(TAG, "Fetched a Firebase Cloud Messaging token for spawn mode")
            sendRequest(freshToken)
        }
    }


    fun uploadDexList(
        context: Context,
        profileId: String,
        profileLabel: String,
        wantedPokemon: List<String>
    ) {
        val appContext = context.applicationContext
        val normalized = wantedPokemon
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        thread(start = true, name = "dex-upload") {
            uploadDexListBlocking(appContext, profileId, profileLabel, normalized)
        }
    }

    /**
     * Deletes this device's server-side app data with dual authentication.
     *
     * [candidateProfileIds] is used only to select a local backend session.
     * The backend derives the authoritative scope from the device record.
     */
    fun deleteServerData(
        context: Context,
        candidateProfileIds: Collection<String>,
        onComplete: (DeleteServerDataResult) -> Unit
    ) {
        val appContext = context.applicationContext

        thread(start = true, name = "delete-server-data") {
            val authDecision = ServerDeletionAuthProvider(
                sessionReader = BackendSessionStore(appContext)
            ).resolve(candidateProfileIds)

            val authorizationHeader = when (authDecision) {
                is ServerDeletionAuthDecision.Bearer -> {
                    Log.d(
                        TAG,
                        "delete_server_data authMode=backend_session"
                    )
                    authDecision.authorizationHeader
                }

                ServerDeletionAuthDecision.SessionMissing -> {
                    Log.w(
                        TAG,
                        "delete_server_data skipped: " +
                            "backend session missing"
                    )
                    postDeleteServerDataResult(
                        onComplete,
                        failedDeleteServerDataResult(
                            appContext.getString(
                                R.string.server_deletion_session_required
                            )
                        )
                    )
                    return@thread
                }

                ServerDeletionAuthDecision.SessionUnavailable -> {
                    Log.w(
                        TAG,
                        "delete_server_data skipped: " +
                            "backend session unavailable"
                    )
                    postDeleteServerDataResult(
                        onComplete,
                        failedDeleteServerDataResult(
                            appContext.getString(
                                R.string.server_deletion_auth_unavailable
                            )
                        )
                    )
                    return@thread
                }
            }

            val deviceSecret = runCatching {
                DeviceCredentialStore.getExistingDeviceSecret(
                    appContext
                )
            }.getOrElse { error ->
                Log.e(
                    TAG,
                    "delete_server_data skipped: " +
                        "invalid device credential " +
                        "errorType=${DiagnosticError.typeOf(error)}"
                )
                null
            }

            if (deviceSecret.isNullOrBlank()) {
                Log.w(
                    TAG,
                    "delete_server_data skipped: " +
                        "device credential missing"
                )
                postDeleteServerDataResult(
                    onComplete,
                    failedDeleteServerDataResult(
                        appContext.getString(
                            R.string
                                .server_deletion_device_credential_missing
                        )
                    )
                )
                return@thread
            }

            val result = postJson(
                urlString = appContext.getString(
                    R.string.delete_server_data_url
                ),
                payload = JSONObject().apply {
                    put(
                        "device_id",
                        DeviceCredentialStore.getOrCreateDeviceId(
                            appContext
                        )
                    )
                    put("device_secret", deviceSecret)
                },
                logLabel = "delete_server_data",
                authorizationHeader = authorizationHeader
            )

            val rawBody = result?.responseBody.orEmpty()

            val body = runCatching {
                if (rawBody.isNotBlank()) {
                    JSONObject(rawBody)
                } else {
                    null
                }
            }.getOrNull()

            val ok =
                result?.responseCode in 200..299 &&
                    body?.optBoolean("ok", false) == true

            val message = when {
                result == null -> appContext.getString(
                    R.string.server_deletion_failed
                )

                ok -> appContext.getString(
                    R.string.server_delete_ok
                )

                else -> {
                    val errors = body?.optJSONArray("errors")

                    if (
                        errors != null &&
                        errors.length() > 0
                    ) {
                        errors
                            .optString(0)
                            .ifBlank {
                                appContext.getString(
                                    R.string.server_deletion_failed
                                )
                            }
                    } else {
                        body
                            ?.optString("error")
                            ?.takeIf(String::isNotBlank)
                            ?: appContext.getString(
                                R.string.server_deletion_failed
                            )
                    }
                }
            }

            Log.d(
                TAG,
                "delete_server_data completed ok=$ok"
            )

            postDeleteServerDataResult(
                onComplete,
                DeleteServerDataResult(
                    ok = ok,
                    message = message
                )
            )
        }
    }

    /** Builds a typed pre-network deletion failure. */
    private fun failedDeleteServerDataResult(
        message: String
    ): DeleteServerDataResult {
        return DeleteServerDataResult(
            ok = false,
            message = message
        )
    }

    /** Delivers one deletion result on the Android main thread. */
    private fun postDeleteServerDataResult(
        callback: (DeleteServerDataResult) -> Unit,
        result: DeleteServerDataResult
    ) {
        Handler(Looper.getMainLooper()).post {
            callback(result)
        }
    }
    private fun normalizeProfileId(value: String?): String {
        return value?.trim()?.lowercase().orEmpty()
    }

    /**
     * Registers one Firebase Cloud Messaging token using the profile's preselected
     * backend authentication mode.
     *
     * Missing or unreadable backend sessions never authorize a request.
     */
    private fun uploadTokenBlocking(context: Context, token: String, profileId: String) {
        val normalizedProfileId = normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            Log.w(TAG, "Blank profileId, skip register_fcm")
            return
        }

        val authDecision = BackendAuthHeaderProvider(
            sessionReader = BackendSessionStore(context)
        ).resolve(normalizedProfileId)

        val payload = JSONObject().apply {
            put("device_id", DeviceCredentialStore.getOrCreateDeviceId(context))
            put("device_name", buildDeviceName(context))
            put("fcm_token", token)
            put("profile_id", normalizedProfileId)
        }

        val authorizationHeader = when (authDecision) {
            BackendSessionAuthDecision.Missing -> {
                Log.w(TAG, "register_fcm skipped: backend session missing")
                return
            }

            is BackendSessionAuthDecision.Bearer -> {
                val deviceSecret = runCatching {
                    DeviceCredentialStore.getOrCreateDeviceSecret(context)
                }.getOrElse { error ->
                    Log.e(
                        TAG,
                        "register_fcm skipped: device credential unavailable " +
                            "errorType=${DiagnosticError.typeOf(error)}"
                    )
                    return
                }

                /*
                 * Device credential enrollment is allowed only when this profile
                 * is authenticated by its backend Bearer session.
                 */
                payload.put("device_secret", deviceSecret)

                Log.d(TAG, "register_fcm authMode=backend_session")
                authDecision.authorizationHeader
            }

            BackendSessionAuthDecision.Unavailable -> {
                /*
                 * Do not downgrade to the legacy key when local session state exists
                 * but cannot be trusted.
                 */
                Log.w(TAG, "register_fcm skipped: backend session unavailable")
                return
            }
        }

        postJson(
            urlString = context.getString(R.string.fcm_register_url),
            payload = payload,
            logLabel = "register_fcm",
            authorizationHeader = authorizationHeader
        )
    }

    /**
     * Uploads one profile's missing Pokédex entries using a preselected authentication mode.
     *
     * Missing or unreadable backend sessions never authorize a request.
     */
    private fun uploadDexListBlocking(
        context: Context,
        profileId: String,
        profileLabel: String,
        wantedPokemon: List<String>
    ) {
        val normalizedProfileId = normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            Log.w(TAG, "Blank profileId, skip upload_dex_list")
            showToast(context, "Error updating dex list for $profileLabel")
            return
        }

        val authDecision = BackendAuthHeaderProvider(
            sessionReader = BackendSessionStore(context)
        ).resolve(normalizedProfileId)

        val wantedArray = JSONArray()
        for (name in wantedPokemon) {
            wantedArray.put(name)
        }

        val payload = JSONObject().apply {
            put("profile_id", normalizedProfileId)
            put("profile_label", profileLabel)
            put("wanted_pokemon", wantedArray)
        }

        val authorizationHeader = when (authDecision) {
            BackendSessionAuthDecision.Missing -> {
                Log.w(TAG, "upload_dex_list skipped: backend session missing")
                showToast(context, "Error updating dex list for $profileLabel")
                return
            }

            is BackendSessionAuthDecision.Bearer -> {
                Log.d(TAG, "upload_dex_list authMode=backend_session")
                authDecision.authorizationHeader
            }

            BackendSessionAuthDecision.Unavailable -> {
                /*
                 * Do not downgrade to the legacy key when local session state exists
                 * but cannot be trusted.
                 */
                Log.w(TAG, "upload_dex_list skipped: backend session unavailable")
                showToast(context, "Error updating dex list for $profileLabel")
                return
            }
        }

        val result = postJson(
            urlString = context.getString(R.string.dex_upload_url),
            payload = payload,
            logLabel = "upload_dex_list",
            authorizationHeader = authorizationHeader
        )

        if (result == null) {
            showToast(context, "Error updating dex list for $profileLabel")
            return
        }

        if (result.responseCode in 200..299) {
            val body = runCatching {
                JSONObject(result.responseBody)
            }.getOrNull()

            val count = body?.optInt("count", wantedPokemon.size) ?: wantedPokemon.size
            val uploadResult = body?.optString("result").orEmpty()

            val message = when (uploadResult) {
                "created" -> "Dex list created for $profileLabel ($count Pokémon)"
                "updated" -> "Dex list updated for $profileLabel ($count Pokémon)"
                else -> "Dex list synced for $profileLabel ($count Pokémon)"
            }

            showToast(context, message)
        } else {
            showToast(context, "Error updating dex list for $profileLabel")
        }
    }

    private data class PostJsonResult(
        val responseCode: Int,
        val responseBody: String
    )

    /**
     * Sends one JavaScript Object Notation (JSON) request with a mandatory,
     * prevalidated Authorization header.
     */
    private fun postJson(
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

    private fun buildDeviceName(context: Context): String {
        val installId =
            DeviceCredentialStore.getOrCreateDeviceId(context)

        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()

        val humanPart = listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "android-device" }

        return "$humanPart [$installId]"
    }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    private fun showToast(context: Context, text: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
        }
    }
    /**
     * Submits one user-triggered message report using the selected authentication mode.
     *
     * Missing or unreadable backend sessions never authorize a request.
     */
    fun reportMessage(
        context: Context,
        reporterProfileId: String,
        channel: String,
        messageUser: String,
        messageText: String,
        messageId: String?,
        messageTimestampSec: Double?,
        reason: String = "user_report",
        onComplete: (ReportMessageResult) -> Unit
    ) {
        val appContext = context.applicationContext
        val normalizedReporterProfileId = normalizeProfileId(reporterProfileId)
        val normalizedChannel = channel.trim().removePrefix("#").lowercase()

        /**
         * Delivers the result on the main thread.
         */
        fun finish(result: ReportMessageResult) {
            Handler(Looper.getMainLooper()).post {
                onComplete(result)
            }
        }

        if (normalizedReporterProfileId.isBlank()) {
            Log.w(TAG, "report_message skipped: blank reporter profile")
            finish(
                ReportMessageResult(
                    ok = false
                )
            )
            return
        }

        thread(start = true, name = "report-message") {
            val authDecision = BackendAuthHeaderProvider(
                sessionReader = BackendSessionStore(appContext)
            ).resolve(normalizedReporterProfileId)

            val payload = JSONObject().apply {
                put("reporter_profile_id", normalizedReporterProfileId)
                put("channel", normalizedChannel)
                put("message_user", messageUser.trim())
                put("message_text", messageText)
                put("message_id", messageId ?: JSONObject.NULL)
                put("message_timestamp", messageTimestampSec ?: JSONObject.NULL)
                put("reason", reason)
            }

            val authorizationHeader = when (authDecision) {
                BackendSessionAuthDecision.Missing -> {
                    Log.w(TAG, "report_message skipped: backend session missing")
                    finish(
                        ReportMessageResult(
                            ok = false
                        )
                    )
                    return@thread
                }

                is BackendSessionAuthDecision.Bearer -> {
                    Log.d(TAG, "report_message authMode=backend_session")
                    authDecision.authorizationHeader
                }

                BackendSessionAuthDecision.Unavailable -> {
                    /*
                     * Do not downgrade to the legacy key when local session state exists
                     * but cannot be trusted.
                     */
                    Log.w(TAG, "report_message skipped: backend session unavailable")
                    finish(
                        ReportMessageResult(
                            ok = false
                        )
                    )
                    return@thread
                }
            }

            val result = postJson(
                urlString = appContext.getString(R.string.report_message_url),
                payload = payload,
                logLabel = "report_message",
                authorizationHeader = authorizationHeader
            )

            val rawBody = result?.responseBody.orEmpty()
            val body = runCatching {
                if (rawBody.isNotBlank()) JSONObject(rawBody) else null
            }.getOrNull()

            val ok =
                result?.responseCode in 200..299 &&
                        body?.optBoolean("ok", false) == true

            /*
             * Do not log the reported text, author, message identifier,
             * backend response body, or authentication credentials.
             */
            Log.d(
                TAG,
                "report_message completed ok=$ok"
            )

            finish(
                ReportMessageResult(
                    ok = ok
                )
            )
        }
    }
}
