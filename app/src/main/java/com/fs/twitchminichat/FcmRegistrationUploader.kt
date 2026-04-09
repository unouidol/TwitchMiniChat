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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.google.firebase.messaging.FirebaseMessaging

object FcmRegistrationUploader {

    private const val TAG = "FCM_REGISTER"

    data class DevicePushState(
        val deviceId: String,
        val enabledProfileIds: Set<String>,
        val pushEnabledByProfile: Map<String, Boolean>
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
    fun setProfilePushEnabled(
        context: Context,
        profileId: String,
        enabled: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("fcm_registration", Context.MODE_PRIVATE)

        fun finish(ok: Boolean) {
            Handler(Looper.getMainLooper()).post {
                onComplete(ok)
            }
        }

        fun sendRequest(token: String) {
            thread(start = true, name = "push-toggle") {
                val result = postJson(
                    urlString = appContext.getString(R.string.fcm_set_push_enabled_url),
                    payload = JSONObject().apply {
                        put("key", BuildConfig.HISTORY_SECRET_KEY)
                        put("device_id", getInstallId(appContext))
                        put("device_name", buildDeviceName(appContext))
                        put("fcm_token", token)
                        put("profile_id", normalizeProfileId(profileId))
                        put("enabled", enabled)
                    },
                    logLabel = "set_profile_push_enabled"
                )

                val ok = result?.responseCode in 200..299
                finish(ok)
            }
        }

        val cachedToken = prefs.getString("latest_fcm_token", null).orEmpty()

        if (!enabled) {
            // per disattivare possiamo anche usare il token cached, anche vuoto
            sendRequest(cachedToken)
            return
        }

        if (cachedToken.isNotBlank()) {
            sendRequest(cachedToken)
            return
        }

        Log.d(TAG, "No cached FCM token, fetching a fresh one for profileId=$profileId")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Unable to fetch fresh FCM token for profileId=$profileId", task.exception)
                finish(false)
                return@addOnCompleteListener
            }

            val freshToken = task.result?.trim().orEmpty()
            if (freshToken.isBlank()) {
                Log.w(TAG, "Fresh FCM token is blank for profileId=$profileId")
                finish(false)
                return@addOnCompleteListener
            }

            prefs.edit {
                putString("latest_fcm_token", freshToken)
            }

            Log.d(TAG, "Fetched fresh FCM token for profileId=$profileId")
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
    fun fetchDevicePushState(
        context: Context,
        knownProfileIds: Collection<String>,
        onComplete: (DevicePushState?) -> Unit
    ) {
        val appContext = context.applicationContext

        thread(start = true, name = "push-state-readback") {
            val normalizedKnown = linkedSetOf<String>()
            for (profileId in knownProfileIds) {
                val n = normalizeProfileId(profileId)
                if (n.isNotEmpty()) {
                    normalizedKnown.add(n)
                }
            }

            val result = postJson(
                urlString = appContext.getString(R.string.fcm_get_device_push_state_url),
                payload = JSONObject().apply {
                    put("key", BuildConfig.HISTORY_SECRET_KEY)
                    put("device_id", getInstallId(appContext))
                    put("known_profile_ids", JSONArray(normalizedKnown.toList()))
                },
                logLabel = "get_device_push_state"
            )

            val state = try {
                if (result == null || result.responseCode !in 200..299) {
                    null
                } else {
                    val obj = JSONObject(result.responseBody)

                    val enabledProfileIds = linkedSetOf<String>()
                    val enabledArray = obj.optJSONArray("enabled_profile_ids")
                    if (enabledArray != null) {
                        for (i in 0 until enabledArray.length()) {
                            val v = normalizeProfileId(enabledArray.optString(i))
                            if (v.isNotBlank()) {
                                enabledProfileIds.add(v)
                            }
                        }
                    }

                    val pushEnabledByProfile = linkedMapOf<String, Boolean>()
                    val mapObj = obj.optJSONObject("push_enabled_by_profile")
                    if (mapObj != null) {
                        val keys = mapObj.keys()
                        while (keys.hasNext()) {
                            val rawKey = keys.next()
                            val normalizedKey = normalizeProfileId(rawKey)
                            if (normalizedKey.isNotBlank()) {
                                pushEnabledByProfile[normalizedKey] = mapObj.optBoolean(rawKey, false)
                            }
                        }
                    }

                    DevicePushState(
                        deviceId = obj.optString("device_id", getInstallId(appContext)),
                        enabledProfileIds = enabledProfileIds,
                        pushEnabledByProfile = pushEnabledByProfile
                    )
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error parsing get_device_push_state response", t)
                null
            }

            Handler(Looper.getMainLooper()).post {
                onComplete(state)
            }
        }
    }

    fun fetchDevicePushStateWithRetry(
        context: Context,
        knownProfileIds: Collection<String>,
        attempts: Int = 2,
        delayMs: Long = 350L,
        onComplete: (DevicePushState?) -> Unit
    ) {
        val appContext = context.applicationContext
        val safeAttempts = attempts.coerceAtLeast(1)

        thread(start = true, name = "push-state-readback-retry") {
            var finalState: DevicePushState? = null

            for (index in 0 until safeAttempts) {
                val latch = CountDownLatch(1)
                var resultState: DevicePushState? = null

                fetchDevicePushState(appContext, knownProfileIds) { state ->
                    resultState = state
                    latch.countDown()
                }

                try {
                    latch.await(12, TimeUnit.SECONDS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }

                if (resultState != null) {
                    finalState = resultState
                    break
                }

                if (index < safeAttempts - 1) {
                    try {
                        Thread.sleep(delayMs)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }

            Handler(Looper.getMainLooper()).post {
                onComplete(finalState)
            }
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
            val count = runCatching {
                JSONObject(result.responseBody).optInt("count", wantedPokemon.size)
            }.getOrDefault(wantedPokemon.size)

            showToast(context, "Dex list updated for $profileLabel ($count Pokémon)")
        } else {
            showToast(context, "Error updating dex list per $profileLabel")
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

            Log.d(TAG, "$logLabel responseCode=$responseCode body=$responseText")
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
}