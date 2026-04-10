package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SafetyPrivacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_privacy)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.safetyPrivacyContainer, SafetyPrivacyFragment())
                .commit()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(
                Intent(context, SafetyPrivacyActivity::class.java)
            )
        }
    }
}