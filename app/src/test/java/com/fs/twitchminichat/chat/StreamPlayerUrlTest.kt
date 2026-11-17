package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the stream player's address: the page it loads, the channel it plays, and the sound it
 * starts with. Until 2026-11-12 the address carried the app's own mute flag, `muted=true` on every
 * new page; with the app's mute button removed it always asks for sound.
 */
class StreamPlayerUrlTest {

    @Test
    fun theAddressNamesThePageAndTheChannel_andAsksForSound() {
        assertEquals(
            "https://unouidol.github.io/ircminichat/player.html?channel=examplechannel&muted=false",
            StreamPlayerUrl.build("examplechannel")
        )
    }

    @Test
    fun theChannelIsFormEncoded_soItCannotAddAParameter() {
        assertEquals(
            "https://unouidol.github.io/ircminichat/player.html?channel=a+b%26muted%3Dtrue&muted=false",
            StreamPlayerUrl.build("a b&muted=true")
        )
    }
}
