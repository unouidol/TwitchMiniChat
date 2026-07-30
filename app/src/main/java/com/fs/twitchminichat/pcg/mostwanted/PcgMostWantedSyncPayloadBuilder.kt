package com.fs.twitchminichat.pcg.mostwanted

import org.json.JSONArray
import org.json.JSONObject

/** Builds the stable backend payload for a PCG Most Wanted watchlist. */
object PcgMostWantedSyncPayloadBuilder {

    /**
     * Creates a device-and-profile scoped payload from validated local state.
     */
    fun build(
        deviceId: String,
        profileId: String,
        state: PcgMostWantedState
    ): JSONObject {
        val normalizedDeviceId = deviceId.trim()
        val normalizedProfileId = profileId.trim().lowercase()

        require(normalizedDeviceId.isNotEmpty()) {
            "deviceId must not be blank"
        }
        require(normalizedProfileId.isNotEmpty()) {
            "profileId must not be blank"
        }

        val pokemon = JSONArray()
        state.selectedDisplayNames.forEach(pokemon::put)

        return JSONObject().apply {
            put("device_id", normalizedDeviceId)
            put("profile_id", normalizedProfileId)
            put("enabled", state.enabled)
            put("pokemon", pokemon)
            put("catalog_version", CATALOG_VERSION)
        }
    }

    /** Version of the bundled catalog used to validate local selections. */
    private const val CATALOG_VERSION = "2026-07-30"
}