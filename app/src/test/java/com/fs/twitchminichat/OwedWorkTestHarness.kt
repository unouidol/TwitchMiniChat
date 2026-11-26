package com.fs.twitchminichat

import android.content.SharedPreferences
import java.io.File
import java.util.concurrent.TimeoutException

/*
 * The local test harness of the owed-work paths (#117, F3).
 *
 * It stands in for every collaborator [OwedWorkEnvironment] names, with the real
 * stores' logic over in-memory or temporary storage: [AccountRepository] over a fake
 * account store, [BackendSessionStore] over a temporary directory, and the owed-work and
 * acknowledgement stores over [ThreadSafeTestSharedPreferences]. Firebase and the
 * backend are modelled. PR-2 and PR-3 extend it with the concurrency they test.
 */

/**
 * A [SharedPreferences] in memory whose every call is atomic.
 *
 * Each read, and each editor's commit or apply, runs under one lock, as one call of the
 * platform's implementation does. Two threads can therefore interleave between calls -
 * between a store's read and its write - but never inside one, which is the shape of the
 * races F3 is about.
 */
internal class ThreadSafeTestSharedPreferences : SharedPreferences {

    private val lock = Any()
    private val values = LinkedHashMap<String, Any?>()

    override fun getAll(): Map<String, *> = synchronized(lock) { values.toMap() }

    override fun getString(key: String, defValue: String?): String? =
        synchronized(lock) { values[key] as? String ?: defValue }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        synchronized(lock) { (values[key] as? Set<String>)?.toSet() ?: defValues }

    override fun getInt(key: String, defValue: Int): Int =
        synchronized(lock) { values[key] as? Int ?: defValue }

    override fun getLong(key: String, defValue: Long): Long =
        synchronized(lock) { values[key] as? Long ?: defValue }

    override fun getFloat(key: String, defValue: Float): Float =
        synchronized(lock) { values[key] as? Float ?: defValue }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        synchronized(lock) { values[key] as? Boolean ?: defValue }

    override fun contains(key: String): Boolean = synchronized(lock) { key in values }

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = throw UnsupportedOperationException("not used by the stores under test")

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = throw UnsupportedOperationException("not used by the stores under test")

    private inner class Editor : SharedPreferences.Editor {

        private val changes = LinkedHashMap<String, Any?>()
        private val removals = LinkedHashSet<String>()
        private var clear = false

        private fun put(key: String, value: Any?) = apply {
            removals.remove(key)
            changes[key] = value
        }

        override fun putString(key: String, value: String?) = put(key, value)
        override fun putStringSet(key: String, values: Set<String>?) = put(key, values?.toSet())
        override fun putInt(key: String, value: Int) = put(key, value)
        override fun putLong(key: String, value: Long) = put(key, value)
        override fun putFloat(key: String, value: Float) = put(key, value)
        override fun putBoolean(key: String, value: Boolean) = put(key, value)

        override fun remove(key: String) = apply {
            changes.remove(key)
            removals.add(key)
        }

        override fun clear() = apply { clear = true }

        override fun commit(): Boolean {
            synchronized(lock) {
                if (clear) values.clear()
                removals.forEach(values::remove)
                changes.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
            }
            return true
        }

        override fun apply() {
            commit()
        }
    }
}

/** The encrypted account store, in memory; [writable] false refuses every write. */
internal class FakeAccountJsonStore : AccountJsonStore {

    @Volatile
    private var lookup: AccountJsonLookup = AccountJsonLookup.Missing

    @Volatile
    var writable = true

    override fun read(): AccountJsonLookup = lookup

    override fun write(json: String): AccountWriteOutcome {
        if (!writable) return AccountWriteOutcome.Failure("test_write_refused")
        lookup = AccountJsonLookup.Present(json)
        return AccountWriteOutcome.Success
    }

    override fun clear(): Boolean {
        lookup = AccountJsonLookup.Missing
        return true
    }
}

/** How one modelled token deletion ends when it is waited on. */
internal enum class FakeTokenDeletionOutcome {
    SUCCEEDS,
    FAILS,
    TIMES_OUT
}

/**
 * Firebase's two token calls, modelled.
 *
 * Each deletion takes the next outcome from [deletionOutcomes], or succeeds when none is
 * left. A test that needs to hold a deletion in flight replaces [awaitHook].
 */
internal class FakeFirebaseTokens : FirebaseTokenOperations {

    val deletionOutcomes = ArrayDeque<FakeTokenDeletionOutcome>()

    /** Timeouts the waits were given, one per deletion waited on. */
    val awaitedTimeouts = mutableListOf<Long>()

    @Volatile
    var startedDeletions = 0
        private set

    /** Runs inside each wait, before its outcome; empty by default. */
    @Volatile
    var awaitHook: () -> Unit = {}

    /** What [fetchToken] answers. */
    @Volatile
    var currentToken: String? = "tok-1"

    override fun startTokenDeletion(): FirebaseTokenDeletionHandle {
        val outcome = synchronized(this) {
            startedDeletions += 1
            deletionOutcomes.removeFirstOrNull() ?: FakeTokenDeletionOutcome.SUCCEEDS
        }

        return object : FirebaseTokenDeletionHandle {
            override fun await(timeoutSeconds: Long) {
                synchronized(this@FakeFirebaseTokens) { awaitedTimeouts += timeoutSeconds }
                awaitHook()
                when (outcome) {
                    FakeTokenDeletionOutcome.SUCCEEDS -> Unit
                    FakeTokenDeletionOutcome.FAILS -> throw IllegalStateException("deletion failed")
                    FakeTokenDeletionOutcome.TIMES_OUT -> throw TimeoutException("deletion timed out")
                }
            }
        }
    }

