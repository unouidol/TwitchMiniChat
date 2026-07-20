package com.fs.twitchminichat

/**
 * Builds the optional feature parameters sent to the Twitch OAuth backend.
 *
 * Keeping feature selection outside the login Fragment prevents OAuth policy
 * details from spreading into user-interface code.
 */
internal object TwitchOAuthRequestFeatures {

    private const val FEATURE_QUERY_PARAMETER = "feature"
    private const val EMOTES_FEATURE = "emotes"

    /**
     * Returns the allow-listed OAuth feature parameters for the current build.
     */
    fun queryParameters(requestEmoteScope: Boolean): List<Pair<String, String>> {
        if (!requestEmoteScope) return emptyList()

        return listOf(FEATURE_QUERY_PARAMETER to EMOTES_FEATURE)
    }
}
