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
import org.mozilla.geckoview.StorageController
import com.fs.twitchminichat.InventoryBallItem
import com.fs.twitchminichat.InventoryBallStore
import android.os.SystemClock
import android.widget.Toast
import com.fs.twitchminichat.R
import org.mozilla.geckoview.GeckoView


@Suppress("unused")
object GeckoSessionManager {

    @Volatile
    private var runtime: GeckoRuntime? = null

    @Volatile
    private var lastInventoryLoadedToastAt: Long = 0L

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

    private fun handleInventoryBallExtract(
        appContext: Context,
        profileId: String,
        profileLabel: String,
        payload: JSONObject
    ) {
        val ok = payload.optBoolean("ok", false)

        if (!ok) {
            Log.w(
                "PCG_PROBE",
                "inventory extract failed profileId=$profileId profileLabel=$profileLabel " +
                        "reason=${payload.optString("reason")} payload=$payload"
            )
            return
        }

        val balls = jsonArrayToInventoryBallList(payload.optJSONArray("balls"))
        if (balls.isEmpty()) {
            Log.w(
                "PCG_PROBE",
                "inventory extract empty profileId=$profileId profileLabel=$profileLabel payload=$payload"
            )
            return
        }

        InventoryBallStore.saveRealSnapshot(
            context = appContext,
            profileId = profileId,
            balls = balls
        )

        val now = SystemClock.elapsedRealtime()
        if (now - lastInventoryLoadedToastAt > 5000L) {
            lastInventoryLoadedToastAt = now
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.inventory_loaded_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        Log.d(
            "PCG_PROBE",
            "inventory extract success profileId=$profileId profileLabel=$profileLabel balls=$balls"
        )
    }

    private fun jsonArrayToInventoryBallList(arr: JSONArray?): List<InventoryBallItem> {
        if (arr == null) return emptyList()

        val out = ArrayList<InventoryBallItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue

            val ballId = obj.optString("ballId").trim()
            val name = obj.optString("name").trim()
            val count = obj.optInt("count", -1)

            if (ballId.isBlank() || name.isBlank() || count < 0) continue

            out.add(
                InventoryBallItem(
                    ballId = ballId,
                    name = name,
                    count = count
                )
            )
        }

        return out
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
                                                    "progress " +
                                                            "step=${payload.optInt("step", -1)} " +
                                                            "phase=${payload.optString("phase")} " +
                                                            "reason=${payload.optString("reason")} " +
                                                            "collected=${payload.optInt("collected", -1)} " +
                                                            "scrollTop=${payload.optInt("scrollTop", -1)}/${payload.optInt("scrollHeight", -1)} " +
                                                            "href=${payload.optString("href")} " +
                                                            "host=${payload.optString("host")}"
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

                                            "pcg_inventory_ball_extract" -> {
                                                handleInventoryBallExtract(appContext, profileId, profileLabel, payload)
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
        cancelScheduledPcgDestroy(accountId)
        loadedPcgUrls.remove(pcgSessionKey(accountId))
        destroy(pcgSessionKey(accountId))
    }

    fun destroyStreamSession(accountId: String) {
        destroy("stream:$accountId")
    }

    fun destroyAccountSession(accountId: String) {
        destroy("account:$accountId")
    }

    fun clearAllWebData(
        context: Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        val appContext = context.applicationContext

        Handler(Looper.getMainLooper()).post {
            try {
                val sessionsToClose = sessions.values.toList()
                Log.d("GECKO_CLEAR", "start existingRuntime=${runtime != null} sessionsToClose=${sessionsToClose.size}")

                for (session in sessionsToClose) {
                    runCatching {
                        session.setActive(false)
                    }
                    runCatching {
                        session.close()
                    }
                }

                sessions.clear()

                for (runnable in pendingPcgDestroyRunnables.values) {
                    mainHandler.removeCallbacks(runnable)
                }

                pendingPcgDestroyRunnables.clear()
                loadedPcgUrls.clear()

                val rt = getRuntime(appContext)

                rt.storageController
                    .clearData(StorageController.ClearFlags.ALL)
                    .accept(
                        {
                            Log.d("GECKO_CLEAR", "clearData(ALL) completed")
                            onComplete(true, "Gecko web data cleared")
                        },
                        { error ->
                            Log.e("GECKO_CLEAR", "clearData(ALL) failed", error)
                            onComplete(false, error?.message ?: "Gecko clear failed")
                        }
                    )
            } catch (t: Throwable) {
                Log.e("GECKO_CLEAR", "unexpected error during Gecko clear", t)
                onComplete(false, t.message ?: "Unexpected Gecko clear error")
            }
        }
    }

    private const val PCG_SESSION_KEEP_ALIVE_MS = 5 * 60 * 1000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val pendingPcgDestroyRunnables = ConcurrentHashMap<String, Runnable>()

    private val loadedPcgUrls = ConcurrentHashMap<String, String>()

    private fun pcgSessionKey(accountId: String): String {
        return "pcg:$accountId"
    }

    private fun assertMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "GeckoSessionManager PCG session calls must run on the main thread"
        }
    }

    fun attachPcgSessionToView(
        context: Context,
        geckoView: GeckoView,
        profileId: String,
        profileLabel: String,
        accountId: String
    ): GeckoSession {
        assertMainThread()

        cancelScheduledPcgDestroy(accountId)

        val session = getOrCreatePcgSession(
            context = context,
            profileId = profileId,
            profileLabel = profileLabel,
            accountId = accountId
        )

        if (geckoView.session !== session) {
            if (geckoView.session != null) {
                runCatching {
                    geckoView.releaseSession()
                }.onFailure { t ->
                    Log.w("PCG_PROBE", "release previous GeckoView session failed", t)
                }
            }

            geckoView.setSession(session)
        }

        runCatching {
            session.setActive(true)
        }

        runCatching {
            session.setFocused(true)
        }

        runCatching {
            session.setPriorityHint(GeckoSession.PRIORITY_HIGH)
        }

        return session
    }

    fun detachPcgSessionFromView(
        geckoView: GeckoView,
        accountId: String
    ) {
        assertMainThread()

        val sessionKey = pcgSessionKey(accountId)
        val session = sessions[sessionKey] ?: return

        if (geckoView.session === session) {
            runCatching {
                geckoView.releaseSession()
            }.onFailure { t ->
                Log.w("PCG_PROBE", "release PCG GeckoView session failed", t)
            }
        }

        runCatching {
            session.setFocused(false)
        }

        runCatching {
            session.setPriorityHint(GeckoSession.PRIORITY_DEFAULT)
        }

        schedulePcgDestroyAfterIdle(accountId)
    }

    fun loadPcgUriIfNeeded(
        accountId: String,
        session: GeckoSession,
        url: String
    ) {
        assertMainThread()

        val sessionKey = pcgSessionKey(accountId)
        val previousUrl = loadedPcgUrls[sessionKey]

        if (previousUrl == url) {
            Log.d("PCG_PROBE", "skip PCG reload, already loaded url=$url")
            return
        }

        loadedPcgUrls[sessionKey] = url
        session.loadUri(url)

        Log.d("PCG_PROBE", "load PCG url=$url")
    }

    fun isPcgUriAlreadyLoaded(
        accountId: String,
        url: String
    ): Boolean {
        val sessionKey = pcgSessionKey(accountId)
        return loadedPcgUrls[sessionKey] == url
    }

    private fun schedulePcgDestroyAfterIdle(accountId: String) {
        val sessionKey = pcgSessionKey(accountId)

        cancelScheduledPcgDestroy(accountId)

        val runnable = Runnable {
            pendingPcgDestroyRunnables.remove(sessionKey)

            val session = sessions.remove(sessionKey) ?: return@Runnable

            loadedPcgUrls.remove(sessionKey)

            runCatching {
                session.setFocused(false)
            }

            runCatching {
                session.setActive(false)
            }

            runCatching {
                session.close()
            }.onFailure { t ->
                Log.w("PCG_PROBE", "idle PCG session close failed accountId=$accountId", t)
            }

            Log.d("PCG_PROBE", "idle PCG session closed accountId=$accountId")
        }

        pendingPcgDestroyRunnables[sessionKey] = runnable
        mainHandler.postDelayed(runnable, PCG_SESSION_KEEP_ALIVE_MS)

        Log.d("PCG_PROBE", "scheduled PCG session idle close accountId=$accountId")
    }

    private fun cancelScheduledPcgDestroy(accountId: String) {
        val sessionKey = pcgSessionKey(accountId)

        val oldRunnable = pendingPcgDestroyRunnables.remove(sessionKey)
        if (oldRunnable != null) {
            mainHandler.removeCallbacks(oldRunnable)
            Log.d("PCG_PROBE", "cancelled PCG session idle close accountId=$accountId")
        }
    }

}