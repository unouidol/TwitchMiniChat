package com.fs.twitchminichat.pcg

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import com.fs.twitchminichat.FcmRegistrationUploader
import com.fs.twitchminichat.InventoryBallItem
import com.fs.twitchminichat.InventoryBallStore
import com.fs.twitchminichat.PushSettingsStore
import com.fs.twitchminichat.R
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
object GeckoSessionManager {

    @Volatile
    private var runtime: GeckoRuntime? = null

    @Volatile
    private var lastInventoryLoadedToastAt: Long = 0L

    private val sessions = ConcurrentHashMap<String, GeckoSession>()

    private const val PCG_EXT_ID = "pcg-probe@example.com"
    private const val PCG_NATIVE_APP = "pcgprobe"

    private const val TYPE_PCG_INVENTORY_WRONG_TAB = "pcg_inventory_wrong_tab"
    private const val TYPE_PCG_POKEDEX_WRONG_TAB = "pcg_pokedex_wrong_tab"

    /**
     * Time window after the user presses a manual PCG update button.
     *
     * If no valid matching snapshot arrives within this time, TMC assumes the user
     * is probably on the wrong PCG tab or the tab has not finished loading yet.
     */
    private const val MANUAL_PCG_UPDATE_TIMEOUT_MS = 15_000L

    /**
     * Maximum age for a cached Pokédex snapshot that can be accepted when the user
     * presses Register Pokédex.
     */
    private const val CACHED_POKEDEX_SNAPSHOT_MAX_AGE_MS = 15 * 60 * 1000L

    /**
     * Maximum age for a cached Inventory snapshot that can be accepted when the
     * user presses Register inventory.
     */
    private const val CACHED_INVENTORY_SNAPSHOT_MAX_AGE_MS = 15 * 60 * 1000L

    private const val PCG_SESSION_KEEP_ALIVE_MS = 5 * 60 * 1000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val pendingManualInventoryRequests =
        ConcurrentHashMap<String, Long>()

    private val pendingManualPokedexRequests =
        ConcurrentHashMap<String, Long>()

    private val lastInventorySnapshots =
        ConcurrentHashMap<String, CachedInventorySnapshot>()

    private val lastPokedexSnapshots =
        ConcurrentHashMap<String, CachedPokedexSnapshot>()

    private val pendingPcgDestroyRunnables =
        ConcurrentHashMap<String, Runnable>()

    private val loadedPcgUrls =
        ConcurrentHashMap<String, String>()

    /**
     * Last valid inventory snapshot seen passively by the PCG probe.
     *
     * Android only saves this snapshot after the user explicitly presses
     * Register inventory.
     */
    private data class CachedInventorySnapshot(
        val profileId: String,
        val profileLabel: String,
        val balls: List<InventoryBallItem>,
        val capturedAtMs: Long
    )

    /**
     * Last valid Pokédex snapshot seen passively by the PCG probe.
     *
     * Android only uploads this snapshot after the user explicitly presses
     * Register Pokédex.
     */
    private data class CachedPokedexSnapshot(
        val profileId: String,
        val profileLabel: String,
        val wantedPokemon: List<String>,
        val capturedAtMs: Long
    )

    private fun messageToJsonObject(message: Any?): JSONObject? {
        return when (message) {
            null -> null
            is JSONObject -> message
            is String -> runCatching { JSONObject(message) }.getOrNull()
            else -> runCatching { JSONObject(message.toString()) }.getOrNull()
        }
    }

