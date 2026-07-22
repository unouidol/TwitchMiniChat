package com.fs.twitchminichat

/** Twitch emote media formats supported by the official Content Delivery Network. */
enum class TwitchEmoteFormat(val pathSegment: String) {
    ANIMATED("animated"),
    STATIC("static")
}

/** Twitch emote background themes supported by the official Content Delivery Network. */
enum class TwitchEmoteTheme(val pathSegment: String) {
    DARK("dark"),
    LIGHT("light")
}

/** Builds official Twitch emote image URLs without exposing OAuth credentials. */
object TwitchEmoteUrlFactory {

    /** Returns one Content Delivery Network URL for a validated Twitch emote ID. */
    fun build(
        emoteId: String,
        format: TwitchEmoteFormat,
        theme: TwitchEmoteTheme,
        scale: String
    ): String? {
        val normalizedId = emoteId.trim()
        if (!isValidEmoteId(normalizedId)) return null

        val safeScale = scale.takeIf { candidate -> candidate in SUPPORTED_SCALES }
            ?: DEFAULT_SCALE

        return "$BASE_URL/$normalizedId/${format.pathSegment}/${theme.pathSegment}/$safeScale"
    }

    /** Selects the smallest official source image that remains sharp at the target size. */
    fun scaleForRenderSize(renderSizePx: Int): String {
        return when {
            renderSizePx > 56 -> "3.0"
            renderSizePx > 28 -> "2.0"
            else -> "1.0"
        }
    }

    /** Restricts identifiers to the characters currently accepted by Twitch emote IDs. */
    private fun isValidEmoteId(emoteId: String): Boolean {
        return emoteId.isNotBlank() && emoteId.all { character ->
            character.isLetterOrDigit() || character == '-' || character == '_'
        }
    }

    private const val BASE_URL = "https://static-cdn.jtvnw.net/emoticons/v2"
    private const val DEFAULT_SCALE = "2.0"
    private val SUPPORTED_SCALES = setOf("1.0", "2.0", "3.0")
}