    override fun fetchToken(onComplete: (successful: Boolean, token: String?, error: Throwable?) -> Unit) {
        onComplete(true, currentToken, null)
    }
}

/** An [OwedWorkLog] that keeps its lines. */
internal class RecordingOwedWorkLog : OwedWorkLog {

    val lines = java.util.Collections.synchronizedList(mutableListOf<String>())

    override fun debug(message: String) {
        lines += "D $message"
    }

    override fun warn(message: String) {
        lines += "W $message"
    }
}

/**
 * The whole environment of one owed-work pass, for a local test.
 *
 * The disable sender models the backend: it records each request and, when the backend
 * acknowledges it, records the disabled selection as acknowledged, exactly as
 * `FcmRegistrationUploader.setProfileSpawnAlertModeBlocking` does on a 2xx answer.
 */
internal class TestOwedWorkEnvironment(sessionDirectory: File) : OwedWorkEnvironment {

    override val owedRecords = ThreadSafeTestSharedPreferences()
    override val acknowledgements = ThreadSafeTestSharedPreferences()

    val accountStore = FakeAccountJsonStore()
    override val accounts: AccountRepository
        get() = AccountRepository(accountStore)

    override val sessions = BackendSessionStore(sessionDirectory)

    /** Local selections by normalized profile; absent means the stores' defaults. */
    val selections = java.util.concurrent.ConcurrentHashMap<String, PcgProfileAlertSelection>()

    /** The file holding the cached token, and the key it is under. */
    val tokenCache = ThreadSafeTestSharedPreferences()
    val firebase = FakeFirebaseTokens()

    /** Profiles whose disable was sent, in order. */
    val sentDisables = java.util.Collections.synchronizedList(mutableListOf<String>())

    /** Whether the backend acknowledges a disable for a profile; yes by default. */
    @Volatile
    var backendAcknowledges: (String) -> Boolean = { true }

    override val log = RecordingOwedWorkLog()

    override fun readSelection(profileId: String): PcgProfileAlertSelection {
        return selections[AccountProfileIdResolver.normalize(profileId)]
            ?: DEFAULT_LOCAL_SELECTION
    }

    override fun deleteFirebaseToken(): Boolean {
        return FirebaseTokenDeletion.deleteBlocking(
            tokens = firebase,
            timeoutSeconds = TOKEN_DELETION_TIMEOUT_SECONDS,
            tokenCache = tokenCache,
            cachedTokenKey = CACHED_TOKEN_KEY,
            log = log
        )
    }

    override fun sendProfileAlertDisable(profileId: String): Boolean {
        sentDisables += profileId
        val acknowledged = backendAcknowledges(profileId)
        if (acknowledged) {
            PcgProfileAlertAcknowledgementStore.record(
                acknowledgements,
                profileId,
                PcgProfileAlertSelection.DISABLED
            )
        }
        return acknowledged
    }

    /** Signs an account in for [profileId], as a completed sign-in leaves it. */
    fun signIn(profileId: String, sessionToken: String = "session-$profileId") {
        check(accounts.addAccount(account(profileId)))
        check(sessions.putSession(profileId, sessionToken))
    }

    /** Removes [profileId]'s account, keeping its session, as a removal does. */
    fun removeAccount(profileId: String) {
        checkNotNull(accounts.removeById(accountId(profileId)))
    }

    /** Records [selection] as acknowledged by the backend for [profileId]. */
    fun acknowledge(profileId: String, selection: PcgProfileAlertSelection) {
        PcgProfileAlertAcknowledgementStore.record(acknowledgements, profileId, selection)
    }

    fun hasSession(profileId: String): Boolean =
        sessions.lookup(profileId) is BackendSessionLookup.Present

    companion object {
        const val TOKEN_DELETION_TIMEOUT_SECONDS = 20L
        const val CACHED_TOKEN_KEY = "latest_fcm_token"

        /** What the local stores read for a profile nobody configured. */
        val DEFAULT_LOCAL_SELECTION = PcgProfileAlertSelection(
            spawnSettings = PcgSpawnAlertSettings(
                regularMode = PcgSpawnAlertMode.DEFAULT,
                eventSpawnsEnabled = false
            ),
            mostWantedEnabled = false,
            altDexFormAlertsEnabled = false
        )

        /** An active selection: ordinary alerts on. */
        val ACTIVE_SELECTION = PcgProfileAlertSelection(
            spawnSettings = PcgSpawnAlertSettings(
                regularMode = PcgSpawnAlertMode.DEX_ONLY,
                eventSpawnsEnabled = false
            ),
            mostWantedEnabled = false,
            altDexFormAlertsEnabled = false
        )

        fun accountId(profileId: String) = "account-$profileId"

        fun account(profileId: String) = AccountConfig(
            id = accountId(profileId),
            username = "user-$profileId",
            channel = "chan-$profileId",
            accessToken = "access-$profileId",
            profileId = profileId
        )
    }
}
