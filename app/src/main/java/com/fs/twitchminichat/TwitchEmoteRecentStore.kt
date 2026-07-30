package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

/**
 * Persists up to eighteen manually selected Twitch emotes for each local account.
 *
 * A previously unseen emote enters first. Selecting an existing emote keeps all
 * visual slots stable and never sends or queues a Twitch chat message.
 */
class TwitchEmoteRecentStore(context: Context) {

    /** Preferences containing non-secret recent Twitch emote identifiers. */
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Loads recent emote identifiers from newest added to oldest.
     *
     * Data created by the previous stable-slot policy is converted using its
     * chronological replacement order before being displayed.
     */
    fun load(accountId: String): List<String> {
        val recentKey = recentKey(accountId) ?: return emptyList()
        val raw = preferences.getString(recentKey, null) ?: return emptyList()

        val storedEmoteIds = parseEmoteIds(raw)
            .distinct()
            .take(MAX_RECENT_EMOTES)

        val replacementOrderKey =
            replacementOrderKey(accountId) ?: return storedEmoteIds

        val replacementOrderRaw = preferences.getString(
            replacementOrderKey,
            null
        ) ?: return storedEmoteIds

        return TwitchEmoteRecentOrderPolicy.migrateFromStableSlots(
            stableEmoteIds = storedEmoteIds,
            oldestToNewestOrder = parseEmoteIds(replacementOrderRaw),
            maxSize = MAX_RECENT_EMOTES
        )
    }

    /**
     * Adds one previously unseen emote to the first visual position.
     *
     * Existing entries keep their positions. Once eighteen entries are retained,
     * adding a new emote removes the last and oldest entry.
     */
    fun record(accountId: String, emoteId: String): List<String> {
        val recentKey = recentKey(accountId) ?: return emptyList()
        val replacementOrderKey =
            replacementOrderKey(accountId) ?: return emptyList()

        val normalizedEmoteId = emoteId.trim()
        if (normalizedEmoteId.isBlank()) return load(accountId)

        val updatedEmoteIds = TwitchEmoteRecentOrderPolicy.record(
            currentEmoteIds = load(accountId),
            selectedEmoteId = normalizedEmoteId,
            maxSize = MAX_RECENT_EMOTES
        )

        preferences.edit {
            putString(
                recentKey,
                JSONArray(updatedEmoteIds).toString()
            )

            /*
             * The new list directly stores newest-to-oldest order, so the legacy
             * stable-slot replacement queue is no longer required.
             */
            remove(replacementOrderKey)
        }

        return updatedEmoteIds
    }

    /** Removes recent-emote data belonging to one deleted local account. */
    fun clearAccount(accountId: String) {
        val recentKey = recentKey(accountId) ?: return
        val replacementOrderKey = replacementOrderKey(accountId) ?: return

        preferences.edit {
            remove(recentKey)
            remove(replacementOrderKey)
        }
    }

    /** Parses a JSON array while safely ignoring invalid or blank entries. */
    private fun parseEmoteIds(raw: String): List<String> {
        return try {
            val json = JSONArray(raw)

            buildList {
                for (index in 0 until json.length()) {
                    json.optString(index)
                        .trim()
                        .takeIf { emoteId -> emoteId.isNotBlank() }
                        ?.let(::add)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Builds the account-scoped key containing newest-to-oldest recent emotes. */
    private fun recentKey(accountId: String): String? {
        val normalizedAccountId = accountId.trim()
        if (normalizedAccountId.isBlank()) return null

        return "$RECENT_KEY_PREFIX$normalizedAccountId"
    }

    /** Builds the key used only to migrate the previous stable-slot policy. */
    private fun replacementOrderKey(accountId: String): String? {
        val normalizedAccountId = accountId.trim()
        if (normalizedAccountId.isBlank()) return null

        return "$REPLACEMENT_ORDER_KEY_PREFIX$normalizedAccountId"
    }

    companion object {

        /** Maximum number of distinct recent emotes retained per account. */
        private const val MAX_RECENT_EMOTES = 18

        /** SharedPreferences file containing recent non-secret emote identifiers. */
        private const val PREFERENCES_NAME = "twitch_emote_recents"

        /** Prefix used for newest-to-oldest recent-emote lists. */
        private const val RECENT_KEY_PREFIX = "recent:"

        /** Prefix retained only for migration from the previous slot policy. */
        private const val REPLACEMENT_ORDER_KEY_PREFIX =
            "replacement_order:"
    }
}