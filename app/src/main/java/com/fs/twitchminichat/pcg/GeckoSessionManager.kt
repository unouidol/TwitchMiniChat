package com.fs.twitchminichat.pcg

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fs.twitchminichat.FcmRegistrationUploader
import com.fs.twitchminichat.PushSettingsStore
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebExtension
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
object GeckoSessionManager {

    @Volatile
    private var runtime: GeckoRuntime? = null

    private val sessions = ConcurrentHashMap<String, GeckoSession>()

    private const val PCG_EXT_ID = "pcg-probe@example.com"
    private const val PCG_NATIVE_APP = "pcgprobe"

    private fun messageToJsonObject(message: Any?): JSONObject? {
        return when (message) {
            null -> null
            is JSONObject -> message
            is String -> runCatching { JSONObject(message) }.getOrNull()
            else -> runCatching { JSONObject(message.toString()) }.getOrNull()
        }
    }

    private fun normalizeExtractedPokemonName(raw: String?): String {
        return raw
            ?.replace("🔒", "")
            ?.trim()
            .orEmpty()
    }

    private fun jsonArrayToWantedPokemonList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()

        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val cleaned = normalizeExtractedPokemonName(arr.optString(i, null))
            if (cleaned.isNotBlank()) {
                out.add(cleaned)
            }
        }
        return out.distinct()
    }

    private fun rawArrayLockedCount(arr: JSONArray?): Int {
        if (arr == null) return 0

        var count = 0
        for (i in 0 until arr.length()) {
            val value = arr.optString(i, "")
            if (value.contains("🔒")) {
                count++
            }
        }
        return count
    }

    private fun shouldRejectAsClearlyBrokenSnapshot(
        rawNames: JSONArray?,
        wantedPokemon: List<String>
    ): Boolean {
        if (wantedPokemon.isEmpty()) {
            return true
        }

        if (rawArrayLockedCount(rawNames) > 0) {
            return true
        }

        return false
    }

    private fun handleMissingSpawnableExtract(
        appContext: Context,
        profileId: String,
        profileLabel: String,
        payload: JSONObject
    ) {
        val ok = payload.optBoolean("ok", false)
        if (!ok) {
            Log.w(
                "PCG_PROBE",
                "extract failed reason=${payload.optString("reason")} payload=$payload"
            )
            return
        }

        val rawNames = when {
            payload.has("wantedPokemon") -> payload.optJSONArray("wantedPokemon")
            payload.has("names") -> payload.optJSONArray("names")
            else -> null
        }

        val wantedPokemon = jsonArrayToWantedPokemonList(rawNames)
        val payloadCount = payload.optInt("count", -1)
        val lockedCount = rawArrayLockedCount(rawNames)

        Log.d(
            "PCG_PROBE",
            "extract success profileId=$profileId profileLabel=$profileLabel wantedCount=${wantedPokemon.size} payloadCount=$payloadCount lockedCount=$lockedCount"
        )

        if (shouldRejectAsClearlyBrokenSnapshot(rawNames, wantedPokemon)) {
            Log.w(
                "PCG_PROBE",
                "reject suspicious snapshot profileId=$profileId wantedCount=${wantedPokemon.size} payloadCount=$payloadCount lockedCount=$lockedCount"
            )
            return
        }

        Log.d(
            "PCG_PROBE",
            "uploading dex list to server profileId=$profileId profileLabel=$profileLabel count=${wantedPokemon.size}"
        )

        val pushEnabled = PushSettingsStore.isPushEnabled(appContext, profileId)

        if (pushEnabled) {
            val prefs = appContext.getSharedPreferences("fcm_registration", Context.MODE_PRIVATE)
            val token = prefs.getString("latest_fcm_token", null)

            if (!token.isNullOrBlank()) {
                FcmRegistrationUploader.uploadToken(appContext, token, profileId)
                Log.d("PCG_PROBE", "FCM token registration started for profileId=$profileId")
            } else {
                Log.w("PCG_PROBE", "No cached FCM token available for profileId=$profileId")
            }
        } else {
            Log.d("PCG_PROBE", "Push muted for profileId=$profileId, skip FCM registration")
        }

        FcmRegistrationUploader.uploadDexList(
            context = appContext,
            profileId = profileId,
            profileLabel = profileLabel,
            wantedPokemon = wantedPokemon
        )
    }

    private fun installPcgProbeExtension(
        appContext: Context,
        runtime: GeckoRuntime,
        session: GeckoSession,
        profileId: String,
        profileLabel: String
    ) {
        runtime.webExtensionController
            .ensureBuiltIn("resource://android/assets/pcg_probe/", PCG_EXT_ID)
            .accept(
                { ext ->
                    val extension = ext ?: run {
                        Log.e("PCG_PROBE", "Extension install returned null")
                        return@accept
                    }

                    Log.d("PCG_PROBE", "Extension ready: $extension")

                    Handler(Looper.getMainLooper()).post {
                        session.webExtensionController.setMessageDelegate(
                            extension,
                            object : WebExtension.MessageDelegate {
                                override fun onMessage(
                                    nativeApp: String,
                                    message: Any,
                                    sender: WebExtension.MessageSender
                                ): GeckoResult<Any>? {
                                    try {
                                        val json = messageToJsonObject(message) ?: run {
                                            Log.w(
                                                "PCG_PROBE",
                                                "onMessage: unable to parse message=$message"
                                            )
                                            return null
                                        }

                                        Log.d("PCG_PROBE", "nativeApp=$nativeApp message=$json")

                                        if (nativeApp != PCG_NATIVE_APP) {
                                            Log.d(
                                                "PCG_PROBE",
                                                "ignored: nativeApp mismatch ($nativeApp)"
                                            )
                                            return null
                                        }

                                        if (sender.environmentType != WebExtension.MessageSender.ENV_TYPE_CONTENT_SCRIPT) {
                                            Log.d(
                                                "PCG_PROBE",
                                                "ignored: sender is not content script"
                                            )
                                            return null
                                        }

                                        val type = json.optString("type")
                                        val payload = json.optJSONObject("payload") ?: JSONObject()

                                        when (type) {
                                            "pcg_probe_boot" -> {
                                                Log.d(
                                                    "PCG_PROBE",
                                                    "probe boot from frame=${payload.optJSONObject("frame")}"
                                                )
                                            }

                                            "pcg_probe_found_pokedex" -> {
                                                Log.d(
                                                    "PCG_PROBE",
                                                    "pokedex found, filters=${payload.optJSONArray("filterTexts")}"
                                                )
                                            }

                                            "pcg_probe_filters_applied" -> {
                                                Log.d(
                                                    "PCG_PROBE",
                                                    "filters applied spawnable=${payload.optBoolean("spawnableState")} obtained=${payload.optBoolean("obtainedState")}"
                                                )
                                            }

                                            "pcg_probe_progress" -> {
                                                Log.d(
                                                    "PCG_PROBE",
                                                    "progress step=${payload.optInt("step")} collected=${payload.optInt("collected")} scrollTop=${payload.optInt("scrollTop")}/${payload.optInt("scrollHeight")}"
                                                )
                                            }

                                            "pcg_missing_spawnable_extract" -> {
                                                handleMissingSpawnableExtract(
                                                    appContext = appContext,
                                                    profileId = profileId,
                                                    profileLabel = profileLabel,
                                                    payload = payload
                                                )
                                            }

                                            else -> {
                                                Log.d("PCG_PROBE", "ignored type=$type")
                                            }
                                        }
                                    } catch (t: Throwable) {
                                        Log.e("PCG_PROBE", "onMessage error", t)
                                    }

                                    return null
                                }
                            },
                            PCG_NATIVE_APP
                        )
                    }
                },
                { error ->
                    Log.e("PCG_PROBE", "Extension install error", error)
                }
            )
    }

    private fun getRuntime(appContext: Context): GeckoRuntime {
        val existing = runtime
        if (existing != null) return existing

        synchronized(this) {
            val again = runtime
            if (again != null) return again

            val contentBlocking = ContentBlocking.Settings.Builder()
                .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_ALL)
                .build()

            val settings = GeckoRuntimeSettings.Builder()
                .contentBlocking(contentBlocking)
                .build()

            settings.cookieBehaviorOptInPartitioning = false
            settings.cookieBehaviorOptInPartitioningPBM = false

            val rt = GeckoRuntime.create(appContext.applicationContext, settings)
            runtime = rt
            return rt
        }
    }

    private fun buildContextId(storageAccountId: String): String {
        return "acc_$storageAccountId"
    }

    fun getOrCreateSession(
        context: Context,
        sessionKey: String,
        storageAccountId: String
    ): GeckoSession {
        sessions[sessionKey]?.let { return it }

        val appContext = context.applicationContext
        val rt = getRuntime(appContext)

        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .contextId(buildContextId(storageAccountId))
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
            .build()

        val session = GeckoSession(settings)
        session.open(rt)

        sessions[sessionKey] = session
        return session
    }

    fun getOrCreateAccountSession(context: Context, accountId: String): GeckoSession {
        return getOrCreateSession(
            context = context,
            sessionKey = "account:$accountId",
            storageAccountId = accountId
        )
    }

    fun getOrCreatePcgSession(
        context: Context,
        profileId: String,
        profileLabel: String,
        accountId: String
    ): GeckoSession {
        val session = getOrCreateSession(
            context = context,
            sessionKey = "pcg:$accountId",
            storageAccountId = accountId
        )

        val appContext = context.applicationContext
        val rt = getRuntime(appContext)

        Handler(Looper.getMainLooper()).post {
            installPcgProbeExtension(
                appContext = appContext,
                runtime = rt,
                session = session,
                profileId = profileId,
                profileLabel = profileLabel
            )
        }

        return session
    }

    fun getOrCreateStreamSession(context: Context, accountId: String): GeckoSession {
        return getOrCreateSession(
            context = context,
            sessionKey = "stream:$accountId",
            storageAccountId = accountId
        )
    }

    fun destroy(sessionKey: String) {
        sessions.remove(sessionKey)?.let { s ->
            try {
                s.close()
            } catch (_: Throwable) {
            }
        }
    }

    fun destroyPcgSession(accountId: String) {
        destroy("pcg:$accountId")
    }

    fun destroyStreamSession(accountId: String) {
        destroy("stream:$accountId")
    }

    fun destroyAccountSession(accountId: String) {
        destroy("account:$accountId")
    }
}