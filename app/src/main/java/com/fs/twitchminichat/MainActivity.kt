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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var repo: AccountRepository
    private lateinit var adapter: AccountsPagerAdapter
    private var startupAfterDeletionCheckDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            Log.d("MAIN", "Opening single saved account on startup id=${onlyAccount.id}")
        } else {
            pager.setCurrentItem(0, false)
            Log.w("MAIN", "Single account exists but page index not found id=${onlyAccount.id}")
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

            Log.w("MAIN", "Notification target profile not found locally profileId=$normalizedProfileId")
        }

        return false
    }

    private fun openAccountFromIntent(accountId: String): Boolean {
        adapter.reload()

        val index = adapter.pageIndexForAccountId(accountId)
        if (index >= 0) {
            pager.setCurrentItem(index, true)
            Log.d("MAIN", "Opened account from intent accountId=$accountId")
            return true
        }

        Log.w("MAIN", "Account from intent not found accountId=$accountId")
        return false
    }

    private fun findAccountIdForProfileId(profileId: String): String? {
        val normalizedTarget = profileId.trim().lowercase()
        if (normalizedTarget.isBlank()) return null

        return repo.loadAccounts().firstOrNull { account ->
            val explicitProfileId = account.profileId
                .trim()
                .lowercase()

            val derivedProfileId = ProfileIdUtil.fromUsername(account.username)
                .trim()
                .lowercase()

            explicitProfileId == normalizedTarget || derivedProfileId == normalizedTarget
        }?.id
    }

    private fun continueStartupAfterDeletionCheck() {
        if (startupAfterDeletionCheckDone) return
        startupAfterDeletionCheckDone = true

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
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "Current token: $token")

            val accounts = repo.loadAccounts()
            if (accounts.isEmpty()) {
                Log.d("FCM", "No accounts configured, skip profile registration")
                return@addOnCompleteListener
            }

            val profileIds = accounts
                .map { ProfileIdUtil.fromUsername(it.username).trim().lowercase() }
                .filter { it.isNotBlank() }
                .distinct()

            for (profileId in profileIds) {
                val pushEnabled = PushSettingsStore.isPushEnabled(applicationContext, profileId)

                Log.d("FCM", "Boot check profileId=$profileId pushEnabled=$pushEnabled")

                if (!pushEnabled) {
                    Log.d("FCM", "Skip token registration for muted profileId=$profileId")
                    continue
                }

                Log.d("FCM", "Registering token for profileId=$profileId")
                FcmRegistrationUploader.uploadToken(applicationContext, token, profileId)
            }
        }
    }
}