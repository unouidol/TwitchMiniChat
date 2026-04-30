package com.fs.twitchminichat.pcg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.fs.twitchminichat.PcgManualDataUpdateController
import com.fs.twitchminichat.R

class PcgActivity : AppCompatActivity(R.layout.activity_pcg) {

    private var accountId: String = ""
    private var pcgManualDataUpdateController: PcgManualDataUpdateController? = null

    private lateinit var btnRegisterInventory: Button
    private lateinit var progressInventoryCapture: ProgressBar

    private var lastInventoryTabAvailable: Boolean = false

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
     * Inventory uses a one-shot manual capture window:
     * - the WebExtension keeps emitting passive snapshots;
     * - Android accepts a snapshot only after the user taps Update inventory;
     * - after a successful read, the Inventory button stays disabled until PCG
     *   clearly leaves Inventory and enters Inventory again.
     *
     * Pokédex keeps the older temporary feedback for now, because this patch only
     * stabilizes Inventory first.
     */
    private fun setupManualPcgDataUpdateButtons() {
        val btnRegisterPokedex = findViewById<Button>(R.id.btnRegisterPokedex)
        btnRegisterInventory = findViewById(R.id.btnRegisterInventory)
        progressInventoryCapture = findViewById(R.id.progressInventoryCapture)

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

        /*
         * Inventory does not use fake temporary feedback anymore.
         *
         * The loading state is driven by GeckoSessionManager's manual capture
         * callbacks, so the UI reflects the real snapshot capture window.
         */
        btnRegisterInventory.setOnClickListener {
            controller.onRegisterInventoryClicked()
        }

        setupInventoryCaptureUiListener()
    }

    /**
     * Hooks the Inventory button/progress bar to GeckoSessionManager.
     *
     * PcgActivity owns the Android views, while GeckoSessionManager owns the passive
     * PCG probe messages. This listener is the thin bridge between them.
     */
    private fun setupInventoryCaptureUiListener() {
        btnRegisterInventory.isEnabled = false
        btnRegisterInventory.setText(R.string.pcg_inventory_open_tab_first)

        progressInventoryCapture.isVisible = false
        progressInventoryCapture.progress = 0

        GeckoSessionManager.setInventoryCaptureUiListener(
            accountId = accountId,
            listener = object : GeckoSessionManager.InventoryCaptureUiListener {
                override fun onInventoryTabAvailabilityChanged(isAvailable: Boolean) {
                    if (!isUiAlive()) return

                    lastInventoryTabAvailable = isAvailable

                    /*
                     * While a manual capture is running, the loading UI owns the
                     * button state. Do not overwrite "Reading inventory…" halfway.
                     */
                    if (progressInventoryCapture.isVisible) {
                        return
                    }

                    btnRegisterInventory.isEnabled = isAvailable
                    btnRegisterInventory.setText(
                        if (isAvailable) {
                            R.string.pcg_inventory_update
                        } else {
                            R.string.pcg_inventory_open_tab_first
                        }
                    )
                }

                override fun onInventoryCaptureStarted() {
                    if (!isUiAlive()) return

                    btnRegisterInventory.isEnabled = false
                    btnRegisterInventory.setText(R.string.pcg_inventory_reading)

                    progressInventoryCapture.progress = 0
                    progressInventoryCapture.isVisible = true
                }

                override fun onInventoryCaptureProgress(progress: Float) {
                    if (!isUiAlive()) return

                    progressInventoryCapture.progress =
                        (progress.coerceIn(0f, 1f) * progressInventoryCapture.max).toInt()
                }

                override fun onInventoryCaptureFinished() {
                    if (!isUiAlive()) return

                    progressInventoryCapture.isVisible = false
                    progressInventoryCapture.progress = 0

                    btnRegisterInventory.isEnabled = lastInventoryTabAvailable
                    btnRegisterInventory.setText(
                        if (lastInventoryTabAvailable) {
                            R.string.pcg_inventory_update
                        } else {
                            R.string.pcg_inventory_open_tab_first
                        }
                    )
                }
            }
        )
    }

    /**
     * Returns false when delayed Gecko/probe callbacks should no longer touch views.
     */
    private fun isUiAlive(): Boolean {
        return !isFinishing && !isDestroyed
    }

    /**
     * Gives immediate visual feedback for manual PCG update buttons.
     *
     * This is used only for Pokédex. Inventory has its own progress bar tied to the
     * manual snapshot capture window.
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
        if (accountId.isNotBlank()) {
            GeckoSessionManager.cancelManualInventoryUpdate(accountId)
            GeckoSessionManager.setInventoryCaptureUiListener(
                accountId = accountId,
                listener = null
            )
        }

        pcgManualDataUpdateController = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ACCOUNT_ID = "account_id"

        /**
         * Small delay used only for Pokédex visual button feedback.
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