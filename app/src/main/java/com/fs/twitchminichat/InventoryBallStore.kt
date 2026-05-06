package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class InventoryBallItem(
    val ballId: String,
    val name: String,
    val count: Int
)

object InventoryBallStore {

    private const val PREFS_NAME = "inventory_ball_store"

    private fun realKey(profileId: String) = "real_$profileId"
    private fun optimisticKey(profileId: String) = "optimistic_$profileId"

    private fun boughtKey(profileId: String) = "bought_$profileId"

    fun saveRealSnapshot(
        context: Context,
        profileId: String,
        balls: List<InventoryBallItem>
    ) {
        val array = JSONArray()
        balls.forEach { ball ->
            array.put(
                JSONObject().apply {
                    put("ballId", ball.ballId)
                    put("name", ball.name)
                    put("count", ball.count)
                }
            )
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(realKey(profileId), array.toString())
            remove(optimisticKey(profileId))
            remove(boughtKey(profileId))
        }
    }

    fun loadRealSnapshot(
        context: Context,
        profileId: String
    ): List<InventoryBallItem> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(realKey(profileId), null)
            ?: return emptyList()

        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val out = ArrayList<InventoryBallItem>(arr.length())

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val ballId = obj.optString("ballId").trim()
            val name = obj.optString("name").trim()
            val count = obj.optInt("count", -1)

            if (ballId.isBlank() || name.isBlank() || count < 0) continue

            out.add(
                InventoryBallItem(
                    ballId = ballId,
                    name = name,
                    count = count
                )
            )
        }

        return out
    }

    fun getDisplayCounts(
        context: Context,
        profileId: String
    ): Map<String, Int> {
        val real = loadRealSnapshot(context, profileId)
            .associate { it.ballId to it.count }

        if (real.isEmpty()) return emptyMap()

        val optimisticUsed = loadOptimisticUsage(context, profileId)
        val optimisticBought = loadOptimisticBought(context, profileId)

        val out = LinkedHashMap<String, Int>()

        for ((ballId, realCount) in real) {
            val used = optimisticUsed[ballId] ?: 0
            val bought = optimisticBought[ballId] ?: 0
            out[ballId] = (realCount - used + bought).coerceAtLeast(0)
        }

        for ((ballId, bought) in optimisticBought) {
            if (!out.containsKey(ballId) && bought > 0) {
                out[ballId] = bought
            }
        }

        return out
    }

    fun noteBallBought(
        context: Context,
        profileId: String,
        ballId: String,
        quantity: Int
    ) {
        if (quantity <= 0) return

        val bought = loadOptimisticBought(context, profileId).toMutableMap()
        bought[ballId] = (bought[ballId] ?: 0) + quantity
        saveOptimisticBought(context, profileId, bought)
    }

    private fun loadOptimisticBought(
        context: Context,
        profileId: String
    ): Map<String, Int> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(boughtKey(profileId), null)
            ?: return emptyMap()

        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        val out = LinkedHashMap<String, Int>()

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.optInt(key, -1)
            if (key.isNotBlank() && value >= 0) {
                out[key] = value
            }
        }

        return out
    }

    private fun saveOptimisticBought(
        context: Context,
        profileId: String,
        bought: Map<String, Int>
    ) {
        val obj = JSONObject()
        bought.forEach { (ballId, count) ->
            obj.put(ballId, count)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(boughtKey(profileId), obj.toString())
        }
    }

    fun noteBallUsed(
        context: Context,
        profileId: String,
        ballId: String
    ): Boolean {
        val currentDisplay = getDisplayCounts(context, profileId)[ballId] ?: return false
        if (currentDisplay <= 0) return false

        val optimistic = loadOptimisticUsage(context, profileId).toMutableMap()
        optimistic[ballId] = (optimistic[ballId] ?: 0) + 1
        saveOptimisticUsage(context, profileId, optimistic)
        return true
    }

    private fun loadOptimisticUsage(
        context: Context,
        profileId: String
    ): Map<String, Int> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(optimisticKey(profileId), null)
            ?: return emptyMap()

        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        val out = LinkedHashMap<String, Int>()

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.optInt(key, -1)
            if (key.isNotBlank() && value >= 0) {
                out[key] = value
            }
        }

        return out
    }

    private fun saveOptimisticUsage(
        context: Context,
        profileId: String,
        usage: Map<String, Int>
    ) {
        val obj = JSONObject()
        usage.forEach { (ballId, count) ->
            obj.put(ballId, count)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(optimisticKey(profileId), obj.toString())
        }
    }

    /**
     * Deletes every local inventory value associated with one profile.
     *
     * Inventory currently stores three profile-scoped entries:
     * - the last real snapshot extracted from PCG;
     * - optimistic usage caused by manual quick-catch taps;
     * - optimistic bought counts caused by shop shortcuts.
     */
    fun clearProfile(context: Context, profileId: String) {
        val cleanProfileId = profileId.trim()
        if (cleanProfileId.isBlank()) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(realKey(cleanProfileId))
            remove(optimisticKey(cleanProfileId))
            remove(boughtKey(cleanProfileId))

            /*
             * Deletion should be forgiving with older or accidental mixed-case ids.
             * Normal profile ids are expected to be lowercase, but this second pass
             * prevents stale entries from surviving just because the stored id had a
             * different casing.
             */
            val normalizedProfileId = cleanProfileId.lowercase()
            if (normalizedProfileId != cleanProfileId) {
                remove(realKey(normalizedProfileId))
                remove(optimisticKey(normalizedProfileId))
                remove(boughtKey(normalizedProfileId))
            }
        }
    }
}