package com.fs.twitchminichat

/** Reports the result of one user-triggered IRC socket write. */
sealed interface TwitchChatWriteResult {

    /** The command was written to the active IRC socket exactly once. */
    data object Written : TwitchChatWriteResult

    /** No active IRC writer was available for this command. */
    data object NotConnected : TwitchChatWriteResult

    /** The single socket write failed and was not retried. */
    data class Failed(
        val cause: Throwable
    ) : TwitchChatWriteResult
}
