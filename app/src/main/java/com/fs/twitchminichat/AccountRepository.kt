package com.fs.twitchminichat

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads and writes the locally stored Twitch accounts.
 *
 * Credentials are held by an [AccountJsonStore]; the production store encrypts them
 * with an Android Keystore key. The list logic itself stays free of Android types so
 * it can be covered by local unit tests.
 */
class AccountRepository internal constructor(
    private val store: AccountJsonStore
) {

    /** Creates the production repository and migrates any legacy plain-text list. */
    constructor(ctx: Context) : this(createProductionStore(ctx))

    /**
     * Returns every stored account.
     *
     * An unreadable store is reported as an empty list rather than falling back to a
     * less protected source: the user re-authenticates instead of the application
     * silently using credentials it could not authenticate.
     */
    fun loadAccounts(): List<AccountConfig> {
        val json = when (val lookup = store.read()) {
            is AccountJsonLookup.Present -> lookup.json
            AccountJsonLookup.Missing -> EMPTY_ACCOUNT_LIST
            AccountJsonLookup.Unavailable -> EMPTY_ACCOUNT_LIST
        }

        val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<AccountConfig>()

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").trim()
            val username = o.optString("username").trim()
            val accessToken = o.optString("accessToken").trim()

            if (id.isEmpty() || username.isEmpty() || accessToken.isEmpty()) continue

            out.add(
                AccountConfig(
                    id = id,
                    username = username,
                    channel = o.optString("channel").trim(),
                    accessToken = accessToken,
                    profileId = o.optString("profileId", "")
                )
            )
        }
        return out
    }

    fun addAccount(cfg: AccountConfig) {
        val list = loadAccounts().toMutableList()
        list.add(cfg)
        saveAll(list)
    }

    fun saveAll(list: List<AccountConfig>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("username", it.username)
            o.put("channel", it.channel)
            o.put("accessToken", it.accessToken)
            o.put("profileId", it.profileId)
            arr.put(o)
        }
        store.write(arr.toString())
    }

    fun getById(id: String): AccountConfig? = loadAccounts().firstOrNull { it.id == id }

    /**
     * Replaces OAuth credentials for one existing account without changing its local identity.
     *
     * The account id, channel, list position, and all profile-scoped stores remain unchanged.
     * Returns false when the requested local account no longer exists.
     */
    fun updateCredentialsInPlace(
        accountId: String,
        username: String,
        accessToken: String,
        profileId: String
    ): Boolean {
        val list = loadAccounts().toMutableList()
        val index = list.indexOfFirst { it.id == accountId }
        if (index == -1) return false

        val current = list[index]
        list[index] = current.copy(
            username = username.trim(),
            accessToken = accessToken.trim(),
            profileId = profileId.trim().ifBlank { current.profileId }
        )
        saveAll(list)
        return true
    }

    /**
     * Removes one saved account by id and returns the removed config.
     *
     * Returning the removed account keeps deletion flows safer because callers can
     * still resolve profile-scoped cleanup data from the exact account that was
     * deleted.
     */
    fun removeById(id: String): AccountConfig? {
        val list = loadAccounts().toMutableList()
        val index = list.indexOfFirst { it.id == id }

        if (index == -1) return null

        val removed = list.removeAt(index)
        saveAll(list)
        return removed
    }

    fun updateChannel(accountId: String, newChannel: String) {
        val ch = newChannel.trim().removePrefix("#")
        if (ch.isBlank()) return

        val list = loadAccounts().toMutableList()
        val idx = list.indexOfFirst { it.id == accountId }
        if (idx == -1) return

        val old = list[idx]
        val updated = old.copy(channel = ch)
        list[idx] = updated

        saveAll(list)
    }

    fun reorderAccounts(orderedIds: List<String>) {
        val current = loadAccounts()
        if (current.isEmpty()) return

        val byId = current.associateBy { it.id }.toMutableMap()
        val reordered = mutableListOf<AccountConfig>()

        for (id in orderedIds) {
            val cfg = byId.remove(id)
            if (cfg != null) reordered += cfg
        }

        reordered += byId.values
        saveAll(reordered)
    }

    companion object {

        /** Preferences file used by releases that stored accounts in plain text. */
        internal const val LEGACY_PREFERENCES_NAME = "v2_accounts"

        /** Key holding the serialized account list inside the legacy file. */
        private const val LEGACY_KEY = "accounts_json"

        /** Serialized form of a stored account list containing no accounts. */
        private const val EMPTY_ACCOUNT_LIST = "[]"

        /** Builds the encrypted store and completes any pending upgrade. */
        private fun createProductionStore(context: Context): AccountJsonStore {
            val appContext = context.applicationContext
            val store = EncryptedAccountStore(appContext)
            migrateLegacyAccounts(appContext, store)
            return store
        }

        /**
         * Moves a legacy plain-text account list into the encrypted store, once.
         *
         * The legacy file is removed only after the encrypted write succeeded, so an
         * interrupted upgrade retries on the next launch instead of losing accounts.
         */
        private fun migrateLegacyAccounts(appContext: Context, store: AccountJsonStore) {
            if (!legacyPreferencesFile(appContext).exists()) return

            val legacyJson = runCatching {
                appContext
                    .getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .getString(LEGACY_KEY, null)
            }.getOrNull()

            val current = store.read()
            val pending = AccountStorageMigration.jsonToMigrate(
                current = current,
                legacyJson = legacyJson
            )

            if (pending == null) {
                /*
                 * The encrypted list is already authoritative, so the leftover
                 * plain-text copy is both redundant and the weaker of the two.
                 */
                if (current is AccountJsonLookup.Present) {
                    runCatching { appContext.deleteSharedPreferences(LEGACY_PREFERENCES_NAME) }
                }
                return
            }

            if (store.write(pending)) {
                runCatching { appContext.deleteSharedPreferences(LEGACY_PREFERENCES_NAME) }
            }
        }

        /** Locates the legacy file without opening it, to keep start-up cheap. */
        private fun legacyPreferencesFile(appContext: Context): File {
            return File(
                File(appContext.applicationInfo.dataDir, "shared_prefs"),
                "$LEGACY_PREFERENCES_NAME.xml"
            )
        }
    }
}
