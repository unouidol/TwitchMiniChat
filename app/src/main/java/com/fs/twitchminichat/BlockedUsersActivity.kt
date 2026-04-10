package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class BlockedUsersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_users)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.blockedUsersContainer, BlockedUsersFragment())
                .commit()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, BlockedUsersActivity::class.java))
        }
    }
}