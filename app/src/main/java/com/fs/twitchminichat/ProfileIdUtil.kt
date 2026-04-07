package com.fs.twitchminichat

import java.util.Locale

object ProfileIdUtil {
    fun fromUsername(username: String): String {
        return username.trim().lowercase(Locale.ROOT)
    }
}