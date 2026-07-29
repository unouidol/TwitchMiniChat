package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts the operational local-data and server-data controls.
 *
 * The destructive workflow remains owned by [SafetyPrivacyFragment], so the
 * Safety & Privacy hub only provides navigation and cannot duplicate requests.
 */
class DataAccountControlActivity :
    AppCompatActivity(R.layout.activity_data_account_control) {

    /** Adds the operational data-control fragment on first creation only. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.dataAccountControlContainer,
                    SafetyPrivacyFragment()
                )
                .commit()
        }
    }

    /** Provides navigation to the operational data-control screen. */
    companion object {

        /** Opens the operational data-control screen. */
        fun start(context: Context) {
            context.startActivity(
                Intent(
                    context,
                    DataAccountControlActivity::class.java
                )
            )
        }
    }
}
