package com.fs.twitchminichat

import android.content.Context

data class AccountConfig(
    val username: String,
    val channel: String,
    val accessToken: String
)

object AccountStorage {

    private const val PREFS_NAME = "twitch_accounts"
    private const val KEY_ACTIVE_SLOT = "active_slot"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAccount(context: Context, slot: Int, config: AccountConfig) {
        val p = prefs(context).edit()
        p.putString("username_$slot", config.username)
        p.putString("channel_$slot", config.channel)
        p.putString("access_$slot", config.accessToken)
        p.apply()
    }

    fun loadAccount(context: Context, slot: Int): AccountConfig? {
        val p = prefs(context)
        val username = p.getString("username_$slot", null) ?: return null
        val channel = p.getString("channel_$slot", null) ?: return null
        val access = p.getString("access_$slot", null) ?: return null

        return AccountConfig(
            username = username,
            channel = channel,
            accessToken = access
        )
    }

    fun setActiveAccount(context: Context, slot: Int) {
        prefs(context).edit()
            .putInt(KEY_ACTIVE_SLOT, slot)
            .apply()
    }

    fun getActiveAccount(context: Context): Int {
        return prefs(context).getInt(KEY_ACTIVE_SLOT, 1)
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
