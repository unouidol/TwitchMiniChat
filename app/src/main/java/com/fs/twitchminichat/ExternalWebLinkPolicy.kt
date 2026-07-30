package com.fs.twitchminichat

import java.net.URI

/**
 * Accepts only regular Hypertext Transfer Protocol web links.
 */
object ExternalWebLinkPolicy {

    /**
     * Returns a trimmed HTTP or HTTPS URL with an authority, or null when unsafe.
     */
    fun normalize(rawUrl: String): String? {
        val normalizedUrl = rawUrl.trim()
        if (normalizedUrl.isBlank()) return null

        val parsed = runCatching {
            URI(normalizedUrl)
        }.getOrNull() ?: return null

        val scheme = parsed.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return null
        }

        if (parsed.rawAuthority.isNullOrBlank()) {
            return null
        }

        return normalizedUrl
    }
}
