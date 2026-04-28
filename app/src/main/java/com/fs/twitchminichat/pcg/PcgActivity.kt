package com.fs.twitchminichat.pcg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.fs.twitchminichat.PcgManualDataUpdateController
import com.fs.twitchminichat.R

class PcgActivity : AppCompatActivity(R.layout.activity_pcg) {

    private var accountId: String = ""
    private var pcgManualDataUpdateController: PcgManualDataUpdateController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID).orEmpty()

        setupManualPcgDataUpdateButtons()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.pcgContainer,
                    PcgFragment().apply {
                        arguments = Bundle().apply {
                            putString("account_id", accountId)
                        }
                    }
                )
                .commit()
        }
    }

    /**
     * Connects the visible manual PCG data update buttons.
     *
     * These buttons live above the PCG GeckoView container, so the user clearly
     * chooses when to register Inventory or Pokédex data.
     */
    private fun setupManualPcgDataUpdateButtons() {
        val btnRegisterPokedex = findViewById<Button>(R.id.btnRegisterPokedex)
        val btnRegisterInventory = findViewById<Button>(R.id.btnRegisterInventory)

        val controller = PcgManualDataUpdateController(
            context = this,
            bridge = object : PcgManualDataUpdateController.Bridge {
                override fun requestManualPokedexUpdate(): Boolean {
                    if (accountId.isBlank()) return false

                    return GeckoSessionManager.requestManualPokedexUpdate(
                        context = this@PcgActivity,
                        accountId = accountId
                    )
                }

                override fun requestManualInventoryUpdate(): Boolean {
                    if (accountId.isBlank()) return false

                    return GeckoSessionManager.requestManualInventoryUpdate(
                        context = this@PcgActivity,
                        accountId = accountId
                    )
                }
            }
        )

        pcgManualDataUpdateController = controller

        btnRegisterPokedex.setOnClickListener {
            controller.onRegisterPokedexClicked()
        }

        btnRegisterInventory.setOnClickListener {
            controller.onRegisterInventoryClicked()
        }
    }

    override fun onDestroy() {
        pcgManualDataUpdateController = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ACCOUNT_ID = "account_id"

        fun start(ctx: Context, accountId: String) {
            ctx.startActivity(
                Intent(ctx, PcgActivity::class.java).apply {
                    putExtra(EXTRA_ACCOUNT_ID, accountId)
                }
            )
        }
    }
}