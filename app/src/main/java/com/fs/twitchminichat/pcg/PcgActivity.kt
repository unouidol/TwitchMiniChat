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
            showTemporaryButtonFeedback(
                button = btnRegisterPokedex,
                normalTextRes = R.string.pcg_register_pokedex,
                pendingTextRes = R.string.pcg_register_pokedex_checking
            )

            controller.onRegisterPokedexClicked()
        }

        btnRegisterInventory.setOnClickListener {
            showTemporaryButtonFeedback(
                button = btnRegisterInventory,
                normalTextRes = R.string.pcg_register_inventory,
                pendingTextRes = R.string.pcg_register_inventory_checking
            )

            controller.onRegisterInventoryClicked()
        }
    }

    /**
     * Gives immediate visual feedback for manual PCG update buttons.
     *
     * This intentionally is not a success message. It only confirms that the tap
     * was received while GeckoSessionManager waits for the probe result.
     */
    private fun showTemporaryButtonFeedback(
        button: Button,
        normalTextRes: Int,
        pendingTextRes: Int
    ) {
        button.isEnabled = false
        button.setText(pendingTextRes)

        button.postDelayed(
            {
                if (!isFinishing && !isDestroyed) {
                    button.isEnabled = true
                    button.setText(normalTextRes)
                }
            },
            MANUAL_BUTTON_FEEDBACK_MS
        )
    }

    override fun onDestroy() {
        pcgManualDataUpdateController = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ACCOUNT_ID = "account_id"

        /**
         * Small delay used only for visual button feedback.
         *
         * The actual Inventory/Pokédex result is still handled asynchronously by
         * GeckoSessionManager and the PCG probe.
         */
        private const val MANUAL_BUTTON_FEEDBACK_MS = 4_000L

        fun start(ctx: Context, accountId: String) {
            ctx.startActivity(
                Intent(ctx, PcgActivity::class.java).apply {
                    putExtra(EXTRA_ACCOUNT_ID, accountId)
                }
            )
        }
    }
}