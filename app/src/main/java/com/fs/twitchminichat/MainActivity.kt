package com.fs.twitchminichat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.fs.twitchminichat.pcg.PcgNotificationChannelManager
import com.google.firebase.messaging.FirebaseMessaging
import com.fs.twitchminichat.ui.input.PagerKeyboardDismissController


class MainActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var repo: AccountRepository
    private lateinit var adapter: AccountsPagerAdapter
    private var startupAfterDeletionCheckDone = false

    /** The acceptance gate, while it is on screen. Owned here so there is only one. */
    private var termsGateDialog: AlertDialog? = null

    private var pagerKeyboardDismissController: PagerKeyboardDismissController? = null
    private var pagerKeyboardDismissCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Applied before anything else can fail, so a crash during start-up is still
         * reported for users who left reporting on.
         */
        CrashReporting.applyStoredPreference(this)

        /*
         * Creating the alert channels at start-up also retires the ones earlier
         * versions left behind, so the cleanup does not depend on a push arriving.
         */
        PcgNotificationChannelManager.ensureChannels(this)

        setContentView(R.layout.activity_main)

        repo = AccountRepository(this)

        val vp = getViewPager()
        if (vp == null) {
            Log.e("MAIN", "ViewPager2 con id R.id.pager NOT found in activity_main.xml")
        }

        pager = findViewById(R.id.pager)
        pager.offscreenPageLimit = 1

        adapter = AccountsPagerAdapter(this, repo)
        pager.adapter = adapter

        pagerKeyboardDismissController = PagerKeyboardDismissController(pager)
        pagerKeyboardDismissCallback = pagerKeyboardDismissController?.installOn(pager)

        val openedFromIntent = handleIntent(intent)

        if (savedInstanceState == null && !openedFromIntent) {
            openSingleAccountOnStartupIfPossible()
        }

        RemoteDeletionChecker.checkOnceOnAppOpen(
            context = this,
            onNoDeletionDetected = {
                continueStartupAfterDeletionCheck()
            }
        )
    }

    private fun getViewPager(): ViewPager2? {
        return findViewById(R.id.pager)
    }

    fun goToLoginPage() {
        pager.currentItem = 0
    }

    private fun openSingleAccountOnStartupIfPossible() {
        val accounts = repo.loadAccounts()

        if (accounts.size != 1) {
            pager.setCurrentItem(0, false)
            return
        }

        val onlyAccount = accounts.first()
        val index = adapter.pageIndexForAccountId(onlyAccount.id)

        if (index >= 0) {
            pager.setCurrentItem(index, false)
            Log.d("MAIN", "Opening the single saved account on startup")
        } else {
            pager.setCurrentItem(0, false)
            Log.w("MAIN", "Single saved account has no page index")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent): Boolean {
        val directAccountId = intent.getStringExtra(EXTRA_NEW_ACCOUNT_ID)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (directAccountId != null) {
            return openAccountFromIntent(directAccountId)
        }

        val targetProfileId = intent.getStringExtra(EXTRA_TARGET_PROFILE_ID)
            ?: intent.getStringExtra(EXTRA_PROFILE_ID)

        val normalizedProfileId = targetProfileId
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }

        if (normalizedProfileId != null) {
            val accountId = findAccountIdForProfileId(normalizedProfileId)
            if (accountId != null) {
                return openAccountFromIntent(accountId)
            }

            Log.w("MAIN", "Notification target profile not found locally")
        }

        return false
    }

    private fun openAccountFromIntent(accountId: String): Boolean {
        adapter.reload()

        val index = adapter.pageIndexForAccountId(accountId)
        if (index >= 0) {
            pager.setCurrentItem(index, true)
            Log.d("MAIN", "Opened account requested by intent")
            return true
        }

        Log.w("MAIN", "Account requested by intent not found")
        return false
    }

    /**
     * Finds the saved account whose canonical profile matches [profileId].
     *
     * A migrated account is never matched through its username when an explicit
     * backend profile identifier is available.
     */
    private fun findAccountIdForProfileId(profileId: String): String? {
        val normalizedTarget = AccountProfileIdResolver.normalize(profileId)
        if (normalizedTarget.isBlank()) return null

        return repo.loadAccounts().firstOrNull { account ->
            AccountProfileIdResolver.resolve(account) == normalizedTarget
        }?.id
    }

    private fun continueStartupAfterDeletionCheck() {
        if (startupAfterDeletionCheckDone) return
        startupAfterDeletionCheckDone = true

        /*
         * Before anything touches registration. What is owed here is work that stops
         * a registration, so running it after fetchFcmToken could undo a registration
         * made moments earlier.
         */
        OwedServerOperationRunner.attemptNowAndSchedule(applicationContext)

        askNotificationPermissionIfNeeded()
        fetchFcmToken()
    }

    private class AccountsPagerAdapter(
        activity: FragmentActivity,
        private val repo: AccountRepository
    ) : FragmentStateAdapter(activity) {

        private var accounts: List<AccountConfig> = repo.loadAccounts()
        override fun getItemId(position: Int): Long {
            return if (position == 0) {
                Long.MIN_VALUE
            } else {
                accounts[position - 1].id.hashCode().toLong()
            }
        }

        override fun containsItem(itemId: Long): Boolean {
            if (itemId == Long.MIN_VALUE) return true
            return accounts.any { it.id.hashCode().toLong() == itemId }
        }
        fun reload() {
            val oldAccounts = accounts
            val newAccounts = repo.loadAccounts()

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = 1 + oldAccounts.size
                override fun getNewListSize(): Int = 1 + newAccounts.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    if (oldItemPosition == 0 && newItemPosition == 0) return true
                    if (oldItemPosition == 0 || newItemPosition == 0) return false

                    val oldAcc = oldAccounts[oldItemPosition - 1]
                    val newAcc = newAccounts[newItemPosition - 1]
                    return oldAcc.id == newAcc.id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    if (oldItemPosition == 0 && newItemPosition == 0) return true
                    if (oldItemPosition == 0 || newItemPosition == 0) return false

                    val oldAcc = oldAccounts[oldItemPosition - 1]
                    val newAcc = newAccounts[newItemPosition - 1]
                    return oldAcc == newAcc
                }
            })

            accounts = newAccounts
            diff.dispatchUpdatesTo(this)
        }

        override fun getItemCount(): Int = 1 + accounts.size

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                LoginFragment()
            } else {
                val cfg = accounts[position - 1]
                ChatFragment.newInstance(cfg.id)
            }
        }

        fun pageIndexForAccountId(id: String): Int {
            val idx = accounts.indexOfFirst { it.id == id }
            return if (idx >= 0) idx + 1 else -1
        }
    }

    companion object {
        const val ACTION_ACCOUNTS_CHANGED = "com.fs.twitchminichat.ACCOUNTS_CHANGED"

        const val EXTRA_NEW_ACCOUNT_ID = "new_account_id"
        const val EXTRA_TARGET_PROFILE_ID = "target_profile_id"
        const val EXTRA_PROFILE_ID = "profile_id"
    }

    fun openAccount(accountId: String) {
        adapter.reload()
        val idx = adapter.pageIndexForAccountId(accountId)
        if (idx >= 0) {
            pager.setCurrentItem(idx, true)
        }
    }

    private val accountsChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            adapter.reload()
            if (pager.currentItem >= adapter.itemCount) {
                pager.setCurrentItem(0, false)
            }

            fetchFcmToken()
        }
    }

    /**
     * Puts the acceptance gate in front of whatever page is showing, when the stored
     * acceptance is older than the current terms.
     *
     * Checked here rather than before the pages are routed: start-up is what every
     * user meets first, and a mistake there opens nothing for anyone, while a mistake
     * here is one dialog too many. Nothing about navigation changes.
     */
    override fun onResume() {
        super.onResume()
        requireTermsAccepted()
    }

    /**
     * Runs [onAccepted] when the current terms are accepted, and otherwise shows the
     * gate. Refusing closes this activity, wherever the gate was raised from.
     *
     * Every gesture that used to gate itself comes through here, so the guard against
     * a second dialog has a single owner: resuming behind an open gate - which happens
     * as soon as the user opens a policy page from it - must not raise another one.
     */
    fun requireTermsAccepted(onAccepted: () -> Unit = {}) {
        val accepted = TermsPrefs.hasAcceptedCurrentVersion(this)

        if (accepted) {
            onAccepted()
            return
        }

        if (
            !TermsGatePolicy.shouldShow(
                accepted = accepted,
                alreadyOnScreen = termsGateDialog?.isShowing == true
            )
        ) {
            return
        }

        val dialog = TermsGateDialog.create(
            activity = this,
            onAccepted = onAccepted,
            onDeclined = { finish() }
        )

        dialog.setOnDismissListener { termsGateDialog = null }
        termsGateDialog = dialog
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            accountsChangedReceiver,
            IntentFilter(ACTION_ACCOUNTS_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(accountsChangedReceiver)
    }

    /**
     * Unregisters ViewPager2 callbacks owned by this Activity.
     *
     * This prevents the pager from keeping references to the Activity after it is
     * destroyed.
     */
    override fun onDestroy() {
        /* The dialog holds this window; leaving it up would leak it on rotation. */
        termsGateDialog?.dismiss()
        termsGateDialog = null

        pagerKeyboardDismissCallback?.let { callback ->
            if (this::pager.isInitialized) {
                pager.unregisterOnPageChangeCallback(callback)
            }
        }

        pagerKeyboardDismissCallback = null
        pagerKeyboardDismissController = null

        super.onDestroy()
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("FCM", "POST_NOTIFICATIONS granted = $isGranted")
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("FCM", "Notification permission already granted")
                }

                else -> {
                    requestNotificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    "FCM",
                    "Fetching Firebase Cloud Messaging registration token failed " +
                        "errorType=${DiagnosticError.typeOf(task.exception)}"
                )
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "Current registration token fetched")

            val accounts = repo.loadAccounts()
            if (accounts.isEmpty()) {
                Log.d("FCM", "No accounts configured, skip profile registration")
                return@addOnCompleteListener
            }

            val profileIds = accounts
                .map(AccountProfileIdResolver::resolve)
                .filter(String::isNotBlank)
                .distinct()

            Log.d("FCM", "Boot registration pass profileCount=${profileIds.size}")

            for (profileId in profileIds) {
                /*
                 * No skip here any more. This loop used to leave out every profile whose
                 * selection required no delivery, which is exactly the profile the
                 * server has to be told about: one that wants no alerts while the server
                 * may still hold an active mode. What to send is decided in one place,
                 * PcgProfileRegistrationSyncPlanner, which also decides when there is
                 * nothing left to send.
                 */
                FcmRegistrationUploader.uploadToken(applicationContext, token, profileId)
            }
        }
    }
}
