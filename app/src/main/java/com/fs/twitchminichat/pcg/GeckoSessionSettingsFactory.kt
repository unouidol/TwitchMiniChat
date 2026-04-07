package com.fs.twitchminichat.v2gecko.pcg

import org.mozilla.geckoview.GeckoSessionSettings

object GeckoSessionSettingsFactory {
    fun default(): GeckoSessionSettings {
        return GeckoSessionSettings.Builder()
            .usePrivateMode(false) // deve essere false: vogliamo persistenza per quell’account
            .build()
    }
}

