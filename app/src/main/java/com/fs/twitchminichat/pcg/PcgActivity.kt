package com.fs.twitchminichat.pcg

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.fs.twitchminichat.PcgManualDataUpdateController
import com.fs.twitchminichat.R

class PcgActivity : AppCompatActivity(R.layout.activity_pcg) {

    private var accountId: String = ""
    private var pcgManualDataUpdateController: PcgManualDataUpdateController? = null

    private var pokedexProgressAnimator: ValueAnimator? = null
    private var pokedexRestoreRunnable: Runnable? = null
    private var inventoryProgressAnimator: ValueAnimator? = null
    private var inventoryRestoreRunnable: Runnable? = null

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
        val progressRegisterPokedex = findViewById<ProgressBar>(R.id.progressRegisterPokedex)
        val progressRegisterInventory = findViewById<ProgressBar>(R.id.progressRegisterInventory)

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
            if (accountId.isBlank()) return@setOnClickListener

            showPokedexButtonProgressFeedback(
                button = btnRegisterPokedex,
                progressBar = progressRegisterPokedex
            )

            controller.onRegisterPokedexClicked()
        }

        btnRegisterInventory.setOnClickListener {
            if (accountId.isBlank()) return@setOnClickListener

            showInventoryButtonProgressFeedback(
                button = btnRegisterInventory,
                progressBar = progressRegisterInventory
            )

            controller.onRegisterInventoryClicked()
        }
    }

    /**
     * Shows Inventory checking feedback with the same visual pattern used by Pokédex.
     *
     * This is UI-only. The actual Inventory result is still handled by
     * GeckoSessionManager and the passive PCG probe.
     */
    private fun showInventoryButtonProgressFeedback(
        button: Button,
        progressBar: ProgressBar
    ) {
        cancelInventoryButtonProgressFeedback(
            button = button,
            progressBar = progressBar,
            resetButton = false
        )

        button.isEnabled = false
        button.setText(R.string.pcg_register_inventory_checking)

        progressBar.max = PROGRESS_BAR_MAX
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE

        inventoryProgressAnimator = ValueAnimator.ofInt(0, PROGRESS_BAR_MAX).apply {
            duration = INVENTORY_BUTTON_FEEDBACK_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
            start()
        }

        val restoreRunnable = Runnable {
            restoreInventoryButtonFeedback(
                button = button,
                progressBar = progressBar
            )
        }

        inventoryRestoreRunnable = restoreRunnable
        button.postDelayed(restoreRunnable, INVENTORY_BUTTON_FEEDBACK_MS)
    }

    /**
     * Restores the Inventory button after the visual checking window finishes.
     */
    private fun restoreInventoryButtonFeedback(
        button: Button,
        progressBar: ProgressBar
    ) {
        inventoryProgressAnimator?.cancel()
        inventoryProgressAnimator = null
        inventoryRestoreRunnable = null

        if (!isFinishing && !isDestroyed) {
            button.isEnabled = true
            button.setText(R.string.pcg_register_inventory)

            progressBar.progress = 0
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * Cancels pending Inventory button feedback work.
     */
    private fun cancelInventoryButtonProgressFeedback(
        button: Button,
        progressBar: ProgressBar,
        resetButton: Boolean
    ) {
        inventoryProgressAnimator?.cancel()
        inventoryProgressAnimator = null

        inventoryRestoreRunnable?.let { runnable ->
            button.removeCallbacks(runnable)
        }
        inventoryRestoreRunnable = null

        if (resetButton && !isFinishing && !isDestroyed) {
            button.isEnabled = true
            button.setText(R.string.pcg_register_inventory)

            progressBar.progress = 0
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * Shows the longer Pokédex checking feedback.
     *
     * This is UI-only. The real result is still controlled by GeckoSessionManager
     * and the passive PCG probe. The duration is intentionally a little longer
     * than the manual Pokédex update timeout so the button does not become
     * clickable again while the probe result/toast is still pending.
     */
    private fun showPokedexButtonProgressFeedback(
        button: Button,
        progressBar: ProgressBar
    ) {
        cancelPokedexButtonProgressFeedback(
            button = button,
            progressBar = progressBar,
            resetButton = false
        )

        button.isEnabled = false
        button.setText(R.string.pcg_register_pokedex_checking)

        progressBar.max = PROGRESS_BAR_MAX
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE

        pokedexProgressAnimator = ValueAnimator.ofInt(0, PROGRESS_BAR_MAX).apply {
            duration = POKEDEX_BUTTON_FEEDBACK_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
            start()
        }

        val restoreRunnable = Runnable {
            restorePokedexButtonFeedback(
                button = button,
                progressBar = progressBar
            )
        }

        pokedexRestoreRunnable = restoreRunnable
        button.postDelayed(restoreRunnable, POKEDEX_BUTTON_FEEDBACK_MS)
    }

    /**
     * Restores the Pokédex button after the visual checking window finishes.
     */
    private fun restorePokedexButtonFeedback(
        button: Button,
        progressBar: ProgressBar
    ) {
        pokedexProgressAnimator?.cancel()
        pokedexProgressAnimator = null
        pokedexRestoreRunnable = null

        if (!isFinishing && !isDestroyed) {
            button.isEnabled = true
            button.setText(R.string.pcg_register_pokedex)

            progressBar.progress = 0
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * Cancels pending Pokédex button feedback work.
     *
     * This prevents delayed UI callbacks from touching a button after the Activity
     * is already being destroyed.
     */
    private fun cancelPokedexButtonProgressFeedback(
        button: Button,
        progressBar: ProgressBar,
        resetButton: Boolean
    ) {
        pokedexProgressAnimator?.cancel()
        pokedexProgressAnimator = null

        pokedexRestoreRunnable?.let { runnable ->
            button.removeCallbacks(runnable)
        }
        pokedexRestoreRunnable = null

        if (resetButton && !isFinishing && !isDestroyed) {
            button.isEnabled = true
            button.setText(R.string.pcg_register_pokedex)

            progressBar.progress = 0
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * Gives immediate visual feedback for the Inventory update button.
     *
     * Inventory keeps the shorter feedback because its probe usually responds
     * faster and does not need the longer Pokédex filter/snapshot window.
     */
    private fun showTemporaryInventoryButtonFeedback(
        button: Button
    ) {
        inventoryRestoreRunnable?.let { runnable ->
            button.removeCallbacks(runnable)
        }

        button.isEnabled = false
        button.setText(R.string.pcg_register_inventory_checking)

        val restoreRunnable = Runnable {
            if (!isFinishing && !isDestroyed) {
                button.isEnabled = true
                button.setText(R.string.pcg_register_inventory)
            }
        }

        inventoryRestoreRunnable = restoreRunnable
        button.postDelayed(restoreRunnable, INVENTORY_BUTTON_FEEDBACK_MS)
    }

    override fun onDestroy() {
        val btnRegisterPokedex = findViewById<Button?>(R.id.btnRegisterPokedex)
        val progressRegisterPokedex = findViewById<ProgressBar?>(R.id.progressRegisterPokedex)
        val btnRegisterInventory = findViewById<Button?>(R.id.btnRegisterInventory)

        if (btnRegisterPokedex != null && progressRegisterPokedex != null) {
            cancelPokedexButtonProgressFeedback(
                button = btnRegisterPokedex,
                progressBar = progressRegisterPokedex,
                resetButton = false
            )
        }

        if (btnRegisterInventory != null) {
            inventoryRestoreRunnable?.let { runnable ->
                btnRegisterInventory.removeCallbacks(runnable)
            }
        }
        inventoryRestoreRunnable = null

        pcgManualDataUpdateController = null
        super.onDestroy()

        val progressRegisterInventory = findViewById<ProgressBar?>(R.id.progressRegisterInventory)

        if (btnRegisterInventory != null && progressRegisterInventory != null) {
            cancelInventoryButtonProgressFeedback(
                button = btnRegisterInventory,
                progressBar = progressRegisterInventory,
                resetButton = false
            )
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT_ID = "account_id"

        /**
         * ProgressBar max used for the manual Pokédex button feedback animation.
         */
        private const val PROGRESS_BAR_MAX = 1_000

        /**
         * Longer visual window for Register Pokédex.
         *
         * GeckoSessionManager waits around 6 seconds for a fresh valid Pokédex
         * snapshot/toast result, so the button feedback should not end earlier.
         */
        private const val POKEDEX_BUTTON_FEEDBACK_MS = 6_500L

        /**
         * Shorter visual window for Register Inventory.
         */
        private const val INVENTORY_BUTTON_FEEDBACK_MS = 6_500L

        fun start(ctx: Context, accountId: String) {
            ctx.startActivity(
                Intent(ctx, PcgActivity::class.java).apply {
                    putExtra(EXTRA_ACCOUNT_ID, accountId)
                }
            )
        }
    }
}