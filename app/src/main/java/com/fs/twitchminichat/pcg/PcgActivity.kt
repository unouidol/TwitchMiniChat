package com.fs.twitchminichat.pcg

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fs.twitchminichat.PcgManualDataUpdateController
import com.fs.twitchminichat.R

class PcgActivity : AppCompatActivity(R.layout.activity_pcg) {

    private var accountId: String = ""

    private lateinit var btnRegisterPokedex: Button
    private lateinit var btnRegisterInventory: Button
    private lateinit var btnRefreshPcgExtension: ImageButton

    private lateinit var progressRegisterPokedex: ProgressBar
    private lateinit var progressRegisterInventory: ProgressBar

    private var latestManualUpdateButtonState: GeckoSessionManager.PcgManualUpdateButtonState? = null

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
     * chooses when to register Inventory data, Pokédex data, or refresh the PCG
     * extension surface.
     */
    private fun setupManualPcgDataUpdateButtons() {
        btnRegisterPokedex = findViewById(R.id.btnRegisterPokedex)
        btnRegisterInventory = findViewById(R.id.btnRegisterInventory)
        btnRefreshPcgExtension = findViewById(R.id.btnRefreshPcgExtension)

        progressRegisterPokedex = findViewById(R.id.progressRegisterPokedex)
        progressRegisterInventory = findViewById(R.id.progressRegisterInventory)

        applyManualUpdateButtonsDisabled()

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

        btnRefreshPcgExtension.setOnClickListener {
            refreshPcgExtensionFromUserTap()
        }

        if (accountId.isNotBlank()) {
            GeckoSessionManager.setManualUpdateButtonStateListener(
                accountId = accountId
            ) { state ->
                runOnUiThread {
                    applyManualUpdateButtonState(state)
                }
            }
        }
    }

    /**
     * Disables both manual update buttons until GeckoSessionManager reports a fresh
     * confirmed PCG tab state.
     */
    private fun applyManualUpdateButtonsDisabled() {
        btnRegisterInventory.isEnabled = false
        btnRegisterPokedex.isEnabled = false

        btnRegisterInventory.alpha = DISABLED_BUTTON_ALPHA
        btnRegisterPokedex.alpha = DISABLED_BUTTON_ALPHA
    }

    /**
     * Applies the latest PCG tab state to the manual update buttons.
     *
     * Inventory and Pokédex are mutually exclusive: only the button matching the
     * confirmed active PCG tab can be enabled. Button progress feedback still wins
     * temporarily, so a button stays disabled while its checking animation is shown.
     */
    private fun applyManualUpdateButtonState(
        state: GeckoSessionManager.PcgManualUpdateButtonState
    ) {
        latestManualUpdateButtonState = state
        applyLatestManualUpdateButtonState()
    }

    /**
     * Re-applies the last known button state after progress feedback starts/ends.
     *
     * This prevents restoreInventoryButtonFeedback(...) and
     * restorePokedexButtonFeedback(...) from accidentally enabling a button that
     * should remain disabled because the user is on the other PCG tab.
     */
    private fun applyLatestManualUpdateButtonState() {
        val state = latestManualUpdateButtonState

        if (state == null) {
            applyManualUpdateButtonsDisabled()
            return
        }

        val inventoryFeedbackRunning = inventoryProgressAnimator != null || inventoryRestoreRunnable != null
        val pokedexFeedbackRunning = pokedexProgressAnimator != null || pokedexRestoreRunnable != null

        val inventoryEnabled = state.inventoryEnabled && !inventoryFeedbackRunning
        val pokedexEnabled = state.pokedexEnabled && !pokedexFeedbackRunning

        btnRegisterInventory.isEnabled = inventoryEnabled
        btnRegisterPokedex.isEnabled = pokedexEnabled

        btnRegisterInventory.alpha = if (inventoryEnabled) {
            ENABLED_BUTTON_ALPHA
        } else {
            DISABLED_BUTTON_ALPHA
        }

        btnRegisterPokedex.alpha = if (pokedexEnabled) {
            ENABLED_BUTTON_ALPHA
        } else {
            DISABLED_BUTTON_ALPHA
        }
    }

