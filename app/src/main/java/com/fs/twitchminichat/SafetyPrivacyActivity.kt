package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SafetyPrivacyActivity : AppCompatActivity(R.layout.activity_safety_privacy) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<Button>(R.id.btnReportBlock).setOnClickListener {
            PolicyPageActivity.open(
                context = this,
                title = "Report & Block",
                asset = "report_block.html",
                webUrl = WebPolicies.REPORT_BLOCK_URL
            )
        }

        findViewById<Button>(R.id.btnPrivacyPolicy).setOnClickListener {
            PolicyPageActivity.open(
                context = this,
                title = "Privacy Policy",
                asset = "privacy.html",
                webUrl = WebPolicies.PRIVACY_URL
            )
        }

        findViewById<Button>(R.id.btnTerms).setOnClickListener {
            PolicyPageActivity.open(
                context = this,
                title = "Terms of Use",
                asset = "terms.html",
                webUrl = WebPolicies.TERMS_URL
            )
        }

        findViewById<Button>(R.id.btnBlockedUsers).setOnClickListener {
            startActivity(Intent(this, BlockedUsersActivity::class.java))
        }

        findViewById<Button>(R.id.btnDataControl).setOnClickListener {
            DataAccountControlActivity.start(this)
        }

        findViewById<Button>(
            R.id.btnResetExternalLinkPreferences
        ).setOnClickListener {
            resetExternalLinkPreferences()
        }

        findViewById<Button>(R.id.btnCredits).setOnClickListener {
            PolicyPageActivity.open(
                context = this,
                title = "Credits",
                asset = "credits.html",
                webUrl = WebPolicies.CREDITS_URL
            )
        }
    }

    /** Restores the warning and browser chooser for future chat links. */
    private fun resetExternalLinkPreferences() {
        ExternalLinkPreferences(this).reset()

        Toast.makeText(
            this,
            R.string.external_link_preferences_reset,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SafetyPrivacyActivity::class.java))
        }
    }
}