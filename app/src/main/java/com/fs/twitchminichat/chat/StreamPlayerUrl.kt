package com.fs.twitchminichat.chat

import java.net.URLEncoder

/**
 * The address of the stream player page that the chat page's GeckoView loads.
 *
 * The page, `player.html` in the public repository `unouidol/ircminichat`, served by GitHub Pages,
 * builds the Twitch embed with `autoplay: false` and reads `muted` from this address once, when it
 * builds the player.
 *
 * The stream starts with sound ON: the player never autoplays, so whoever presses its play button
 * wants to hear it. The app has no mute control of its own; the Twitch player's own control
 * mutes inside the page, without a reload. The parameter is sent explicitly because the page's
 * default, when it is absent, is muted.
 */
object StreamPlayerUrl {

    /** The player page, without its query. */
    const val PAGE = "https://unouidol.github.io/ircminichat/player.html"

    /** The address that plays [channel], a normalized channel name, with sound on. */
    fun build(channel: String): String {
        val encodedChannel = URLEncoder.encode(channel, "UTF-8")
        return "$PAGE?channel=$encodedChannel&muted=false"
    }
}
