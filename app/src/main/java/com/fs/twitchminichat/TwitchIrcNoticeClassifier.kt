package com.fs.twitchminichat

/**
 * High-level action associated with a Twitch Internet Relay Chat (IRC) NOTICE.
 */
enum class TwitchIrcNoticeCategory {

    /** Twitch rejected the credentials used by the current IRC session. */
    AUTHENTICATION_FAILED,

    /** The NOTICE belongs to the existing non-authentication handling flow. */
    OTHER
}

/**
 * Classifies Twitch IRC NOTICE messages without coupling protocol rules to the user interface.
 */
object TwitchIrcNoticeClassifier {

    /**
     * Returns the stable category for one parsed NOTICE.
     */
    fun classify(
        msgId: String?,
        message: String
    ): TwitchIrcNoticeCategory {
        val normalizedMsgId = msgId?.trim()?.lowercase().orEmpty()
        val normalizedMessage = message.trim().lowercase()

        val isAuthenticationFailure =
            normalizedMsgId in AUTHENTICATION_FAILURE_MESSAGE_IDS ||
                    AUTHENTICATION_FAILURE_MESSAGE_PARTS.any { part ->
                        normalizedMessage.contains(part)
                    }

        return if (isAuthenticationFailure) {
            TwitchIrcNoticeCategory.AUTHENTICATION_FAILED
        } else {
            TwitchIrcNoticeCategory.OTHER
        }
    }

    /** Known message identifiers for Twitch IRC authentication failures. */
    private val AUTHENTICATION_FAILURE_MESSAGE_IDS = setOf(
        "login_authentication_failed",
        "msg_login_authentication_failed"
    )

    /** Stable text fragments used by Twitch when IRC credentials are rejected. */
    private val AUTHENTICATION_FAILURE_MESSAGE_PARTS = listOf(
        "login authentication failed",
        "login unsuccessful",
        "improperly formatted auth"
    )
}
