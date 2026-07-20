package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchOAuthRequestFeaturesTest {

    @Test
    fun stableBuildDoesNotRequestOptionalEmoteScope() {
        assertTrue(
            TwitchOAuthRequestFeatures.queryParameters(
                requestEmoteScope = false
            ).isEmpty()
        )
    }

    @Test
    fun devBuildRequestsAllowListedEmoteFeature() {
        assertEquals(
            listOf("feature" to "emotes"),
            TwitchOAuthRequestFeatures.queryParameters(
                requestEmoteScope = true
            )
        )
    }
}
