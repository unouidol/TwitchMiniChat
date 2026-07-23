package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

/**
 * Persists the ten most recently selected Twitch emotes for each local account.
 *
 * Stable visual slots and chronological replacement order are stored separately.
 * Selecting an emote never sends or queues a Twitch chat message.
 */
class TwitchEmoteRecentStore(context: Context) {

    /** Preferences containing non-secret recent Twitch emote identifiers. */
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    /** Loads recent emote identifiers in their stable visual-slot order. */
    fun load(accountId: String): List<String> {
        val key = recentKey(accountId) ?: return emptyList()
        val raw = preferences.getString(key, null) ?: return emptyList()

        return parseEmoteIds(raw)
            .distinct()
            .take(MAX_RECENT_EMOTES)
    }

    /**
     * Records one manually selected emote and returns the stable visual slots.
     *
     * Existing emotes retain their positions. Once all slots are occupied, the
     * oldest added emote is replaced without moving the remaining entries.
     */
    fun record(accountId: String, emoteId: String): List<String> {
        val recentKey = recentKey(accountId) ?: return emptyList()
        val replacementOrderKey =
            replacementOrderKey(accountId) ?: return emptyList()

        val normalizedEmoteId = emoteId.trim()
        if (normalizedEmoteId.isBlank()) return load(accountId)

        val currentEmoteIds = load(accountId)

        val update = TwitchEmoteRecentSlotPolicy.record(
            currentEmoteIds = currentEmoteIds,
            currentReplacementOrder = loadReplacementOrder(
                accountId = accountId,
                currentEmoteIds = currentEmoteIds
            ),
            selectedEmoteId = normalizedEmoteId,
            maxSize = MAX_RECENT_EMOTES
        )

        preferences.edit {
            putString(
                recentKey,
                JSONArray(update.emoteIds).toString()
            )
            putString(
                replacementOrderKey,
                JSONArray(update.replacementOrder).toString()
            )
        }

        return update.emoteIds
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

    /**
     * Loads the chronological order used to choose the next emote to replace.
     *
     * Legacy lists were stored from newest to oldest. When no separate order is
     * available, reversing the visual list preserves the existing chronology.
     */
    private fun loadReplacementOrder(
        accountId: String,
        currentEmoteIds: List<String>
    ): List<String> {
        val key = replacementOrderKey(accountId)
            ?: return currentEmoteIds.reversed()

        val raw = preferences.getString(key, null)
            ?: return currentEmoteIds.reversed()

        val storedOrder = parseEmoteIds(raw)

        return if (storedOrder.isEmpty() && currentEmoteIds.isNotEmpty()) {
            currentEmoteIds.reversed()
        } else {
            storedOrder
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

    /** Builds the account-scoped key containing stable visual slots. */
    private fun recentKey(accountId: String): String? {
        val normalizedAccountId = accountId.trim()
        if (normalizedAccountId.isBlank()) return null

        return "$RECENT_KEY_PREFIX$normalizedAccountId"
    }

    /** Builds the account-scoped key containing chronological replacement order. */
    private fun replacementOrderKey(accountId: String): String? {
        val normalizedAccountId = accountId.trim()
        if (normalizedAccountId.isBlank()) return null

        return "$REPLACEMENT_ORDER_KEY_PREFIX$normalizedAccountId"
    }

    companion object {

        /** Maximum number of distinct recent emotes retained per account. */
        private const val MAX_RECENT_EMOTES = 10

        /** SharedPreferences file containing recent non-secret emote identifiers. */
        private const val PREFERENCES_NAME = "twitch_emote_recents"

        /** Prefix used for stable recent-emote visual slots. */
        private const val RECENT_KEY_PREFIX = "recent:"

        /** Prefix used for chronological replacement queues. */
        private const val REPLACEMENT_ORDER_KEY_PREFIX =
            "replacement_order:"
    }
}