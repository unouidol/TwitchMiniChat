package com.fs.twitchminichat

import android.content.SharedPreferences
import java.io.File
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong

/*
 * The local test harness of the owed-work paths (#117, F3).
 *
 * It stands in for every collaborator [OwedWorkEnvironment] names, with the real
 * stores' logic over in-memory or temporary storage: [AccountRepository] over a fake
 * account store, [BackendSessionStore] over a temporary directory, and the owed-work and
 * acknowledgement stores over [ThreadSafeTestSharedPreferences]. Firebase and the
 * backend are modelled. PR-2 adds what its races need: a hook inside each commit, the log of
 * commits, a hook inside each modelled disable request, and the count of network calls made
 * while lock L was held. PR-3 adds the tokens that deletions remove, deletions that complete
 * after their wait timed out, and the coordinator's clock, which only the test moves.
 */

/** A coordinator for a test that needs neither its log nor its clock. */
internal fun testCoordinator(): OwedWorkCoordinator =
    OwedWorkCoordinator(log = RecordingOwedWorkLog(), nanoTime = { 0L })

/** One commit applied to a [ThreadSafeTestSharedPreferences]: who made it, and what it wrote. */
internal data class TestCommit(
    val thread: String,
    /** Each key the commit wrote, with its new value; null for a removal. */
    val changes: Map<String, Any?>
)

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

    /**
     * Runs at the start of every commit, on the committing thread, before anything is
     * written and outside this file's own lock. A test holds a store between its read and its
     * write by blocking here; empty by default.
     */
    @Volatile
    var beforeCommit: () -> Unit = {}

    /** Every commit applied, in the order applied. */
    val commits: MutableList<TestCommit> = java.util.Collections.synchronizedList(mutableListOf())

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
            beforeCommit()
            synchronized(lock) {
                if (clear) values.clear()
                removals.forEach(values::remove)
                changes.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
                commits += TestCommit(
                    thread = Thread.currentThread().name,
                    changes = removals.associateWith { null } + changes
                )
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
 *
 * A deletion that succeeds removes [currentToken], which it adds to [deletedTokens], and the
 * installation then has a new one, as Firebase issues after a deletion. One whose wait timed out
 * is still in flight until [completeTimedOutDeletions], which is Firebase completing it later.
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

    /** What [fetchToken] answers: the installation's token now. */
    @Volatile
    var currentToken: String? = "tok-1"

    /** The tokens deletions removed, in order. */
    val deletedTokens: MutableList<String> = java.util.Collections.synchronizedList(mutableListOf())

    private var issuedTokens = 1
    private val timedOut = mutableListOf<Deletion>()

    override fun startTokenDeletion(): FirebaseTokenDeletionHandle {
        val outcome = synchronized(this) {
            startedDeletions += 1
            deletionOutcomes.removeFirstOrNull() ?: FakeTokenDeletionOutcome.SUCCEEDS
        }
        return Deletion(outcome)
    }

    /** Completes, successfully, every deletion whose wait timed out, on the calling thread. */
    fun completeTimedOutDeletions() {
        val pending = synchronized(this) { timedOut.toList().also { timedOut.clear() } }
        pending.forEach { it.complete(succeeded = true) }
    }

    private fun removeCurrentToken() = synchronized(this) {
        currentToken?.let { deletedTokens += it }
        issuedTokens += 1
        currentToken = "tok-$issuedTokens"
    }

    /** One modelled deletion, and the listeners waiting for it to complete. */
    private inner class Deletion(
        private val outcome: FakeTokenDeletionOutcome
    ) : FirebaseTokenDeletionHandle {

        private var completed: Boolean? = null
        private val listeners = mutableListOf<(Boolean) -> Unit>()

        override fun await(timeoutSeconds: Long) {
            synchronized(this@FakeFirebaseTokens) { awaitedTimeouts += timeoutSeconds }
            awaitHook()
            when (outcome) {
                FakeTokenDeletionOutcome.SUCCEEDS -> complete(succeeded = true)
                FakeTokenDeletionOutcome.FAILS -> {
                    complete(succeeded = false)
                    throw IllegalStateException("deletion failed")
                }
                FakeTokenDeletionOutcome.TIMES_OUT -> {
                    synchronized(this@FakeFirebaseTokens) { timedOut += this }
                    throw TimeoutException("deletion timed out")
                }
            }
        }

        override fun onCompletion(listener: (succeeded: Boolean) -> Unit) {
            val done = synchronized(this) {
                if (completed == null) listeners += listener
                completed
            }
            if (done != null) listener(done)
        }

        fun complete(succeeded: Boolean) {
            if (succeeded) removeCurrentToken()
            val waiting = synchronized(this) {
                completed = succeeded
                listeners.toList().also { listeners.clear() }
            }
            waiting.forEach { it(succeeded) }
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
 * The disable sender models the backend: it records each request, fails without a backend
 * session as the real one does, and, when the backend acknowledges it, records the disabled
 * selection as acknowledged under lock L, exactly as
 * `FcmRegistrationUploader.setProfileSpawnAlertModeBlocking` does on a 2xx answer.
 *
 * Each modelled network call counts itself in [networkCallsUnderLock] if it is made while the
 * calling thread holds lock L, which the design forbids (T15 on #117).
 */
internal class TestOwedWorkEnvironment(sessionDirectory: File) : OwedWorkEnvironment {

    /** The coordinator's clock, in nanoseconds; it moves only when the test moves it. */
    val clock = AtomicLong(0L)

    /** The coordinator's own lines: one per section under L. */
    val lockLog = RecordingOwedWorkLog()

    override val coordinator = OwedWorkCoordinator(log = lockLog, nanoTime = clock::get)

    /** Network calls made while lock L was held; zero is the only correct value. */
    val networkCallsUnderLock = java.util.concurrent.atomic.AtomicInteger()

    /** Profiles a registration was requested for, in order (R5). */
    val requestedRegistrations = java.util.Collections.synchronizedList(mutableListOf<String>())

    /**
     * Runs inside each modelled disable request, after it is counted in [sentDisables] and
     * before the backend reads the session. A test holds a request in flight by blocking
     * here; empty by default.
     */
    @Volatile
    var beforeDisableAnswered: (profileId: String) -> Unit = {}

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
        countIfUnderLock()
        return FirebaseTokenDeletion.deleteBlocking(
            tokens = firebase,
            timeoutSeconds = TOKEN_DELETION_TIMEOUT_SECONDS,
            tokenCache = tokenCache,
            cachedTokenKey = CACHED_TOKEN_KEY,
            log = log,
            onLateSuccess = {
                LateTokenDeletion.completed(
                    coordinator = coordinator,
                    accounts = accounts,
                    tokenCache = tokenCache,
                    cachedTokenKey = CACHED_TOKEN_KEY,
                    requestRegistration = ::requestProfileRegistration,
                    log = log
                )
            }
        )
    }

    override fun sendProfileAlertDisable(profileId: String): Boolean {
        countIfUnderLock()
        sentDisables += profileId
        beforeDisableAnswered(profileId)
        if (sessions.lookup(profileId) !is BackendSessionLookup.Present) return false

        val acknowledged = backendAcknowledges(profileId)
        if (acknowledged) {
            coordinator.locked("test.backend-acknowledgement") {
                PcgProfileAlertAcknowledgementStore.record(
                    acknowledgements,
                    profileId,
                    PcgProfileAlertSelection.DISABLED
                )
            }
        }
        return acknowledged
    }

    override fun requestProfileRegistration(profileId: String) {
        countIfUnderLock()
        requestedRegistrations += profileId
    }

    private fun countIfUnderLock() {
        if (coordinator.lock.isHeldByCurrentThread) networkCallsUnderLock.incrementAndGet()
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
        coordinator.locked("test.acknowledge") {
            PcgProfileAlertAcknowledgementStore.record(acknowledgements, profileId, selection)
        }
    }

    /** The session stored for [profileId], or null. */
    fun sessionToken(profileId: String): String? =
        (sessions.lookup(profileId) as? BackendSessionLookup.Present)?.token

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
