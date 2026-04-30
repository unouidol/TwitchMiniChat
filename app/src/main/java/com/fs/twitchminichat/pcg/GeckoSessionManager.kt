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

    private const val TAG_PCG_PROBE = "PCG_PROBE"
    private const val TAG_INV_GATE = "PCG_INV_GATE"
    private const val TAG_INV_CAPTURE = "PCG_INV_CAPTURE"
    private const val TAG_INV_UI = "PCG_INV_UI"

    private const val PCG_EXT_ID = "pcg-probe@example.com"
    private const val PCG_NATIVE_APP = "pcgprobe"

    private const val TYPE_PCG_INVENTORY_WRONG_TAB = "pcg_inventory_wrong_tab"
    private const val TYPE_PCG_POKEDEX_WRONG_TAB = "pcg_pokedex_wrong_tab"
    private const val TYPE_PCG_TAB_STATE = "pcg_tab_state"

    /**
     * How long a manual Pokédex update request waits for a fresh probe snapshot.
     *
     * Inventory no longer uses this old pending-request flow. Inventory uses
     * PcgManualSnapshotCapture instead.
     */
    private const val MANUAL_PCG_UPDATE_TIMEOUT_MS = 2_000L

    /**
     * How long a successful passive Pokédex snapshot can be reused for a manual
     * Register Pokédex press.
     *
     * The snapshot is still invalidated earlier if the Pokédex probe later reports
     * that the Pokédex DOM is no longer readable.
     */
    private const val CACHED_POKEDEX_SNAPSHOT_MAX_AGE_MS = 2 * 60 * 1000L

    private const val PCG_SESSION_KEEP_ALIVE_MS = 15 * 60 * 1_000L

    private const val MANUAL_INVENTORY_CAPTURE_DURATION_MS = 5_000L
    private const val MANUAL_INVENTORY_CAPTURE_TIMEOUT_MS = 10_000L

    /**
     * A short grace window for detecting that Inventory was recently visible.
     *
     * This is not used as an expiry timer anymore. It is only used when the UI asks
     * "is Inventory currently available?" after a recent marker/snapshot.
     */
    private const val PCG_INVENTORY_TAB_SIGNAL_MAX_AGE_MS = 3_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val pendingManualPokedexRequests =
        ConcurrentHashMap<String, Long>()

    private val lastInventorySnapshots =
        ConcurrentHashMap<String, CachedInventorySnapshot>()

    private val lastPokedexSnapshots =
        ConcurrentHashMap<String, CachedPokedexSnapshot>()

    private val manualInventoryCaptures =
        ConcurrentHashMap<String, PcgManualSnapshotCapture<CachedInventorySnapshot>>()

    private val inventoryCaptureUiListeners =
        ConcurrentHashMap<String, InventoryCaptureUiListener>()

    /**
     * Prevents duplicate manual Pokédex uploads/toasts caused by multiple valid
     * probe snapshots arriving very close together.
     */
    private const val MANUAL_POKEDEX_UPLOAD_DEDUP_MS = 1_500L

    private val recentManualPokedexUploadKeys =
        ConcurrentHashMap<String, Long>()

    private val lastPcgTabStates =
        ConcurrentHashMap<String, CachedPcgTabState>()

    /**
     * Last moment when Android had a lightweight signal that the Inventory page was visible.
     *
     * This is separate from lastInventorySnapshots because a cached snapshot may stay
     * recent even after the user moved to another PCG tab.
     */
    private val lastInventoryPageSeenAtMs =
        ConcurrentHashMap<String, Long>()

    /**
     * Locks the Inventory update button after a successful manual read.
     *
     * Once locked, passive Inventory snapshots from the same page visit must not
     * re-enable the button. The user must leave Inventory and enter it again.
     */
    private val inventoryUpdateLockedUntilReentry =
        ConcurrentHashMap<String, Boolean>()

    /**
     * Tracks whether the user left Inventory after the button was locked.
     *
     * This lets us distinguish:
     * - same Inventory visit still emitting passive snapshots;
     * - a real new Inventory visit after the user changed tab and came back.
     */
    private val inventoryTabLeftAfterUpdate =
        ConcurrentHashMap<String, Boolean>()

    private val pendingPcgDestroyRunnables =
        ConcurrentHashMap<String, Runnable>()

    private val loadedPcgUrls =
        ConcurrentHashMap<String, String>()

    /**
     * Last valid Inventory snapshot seen passively by the PCG probe.
     *
     * Android only saves this snapshot after the user explicitly presses
     * Update inventory.
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

    /**
     * Last known PCG tab state reported by the WebExtension.
     *
     * activeTab is preferred over raw DOM visibility because PCG can keep inactive
     * tab content mounted in the DOM. Visibility remains only as a fallback when
     * activeTab cannot be identified.
     */
    private data class CachedPcgTabState(
        val activeTab: String,
        val pokedexVisible: Boolean,
        val inventoryVisible: Boolean,
        val anyLoadedSurface: Boolean,
        val capturedAtMs: Long
    ) {
        /**
         * True only when the probe identified a concrete active tab.
         *
         * Unknown tab-state must not be used as a hard negative signal, otherwise the
         * Inventory button can turn off while the user is still on Inventory.
         */
        val hasKnownActiveTab: Boolean
            get() = activeTab.isNotBlank() && activeTab != "unknown"

        val isInventoryActive: Boolean
            get() = when (activeTab) {
                "inventory" -> true
                "unknown", "" -> inventoryVisible
                else -> false
            }

        val isPokedexActive: Boolean
            get() = when (activeTab) {
                "pokedex" -> true
                "unknown", "" -> pokedexVisible
                else -> false
            }
    }

    /**
     * Small UI bridge used by the PCG screen to reflect manual Inventory capture state.
     *
     * GeckoSessionManager owns the passive PCG probe messages, while PcgActivity owns
     * the Android views. This listener keeps button/progress-bar UI outside of the
     * low-level Gecko message handling.
     */
    interface InventoryCaptureUiListener {
        fun onInventoryTabAvailabilityChanged(isAvailable: Boolean)
        fun onInventoryCaptureStarted()
        fun onInventoryCaptureProgress(progress: Float)
        fun onInventoryCaptureFinished()
    }

    /**
     * Registers or clears the Inventory capture UI listener for one account.
     *
     * PcgActivity calls this when its PCG controls are created/destroyed. The listener
     * is keyed by the same accountId used for the PCG GeckoSession.
     */
    fun setInventoryCaptureUiListener(
        accountId: String,
        listener: InventoryCaptureUiListener?
    ) {
        val sessionKey = pcgSessionKey(accountId)

        if (listener == null) {
            inventoryCaptureUiListeners.remove(sessionKey)
            return
        }

        inventoryCaptureUiListeners[sessionKey] = listener

        mainHandler.post {
            listener.onInventoryTabAvailabilityChanged(
                isInventoryTabRecentlyVisible(sessionKey)
            )

            if (manualInventoryCaptures[sessionKey]?.isActive == true) {
                listener.onInventoryCaptureStarted()
            }
        }
    }

    /**
     * Cancels any in-flight Inventory capture for this account.
     *
     * PcgActivity calls this before destroying its UI so a late progress/completion
     * callback cannot update a closed screen.
     */
    fun cancelManualInventoryUpdate(accountId: String) {
        val sessionKey = pcgSessionKey(accountId)
        manualInventoryCaptures[sessionKey]?.cancel()
    }

    private fun notifyInventoryTabAvailabilityForSession(sessionKey: String) {
        val listener = inventoryCaptureUiListeners[sessionKey] ?: return
        val available = isInventoryTabRecentlyVisible(sessionKey)

        Log.d(
            TAG_INV_UI,
            "notify_availability sessionKey=$sessionKey available=$available " +
                    "captureActive=${manualInventoryCaptures[sessionKey]?.isActive == true}"
        )

        mainHandler.post {
            listener.onInventoryTabAvailabilityChanged(available)
        }
    }

    private fun notifyInventoryCaptureStarted(sessionKey: String) {
        val listener = inventoryCaptureUiListeners[sessionKey] ?: return

        mainHandler.post {
            listener.onInventoryCaptureStarted()
        }
    }

    private fun notifyInventoryCaptureProgress(
        sessionKey: String,
        progress: Float
    ) {
        val listener = inventoryCaptureUiListeners[sessionKey] ?: return

        mainHandler.post {
            listener.onInventoryCaptureProgress(progress)
        }
    }

    private fun notifyInventoryCaptureFinished(sessionKey: String) {
        val listener = inventoryCaptureUiListeners[sessionKey] ?: return

        mainHandler.post {
            listener.onInventoryCaptureFinished()
            listener.onInventoryTabAvailabilityChanged(
                isInventoryTabRecentlyVisible(sessionKey)
            )
        }
    }

    private fun isInventoryUpdateLocked(sessionKey: String): Boolean {
        return inventoryUpdateLockedUntilReentry[sessionKey] == true
    }

    /**
     * Locks the Inventory button after a completed manual read.
     *
     * The lock prevents passive snapshots from the same Inventory page from
     * immediately re-enabling the button after "Inventory updated".
     */
    private fun lockInventoryUpdateUntilReentry(sessionKey: String) {
        inventoryUpdateLockedUntilReentry[sessionKey] = true
        inventoryTabLeftAfterUpdate.remove(sessionKey)
        lastInventoryPageSeenAtMs.remove(sessionKey)
        lastInventorySnapshots.remove(sessionKey)

        Log.d(
            TAG_INV_GATE,
            "inventory update locked until reentry sessionKey=$sessionKey"
        )
    }

    /**
     * Notes that the user reached a known non-Inventory tab.
     *
     * If the Inventory button is locked, this is the first half of the unlock
     * handshake: leave Inventory first, then re-enter Inventory.
     */
    private fun markInventoryTabLeftIfLocked(sessionKey: String) {
        if (isInventoryUpdateLocked(sessionKey)) {
            inventoryTabLeftAfterUpdate[sessionKey] = true

            Log.d(
                TAG_INV_GATE,
                "inventory tab left after update sessionKey=$sessionKey"
            )
        }
    }

    /**
     * Returns true when a newly detected Inventory tab should enable the button.
     *
     * If the button is locked after an update, we only unlock after Android has first
     * seen a known non-Inventory tab and then sees Inventory again.
     */
    private fun canEnableInventoryForCurrentVisit(sessionKey: String): Boolean {
        if (!isInventoryUpdateLocked(sessionKey)) {
            return true
        }

        val leftInventoryAfterUpdate = inventoryTabLeftAfterUpdate[sessionKey] == true
        if (!leftInventoryAfterUpdate) {
            Log.d(
                TAG_INV_GATE,
                "inventory enable blocked; still same Inventory visit sessionKey=$sessionKey"
            )
            return false
        }

        inventoryUpdateLockedUntilReentry.remove(sessionKey)
        inventoryTabLeftAfterUpdate.remove(sessionKey)

        Log.d(
            TAG_INV_GATE,
            "inventory update unlocked after reentry sessionKey=$sessionKey"
        )

        return true
    }

    /**
     * Returns true when Android recently saw a trustworthy Inventory signal.
     *
     * The one-shot lock wins over all passive signals. This prevents the button from
     * re-enabling right after a successful manual read.
     */
    private fun isInventoryTabRecentlyVisible(sessionKey: String): Boolean {
        if (isInventoryUpdateLocked(sessionKey)) {
            Log.d(
                TAG_INV_GATE,
                "availability false because inventory update is locked sessionKey=$sessionKey"
            )
            return false
        }

        val now = SystemClock.elapsedRealtime()
        val freshState = getFreshInventoryGateTabState(sessionKey)

        if (freshState != null && freshState.hasKnownActiveTab) {
            val available = freshState.isInventoryActive

            Log.d(
                TAG_INV_GATE,
                "availability from known tab_state sessionKey=$sessionKey " +
                        "available=$available activeTab=${freshState.activeTab}"
            )

            return available
        }

        if (freshState != null && freshState.inventoryVisible) {
            Log.d(
                TAG_INV_GATE,
                "availability from unknown tab_state visibility sessionKey=$sessionKey"
            )

            return true
        }

        val inventorySeenAtMs = lastInventoryPageSeenAtMs[sessionKey]
        if (inventorySeenAtMs != null) {
            val markerAgeMs = now - inventorySeenAtMs
            if (markerAgeMs in 0L..PCG_INVENTORY_TAB_SIGNAL_MAX_AGE_MS) {
                Log.d(
                    TAG_INV_GATE,
                    "availability from inventory marker sessionKey=$sessionKey " +
                            "markerAgeMs=$markerAgeMs"
                )

                return true
            }
        }

        val snapshot = lastInventorySnapshots[sessionKey]
        if (snapshot != null) {
            val snapshotAgeMs = now - snapshot.capturedAtMs
            if (snapshotAgeMs in 0L..PCG_INVENTORY_TAB_SIGNAL_MAX_AGE_MS) {
                Log.d(
                    TAG_INV_GATE,
                    "availability from cached snapshot sessionKey=$sessionKey " +
                            "snapshotAgeMs=$snapshotAgeMs"
                )

                return true
            }
        }

        Log.d(TAG_INV_GATE, "availability false sessionKey=$sessionKey")
        return false
    }

    /**
     * Returns a recent PCG tab state when available.
     *
     * A fresh tab-state message is stronger than cached Inventory snapshots because
     * PCG may keep inactive tab DOM mounted after the user changes page.
     */
    private fun getFreshInventoryGateTabState(sessionKey: String): CachedPcgTabState? {
        val state = lastPcgTabStates[sessionKey] ?: return null
        val ageMs = SystemClock.elapsedRealtime() - state.capturedAtMs

        return if (ageMs in 0L..PCG_INVENTORY_TAB_SIGNAL_MAX_AGE_MS) {
            state
        } else {
            null
        }
    }

    /**
     * Returns true when an Inventory snapshot can be used as a manual-capture
     * candidate under the current tab state.
     *
     * Only a known non-Inventory active tab is allowed to veto the snapshot. Unknown
     * tab-state is not enough to reject a valid Inventory payload.
     */
    private fun canUseInventorySnapshotUnderCurrentTabState(sessionKey: String): Boolean {
        val freshState = getFreshInventoryGateTabState(sessionKey)

        if (freshState == null) {
            Log.d(
                TAG_INV_GATE,
                "snapshot_gate allowed no_fresh_tab_state sessionKey=$sessionKey"
            )
            return true
        }

        if (!freshState.hasKnownActiveTab) {
            Log.d(
                TAG_INV_GATE,
                "snapshot_gate allowed unknown_tab_state sessionKey=$sessionKey " +
                        "activeTab=${freshState.activeTab} inventoryVisible=${freshState.inventoryVisible}"
            )
            return true
        }

        val allowed = freshState.isInventoryActive

        Log.d(
            TAG_INV_GATE,
            "snapshot_gate from known_tab_state sessionKey=$sessionKey " +
                    "allowed=$allowed activeTab=${freshState.activeTab}"
        )

        return allowed
    }

    /**
     * Records that the Inventory page was seen recently without overwriting tab-state.
     *
     * A valid Inventory snapshot is useful as a lightweight availability signal, but
     * it must not replace lastPcgTabStates. Fresh tab-state is the stronger source of
     * truth because PCG can keep inactive tab DOM mounted after navigation.
     */
    private fun markInventoryTabSeen(
        sessionKey: String,
        source: String
    ) {
        lastInventoryPageSeenAtMs[sessionKey] = SystemClock.elapsedRealtime()

        Log.d(
            TAG_INV_GATE,
            "inventory_seen source=$source sessionKey=$sessionKey"
        )

        notifyInventoryTabAvailabilityForSession(sessionKey)
    }

    /**
     * Checks that the payload came from the real PCG extension iframe, not from the
     * Twitch top page or the extension supervisor frame.
     */
    private fun isTrustedPcgExtensionFrame(frame: JSONObject?): Boolean {
        if (frame == null) return false

        val host = frame.optString("host", "")
            .trim()
            .lowercase()

        val isTop = frame.optBoolean("isTop", true)

        return !isTop &&
                host.endsWith(".ext-twitch.tv") &&
                host != "supervisor.ext-twitch.tv"
    }

    private fun getManualInventoryCapture(
        appContext: Context,
        sessionKey: String
    ): PcgManualSnapshotCapture<CachedInventorySnapshot> {
        return manualInventoryCaptures.getOrPut(sessionKey) {
            PcgManualSnapshotCapture(
                debugLabel = "inventory:$sessionKey",
                captureDurationMs = MANUAL_INVENTORY_CAPTURE_DURATION_MS,
                timeoutMs = MANUAL_INVENTORY_CAPTURE_TIMEOUT_MS,
                onStarted = {
                    Log.d(TAG_INV_CAPTURE, "capture_started sessionKey=$sessionKey")
                    notifyInventoryCaptureStarted(sessionKey)
                },
                onProgress = { progress ->
                    notifyInventoryCaptureProgress(
                        sessionKey = sessionKey,
                        progress = progress
                    )
                },
                onSnapshotReady = { snapshot ->
                    Log.d(
                        TAG_INV_CAPTURE,
                        "capture_snapshot_ready sessionKey=$sessionKey " +
                                "profileId=${snapshot.profileId} balls=${snapshot.balls.size}"
                    )

                    saveManualInventorySnapshot(
                        appContext = appContext,
                        sessionKey = sessionKey,
                        snapshot = snapshot,
                        source = "manual_capture_window"
                    )

                    /*
                     * After a successful manual read, make the Inventory button one-shot.
                     * Passive snapshots from the same Inventory visit must not immediately
                     * re-enable it.
                     */
                    lockInventoryUpdateUntilReentry(sessionKey)

                    notifyInventoryCaptureFinished(sessionKey)
                },
                onTimedOut = {
                    Log.d(TAG_INV_CAPTURE, "capture_timeout sessionKey=$sessionKey")

                    showToastOnMain(
                        appContext = appContext,
                        messageRes = R.string.pcg_inventory_timeout,
                        duration = Toast.LENGTH_LONG
                    )

                    notifyInventoryCaptureFinished(sessionKey)
                },
                onCancelled = {
                    Log.d(TAG_INV_CAPTURE, "capture_cancelled sessionKey=$sessionKey")
                    notifyInventoryCaptureFinished(sessionKey)
                }
            )
        }
    }

    private fun shouldSkipDuplicateManualPokedexUpload(
        sessionKey: String,
        snapshot: CachedPokedexSnapshot
    ): Boolean {
        val now = SystemClock.elapsedRealtime()

        val namesSignature = snapshot.wantedPokemon
            .joinToString(separator = "\u001F")
            .hashCode()

        val key = "$sessionKey|${snapshot.profileId}|${snapshot.wantedPokemon.size}|$namesSignature"

        val previousAtMs = recentManualPokedexUploadKeys[key]
        if (previousAtMs != null && now - previousAtMs <= MANUAL_POKEDEX_UPLOAD_DEDUP_MS) {
            Log.d(
                TAG_PCG_PROBE,
                "skip duplicate manual pokedex upload sessionKey=$sessionKey " +
                        "profileId=${snapshot.profileId} count=${snapshot.wantedPokemon.size}"
            )
            return true
        }

        recentManualPokedexUploadKeys[key] = now

        for ((oldKey, oldAtMs) in recentManualPokedexUploadKeys.entries) {
            if (now - oldAtMs > MANUAL_POKEDEX_UPLOAD_DEDUP_MS * 3) {
                recentManualPokedexUploadKeys.remove(oldKey, oldAtMs)
            }
        }

        return false
    }

    private fun invalidateOlderPokedexSnapshot(
        sessionKey: String,
        newerCapturedAtMs: Long
    ) {
        val oldSnapshot = lastPokedexSnapshots[sessionKey] ?: return

        if (oldSnapshot.capturedAtMs <= newerCapturedAtMs) {
            val removed = lastPokedexSnapshots.remove(sessionKey, oldSnapshot)

            if (removed) {
                Log.d(
                    TAG_PCG_PROBE,
                    "invalidated older pokedex snapshot after inventory extract success " +
                            "sessionKey=$sessionKey oldCapturedAtMs=${oldSnapshot.capturedAtMs} " +
                            "newerCapturedAtMs=$newerCapturedAtMs"
                )
            }
        }
    }

    private fun invalidateOlderInventorySnapshot(
        sessionKey: String,
        newerCapturedAtMs: Long
    ) {
        /*
         * A newer Pokédex read is also a strong signal that the user left Inventory.
         * This helps the one-shot Inventory button unlock when the user returns.
         */
        markInventoryTabLeftIfLocked(sessionKey)

        val oldSnapshot = lastInventorySnapshots[sessionKey] ?: return

        if (oldSnapshot.capturedAtMs <= newerCapturedAtMs) {
            val removed = lastInventorySnapshots.remove(sessionKey, oldSnapshot)

            if (removed) {
                Log.d(
                    TAG_PCG_PROBE,
                    "invalidated older inventory snapshot after pokedex extract success " +
                            "sessionKey=$sessionKey oldCapturedAtMs=${oldSnapshot.capturedAtMs} " +
                            "newerCapturedAtMs=$newerCapturedAtMs"
                )
            }
        }
    }

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
            TAG_PCG_PROBE,
            "cached $debugLabel snapshot check sessionKey=$sessionKey ageMs=$ageMs maxAgeMs=$maxAgeMs fresh=$fresh"
        )

        return fresh
    }

    private fun handlePcgTabState(
        appContext: Context,
        accountId: String,
        payload: JSONObject
    ) {
        val sessionKey = pcgSessionKey(accountId)

        val state = CachedPcgTabState(
            activeTab = payload.optString("activeTab", "unknown"),
            pokedexVisible = payload.optBoolean("pokedexVisible", false),
            inventoryVisible = payload.optBoolean("inventoryVisible", false),
            anyLoadedSurface = payload.optBoolean("anyLoadedSurface", false),
            capturedAtMs = SystemClock.elapsedRealtime()
        )

        lastPcgTabStates[sessionKey] = state

        invalidateCachedSnapshotsForHiddenTabs(
            sessionKey = sessionKey,
            state = state
        )

        Log.d(
            TAG_INV_GATE,
            "tab_state sessionKey=$sessionKey " +
                    "activeTab=${state.activeTab} " +
                    "known=${state.hasKnownActiveTab} " +
                    "inventoryActive=${state.isInventoryActive} " +
                    "inventoryVisible=${state.inventoryVisible} " +
                    "pokedexActive=${state.isPokedexActive} " +
                    "pokedexVisible=${state.pokedexVisible} " +
                    "anyLoadedSurface=${state.anyLoadedSurface}"
        )

        if (state.isInventoryActive) {
            if (canEnableInventoryForCurrentVisit(sessionKey)) {
                markInventoryTabSeen(
                    sessionKey = sessionKey,
                    source = "tab_state"
                )
            } else {
                notifyInventoryTabAvailabilityForSession(sessionKey)
            }
        } else if (state.hasKnownActiveTab) {
            /*
             * We clearly moved to another PCG tab.
             *
             * This does not unlock the Inventory button yet, but it allows the next
             * real Inventory tab visit to unlock it.
             */
            markInventoryTabLeftIfLocked(sessionKey)
            lastInventoryPageSeenAtMs.remove(sessionKey)
            lastInventorySnapshots.remove(sessionKey)
            notifyInventoryTabAvailabilityForSession(sessionKey)
        } else {
            /*
             * Unknown tab-state is ambiguous. Do not treat it as a real tab change.
             */
            notifyInventoryTabAvailabilityForSession(sessionKey)
        }

        completeOrRejectPendingManualUpdatesFromTabState(
            appContext = appContext,
            sessionKey = sessionKey,
            state = state
        )
    }

    /**
     * Invalidates cached snapshots only when the active tab is known.
     *
     * If activeTab is unknown, we avoid destroying valid snapshots based on ambiguous
     * DOM visibility.
     */
    private fun invalidateCachedSnapshotsForHiddenTabs(
        sessionKey: String,
        state: CachedPcgTabState
    ) {
        when (state.activeTab) {
            "inventory" -> {
                val removed = lastPokedexSnapshots.remove(sessionKey)
                if (removed != null) {
                    Log.d(
                        TAG_PCG_PROBE,
                        "invalidated cached pokedex snapshot because Inventory tab is active sessionKey=$sessionKey"
                    )
                }
            }

            "pokedex" -> {
                val removed = lastInventorySnapshots.remove(sessionKey)
                if (removed != null) {
                    Log.d(
                        TAG_PCG_PROBE,
                        "invalidated cached inventory snapshot because Pokédex tab is active sessionKey=$sessionKey"
                    )
                }
            }

            else -> {
                Log.d(
                    TAG_PCG_PROBE,
                    "skip snapshot invalidation because active tab is unknown sessionKey=$sessionKey"
                )
            }
        }
    }

    private fun completeOrRejectPendingManualUpdatesFromTabState(
        appContext: Context,
        sessionKey: String,
        state: CachedPcgTabState
    ) {
        if (state.activeTab == "unknown" && !state.anyLoadedSurface) {
            return
        }

        if (pendingManualPokedexRequests.containsKey(sessionKey)) {
            when {
                state.isPokedexActive -> {
                    val snapshot = lastPokedexSnapshots[sessionKey]

                    if (
                        snapshot != null &&
                        isCachedSnapshotFresh(
                            capturedAtMs = snapshot.capturedAtMs,
                            maxAgeMs = CACHED_POKEDEX_SNAPSHOT_MAX_AGE_MS,
                            debugLabel = "pokedex",
                            sessionKey = sessionKey
                        ) &&
                        consumeManualPokedexUpdateRequest(sessionKey)
                    ) {
                        uploadManualPokedexSnapshot(
                            appContext = appContext,
                            sessionKey = sessionKey,
                            snapshot = snapshot,
                            source = "cached_after_tab_confirmed"
                        )
                    }
                }

                state.activeTab == "inventory" -> {
                    val removedAtMs = pendingManualPokedexRequests.remove(sessionKey)
                    if (removedAtMs != null) {
                        Log.d(
                            TAG_PCG_PROBE,
                            "manual pokedex rejected because active tab is Inventory sessionKey=$sessionKey"
                        )

                        showToastOnMain(
                            appContext = appContext,
                            messageRes = R.string.pcg_pokedex_wrong_tab,
                            duration = Toast.LENGTH_LONG
                        )
                    }
                }

                else -> {
                    Log.d(
                        TAG_PCG_PROBE,
                        "manual pokedex still pending because active tab is unknown sessionKey=$sessionKey"
                    )
                }
            }
        }
    }

    /**
     * Consumes one pending manual Pokédex update request.
     *
     * Inventory no longer uses this old pending-request flow because it now has a
     * dedicated manual snapshot capture window with progress and timeout handling.
     */
    private fun consumeManualPokedexUpdateRequest(sessionKey: String): Boolean {
        val requestedAtMs = pendingManualPokedexRequests[sessionKey] ?: return false
        val ageMs = SystemClock.elapsedRealtime() - requestedAtMs

        if (ageMs > MANUAL_PCG_UPDATE_TIMEOUT_MS) {
            pendingManualPokedexRequests.remove(sessionKey, requestedAtMs)

            Log.d(
                TAG_PCG_PROBE,
                "ignore expired manual pokedex snapshot sessionKey=$sessionKey ageMs=$ageMs"
            )

            return false
        }

        pendingManualPokedexRequests.remove(sessionKey, requestedAtMs)

        Log.d(
            TAG_PCG_PROBE,
            "manual pokedex snapshot accepted sessionKey=$sessionKey ageMs=$ageMs"
        )

        return true
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
        return wantedPokemon.isEmpty() || rawArrayLockedCount(rawNames) > 0
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
            val sessionKey = pcgSessionKey(accountId)
            lastPokedexSnapshots.remove(sessionKey)

            Log.w(
                TAG_PCG_PROBE,
                "pokedex extract failed; cached pokedex snapshot invalidated " +
                        "sessionKey=$sessionKey reason=${payload.optString("reason")} payload=$payload"
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
            TAG_PCG_PROBE,
            "extract success profileId=$profileId profileLabel=$profileLabel wantedCount=${wantedPokemon.size} payloadCount=$payloadCount lockedCount=$lockedCount"
        )

        if (shouldRejectAsClearlyBrokenSnapshot(rawNames, wantedPokemon)) {
            Log.w(
                TAG_PCG_PROBE,
                "reject suspicious snapshot profileId=$profileId wantedCount=${wantedPokemon.size} payloadCount=$payloadCount lockedCount=$lockedCount"
            )
            return
        }

        val sessionKey = pcgSessionKey(accountId)

        /*
         * A valid Pokédex snapshot is a strong signal that the user is no longer on
         * Inventory. If the Inventory update button was locked after a successful read,
         * this marks the "left Inventory" half of the re-entry handshake.
         */
        markInventoryTabLeftIfLocked(sessionKey)

        val snapshot = CachedPokedexSnapshot(
            profileId = profileId,
            profileLabel = profileLabel,
            wantedPokemon = wantedPokemon,
            capturedAtMs = SystemClock.elapsedRealtime()
        )

        /*
         * A successful Pokédex read claims the current PCG surface for Pokédex.
         * Any older Inventory snapshot should no longer be reused.
         */
        invalidateOlderInventorySnapshot(
            sessionKey = sessionKey,
            newerCapturedAtMs = snapshot.capturedAtMs
        )

        lastPokedexSnapshots[sessionKey] = snapshot

        if (!consumeManualPokedexUpdateRequest(sessionKey)) {
            Log.d(
                TAG_PCG_PROBE,
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
            val sessionKey = pcgSessionKey(accountId)
            lastInventorySnapshots.remove(sessionKey)

            val reason = payload.optString("reason")
            if (reason == "ball_grid_not_found") {
                /*
                 * If the inventory probe fails because the grid is missing, it's a
                 * strong signal that the user left the Inventory tab.
                 */
                markInventoryTabLeftIfLocked(sessionKey)
                notifyInventoryTabAvailabilityForSession(sessionKey)
            }

            Log.w(
                TAG_PCG_PROBE,
                "inventory extract failed; cached inventory snapshot invalidated " +
                        "sessionKey=$sessionKey profileId=$profileId profileLabel=$profileLabel " +
                        "reason=$reason payload=$payload"
            )
            return
        }

        val balls = jsonArrayToInventoryBallList(payload.optJSONArray("balls"))
        if (balls.isEmpty()) {
            Log.w(
                TAG_PCG_PROBE,
                "inventory extract empty profileId=$profileId profileLabel=$profileLabel payload=$payload"
            )
            return
        }

        val frame = payload.optJSONObject("frame")
        if (!isTrustedPcgExtensionFrame(frame)) {
            Log.w(
                TAG_PCG_PROBE,
                "inventory extract rejected because frame is not trusted " +
                        "profileId=$profileId profileLabel=$profileLabel frame=$frame"
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

        /*
         * A successful Inventory read claims the current PCG surface for Inventory.
         * Any older Pokédex snapshot should no longer be reused.
         */
        invalidateOlderPokedexSnapshot(
            sessionKey = sessionKey,
            newerCapturedAtMs = snapshot.capturedAtMs
        )

        if (!canUseInventorySnapshotUnderCurrentTabState(sessionKey)) {
            /*
             * We received an Inventory-shaped snapshot, but the latest known tab-state
             * says the active PCG page is not Inventory. Treat it as hidden/stale DOM.
             */
            lastInventorySnapshots.remove(sessionKey)
            lastInventoryPageSeenAtMs.remove(sessionKey)
            notifyInventoryTabAvailabilityForSession(sessionKey)

            Log.d(
                TAG_INV_GATE,
                "inventory snapshot ignored because fresh tab-state says Inventory is not active " +
                        "sessionKey=$sessionKey profileId=$profileId count=${balls.size}"
            )
            return
        }

        lastInventorySnapshots[sessionKey] = snapshot

        if (isInventoryUpdateLocked(sessionKey)) {
            if (!canEnableInventoryForCurrentVisit(sessionKey)) {
                Log.d(
                    TAG_INV_GATE,
                    "inventory snapshot received but button stays locked until reentry " +
                            "sessionKey=$sessionKey profileId=$profileId count=${balls.size}"
                )

                notifyInventoryTabAvailabilityForSession(sessionKey)
                return
            }
        }

        markInventoryTabSeen(
            sessionKey = sessionKey,
            source = "inventory_snapshot"
        )

        val acceptedByManualCapture =
            manualInventoryCaptures[sessionKey]?.submitCandidate(snapshot) == true

        if (!acceptedByManualCapture) {
            Log.d(
                TAG_PCG_PROBE,
                "inventory snapshot cached but not saved because no manual capture is active " +
                        "sessionKey=$sessionKey profileId=$profileId count=${balls.size}"
            )
            return
        }

        Log.d(
            TAG_INV_CAPTURE,
            "inventory snapshot accepted by manual capture; waiting for progress completion " +
                    "sessionKey=$sessionKey profileId=$profileId count=${balls.size}"
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
                        Log.e(TAG_PCG_PROBE, "Extension install returned null")
                        return@accept
                    }

                    Log.d(TAG_PCG_PROBE, "Extension ready: $extension")

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
                                                TAG_PCG_PROBE,
                                                "onMessage: unable to parse message=$message"
                                            )
                                            return null
                                        }

                                        Log.d(TAG_PCG_PROBE, "nativeApp=$nativeApp message=$json")

                                        if (nativeApp != PCG_NATIVE_APP) {
                                            Log.d(
                                                TAG_PCG_PROBE,
                                                "ignored: nativeApp mismatch ($nativeApp)"
                                            )
                                            return null
                                        }

                                        if (sender.environmentType != WebExtension.MessageSender.ENV_TYPE_CONTENT_SCRIPT) {
                                            Log.d(
                                                TAG_PCG_PROBE,
                                                "ignored: sender is not content script"
                                            )
                                            return null
                                        }

                                        val type = json.optString("type")
                                        val payload = json.optJSONObject("payload") ?: JSONObject()

                                        when (type) {
                                            "pcg_probe_boot_debug" -> {
                                                Log.d(
                                                    TAG_PCG_PROBE,
                                                    "boot debug payload=$payload"
                                                )
                                            }

                                            "pcg_content_absolute_boot" -> {
                                                Log.d(
                                                    TAG_PCG_PROBE,
                                                    "content absolute boot payload=$payload"
                                                )
                                            }

                                            "pcg_inventory_absolute_boot" -> {
                                                Log.d(
                                                    TAG_PCG_PROBE,
                                                    "inventory absolute boot payload=$payload"
                                                )
                                            }

                                            "pcg_probe_boot" -> {
                                                Log.d(
                                                    TAG_PCG_PROBE,
                                                    "probe boot from frame=${payload.optJSONObject("frame")}"
                                                )
                                            }

                                            "pcg_probe_found_pokedex" -> {
                                                Log.d(
                                                    TAG_PCG_PROBE,
                                                    "pokedex found, filters=${payload.optJSONArray("filterTexts")}"
                                                )
                                            }

                                            "pcg_probe_filters_applied" -> {
                                                Log.d(
                                                    TAG_PCG_PROBE,
                                                    "filters applied spawnable=${payload.optBoolean("spawnableState")} obtained=${payload.optBoolean("obtainedState")}"
                                                )
                                            }

                                            "pcg_probe_progress" -> {
                                                Log.d(
                                                    TAG_PCG_PROBE,
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

                                            TYPE_PCG_TAB_STATE -> {
                                                handlePcgTabState(
                                                    appContext = appContext,
                                                    accountId = accountId,
                                                    payload = payload
                                                )
                                            }

                                            TYPE_PCG_INVENTORY_WRONG_TAB -> {
                                                val sessionKey = pcgSessionKey(accountId)
                                                markInventoryTabLeftIfLocked(sessionKey)
                                                notifyInventoryTabAvailabilityForSession(sessionKey)

                                                Log.d(
                                                    TAG_INV_GATE,
                                                    "JS inventory wrong-tab; mark left sessionKey=$sessionKey"
                                                )
                                            }

                                            TYPE_PCG_POKEDEX_WRONG_TAB -> {
                                                Log.d(
                                                    TAG_PCG_PROBE,
                                                    "ignored JS pokedex wrong-tab payload=$payload"
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
                                                Log.d(TAG_PCG_PROBE, "ignored type=$type")
                                            }
                                        }
                                    } catch (t: Throwable) {
                                        Log.e(TAG_PCG_PROBE, "onMessage error", t)
                                    }

                                    return null
                                }
                            },
                            PCG_NATIVE_APP
                        )
                    }
                },
                { error ->
                    Log.e(TAG_PCG_PROBE, "Extension install error", error)
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
                TAG_PCG_PROBE,
                "manual inventory update ignored, PCG session not ready sessionKey=$sessionKey"
            )
            return false
        }

        val appContext = context.applicationContext

        if (!isInventoryTabRecentlyVisible(sessionKey)) {
            Log.d(
                TAG_INV_GATE,
                "manual inventory update rejected because Inventory tab is not available " +
                        "sessionKey=$sessionKey"
            )

            showToastOnMain(
                appContext = appContext,
                messageRes = R.string.pcg_inventory_open_tab_first,
                duration = Toast.LENGTH_LONG
            )

            notifyInventoryTabAvailabilityForSession(sessionKey)
            return false
        }

        getManualInventoryCapture(
            appContext = appContext,
            sessionKey = sessionKey
        ).begin()

        Log.d(
            TAG_INV_CAPTURE,
            "manual inventory capture armed; waiting for passive snapshot " +
                    "sessionKey=$sessionKey durationMs=$MANUAL_INVENTORY_CAPTURE_DURATION_MS " +
                    "timeoutMs=$MANUAL_INVENTORY_CAPTURE_TIMEOUT_MS"
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
                TAG_PCG_PROBE,
                "manual pokedex registration ignored, PCG session not ready sessionKey=$sessionKey"
            )
            return false
        }

        val appContext = context.applicationContext
        val requestedAtMs = SystemClock.elapsedRealtime()

        val cachedSnapshot = lastPokedexSnapshots[sessionKey]
        if (
            cachedSnapshot != null &&
            isCachedSnapshotFresh(
                capturedAtMs = cachedSnapshot.capturedAtMs,
                maxAgeMs = CACHED_POKEDEX_SNAPSHOT_MAX_AGE_MS,
                debugLabel = "pokedex_reusable",
                sessionKey = sessionKey
            )
        ) {
            Log.d(
                TAG_PCG_PROBE,
                "manual pokedex registration completed from reusable snapshot sessionKey=$sessionKey"
            )

            uploadManualPokedexSnapshot(
                appContext = appContext,
                sessionKey = sessionKey,
                snapshot = cachedSnapshot,
                source = "reusable_cached_snapshot"
            )

            return true
        }

        pendingManualPokedexRequests[sessionKey] = requestedAtMs

        Log.d(
            TAG_PCG_PROBE,
            "manual pokedex registration armed; waiting for next valid pokedex snapshot " +
                    "sessionKey=$sessionKey requestedAtMs=$requestedAtMs " +
                    "timeoutMs=$MANUAL_PCG_UPDATE_TIMEOUT_MS"
        )

        scheduleManualUpdateTimeout(
            appContext = appContext,
            sessionKey = sessionKey,
            requestedAtMs = requestedAtMs,
            pendingRequests = pendingManualPokedexRequests,
            timeoutMessageRes = R.string.pcg_pokedex_wrong_tab,
            debugLabel = "pokedex"
        )

        return true
    }

    /**
     * Completes a manual PCG update request with a tab-specific timeout message.
     *
     * Inventory no longer uses this method; it is kept for the Pokédex flow.
     */
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
                    TAG_PCG_PROBE,
                    "manual $debugLabel registration timed out sessionKey=$sessionKey timeoutMs=$MANUAL_PCG_UPDATE_TIMEOUT_MS"
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
                messageRes = R.string.pcg_inventory_updated,
                duration = Toast.LENGTH_SHORT
            )
        }

        Log.d(
            TAG_PCG_PROBE,
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
        if (shouldSkipDuplicateManualPokedexUpload(sessionKey, snapshot)) {
            return
        }

        Log.d(
            TAG_PCG_PROBE,
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
                Log.d(TAG_PCG_PROBE, "FCM token registration started for profileId=${snapshot.profileId}")
            } else {
                Log.w(TAG_PCG_PROBE, "No cached FCM token available for profileId=${snapshot.profileId}")
            }
        } else {
            Log.d(TAG_PCG_PROBE, "Push muted for profileId=${snapshot.profileId}, skip FCM registration")
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
                    Log.w(TAG_PCG_PROBE, "release previous GeckoView session failed", t)
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
                Log.w(TAG_PCG_PROBE, "release PCG GeckoView session failed", t)
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
            Log.d(TAG_PCG_PROBE, "skip PCG reload, already loaded url=$url")
            return
        }

        loadedPcgUrls[sessionKey] = url
        session.loadUri(url)

        Log.d(TAG_PCG_PROBE, "load PCG url=$url")
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
                Log.w(TAG_PCG_PROBE, "idle PCG session close failed accountId=$accountId", t)
            }

            Log.d(TAG_PCG_PROBE, "idle PCG session closed accountId=$accountId")
        }

        pendingPcgDestroyRunnables[sessionKey] = runnable
        mainHandler.postDelayed(runnable, PCG_SESSION_KEEP_ALIVE_MS)

        Log.d(TAG_PCG_PROBE, "scheduled PCG session idle close accountId=$accountId")
    }

    private fun cancelScheduledPcgDestroy(accountId: String) {
        val sessionKey = pcgSessionKey(accountId)

        val oldRunnable = pendingPcgDestroyRunnables.remove(sessionKey)
        if (oldRunnable != null) {
            mainHandler.removeCallbacks(oldRunnable)
            Log.d(TAG_PCG_PROBE, "cancelled PCG session idle close accountId=$accountId")
        }
    }
}