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

    private const val TYPE_PCG_TAB_STATE = "pcg_tab_state"

    private val lastPcgPokedexStates =
        ConcurrentHashMap<String, PcgPokedexState>()

    /**
     * How long a manual PCG update request waits for a fresh probe snapshot before
     * showing the tab-specific instruction toast.
     *
     * This must be long enough for the Pokédex probe, because it waits for the DOM
     * to settle and may scroll the list before sending a valid snapshot.
     */
    private const val MANUAL_PCG_UPDATE_TIMEOUT_MS = 6_000L

    /**
     * Maximum age for PCG tab visibility state reported by the content script.
     *
     * Cached Inventory/Pokédex snapshots are accepted only when this recent tab
     * state confirms that the user is currently looking at the matching tab.
     */
    private const val PCG_TAB_STATE_MAX_AGE_MS = 1_000L

    /**
     * Maximum age for the passive Pokédex filter/tab state reported by the content script.
     *
     * This state is used to decide whether a manual Pokédex update can proceed or
     * should show a filter/tab hint to the user.
     */
    private const val PCG_POKEDEX_STATE_MAX_AGE_MS = 3_000L

    /**
     * How long a successful passive Pokédex snapshot can be reused for a manual
     * Register Pokédex press.
     *
     * The snapshot is still invalidated earlier if the Pokédex probe later reports
     * that the Pokédex DOM is no longer readable.
     */
    private const val CACHED_POKEDEX_SNAPSHOT_MAX_AGE_MS = 90_000L

    /**
     * How long a successful passive Inventory snapshot can be reused for a manual
     * Register inventory press.
     *
     * The snapshot is still invalidated earlier if the Inventory probe later reports
     * that the ball grid is no longer readable.
     */
    private const val CACHED_INVENTORY_SNAPSHOT_MAX_AGE_MS = 30_000L


    /**
     * Maximum age for a passive DOM snapshot that can be accepted immediately
     * after the user presses one of the manual PCG registration buttons.
     *
     * This value should stay short because passive snapshots are continuously
     * refreshed while the correct PCG tab is active. We do not want to preserve
     * old Pokédex/Inventory data for minutes; we only want the latest fresh
     * candidate from the currently visible PCG surface.
     */
    private const val RECENT_PASSIVE_SNAPSHOT_MAX_AGE_MS = 5_000L

    private const val PCG_SESSION_KEEP_ALIVE_MS = 15*60*1_000L

    /**
     * Tiny grace window for accepting a passive snapshot that was captured just
     * before the user pressed a manual update button.
     *
     * This avoids ignoring a valid probe read that happened milliseconds before the
     * tap, while still preventing old tab snapshots from being reused later.
     */
    private const val JUST_CAPTURED_SNAPSHOT_MAX_AGE_MS = 1_500L

    /**
     * Minimum delay before showing a manual PCG update result toast.
     *
     * This keeps the UI feeling consistent: after the user taps Register Pokédex or
     * Register Inventory, the button/progress feedback has time to communicate that
     * a check is in progress before a toast appears.
     */
    private const val MANUAL_PCG_RESULT_TOAST_MIN_DELAY_MS = 4_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val pendingManualInventoryRequests =
        ConcurrentHashMap<String, Long>()

    private val pendingManualPokedexRequests =
        ConcurrentHashMap<String, Long>()

    private val lastInventorySnapshots =
        ConcurrentHashMap<String, CachedInventorySnapshot>()

    private val lastPokedexSnapshots =
        ConcurrentHashMap<String, CachedPokedexSnapshot>()

    /**
     * Prevents duplicate manual Pokédex uploads/toasts caused by multiple valid
     * probe snapshots arriving very close together.
     */
    private const val MANUAL_POKEDEX_UPLOAD_DEDUP_MS = 1_500L

    private val recentManualPokedexUploadKeys =
        ConcurrentHashMap<String, Long>()

    private val lastPcgTabStates =
        ConcurrentHashMap<String, CachedPcgTabState>()

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

    /**
     * Last known PCG tab state reported by the WebExtension.
     *
     * activeTab is preferred over raw DOM visibility because PCG can keep inactive
     * tab content mounted in the DOM. Visibility remains as a fallback/debug signal.
     */
    private data class CachedPcgTabState(
        val activeTab: String,
        val pokedexVisible: Boolean,
        val inventoryVisible: Boolean,
        val anyLoadedSurface: Boolean,
        val capturedAtMs: Long
    ) {
        val hasKnownActiveTab: Boolean
            get() = activeTab.isNotBlank() && activeTab != "unknown"

        val isInventoryActive: Boolean
            get() {
                /*
                 * Prefer activeTab when PCG reports it clearly.
                 *
                 * Inventory DOM can remain mounted while another tab is selected, so
                 * inventoryVisible is only a fallback when activeTab is unknown.
                 */
                return if (hasKnownActiveTab) {
                    activeTab == "inventory"
                } else {
                    inventoryVisible
                }
            }

        val isPokedexActive: Boolean
            get() {
                /*
                 * Prefer activeTab when PCG reports it clearly.
                 *
                 * Pokédex DOM can remain mounted while another tab is selected, so
                 * pokedexVisible is only a fallback when activeTab is unknown.
                 */
                return if (hasKnownActiveTab) {
                    activeTab == "pokedex"
                } else {
                    pokedexVisible
                }
            }

        val shouldInvalidateInventorySnapshot: Boolean
            get() = hasKnownActiveTab && activeTab != "inventory"

        val shouldInvalidatePokedexSnapshot: Boolean
            get() = hasKnownActiveTab && activeTab != "pokedex"
    }

    /**
     * Last known passive Pokédex state reported by the WebExtension.
     *
     * The content script does not change PCG filters any more. This state only tells
     * Android whether the user is currently on the Pokédex with the Spawnable-only
     * filter state required for a valid missing-Dex snapshot.
     */
    private data class PcgPokedexState(
        val onPokedexTab: Boolean,
        val spawnable: Boolean,
        val obtained: Boolean,
        val activeNonSpawnableFilters: List<String>,
        val validForMissingDexUpload: Boolean,
        val reason: String?,
        val capturedAtMs: Long
    ) {
        val needsSpawnableOnlyHint: Boolean
            get() {
                return onPokedexTab &&
                        !validForMissingDexUpload &&
                        (
                                reason == "spawnable_filter_off" ||
                                        reason == "spawnable_only_required" ||
                                        activeNonSpawnableFilters.isNotEmpty()
                                )
            }

        companion object {
            /**
             * Parses the passive Pokédex state emitted by the content script.
             */
            fun fromPayload(
                payload: JSONObject,
                capturedAtMs: Long
            ): PcgPokedexState {
                val filters = payload.optJSONObject("filters")
                val activeFiltersJson = filters?.optJSONArray("activeNonSpawnableFilters")

                val activeFilters = ArrayList<String>()
                if (activeFiltersJson != null) {
                    for (i in 0 until activeFiltersJson.length()) {
                        val value = activeFiltersJson.optString(i).trim()
                        if (value.isNotEmpty()) {
                            activeFilters.add(value)
                        }
                    }
                }

                val rawReason = payload.optString("reason", "").trim()
                val reason = rawReason.takeIf { it.isNotBlank() && it != "null" }

                return PcgPokedexState(
                    onPokedexTab = payload.optBoolean("onPokedexTab", false),
                    spawnable = filters?.optBoolean("spawnable", false) == true,
                    obtained = filters?.optBoolean("obtained", false) == true,
                    activeNonSpawnableFilters = activeFilters,
                    validForMissingDexUpload = payload.optBoolean("validForMissingDexUpload", false),
                    reason = reason,
                    capturedAtMs = capturedAtMs
                )
            }
        }
    }

    /**
     * Returns true when the same manual Pokédex payload was already uploaded very
     * recently.
     *
     * The PCG probe can emit multiple valid snapshots during DOM mutations. Without
     * this guard, FcmRegistrationUploader.uploadDexList(...) can run twice and show
     * the same "Dex list synced..." toast twice.
     */
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
                "PCG_PROBE",
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

    /**
     * Invalidates an older Pokédex snapshot when Inventory has been read after it.
     *
     * A successful Inventory read is the strongest practical signal that the current
     * PCG surface is no longer the Pokédex surface that produced the older snapshot.
     */
    private fun invalidateOlderPokedexSnapshot(
        sessionKey: String,
        newerCapturedAtMs: Long
    ) {
        val oldSnapshot = lastPokedexSnapshots[sessionKey] ?: return

        if (oldSnapshot.capturedAtMs <= newerCapturedAtMs) {
            val removed = lastPokedexSnapshots.remove(sessionKey, oldSnapshot)

            if (removed) {
                Log.d(
                    "PCG_PROBE",
                    "invalidated older pokedex snapshot after inventory extract success " +
                            "sessionKey=$sessionKey oldCapturedAtMs=${oldSnapshot.capturedAtMs} " +
                            "newerCapturedAtMs=$newerCapturedAtMs"
                )
            }
        }
    }

    /**
     * Invalidates an older Inventory snapshot when Pokédex has been read after it.
     *
     * This mirrors the Inventory-to-Pokédex invalidation, so both manual update
     * buttons use the same cache ownership rule.
     */
    private fun invalidateOlderInventorySnapshot(
        sessionKey: String,
        newerCapturedAtMs: Long
    ) {
        val oldSnapshot = lastInventorySnapshots[sessionKey] ?: return

        if (oldSnapshot.capturedAtMs <= newerCapturedAtMs) {
            val removed = lastInventorySnapshots.remove(sessionKey, oldSnapshot)

            if (removed) {
                Log.d(
                    "PCG_PROBE",
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

    /**
     * Shows a short app toast on the main thread.
     *
     * Use this only for immediate lightweight confirmations. Manual PCG update
     * result messages should use showManualUpdateResultToastOnMain(...) instead,
     * because those toasts are intentionally delayed to match the checking UI.
     */
    private fun showToastOnMain(
        appContext: Context,
        @StringRes messageRes: Int
    ) {
        mainHandler.post {
            Toast.makeText(
                appContext,
                appContext.getString(messageRes),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Shows a manual update result toast no earlier than a fixed delay from the
     * original button press.
     *
     * Manual PCG update messages are always long to give the user enough time to
     * read tab/filter instructions after the checking UI finishes.
     */
    private fun showManualUpdateResultToastOnMain(
        appContext: Context,
        requestedAtMs: Long,
        @StringRes messageRes: Int,
        debugLabel: String
    ) {
        val elapsedMs = SystemClock.elapsedRealtime() - requestedAtMs
        val delayMs = (MANUAL_PCG_RESULT_TOAST_MIN_DELAY_MS - elapsedMs).coerceAtLeast(0L)

        Log.d(
            "PCG_PROBE",
            "schedule manual $debugLabel result toast messageRes=$messageRes " +
                    "elapsedMs=$elapsedMs delayMs=$delayMs"
        )

        mainHandler.postDelayed(
            {
                Toast.makeText(
                    appContext,
                    appContext.getString(messageRes),
                    Toast.LENGTH_LONG
                ).show()
            },
            delayMs
        )
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

    /**
     * Checks whether a passive PCG DOM snapshot is recent enough to be promoted to
     * a user-confirmed manual registration.
     *
     * Passive snapshots are only candidates kept in memory. They become real app
     * data only when the user presses one of the manual Register buttons.
     */
    private fun isRecentPassiveSnapshot(
        capturedAtMs: Long,
        sessionKey: String
    ): Boolean {
        return isCachedSnapshotFresh(
            capturedAtMs = capturedAtMs,
            maxAgeMs = RECENT_PASSIVE_SNAPSHOT_MAX_AGE_MS,
            debugLabel = "recent_passive_snapshot",
            sessionKey = sessionKey
        )
    }

    private fun getFreshPcgTabState(
        sessionKey: String,
    ): CachedPcgTabState? {
        val state = lastPcgTabStates[sessionKey] ?: return null
        val ageMs = SystemClock.elapsedRealtime() - state.capturedAtMs
        val fresh = ageMs in 0L..PCG_TAB_STATE_MAX_AGE_MS

        Log.d(
            "PCG_PROBE",
            "tab state check sessionKey=$sessionKey " +
                    "ageMs=$ageMs fresh=$fresh pokedexVisible=${state.pokedexVisible} " +
                    "inventoryVisible=${state.inventoryVisible} anyLoadedSurface=${state.anyLoadedSurface}"
        )

        return if (fresh) state else null
    }

    /**
     * Returns the latest fresh passive Pokédex state for the current PCG session.
     */
    private fun getFreshPcgPokedexState(
        sessionKey: String,
    ): PcgPokedexState? {
        val state = lastPcgPokedexStates[sessionKey] ?: return null
        val ageMs = SystemClock.elapsedRealtime() - state.capturedAtMs
        val fresh = ageMs in 0L..PCG_POKEDEX_STATE_MAX_AGE_MS

        Log.d(
            "PCG_PROBE",
            "pokedex state check sessionKey=$sessionKey " +
                    "ageMs=$ageMs fresh=$fresh onPokedexTab=${state.onPokedexTab} " +
                    "spawnable=${state.spawnable} obtained=${state.obtained} " +
                    "otherFilters=${state.activeNonSpawnableFilters} " +
                    "valid=${state.validForMissingDexUpload} reason=${state.reason}"
        )

        return if (fresh) state else null
    }

    /**
     * Stores the latest passive Pokédex state and completes/rejects pending manual
     * Pokédex updates when the state is clear enough.
     */
    private fun handlePcgPokedexState(
        appContext: Context,
        accountId: String,
        payload: JSONObject
    ) {
        val sessionKey = pcgSessionKey(accountId)
        val state = PcgPokedexState.fromPayload(
            payload = payload,
            capturedAtMs = SystemClock.elapsedRealtime()
        )

        lastPcgPokedexStates[sessionKey] = state

        if (state.onPokedexTab) {
            val removedInventorySnapshot = lastInventorySnapshots.remove(sessionKey)
            if (removedInventorySnapshot != null) {
                Log.d(
                    "PCG_PROBE",
                    "invalidated cached inventory snapshot because passive Pokédex state is active " +
                            "sessionKey=$sessionKey reason=${state.reason}"
                )
            }
        }

        if (!state.validForMissingDexUpload) {
            val removedSnapshot = lastPokedexSnapshots.remove(sessionKey)
            if (removedSnapshot != null) {
                Log.d(
                    "PCG_PROBE",
                    "invalidated cached pokedex snapshot because passive state is not uploadable " +
                            "sessionKey=$sessionKey reason=${state.reason} " +
                            "onPokedexTab=${state.onPokedexTab} spawnable=${state.spawnable} " +
                            "otherFilters=${state.activeNonSpawnableFilters}"
                )
            }
        }

        Log.d(
            "PCG_PROBE",
            "pokedex state updated sessionKey=$sessionKey " +
                    "valid=${state.validForMissingDexUpload} " +
                    "onPokedexTab=${state.onPokedexTab} " +
                    "spawnable=${state.spawnable} " +
                    "obtained=${state.obtained} " +
                    "otherFilters=${state.activeNonSpawnableFilters} " +
                    "reason=${state.reason}"
        )

        completeOrRejectPendingManualPokedexUpdateFromState(
            appContext = appContext,
            sessionKey = sessionKey,
            state = state
        )
    }

    /**
     * Handles a pending manual Register Pokédex request using the passive filter state.
     *
     * If the user is on the wrong tab, Android shows the existing wrong-tab message.
     * If the user is on Pokédex but filters are not Spawnable-only, Android shows a
     * filter-specific hint. If the state is valid, Android waits for a fresh snapshot
     * or consumes an already cached one.
     */
    private fun completeOrRejectPendingManualPokedexUpdateFromState(
        appContext: Context,
        sessionKey: String,
        state: PcgPokedexState
    ) {
        if (!pendingManualPokedexRequests.containsKey(sessionKey)) {
            return
        }

        when {
            !state.onPokedexTab -> {
                val removedAtMs = pendingManualPokedexRequests.remove(sessionKey)
                if (removedAtMs != null) {
                    Log.d(
                        "PCG_PROBE",
                        "manual pokedex rejected because passive state says tab is not Pokédex " +
                                "sessionKey=$sessionKey reason=${state.reason}"
                    )

                    showManualUpdateResultToastOnMain(
                        appContext = appContext,
                        requestedAtMs = removedAtMs,
                        messageRes = R.string.pcg_pokedex_wrong_tab,
                        debugLabel = "pokedex_wrong_tab_from_state"
                    )
                }
            }

            state.needsSpawnableOnlyHint -> {
                val removedAtMs = pendingManualPokedexRequests.remove(sessionKey)
                if (removedAtMs != null) {
                    Log.d(
                        "PCG_PROBE",
                        "manual pokedex rejected because Spawnable-only filter is required " +
                                "sessionKey=$sessionKey reason=${state.reason} " +
                                "otherFilters=${state.activeNonSpawnableFilters}"
                    )

                    showManualUpdateResultToastOnMain(
                        appContext = appContext,
                        requestedAtMs = removedAtMs,
                        messageRes = R.string.pcg_pokedex_spawnable_only_required,
                        debugLabel = "pokedex_spawnable_only_required_from_state"
                    )
                }
            }

            state.validForMissingDexUpload -> {
                val snapshot = lastPokedexSnapshots[sessionKey]

                if (
                    snapshot != null &&
                    isCachedSnapshotFresh(
                        capturedAtMs = snapshot.capturedAtMs,
                        maxAgeMs = CACHED_POKEDEX_SNAPSHOT_MAX_AGE_MS,
                        debugLabel = "pokedex",
                        sessionKey = sessionKey
                    ) &&
                    consumeManualUpdateRequest(
                        sessionKey = sessionKey,
                        pendingRequests = pendingManualPokedexRequests,
                        debugLabel = "pokedex"
                    )
                ) {
                    uploadManualPokedexSnapshot(
                        appContext = appContext,
                        sessionKey = sessionKey,
                        snapshot = snapshot,
                        source = "cached_after_pokedex_state_confirmed"
                    )
                } else {
                    Log.d(
                        "PCG_PROBE",
                        "manual pokedex still pending; valid state received but no fresh snapshot yet " +
                                "sessionKey=$sessionKey"
                    )
                }
            }

            else -> {
                Log.d(
                    "PCG_PROBE",
                    "manual pokedex still pending; passive state not decisive yet " +
                            "sessionKey=$sessionKey reason=${state.reason}"
                )
            }
        }
    }

    /**
     * Verifies that a successful Pokédex payload was captured with the Spawnable-only
     * filter state required for missing-Dex upload.
     */
    private fun isPokedexPayloadSpawnableOnly(payload: JSONObject): Boolean {
        if (!payload.optBoolean("validForMissingDexUpload", false)) {
            return false
        }

        val filters = payload.optJSONObject("filters") ?: return false
        val spawnable = filters.optBoolean("spawnable", false)
        val obtained = filters.optBoolean("obtained", true)
        val activeNonSpawnableFilters = filters.optJSONArray("activeNonSpawnableFilters")

        val hasOtherFilters =
            activeNonSpawnableFilters != null && activeNonSpawnableFilters.length() > 0

        return spawnable && !obtained && !hasOtherFilters
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
            "PCG_PROBE",
            "tab state updated sessionKey=$sessionKey " +
                    "activeTab=${state.activeTab} " +
                    "pokedexVisible=${state.pokedexVisible} " +
                    "inventoryVisible=${state.inventoryVisible} " +
                    "anyLoadedSurface=${state.anyLoadedSurface}"
        )

        completeOrRejectPendingManualUpdatesFromTabState(
            appContext = appContext,
            sessionKey = sessionKey,
            state = state
        )
    }

    //**
    /* Invalidates cached passive snapshots when the active PCG surface changes.
    *
    * A snapshot belongs to the tab that produced it. If the user moves to another
    * known tab, that snapshot must not be reused for a later manual update.
    *
    * Passive snapshots are only RAM candidates. They become real app data only
    * when the user presses one of the manual Register buttons.
    */
    private fun invalidateCachedSnapshotsForHiddenTabs(
        sessionKey: String,
        state: CachedPcgTabState
    ) {
        if (!state.anyLoadedSurface) {
            /*
             * No usable PCG surface is currently loaded or visible. Both candidates
             * are stale by definition and must not survive this state.
             */
            val removedInventory = lastInventorySnapshots.remove(sessionKey)
            val removedPokedex = lastPokedexSnapshots.remove(sessionKey)

            if (removedInventory != null || removedPokedex != null) {
                Log.d(
                    "PCG_PROBE",
                    "invalidated all cached PCG snapshots because no PCG surface is loaded " +
                            "sessionKey=$sessionKey activeTab=${state.activeTab}"
                )
            }

            return
        }

        if (!state.hasKnownActiveTab) {
            /*
             * PCG is loaded, but the active tab cannot be identified yet. Keep the
             * current candidates instead of deleting them aggressively.
             *
             * They are still protected by the short freshness check before any
             * manual Register action can reuse them.
             */
            Log.d(
                "PCG_PROBE",
                "skip snapshot invalidation because active tab is unknown " +
                        "sessionKey=$sessionKey inventoryVisible=${state.inventoryVisible} " +
                        "pokedexVisible=${state.pokedexVisible}"
            )
            return
        }

        if (state.shouldInvalidateInventorySnapshot) {
            val removed = lastInventorySnapshots.remove(sessionKey)

            if (removed != null) {
                Log.d(
                    "PCG_PROBE",
                    "invalidated cached inventory snapshot because active tab is ${state.activeTab} " +
                            "sessionKey=$sessionKey"
                )
            }
        }

        if (state.shouldInvalidatePokedexSnapshot) {
            val removed = lastPokedexSnapshots.remove(sessionKey)

            if (removed != null) {
                Log.d(
                    "PCG_PROBE",
                    "invalidated cached pokedex snapshot because active tab is ${state.activeTab} " +
                            "sessionKey=$sessionKey"
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

        if (pendingManualInventoryRequests.containsKey(sessionKey)) {
            when {
                state.isInventoryActive -> {
                    val snapshot = lastInventorySnapshots[sessionKey]

                    if (
                        snapshot != null &&
                        isCachedSnapshotFresh(
                            capturedAtMs = snapshot.capturedAtMs,
                            maxAgeMs = CACHED_INVENTORY_SNAPSHOT_MAX_AGE_MS,
                            debugLabel = "inventory",
                            sessionKey = sessionKey
                        ) &&
                        consumeManualUpdateRequest(
                            sessionKey = sessionKey,
                            pendingRequests = pendingManualInventoryRequests,
                            debugLabel = "inventory"
                        )
                    ) {
                        saveManualInventorySnapshot(
                            appContext = appContext,
                            sessionKey = sessionKey,
                            snapshot = snapshot,
                            source = "cached_after_tab_confirmed"
                        )
                    }
                }

                state.activeTab == "pokedex" -> {
                    val removedAtMs = pendingManualInventoryRequests.remove(sessionKey)
                    if (removedAtMs != null) {
                        Log.d(
                            "PCG_PROBE",
                            "manual inventory rejected because active tab is Pokédex sessionKey=$sessionKey"
                        )

                        showManualUpdateResultToastOnMain(
                            appContext = appContext,
                            requestedAtMs = removedAtMs,
                            messageRes = R.string.pcg_inventory_wrong_tab,
                            debugLabel = "inventory_wrong_tab_from_tab_state"
                        )
                    }
                }

                else -> {
                    Log.d(
                        "PCG_PROBE",
                        "manual inventory still pending because active tab is unknown sessionKey=$sessionKey"
                    )
                }
            }
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
                        consumeManualUpdateRequest(
                            sessionKey = sessionKey,
                            pendingRequests = pendingManualPokedexRequests,
                            debugLabel = "pokedex"
                        )
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
                            "PCG_PROBE",
                            "manual pokedex rejected because active tab is Inventory sessionKey=$sessionKey"
                        )

                        showManualUpdateResultToastOnMain(
                            appContext = appContext,
                            requestedAtMs = removedAtMs,
                            messageRes = R.string.pcg_pokedex_wrong_tab,
                            debugLabel = "pokedex_wrong_tab_from_tab_state"
                        )
                    }
                }

                else -> {
                    Log.d(
                        "PCG_PROBE",
                        "manual pokedex still pending because active tab is unknown sessionKey=$sessionKey"
                    )
                }
            }
        }
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

        showManualUpdateResultToastOnMain(
            appContext = appContext,
            requestedAtMs = requestedAtMs,
            messageRes = messageRes,
            debugLabel = debugLabel
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
        val sessionKey = pcgSessionKey(accountId)
        if (!ok) {
            lastPokedexSnapshots.remove(sessionKey)

            Log.w(
                "PCG_PROBE",
                "pokedex extract failed; cached pokedex snapshot invalidated " +
                        "sessionKey=$sessionKey reason=${payload.optString("reason")} payload=$payload"
            )
            return
        }

        if (!isPokedexPayloadSpawnableOnly(payload)) {
            lastPokedexSnapshots.remove(sessionKey)

            Log.w(
                "PCG_PROBE",
                "reject pokedex snapshot because it was not captured with Spawnable-only filters " +
                        "sessionKey=$sessionKey profileId=$profileId payload=$payload"
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
        val sessionKey = pcgSessionKey(accountId)

        val tabState = getFreshPcgTabState(
            sessionKey = sessionKey,
        )

        if (tabState != null && tabState.hasKnownActiveTab && !tabState.isInventoryActive) {
            /*
             * A late Inventory extract can arrive after the user has already moved
             * to another PCG tab. Do not let that late message refresh the Inventory
             * candidate, otherwise the manual Register button could reuse data from
             * a surface that is no longer visible.
             */
            lastInventorySnapshots.remove(sessionKey)

            Log.d(
                "PCG_PROBE",
                "inventory extract ignored because active tab is not Inventory " +
                        "sessionKey=$sessionKey activeTab=${tabState.activeTab} " +
                        "profileId=$profileId profileLabel=$profileLabel"
            )

            return
        }

        val ok = payload.optBoolean("ok", false)

        if (!ok) {
            /*
             * The Inventory surface is either known to be active or the active tab
             * is not known yet. In both cases, a failed extract should not leave an
             * old Inventory candidate available for immediate manual reuse.
             */
            lastInventorySnapshots.remove(sessionKey)

            Log.w(
                "PCG_PROBE",
                "inventory extract failed; cached inventory snapshot invalidated " +
                        "sessionKey=$sessionKey profileId=$profileId profileLabel=$profileLabel " +
                        "reason=${payload.optString("reason")} payload=$payload"
            )
            return
        }

        val balls = jsonArrayToInventoryBallList(payload.optJSONArray("balls"))
        if (balls.isEmpty()) {
            /*
             * Empty Inventory reads are not valid candidates. If the Inventory tab
             * is confirmed active, drop the previous candidate too: the latest DOM
             * read did not produce usable inventory data.
             */
            if (tabState?.isInventoryActive == true) {
                lastInventorySnapshots.remove(sessionKey)
            }

            Log.w(
                "PCG_PROBE",
                "inventory extract empty profileId=$profileId profileLabel=$profileLabel payload=$payload"
            )
            return
        }

        val snapshot = CachedInventorySnapshot(
            profileId = profileId,
            profileLabel = profileLabel,
            balls = balls,
            capturedAtMs = SystemClock.elapsedRealtime()
        )

        /*
         * Only invalidate the Pokédex candidate when Android has a fresh signal
         * confirming that Inventory is the active PCG tab. This avoids letting a
         * late Inventory message wipe a valid Pokédex candidate after a tab change.
         */
        if (tabState?.isInventoryActive == true) {
            lastPokedexSnapshots.remove(sessionKey)

            Log.d(
                "PCG_PROBE",
                "pokedex passive snapshot invalidated because Inventory tab is active " +
                        "sessionKey=$sessionKey"
            )
        } else {
            Log.d(
                "PCG_PROBE",
                "inventory extract accepted with no confirmed active Inventory tab; " +
                        "keeping Pokédex snapshot untouched sessionKey=$sessionKey " +
                        "activeTab=${tabState?.activeTab}"
            )
        }

        /*
         * This is still only a passive candidate. It becomes real saved Inventory
         * data only if the user has pressed Register Inventory, either just before
         * this snapshot arrived or while this snapshot is still recent.
         */
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

                                            "pcg_pokedex_state" -> {
                                                handlePcgPokedexState(
                                                    appContext = appContext,
                                                    accountId = accountId,
                                                    payload = payload
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

                                            TYPE_PCG_TAB_STATE -> {
                                                handlePcgTabState(
                                                    appContext = appContext,
                                                    accountId = accountId,
                                                    payload = payload
                                                )
                                            }

                                            TYPE_PCG_INVENTORY_WRONG_TAB -> {
                                                Log.d("PCG_PROBE", "ignored JS inventory wrong-tab payload=$payload")
                                            }

                                            TYPE_PCG_POKEDEX_WRONG_TAB -> {
                                                Log.d("PCG_PROBE", "ignored JS pokedex wrong-tab payload=$payload")
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

        val appContext = context.applicationContext
        val requestedAtMs = SystemClock.elapsedRealtime()

        val tabState = getFreshPcgTabState(
            sessionKey = sessionKey,
        )

        if (tabState != null && tabState.hasKnownActiveTab && !tabState.isInventoryActive) {
            /*
             * Android has a fresh tab-state signal, and it says another PCG tab is
             * active. Remove the Inventory candidate so it cannot be reused after the
             * user has moved away from Inventory.
             */
            lastInventorySnapshots.remove(sessionKey)

            Log.d(
                "PCG_PROBE",
                "manual inventory rejected immediately because active tab is not Inventory " +
                        "sessionKey=$sessionKey activeTab=${tabState.activeTab}"
            )

            showManualUpdateResultToastOnMain(
                appContext = appContext,
                requestedAtMs = requestedAtMs,
                messageRes = R.string.pcg_inventory_wrong_tab,
                debugLabel = "inventory_wrong_tab_immediate"
            )

            return true
        }

        if (tabState != null && !tabState.hasKnownActiveTab) {
            /*
             * We have a fresh PCG tab-state payload, but it cannot identify the active
             * tab yet. Do not reject immediately.
             *
             * A recent Inventory snapshot is still safe to use because it is short-lived
             * and came from the Inventory extractor. If it is stale or missing, the
             * manual request below will wait for the next valid Inventory snapshot.
             */
            Log.d(
                "PCG_PROBE",
                "manual inventory press has fresh tab state but active tab is unknown " +
                        "sessionKey=$sessionKey activeTab=${tabState.activeTab} " +
                        "inventoryVisible=${tabState.inventoryVisible} " +
                        "pokedexVisible=${tabState.pokedexVisible}"
            )
        }

        val cachedSnapshot = lastInventorySnapshots[sessionKey]

        if (
            cachedSnapshot != null &&
            isRecentPassiveSnapshot(
                capturedAtMs = cachedSnapshot.capturedAtMs,
                sessionKey = sessionKey
            )
        ) {
            Log.d(
                "PCG_PROBE",
                "manual inventory registration completed from recent passive Inventory snapshot " +
                        "sessionKey=$sessionKey activeTab=${tabState?.activeTab}"
            )

            saveManualInventorySnapshot(
                appContext = appContext,
                sessionKey = sessionKey,
                snapshot = cachedSnapshot,
                source = "recent_passive_inventory_snapshot"
            )

            return true
        }

        if (cachedSnapshot != null) {
            /*
             * The cached candidate exists, but it is too old for the new passive-snapshot
             * model. Drop it and wait for the next fresh Inventory extract.
             */
            lastInventorySnapshots.remove(sessionKey)

            Log.d(
                "PCG_PROBE",
                "stale inventory passive snapshot dropped before arming manual registration " +
                        "sessionKey=$sessionKey activeTab=${tabState?.activeTab}"
            )
        }

        /*
         * No recent passive candidate is available. Keep the user action alive for
         * a short window: the next valid Inventory extract from the active Inventory
         * tab will be promoted from passive candidate to saved app data.
         */
        pendingManualInventoryRequests[sessionKey] = requestedAtMs

        Log.d(
            "PCG_PROBE",
            "manual inventory registration armed; waiting for next valid recent inventory snapshot " +
                    "sessionKey=$sessionKey requestedAtMs=$requestedAtMs " +
                    "timeoutMs=$MANUAL_PCG_UPDATE_TIMEOUT_MS"
        )

        scheduleManualUpdateTimeout(
            appContext = appContext,
            sessionKey = sessionKey,
            requestedAtMs = requestedAtMs,
            pendingRequests = pendingManualInventoryRequests,
            timeoutMessageRes = R.string.pcg_inventory_read_timeout,
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

        val appContext = context.applicationContext
        val requestedAtMs = SystemClock.elapsedRealtime()

        val pokedexState = getFreshPcgPokedexState(
            sessionKey = sessionKey,
        )

        if (pokedexState != null) {
            when {
                !pokedexState.onPokedexTab -> {
                    /*
                     * The user pressed the Pokédex registration button, but Android
                     * has a fresh signal saying the currently visible PCG surface is
                     * not the Pokédex tab. Do not arm a manual request here, because
                     * the next passive snapshot could belong to another surface.
                     */
                    lastPokedexSnapshots.remove(sessionKey)

                    Log.d(
                        "PCG_PROBE",
                        "manual pokedex rejected immediately because Pokédex tab is not active " +
                                "sessionKey=$sessionKey reason=${pokedexState.reason}"
                    )

                    showManualUpdateResultToastOnMain(
                        appContext = appContext,
                        requestedAtMs = requestedAtMs,
                        messageRes = R.string.pcg_pokedex_wrong_tab,
                        debugLabel = "pokedex_wrong_tab_immediate"
                    )

                    return true
                }

                pokedexState.needsSpawnableOnlyHint -> {
                    /*
                     * The Pokédex tab is visible, but the active filters are not the
                     * safe Spawnable-only state required for a missing-dex upload.
                     * Drop the cached candidate so an old valid snapshot cannot be
                     * reused while the visible UI is now in a different filter state.
                     */
                    lastPokedexSnapshots.remove(sessionKey)

                    Log.d(
                        "PCG_PROBE",
                        "manual pokedex rejected immediately because Spawnable-only filter is required " +
                                "sessionKey=$sessionKey reason=${pokedexState.reason} " +
                                "otherFilters=${pokedexState.activeNonSpawnableFilters}"
                    )

                    showManualUpdateResultToastOnMain(
                        appContext = appContext,
                        requestedAtMs = requestedAtMs,
                        messageRes = R.string.pcg_pokedex_spawnable_only_required,
                        debugLabel = "pokedex_spawnable_only_required_immediate"
                    )

                    return true
                }

                pokedexState.validForMissingDexUpload -> {
                    val cachedSnapshot = lastPokedexSnapshots[sessionKey]

                    if (
                        cachedSnapshot != null &&
                        isRecentPassiveSnapshot(
                            capturedAtMs = cachedSnapshot.capturedAtMs,
                            sessionKey = sessionKey
                        )
                    ) {
                        Log.d(
                            "PCG_PROBE",
                            "manual pokedex registration completed from recent passive Spawnable-only snapshot " +
                                    "sessionKey=$sessionKey"
                        )

                        uploadManualPokedexSnapshot(
                            appContext = appContext,
                            sessionKey = sessionKey,
                            snapshot = cachedSnapshot,
                            source = "recent_passive_spawnable_only_snapshot"
                        )

                        return true
                    }

                    if (cachedSnapshot != null) {
                        /*
                         * A stale candidate is worse than no candidate: keeping it
                         * around would make the manual button depend on old DOM state
                         * instead of the currently visible Pokédex tab.
                         */
                        lastPokedexSnapshots.remove(sessionKey)

                        Log.d(
                            "PCG_PROBE",
                            "stale pokedex passive snapshot dropped before arming manual registration " +
                                    "sessionKey=$sessionKey"
                        )
                    }
                }
            }
        }

        /*
         * Either we have no fresh Pokédex state yet, or the Pokédex tab is valid
         * but no recent passive snapshot is available. Arm a short manual window:
         * the next valid Spawnable-only snapshot received from the visible Pokédex
         * tab will be promoted to a real user-confirmed upload.
         */
        pendingManualPokedexRequests[sessionKey] = requestedAtMs

        Log.d(
            "PCG_PROBE",
            "manual pokedex registration armed; waiting for next valid recent Spawnable-only pokedex snapshot " +
                    "sessionKey=$sessionKey requestedAtMs=$requestedAtMs " +
                    "timeoutMs=$MANUAL_PCG_UPDATE_TIMEOUT_MS"
        )

        scheduleManualUpdateTimeout(
            appContext = appContext,
            sessionKey = sessionKey,
            requestedAtMs = requestedAtMs,
            pendingRequests = pendingManualPokedexRequests,
            timeoutMessageRes = R.string.pcg_pokedex_read_timeout,
            debugLabel = "pokedex"
        )

        return true
    }

    /**
     * Reloads the active PCG Gecko session for the selected account.
     *
     * This is intentionally user-triggered: the app refreshes the PCG surface only
     * when the user taps the visible Refresh PCG button. Inventory and Pokédex reads
     * remain separate manual actions.
     */
    fun refreshPcgExtension(accountId: String): Boolean {
        assertMainThread()

        val sessionKey = pcgSessionKey(accountId)
        val session = sessions[sessionKey]

        if (session == null) {
            Log.w(
                "PCG_PROBE",
                "PCG refresh ignored, session not ready sessionKey=$sessionKey"
            )
            return false
        }

        invalidatePcgProbeStateForSession(sessionKey)

        return try {
            /*
             * GeckoSession.reload() refreshes the currently loaded PCG extension page.
             * We intentionally keep loadedPcgUrls unchanged, because this is a page
             * refresh, not a navigation to a different PCG URL.
             */
            session.reload()

            Log.d(
                "PCG_PROBE",
                "PCG extension refresh requested sessionKey=$sessionKey"
            )

            true
        } catch (t: Throwable) {
            Log.e(
                "PCG_PROBE",
                "PCG extension refresh failed sessionKey=$sessionKey",
                t
            )
            false
        }
    }

    /**
     * Clears cached probe state for one PCG session.
     *
     * A page refresh invalidates the DOM surface that produced previous snapshots.
     * Keeping old snapshots around would let a later manual update accidentally
     * accept stale pre-refresh data.
     */
    private fun invalidatePcgProbeStateForSession(sessionKey: String) {
        lastInventorySnapshots.remove(sessionKey)
        lastPokedexSnapshots.remove(sessionKey)

        pendingManualInventoryRequests.remove(sessionKey)
        pendingManualPokedexRequests.remove(sessionKey)

        lastPcgTabStates.remove(sessionKey)
        lastPcgPokedexStates.remove(sessionKey)

        /*
         * recentManualPokedexUploadKeys is keyed as:
         * "$sessionKey|profileId|count|signature"
         *
         * So it cannot be cleared with remove(sessionKey). Remove all entries that
         * belong to this PCG session.
         */
        val dedupPrefix = "$sessionKey|"
        for (key in recentManualPokedexUploadKeys.keys) {
            if (key.startsWith(dedupPrefix)) {
                recentManualPokedexUploadKeys.remove(key)
            }
        }

        Log.d(
            "PCG_PROBE",
            "cleared cached PCG probe state for sessionKey=$sessionKey"
        )
    }

    /**
     * Completes a manual PCG update request with a tab-specific timeout message.
     *
     * The timeout is a safety net for cases where the WebExtension cannot produce
     * a fresh tab-state message, for example while the PCG iframe is still loading
     * or the DOM does not match the known Inventory/Pokédex selectors.
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
                    "PCG_PROBE",
                    "manual $debugLabel registration timed out sessionKey=$sessionKey timeoutMs=$MANUAL_PCG_UPDATE_TIMEOUT_MS"
                )

                showManualUpdateResultToastOnMain(
                    appContext = appContext,
                    requestedAtMs = requestedAtMs,
                    messageRes = timeoutMessageRes,
                    debugLabel = "${debugLabel}_timeout"
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
                messageRes = R.string.pcg_inventory_updated
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
        if (shouldSkipDuplicateManualPokedexUpload(sessionKey, snapshot)) {
            return
        }
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