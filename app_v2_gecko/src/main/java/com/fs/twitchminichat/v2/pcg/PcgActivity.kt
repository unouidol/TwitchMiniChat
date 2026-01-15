package com.fs.twitchminichat.v2.pcg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fs.twitchminichat.v2.R

class PcgActivity : AppCompatActivity(R.layout.activity_pcg) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID).orEmpty()
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.pcgContainer,
                    PcgFragment().apply {
                        arguments = Bundle().apply { putString("account_id", accountId) }
                    }
                )
                .commit()
        }
}
    companion object {
        private const val EXTRA_ACCOUNT_ID = "account_id"

        fun start(ctx: Context, accountId: String) {
            ctx.startActivity(Intent(ctx, PcgActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT_ID, accountId)
            })
        }
    }
}
