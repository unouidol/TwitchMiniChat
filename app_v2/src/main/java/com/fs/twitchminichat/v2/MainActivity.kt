package com.fs.twitchminichat.v2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var repo: AccountRepository
    private lateinit var adapter: AccountsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = AccountRepository(this)

        pager = findViewById(R.id.pager)
        pager.offscreenPageLimit = 1

        adapter = AccountsPagerAdapter(this, repo)
        pager.adapter = adapter

        handleIntent(intent)
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
            if (index >= 0) pager.setCurrentItem(index, true)
        }
    }

    private class AccountsPagerAdapter(
        activity: FragmentActivity,
        private val repo: AccountRepository

    ) : FragmentStateAdapter(activity) {

        private var accounts: List<AccountConfig> = repo.loadAccounts()

        fun reload() {
            accounts = repo.loadAccounts()
            notifyDataSetChanged()
        }


        override fun getItemCount(): Int = 1 + accounts.size // 0 = login

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

}
