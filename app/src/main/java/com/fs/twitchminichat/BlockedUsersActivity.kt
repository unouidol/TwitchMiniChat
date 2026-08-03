package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fs.twitchminichat.ui.insets.SystemBarsInsetHelper

/**
 * Screen used to review and manage locally blocked chat users.
 *
 * The activity returns RESULT_OK when the blocked-user list changes, so callers
 * can refresh already-rendered chat message visibility when the user comes back.
 */
class BlockedUsersActivity : AppCompatActivity() {

    private var hasBlockedUsersChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Default result when the user opens and closes the screen without
         * changing the blocked-users list.
         */
        setResult(RESULT_CANCELED)

        setContentView(R.layout.activity_blocked_users)

        SystemBarsInsetHelper.enableEdgeToEdgeWithSafePadding(
            window = window,
            rootView = findViewById(R.id.blockedUsersContainer)
        )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.blockedUsersContainer, BlockedUsersFragment())
                .commit()
        }
    }

    /**
     * Called by BlockedUsersFragment after a user is unhidden.
     */
    fun markBlockedUsersChanged() {
        hasBlockedUsersChange = true
        setResult(RESULT_OK)
    }

    override fun finish() {
        setResult(
            if (hasBlockedUsersChange) {
                RESULT_OK
            } else {
                RESULT_CANCELED
            }
        )

        super.finish()
    }

    companion object {

        fun createIntent(context: Context): Intent {
            return Intent(context, BlockedUsersActivity::class.java)
        }

        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}
