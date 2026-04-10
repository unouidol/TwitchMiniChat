package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

object HiddenUsersStore {

    private const val PREFS_NAME = "hidden_users_store"
    private const val KEY_USERS = "hidden_users"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun normalize(user: String?): String {
        return user?.trim()?.lowercase().orEmpty()
    }

    fun getAll(context: Context): Set<String> {
        return prefs(context)
            .getStringSet(KEY_USERS, emptySet())
            ?.map { normalize(it) }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    fun isHidden(context: Context, user: String?): Boolean {
        val normalized = normalize(user)
        if (normalized.isBlank()) return false
        return getAll(context).contains(normalized)
    }

    fun add(context: Context, user: String): Boolean {
        val normalized = normalize(user)
        if (normalized.isBlank()) return false

        val current = getAll(context).toMutableSet()
        val added = current.add(normalized)

        if (added) {
            prefs(context).edit {
                putStringSet(KEY_USERS, current)
            }
        }

        return added
    }

    fun remove(context: Context, user: String): Boolean {
        val normalized = normalize(user)
        if (normalized.isBlank()) return false

        val current = getAll(context).toMutableSet()
        val removed = current.remove(normalized)

        if (removed) {
            prefs(context).edit {
                putStringSet(KEY_USERS, current)
            }
        }

        return removed
    }

    fun clear(context: Context) {
        prefs(context).edit {
            remove(KEY_USERS)
        }
    }
}