    /**
     * Handles the user-triggered PCG extension refresh button.
     *
     * This refreshes only the visible PCG Gecko session. It does not automatically
     * register Inventory or Pokédex data; those remain separate manual actions.
     */
    private fun refreshPcgExtensionFromUserTap() {
        if (accountId.isBlank()) {
            Toast.makeText(
                this,
                R.string.missing_active_profile,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val started = GeckoSessionManager.refreshPcgExtension(
            accountId = accountId
        )

        Toast.makeText(
            this,
            if (started) {
                R.string.pcg_refresh_started
            } else {
                R.string.pcg_refresh_failed
            },
            Toast.LENGTH_SHORT
        ).show()
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
        cancelInventoryButtonProgressFeedback(button = button)

        button.isEnabled = false
        button.alpha = DISABLED_BUTTON_ALPHA
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
     * Restores the Inventory button and progress bar after the visual checking
     * window finishes.
     */
    private fun restoreInventoryButtonFeedback(
        button: Button,
        progressBar: ProgressBar
    ) {
        inventoryProgressAnimator?.cancel()
        inventoryProgressAnimator = null
        inventoryRestoreRunnable = null

        if (!isFinishing && !isDestroyed) {
            button.setText(R.string.pcg_register_inventory)

            progressBar.progress = 0
            progressBar.visibility = View.INVISIBLE

            applyLatestManualUpdateButtonState()
        }
    }

    /**
     * Cancels pending Inventory button feedback work.
     *
     * This only stops delayed/animated feedback. The progress bar itself is still
     * managed by showInventoryButtonProgressFeedback(...) and
     * restoreInventoryButtonFeedback(...).
     */
    private fun cancelInventoryButtonProgressFeedback(
        button: Button
    ) {
        inventoryProgressAnimator?.cancel()
        inventoryProgressAnimator = null

        inventoryRestoreRunnable?.let { runnable ->
            button.removeCallbacks(runnable)
        }
        inventoryRestoreRunnable = null
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
        cancelPokedexButtonProgressFeedback(button = button)

        button.isEnabled = false
        button.alpha = DISABLED_BUTTON_ALPHA
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
     * Restores the Pokédex button and progress bar after the visual checking
     * window finishes.
     */
    private fun restorePokedexButtonFeedback(
        button: Button,
        progressBar: ProgressBar
    ) {
        pokedexProgressAnimator?.cancel()
        pokedexProgressAnimator = null
        pokedexRestoreRunnable = null

        if (!isFinishing && !isDestroyed) {
            button.setText(R.string.pcg_register_pokedex)

            progressBar.progress = 0
            progressBar.visibility = View.INVISIBLE

            applyLatestManualUpdateButtonState()
        }
    }

    /**
     * Cancels pending Pokédex button feedback work.
     *
     * This only stops delayed/animated feedback. The progress bar itself is still
     * managed by showPokedexButtonProgressFeedback(...) and
     * restorePokedexButtonFeedback(...).
     */
    private fun cancelPokedexButtonProgressFeedback(
        button: Button
    ) {
        pokedexProgressAnimator?.cancel()
        pokedexProgressAnimator = null

        pokedexRestoreRunnable?.let { runnable ->
            button.removeCallbacks(runnable)
        }
        pokedexRestoreRunnable = null
    }

    override fun onDestroy() {
        if (accountId.isNotBlank()) {
            GeckoSessionManager.setManualUpdateButtonStateListener(
                accountId = accountId,
                listener = null
            )
        }

        if (this::btnRegisterPokedex.isInitialized) {
            cancelPokedexButtonProgressFeedback(
                button = btnRegisterPokedex
            )
        }

        if (this::btnRegisterInventory.isInitialized) {
            cancelInventoryButtonProgressFeedback(
                button = btnRegisterInventory
            )
        }

        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ACCOUNT_ID = "account_id"

        /**
         * ProgressBar max used for the manual update feedback animations.
         */
        private const val PROGRESS_BAR_MAX = 1_000

        /**
         * Visual opacity for enabled manual update buttons.
         */
        private const val ENABLED_BUTTON_ALPHA = 1.0f

        /**
         * Visual opacity for disabled manual update buttons.
         */
        private const val DISABLED_BUTTON_ALPHA = 0.45f

        /**
         * Visual window for Register Pokédex.
         *
         * GeckoSessionManager waits around 6 seconds for a fresh valid Pokédex
         * snapshot/toast result, so the button feedback should not end earlier.
         */
        private const val POKEDEX_BUTTON_FEEDBACK_MS = 6_500L

        /**
         * Visual window for Register Inventory.
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