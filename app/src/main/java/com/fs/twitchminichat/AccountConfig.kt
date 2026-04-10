package com.fs.twitchminichat

data class AccountConfig(
    val id: String,
    val username: String,
    val channel: String,
    val accessToken: String,
    val profileId: String,
    val sortOrder: Int = 0
)