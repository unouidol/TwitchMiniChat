package com.fs.twitchminichat.pcg.mostwanted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the Most Wanted synchronization payload. */
class PcgMostWantedSyncPayloadBuilderTest {

    /** Preserves profile scope, enabled state and canonical catalog order. */
    @Test
    fun build_createsExpectedInformativePayload() {
        val payload = PcgMostWantedSyncPayloadBuilder.build(
            deviceId = " device-123 ",
            profileId = " UnoUidol ",
            state = PcgMostWantedState(
                enabled = true,
                selectedDisplayNames = linkedSetOf(
                    "Pikachu",
                    "Flab\u00e9b\u00e9 (Blue)"
                )
            )
        )

        assertEquals("device-123", payload.getString("device_id"))
        assertEquals("unouidol", payload.getString("profile_id"))
        assertTrue(payload.getBoolean("enabled"))
        assertEquals(
            listOf("Pikachu", "Flab\u00e9b\u00e9 (Blue)"),
            payload.getJSONArray("pokemon").let { array ->
                List(array.length()) { index ->
                    array.getString(index)
                }
            }
        )
        assertEquals(
            "2026-07-30",
            payload.getString("catalog_version")
        )

        /* Informative synchronization must never contain gameplay actions. */
        assertFalse(payload.has("command"))
        assertFalse(payload.has("catch"))
        assertFalse(payload.has("retry"))
    }
}