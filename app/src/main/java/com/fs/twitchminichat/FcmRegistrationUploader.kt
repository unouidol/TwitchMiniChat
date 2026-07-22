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
import java.util.UUID
import kotlin.concurrent.thread
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging

object FcmRegistrationUploader {

    private const val TAG = "FCM_REGISTER"

    data class DeleteServerDataResult(
        val ok: Boolean,
        val message: String,
        val removedDevice: Boolean,
        val deletedDexProfiles: List<String>,
        val oauthDeletedRows: Int,
        val oauthDeletedTables: List<String>,
        val requestId: String?,
        val auditLogPath: String?,
        val rawResponse: String
    )

    data class DeleteDeviceDataResult(
        val ok: Boolean,
        val message: String,
        val removedDevice: Boolean,
        val removedDeviceProfiles: List<String>,
        val requestId: String?,
        val auditLogPath: String?,
        val rawResponse: String
    )

    data class ProfileDeletionStateResult(
        val ok: Boolean,
        val deletedProfileIds: List<String>,
        val rawResponse: String
    )

    data class ReportMessageResult(
        val ok: Boolean,
        val rawResponse: String,
        val error: String?
    )

    fun fetchProfileDeletionState(
        context: Context,
        knownProfileIds: Collection<String>,
        onComplete: (ProfileDeletionStateResult) -> Unit
    ) {
        val appContext = context.applicationContext

        thread(start = true, name = "profile-deletion-state") {
            val normalizedProfiles = knownProfileIds
                .map { normalizeProfileId(it) }
                .filter { it.isNotBlank() }
                .distinct()

            val result = postJson(
                urlString = appContext.getString(R.string.profile_deletion_state_url),
                payload = JSONObject().apply {
                    put("key", BuildConfig.HISTORY_SECRET_KEY)
                    put("known_profile_ids", JSONArray(normalizedProfiles))
                },
                logLabel = "get_profile_deletion_state"
            )

            val rawBody = result?.responseBody.orEmpty()
            val body = runCatching {
                if (rawBody.isNotBlank()) JSONObject(rawBody) else null
            }.getOrNull()

            val deleted = mutableListOf<String>()
            val deletedArray = body?.optJSONArray("deleted_profile_ids")
            if (deletedArray != null) {
                for (i in 0 until deletedArray.length()) {
                    val v = deletedArray.optString(i).trim().lowercase()
                    if (v.isNotEmpty()) {
                        deleted += v
                    }
                }
            }

            val ok = result?.responseCode in 200..299 && (body?.optBoolean("ok", false) == true)

            Log.d(
                TAG,
                "get_profile_deletion_state ok=$ok " +
                        "knownProfileCount=${normalizedProfiles.size} " +
                        "deletedProfileCount=${deleted.distinct().size}"
            )

            Handler(Looper.getMainLooper()).post {
                onComplete(
                    ProfileDeletionStateResult(
                        ok = ok,
                        deletedProfileIds = deleted.distinct(),
                        rawResponse = rawBody
                    )
                )
            }
        }
    }

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
     * Sends the selected Pokémon Community Game spawn alert mode to the backend.
     *
     * This is the new 4-state preference used by the bell menu:
     *
     * 0 = Dex + Tier A
     * 1 = Dex only
     * 2 = All spawns
     * 3 = No spawns
     *
     * The "enabled" field is also sent as a compatibility bridge:
     * modes 0/1/2 behave like push enabled, while mode 3 behaves like push disabled.
     */
    fun setProfileSpawnAlertMode(
        context: Context,
        profileId: String,
        mode: PcgSpawnAlertMode,
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
                val result = postJson(
                    urlString = appContext.getString(R.string.fcm_set_spawn_alert_mode_url),
                    payload = JSONObject().apply {
                        put("key", BuildConfig.HISTORY_SECRET_KEY)
                        put("device_id", getInstallId(appContext))
                        put("device_name", buildDeviceName(appContext))
                        put("fcm_token", token)
                        put("profile_id", normalizedProfileId)
                        put("spawn_alert_mode", mode.id)

                        /*
                         * Compatibility bridge for the old registration model.
                         *
                         * The server can keep profile_ids aligned while also storing
                         * the more precise profile_spawn_alert_modes map.
                         */
                        put("enabled", mode.isPushEnabledForCompatibility)
                    },
                    logLabel = "set_spawn_alert_mode"
                )

                val ok = result?.responseCode in 200..299
                finish(ok)
            }
        }

        val cachedToken = prefs.getString("latest_fcm_token", null).orEmpty()

        /*
         * For mode NONE we can still send the request without forcing a fresh FCM
         * token fetch, because the important part is disabling this profile's spawn
         * notifications on the backend.
         */
        if (!mode.isPushEnabledForCompatibility) {
            sendRequest(cachedToken)
            return
        }

        if (cachedToken.isNotBlank()) {
            sendRequest(cachedToken)
            return
        }

        Log.d(TAG, "No cached FCM token, fetching a fresh one for spawn mode")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Unable to fetch fresh FCM token for spawn mode", task.exception)
                finish(false)
                return@addOnCompleteListener
            }

            val freshToken = task.result?.trim().orEmpty()
            if (freshToken.isBlank()) {
                Log.w(TAG, "Fresh FCM token is blank for spawn mode")
                finish(false)
                return@addOnCompleteListener
            }

            prefs.edit {
                putString("latest_fcm_token", freshToken)
            }

            Log.d(TAG, "Fetched fresh FCM token for spawn mode")
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

    private fun getInstallId(context: Context): String {
        val prefs = context.getSharedPreferences("fcm_registration", Context.MODE_PRIVATE)
        var installId = prefs.getString("install_id", null)
        if (installId.isNullOrBlank()) {
            installId = UUID.randomUUID().toString()
            prefs.edit { putString("install_id", installId) }
        }
        return installId
    }

    fun deleteServerData(
        context: Context,
        knownProfileIds: Collection<String>,
        onComplete: (DeleteServerDataResult) -> Unit
    ) {
        val appContext = context.applicationContext

        thread(start = true, name = "delete-server-data") {
            val normalizedProfiles = knownProfileIds
                .map { normalizeProfileId(it) }
                .filter { it.isNotBlank() }
                .distinct()

            val result = postJson(
                urlString = appContext.getString(R.string.delete_server_data_url),
                payload = JSONObject().apply {
                    put("key", BuildConfig.HISTORY_SECRET_KEY)
                    put("device_id", getInstallId(appContext))
                    put("known_profile_ids", JSONArray(normalizedProfiles))
                    put("delete_dex_lists", true)
                },
                logLabel = "delete_server_data"
            )

            val rawBody = result?.responseBody.orEmpty()

            val body = runCatching {
                if (rawBody.isNotBlank()) JSONObject(rawBody) else null
            }.getOrNull()

            fun jsonStringList(array: JSONArray?): List<String> {
                if (array == null) return emptyList()
                val out = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val v = array.optString(i).trim()
                    if (v.isNotEmpty()) out += v
                }
                return out
            }

            val removedDevice = body?.optBoolean("removed_device", false) == true
            val deletedDexProfiles = jsonStringList(body?.optJSONArray("deleted_dex_profiles"))
            val oauthDeletedRows = body?.optInt("oauth_deleted_rows", 0) ?: 0
            val oauthDeletedTables = jsonStringList(body?.optJSONArray("oauth_deleted_tables"))
            val requestId = body?.optString("request_id")?.takeIf { it.isNotBlank() }
            val auditLogPath = body?.optString("audit_log_path")?.takeIf { it.isNotBlank() }

            val ok = result?.responseCode in 200..299 && (body?.optBoolean("ok", false) == true)

            val message = when {
                result == null -> "Server deletion failed"
                ok -> "Server delete ok"
                else -> {
                    val errors = body?.optJSONArray("errors")
                    if (errors != null && errors.length() > 0) {
                        errors.optString(0).ifBlank { "Server deletion failed" }
                    } else {
                        body?.optString("error")?.takeIf { it.isNotBlank() }
                            ?: "Server deletion failed"
                    }
                }
            }

            Log.d(
                TAG,
                "delete_server_data ok=$ok " +
                        "removedDevice=$removedDevice " +
                        "knownProfileCount=${normalizedProfiles.size} " +
                        "deletedDexCount=${deletedDexProfiles.size} " +
                        "oauthDeletedRows=$oauthDeletedRows " +
                        "oauthDeletedTableCount=${oauthDeletedTables.size} " +
                        "requestId=$requestId"
            )

            Handler(Looper.getMainLooper()).post {
                onComplete(
                    DeleteServerDataResult(
                        ok = ok,
                        message = message,
                        removedDevice = removedDevice,
                        deletedDexProfiles = deletedDexProfiles,
                        oauthDeletedRows = oauthDeletedRows,
                        oauthDeletedTables = oauthDeletedTables,
                        requestId = requestId,
                        auditLogPath = auditLogPath,
                        rawResponse = rawBody
                    )
                )
            }
        }
    }

    /**
     * Removes only this Android install/device from the backend registry.
     *
     * This is intentionally weaker than deleteServerData(...):
     * - it removes the current device registration;
     * - it does not ask the server to delete Dex lists;
     * - it does not ask the server to delete OAuth/profile data.
     */
    fun deleteDeviceData(
        context: Context,
        knownProfileIds: Collection<String>,
        onComplete: (DeleteDeviceDataResult) -> Unit
    ) {
        val appContext = context.applicationContext

        thread(start = true, name = "delete-device-data") {
            val normalizedProfiles = knownProfileIds
                .map { normalizeProfileId(it) }
                .filter { it.isNotBlank() }
                .distinct()

            val result = postJson(
                urlString = appContext.getString(R.string.delete_device_data_url),
                payload = JSONObject().apply {
                    put("key", BuildConfig.HISTORY_SECRET_KEY)
                    put("device_id", getInstallId(appContext))
                    put("known_profile_ids", JSONArray(normalizedProfiles))
                },
                logLabel = "delete_device_data"
            )

            val rawBody = result?.responseBody.orEmpty()
            val body = runCatching {
                if (rawBody.isNotBlank()) JSONObject(rawBody) else null
            }.getOrNull()

            fun jsonStringList(array: JSONArray?): List<String> {
                if (array == null) return emptyList()
                val out = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val v = array.optString(i).trim()
                    if (v.isNotEmpty()) out += v
                }
                return out
            }

            val ok = result?.responseCode in 200..299 && (body?.optBoolean("ok", false) == true)
            val removedDevice = body?.optBoolean("removed_device", false) == true
            val removedDeviceProfiles = jsonStringList(body?.optJSONArray("removed_device_profiles"))
            val requestId = body?.optString("request_id")?.takeIf { it.isNotBlank() }
            val auditLogPath = body?.optString("audit_log_path")?.takeIf { it.isNotBlank() }

            val message = when {
                result == null -> "Device deletion failed"
                ok -> "Device delete ok"
                else -> body?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Device deletion failed"
            }

            Log.d(
                TAG,
                "delete_device_data ok=$ok " +
                        "removedDevice=$removedDevice " +
                        "knownProfileCount=${normalizedProfiles.size} " +
                        "removedDeviceProfileCount=${removedDeviceProfiles.size} " +
                        "requestId=$requestId"
            )

            Handler(Looper.getMainLooper()).post {
                onComplete(
                    DeleteDeviceDataResult(
                        ok = ok,
                        message = message,
                        removedDevice = removedDevice,
                        removedDeviceProfiles = removedDeviceProfiles,
                        requestId = requestId,
                        auditLogPath = auditLogPath,
                        rawResponse = rawBody
                    )
                )
            }
        }
    }

    private fun normalizeProfileId(value: String?): String {
        return value?.trim()?.lowercase().orEmpty()
    }
    private fun uploadTokenBlocking(context: Context, token: String, profileId: String) {
        postJson(
            urlString = context.getString(R.string.fcm_register_url),
            payload = JSONObject().apply {
                put("key", BuildConfig.HISTORY_SECRET_KEY)
                put("device_id", getInstallId(context))
                put("device_name", buildDeviceName(context))
                put("fcm_token", token)
                put("profile_id", normalizeProfileId(profileId))
            },
            logLabel = "register_fcm"
        )
    }

    private fun uploadDexListBlocking(
        context: Context,
        profileId: String,
        profileLabel: String,
        wantedPokemon: List<String>
    ) {
        val wantedArray = JSONArray()
        for (name in wantedPokemon) {
            wantedArray.put(name)
        }

        val result = postJson(
            urlString = context.getString(R.string.dex_upload_url),
            payload = JSONObject().apply {
                put("key", BuildConfig.HISTORY_SECRET_KEY)
                put("profile_id", normalizeProfileId(profileId))
                put("profile_label", profileLabel)
                put("wanted_pokemon", wantedArray)
            },
            logLabel = "upload_dex_list"
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

    private fun postJson(urlString: String, payload: JSONObject, logLabel: String): PostJsonResult? {
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
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseText = readStream(
                if (responseCode in 200..299) conn.inputStream else conn.errorStream
            )

            Log.d(
                TAG,
                "$logLabel responseCode=$responseCode responseBodyBytes=${responseText.length}"
            )
            PostJsonResult(responseCode, responseText)
        } catch (t: Throwable) {
            Log.e(TAG, "Error $logLabel", t)
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun buildDeviceName(context: Context): String {
        val prefs = context.getSharedPreferences("fcm_registration", Context.MODE_PRIVATE)

        var installId = prefs.getString("install_id", null)
        if (installId.isNullOrBlank()) {
            installId = UUID.randomUUID().toString()
            prefs.edit {
                putString("install_id", installId)
            }
        }

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

        thread(start = true, name = "report-message") {
            val normalizedReporterProfileId = normalizeProfileId(reporterProfileId)
            val normalizedChannel = channel.trim().removePrefix("#").lowercase()

            val result = postJson(
                urlString = appContext.getString(R.string.report_message_url),
                payload = JSONObject().apply {
                    put("key", BuildConfig.HISTORY_SECRET_KEY)
                    put("reporter_profile_id", normalizedReporterProfileId)
                    put("channel", normalizedChannel)
                    put("message_user", messageUser.trim())
                    put("message_text", messageText)
                    put("message_id", messageId ?: JSONObject.NULL)
                    put("message_timestamp", messageTimestampSec ?: JSONObject.NULL)
                    put("reason", reason)
                },
                logLabel = "report_message"
            )

            val rawBody = result?.responseBody.orEmpty()
            val body = runCatching {
                if (rawBody.isNotBlank()) JSONObject(rawBody) else null
            }.getOrNull()

            val ok = result?.responseCode in 200..299 && (body?.optBoolean("ok", false) == true)

            val error = when {
                result == null -> "network_error"
                ok -> null
                else -> body?.optString("error")?.takeIf { it.isNotBlank() } ?: "report_failed"
            }

            Log.d(
                TAG,
                "report_message ok=$ok " +
                        "hasMessageId=${!messageId.isNullOrBlank()} " +
                        "error=${error ?: "none"}"
            )

            Handler(Looper.getMainLooper()).post {
                onComplete(
                    ReportMessageResult(
                        ok = ok,
                        rawResponse = rawBody,
                        error = error
                    )
                )
            }
        }
    }
}