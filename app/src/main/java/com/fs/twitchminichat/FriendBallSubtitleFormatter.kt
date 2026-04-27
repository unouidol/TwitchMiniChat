package com.fs.twitchminichat

import android.content.Context

/**
 * Builds the subtitle shown for Friend Ball rows in the quick catch menu.
 *
 * This is kept outside ChatFragment because it is not chat UI logic.
 * It only formats buddy/profile data into a user-facing subtitle.
 */
object FriendBallSubtitleFormatter {

    fun build(
        context: Context,
        profileId: String?
    ): String? {
        if (profileId.isNullOrBlank()) return null

        val info = BuddyInfoStore.load(context, profileId)
            ?: return context.getString(R.string.buddy_unknown)

        val typeText = when {
            !info.primaryType.isNullOrBlank() && !info.secondaryType.isNullOrBlank() ->
                "${info.primaryType} / ${info.secondaryType}"

            !info.primaryType.isNullOrBlank() ->
                info.primaryType

            else -> null
        }

        return when {
            info.level != null && typeText != null -> context.getString(
                R.string.friend_ball_buddy_subtitle_level_types,
                info.rawName,
                info.level,
                typeText
            )

            info.level != null -> context.getString(
                R.string.friend_ball_buddy_subtitle_level,
                info.rawName,
                info.level
            )

            typeText != null -> context.getString(
                R.string.friend_ball_buddy_subtitle_name_types,
                info.rawName,
                typeText
            )

            else -> context.getString(
                R.string.friend_ball_buddy_subtitle_name_only,
                info.rawName
            )
        }
    }
}