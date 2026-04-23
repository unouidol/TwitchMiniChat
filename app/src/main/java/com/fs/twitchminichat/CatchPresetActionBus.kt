package com.fs.twitchminichat

object CatchPresetActionBus {

    interface Listener {
        fun onBuyBallRequested(
            profileId: String,
            ballId: String,
            shopBallName: String,
            quantity: Int,
            label: String
        ): Boolean

        fun onBuddyInfoRequested(profileId: String): Boolean
    }

    @Volatile
    var listener: Listener? = null

    fun requestBuyBall(
        profileId: String,
        ballId: String,
        shopBallName: String,
        quantity: Int,
        label: String
    ): Boolean {
        return listener?.onBuyBallRequested(
            profileId = profileId,
            ballId = ballId,
            shopBallName = shopBallName,
            quantity = quantity,
            label = label
        ) ?: false
    }

    fun requestBuddyInfo(profileId: String): Boolean {
        return listener?.onBuddyInfoRequested(profileId) ?: false
    }
}