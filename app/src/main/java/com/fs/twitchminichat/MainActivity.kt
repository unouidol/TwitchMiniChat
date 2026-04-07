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
import com.fs.twitchminichat.v2.R
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var repo: AccountRepository
    private lateinit var adapter: AccountsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askNotificationPermissionIfNeeded()

        repo = AccountRepository(this)
        fetchFcmToken()

        val vp = getViewPager()
        if (vp == null) {
            Log.e("MAIN", "ViewPager2 con id R.id.pager NOT found in activity_main.xml")
        }

        pager = findViewById(R.id.pager)
        pager.offscreenPageLimit = 1

        adapter = AccountsPagerAdapter(this, repo)
        pager.adapter = adapter

        handleIntent(intent)

    }

    private fun getViewPager(): ViewPager2? {
        return findViewById(R.id.pager)
    }

    fun goToLoginPage() {
        pager.currentItem = 0
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val newId = intent.getStringExtra("new_account_id")
        if (newId != null) {
            adapter.reload()
            val index = adapter.pageIndexForAccountId(newId)
            if (index >= 0) {
                pager.setCurrentItem(index, true)
            }
        }
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
                ChatFragment.Companion.newInstance(cfg.id)
            }
        }

        fun pageIndexForAccountId(id: String): Int {
            val idx = accounts.indexOfFirst { it.id == id }
            return if (idx >= 0) idx + 1 else -1
        }
    }

    companion object {
        const val ACTION_ACCOUNTS_CHANGED = "com.fs.twitchminichat.v2.ACCOUNTS_CHANGED"
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