    private fun showToastOnMain(
        appContext: Context,
        @StringRes messageRes: Int,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                appContext,
                appContext.getString(messageRes),
                duration
            ).show()
        }
    }

    private fun pcgSessionKey(accountId: String): String {
        return "pcg:$accountId"
    }

    private fun isCachedSnapshotFresh(
        capturedAtMs: Long,
        maxAgeMs: Long,
        debugLabel: String,
        sessionKey: String
    ): Boolean {
        val ageMs = SystemClock.elapsedRealtime() - capturedAtMs
        val fresh = ageMs in 0L..maxAgeMs

        Log.d(
            "PCG_PROBE",
            "cached $debugLabel snapshot check sessionKey=$sessionKey ageMs=$ageMs maxAgeMs=$maxAgeMs fresh=$fresh"
        )

        return fresh
    }

    private fun consumeManualUpdateRequest(
        sessionKey: String,
        pendingRequests: ConcurrentHashMap<String, Long>,
        debugLabel: String
    ): Boolean {
        val requestedAtMs = pendingRequests[sessionKey] ?: return false
        val ageMs = SystemClock.elapsedRealtime() - requestedAtMs

        if (ageMs > MANUAL_PCG_UPDATE_TIMEOUT_MS) {
            pendingRequests.remove(sessionKey, requestedAtMs)

            Log.d(
                "PCG_PROBE",
                "ignore expired manual $debugLabel snapshot sessionKey=$sessionKey ageMs=$ageMs"
            )

            return false
        }

        pendingRequests.remove(sessionKey, requestedAtMs)

        Log.d(
            "PCG_PROBE",
            "manual $debugLabel snapshot accepted sessionKey=$sessionKey ageMs=$ageMs"
        )

        return true
    }

    private fun handleManualWrongTabMessage(
        appContext: Context,
        accountId: String,
        pendingRequests: ConcurrentHashMap<String, Long>,
        @StringRes messageRes: Int,
        debugLabel: String
    ) {
        val sessionKey = pcgSessionKey(accountId)
        val requestedAtMs = pendingRequests.remove(sessionKey)

        if (requestedAtMs == null) {
            Log.d(
                "PCG_PROBE",
                "ignore $debugLabel wrong-tab message because no manual request is pending sessionKey=$sessionKey"
            )
            return
        }

        val ageMs = SystemClock.elapsedRealtime() - requestedAtMs

        Log.d(
            "PCG_PROBE",
            "manual $debugLabel wrong-tab message accepted sessionKey=$sessionKey ageMs=$ageMs"
        )

        showToastOnMain(
            appContext = appContext,
            messageRes = messageRes,
            duration = Toast.LENGTH_LONG
        )
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
        accountId: String,
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

        val sessionKey = pcgSessionKey(accountId)

        val snapshot = CachedPokedexSnapshot(
            profileId = profileId,
            profileLabel = profileLabel,
            wantedPokemon = wantedPokemon,
            capturedAtMs = SystemClock.elapsedRealtime()
        )

        lastPokedexSnapshots[sessionKey] = snapshot

        if (!consumeManualUpdateRequest(
                sessionKey = sessionKey,
                pendingRequests = pendingManualPokedexRequests,
                debugLabel = "pokedex"
            )
        ) {
            Log.d(
                "PCG_PROBE",
                "pokedex snapshot cached but not uploaded because no manual request is pending " +
                        "sessionKey=$sessionKey profileId=$profileId count=${wantedPokemon.size}"
            )
            return
        }

        uploadManualPokedexSnapshot(
            appContext = appContext,
            sessionKey = sessionKey,
            snapshot = snapshot,
            source = "live"
        )
    }

    private fun handleInventoryBallExtract(
        appContext: Context,
        accountId: String,
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

        val sessionKey = pcgSessionKey(accountId)

        val snapshot = CachedInventorySnapshot(
            profileId = profileId,
            profileLabel = profileLabel,
            balls = balls,
            capturedAtMs = SystemClock.elapsedRealtime()
        )

        lastInventorySnapshots[sessionKey] = snapshot

        if (!consumeManualUpdateRequest(
                sessionKey = sessionKey,
                pendingRequests = pendingManualInventoryRequests,
                debugLabel = "inventory"
            )
        ) {
            Log.d(
                "PCG_PROBE",
                "inventory snapshot cached but not saved because no manual request is pending " +
                        "sessionKey=$sessionKey profileId=$profileId count=${balls.size}"
            )
            return
        }

        saveManualInventorySnapshot(
            appContext = appContext,
            sessionKey = sessionKey,
            snapshot = snapshot,
            source = "live"
        )

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
        profileLabel: String,
        accountId: String
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
                                            "pcg_probe_boot_debug" -> {
                                                Log.d(
                                                    "PCG_PROBE",
                                                    "boot debug payload=$payload"
                                                )
                                            }

                                            "pcg_content_absolute_boot" -> {
                                                Log.d(
                                                    "PCG_PROBE",
                                                    "content absolute boot payload=$payload"
                                                )
                                            }

                                            "pcg_inventory_absolute_boot" -> {
                                                Log.d(
                                                    "PCG_PROBE",
                                                    "inventory absolute boot payload=$payload"
                                                )
                                            }

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

                                            TYPE_PCG_INVENTORY_WRONG_TAB -> {
                                                handleManualWrongTabMessage(
                                                    appContext = appContext,
                                                    accountId = accountId,
                                                    pendingRequests = pendingManualInventoryRequests,
                                                    messageRes = R.string.pcg_inventory_wrong_tab,
                                                    debugLabel = "inventory"
                                                )
                                            }

                                            TYPE_PCG_POKEDEX_WRONG_TAB -> {
                                                handleManualWrongTabMessage(
                                                    appContext = appContext,
                                                    accountId = accountId,
                                                    pendingRequests = pendingManualPokedexRequests,
                                                    messageRes = R.string.pcg_pokedex_wrong_tab,
                                                    debugLabel = "pokedex"
                                                )
                                            }

                                            "pcg_missing_spawnable_extract" -> {
                                                handleMissingSpawnableExtract(
                                                    appContext = appContext,
                                                    accountId = accountId,
                                                    profileId = profileId,
                                                    profileLabel = profileLabel,
                                                    payload = payload
                                                )
                                            }

                                            "pcg_inventory_ball_extract" -> {
                                                handleInventoryBallExtract(
                                                    appContext = appContext,
                                                    accountId = accountId,
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

    fun requestManualInventoryUpdate(
        context: Context,
        accountId: String
    ): Boolean {
        val sessionKey = pcgSessionKey(accountId)

        if (!sessions.containsKey(sessionKey)) {
            Log.w(
                "PCG_PROBE",
                "manual inventory registration ignored, PCG session not ready sessionKey=$sessionKey"
            )
            return false
        }

        val requestedAtMs = SystemClock.elapsedRealtime()
        pendingManualInventoryRequests[sessionKey] = requestedAtMs

        Log.d(
            "PCG_PROBE",
            "manual inventory registration armed sessionKey=$sessionKey requestedAtMs=$requestedAtMs"
        )

        val cachedSnapshot = lastInventorySnapshots[sessionKey]
        if (
            cachedSnapshot != null &&
            isCachedSnapshotFresh(
                capturedAtMs = cachedSnapshot.capturedAtMs,
                maxAgeMs = CACHED_INVENTORY_SNAPSHOT_MAX_AGE_MS,
                debugLabel = "inventory",
                sessionKey = sessionKey
            )
        ) {
            if (consumeManualUpdateRequest(
                    sessionKey = sessionKey,
                    pendingRequests = pendingManualInventoryRequests,
                    debugLabel = "inventory"
                )
            ) {
                saveManualInventorySnapshot(
                    appContext = context.applicationContext,
                    sessionKey = sessionKey,
                    snapshot = cachedSnapshot,
                    source = "cached"
                )
            }

            return true
        }

        scheduleManualUpdateTimeout(
            appContext = context.applicationContext,
            sessionKey = sessionKey,
            requestedAtMs = requestedAtMs,
            pendingRequests = pendingManualInventoryRequests,
            timeoutMessageRes = R.string.pcg_inventory_wrong_tab,
            debugLabel = "inventory"
        )

        return true
    }

    fun requestManualPokedexUpdate(
        context: Context,
        accountId: String
    ): Boolean {
        val sessionKey = pcgSessionKey(accountId)

        if (!sessions.containsKey(sessionKey)) {
            Log.w(
                "PCG_PROBE",
                "manual pokedex registration ignored, PCG session not ready sessionKey=$sessionKey"
            )
            return false
        }

        val requestedAtMs = SystemClock.elapsedRealtime()
        pendingManualPokedexRequests[sessionKey] = requestedAtMs

        Log.d(
            "PCG_PROBE",
            "manual pokedex registration armed sessionKey=$sessionKey requestedAtMs=$requestedAtMs"
        )

        val cachedSnapshot = lastPokedexSnapshots[sessionKey]
        if (
            cachedSnapshot != null &&
            isCachedSnapshotFresh(
                capturedAtMs = cachedSnapshot.capturedAtMs,
                maxAgeMs = CACHED_POKEDEX_SNAPSHOT_MAX_AGE_MS,
                debugLabel = "pokedex",
                sessionKey = sessionKey
            )
        ) {
            if (consumeManualUpdateRequest(
                    sessionKey = sessionKey,
                    pendingRequests = pendingManualPokedexRequests,
                    debugLabel = "pokedex"
                )
            ) {
                uploadManualPokedexSnapshot(
                    appContext = context.applicationContext,
                    sessionKey = sessionKey,
                    snapshot = cachedSnapshot,
                    source = "cached"
                )
            }

            return true
        }

        scheduleManualUpdateTimeout(
            appContext = context.applicationContext,
            sessionKey = sessionKey,
            requestedAtMs = requestedAtMs,
            pendingRequests = pendingManualPokedexRequests,
            timeoutMessageRes = R.string.pcg_pokedex_wrong_tab,
            debugLabel = "pokedex"
        )

        return true
    }

    private fun scheduleManualUpdateTimeout(
        appContext: Context,
        sessionKey: String,
        requestedAtMs: Long,
        pendingRequests: ConcurrentHashMap<String, Long>,
        @StringRes timeoutMessageRes: Int,
        debugLabel: String
    ) {
        mainHandler.postDelayed({
            val stillPendingAtMs = pendingRequests[sessionKey]

            if (stillPendingAtMs == requestedAtMs) {
                pendingRequests.remove(sessionKey, requestedAtMs)

                Log.d(
                    "PCG_PROBE",
                    "manual $debugLabel registration timed out sessionKey=$sessionKey"
                )

                showToastOnMain(
                    appContext = appContext,
                    messageRes = timeoutMessageRes,
                    duration = Toast.LENGTH_LONG
                )
            }
        }, MANUAL_PCG_UPDATE_TIMEOUT_MS)
    }

    private fun saveManualInventorySnapshot(
        appContext: Context,
        sessionKey: String,
        snapshot: CachedInventorySnapshot,
        source: String
    ) {
        InventoryBallStore.saveRealSnapshot(
            context = appContext,
            profileId = snapshot.profileId,
            balls = snapshot.balls
        )

        val now = SystemClock.elapsedRealtime()
        if (now - lastInventoryLoadedToastAt > 5000L) {
            lastInventoryLoadedToastAt = now
            showToastOnMain(
                appContext = appContext,
                messageRes = R.string.inventory_loaded_success,
                duration = Toast.LENGTH_SHORT
            )
        }

        Log.d(
            "PCG_PROBE",
            "manual inventory extract success source=$source sessionKey=$sessionKey " +
                    "profileId=${snapshot.profileId} profileLabel=${snapshot.profileLabel} " +
                    "balls=${snapshot.balls}"
        )
    }

    private fun uploadManualPokedexSnapshot(
        appContext: Context,
        sessionKey: String,
        snapshot: CachedPokedexSnapshot,
        source: String
    ) {
        Log.d(
            "PCG_PROBE",
            "manual pokedex upload accepted source=$source sessionKey=$sessionKey " +
                    "profileId=${snapshot.profileId} profileLabel=${snapshot.profileLabel} " +
                    "count=${snapshot.wantedPokemon.size}"
        )

        val pushEnabled = PushSettingsStore.isPushEnabled(appContext, snapshot.profileId)

        if (pushEnabled) {
            val prefs = appContext.getSharedPreferences("fcm_registration", Context.MODE_PRIVATE)
            val token = prefs.getString("latest_fcm_token", null)

            if (!token.isNullOrBlank()) {
                FcmRegistrationUploader.uploadToken(appContext, token, snapshot.profileId)
                Log.d("PCG_PROBE", "FCM token registration started for profileId=${snapshot.profileId}")
            } else {
                Log.w("PCG_PROBE", "No cached FCM token available for profileId=${snapshot.profileId}")
            }
        } else {
            Log.d("PCG_PROBE", "Push muted for profileId=${snapshot.profileId}, skip FCM registration")
        }

        FcmRegistrationUploader.uploadDexList(
            context = appContext,
            profileId = snapshot.profileId,
            profileLabel = snapshot.profileLabel,
            wantedPokemon = snapshot.wantedPokemon
        )
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
            sessionKey = pcgSessionKey(accountId),
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
                profileLabel = profileLabel,
                accountId = accountId
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
        sessions.remove(sessionKey)?.let { session ->
            try {
                session.close()
            } catch (_: Throwable) {
            }
        }
    }

    fun destroyPcgSession(accountId: String) {
        val sessionKey = pcgSessionKey(accountId)

        cancelScheduledPcgDestroy(accountId)
        loadedPcgUrls.remove(sessionKey)
        destroy(sessionKey)
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
                Log.d(
                    "GECKO_CLEAR",
                    "start existingRuntime=${runtime != null} sessionsToClose=${sessionsToClose.size}"
                )

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