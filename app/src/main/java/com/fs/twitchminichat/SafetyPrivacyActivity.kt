package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

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
            showDataDeletionMenu()
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
    /**
     * Shows the top-level Data Deletion menu.
     */
    private fun showDataDeletionMenu() {
        val actions = arrayOf(
            getString(R.string.data_deletion_ask),
            getString(R.string.data_deletion_remove_this_device),
            getString(R.string.data_deletion_remove_device_and_server_profiles)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.data_deletion_title)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> openDataDeletionPage()
                    1 -> showRemoveThisDeviceDataDialog()
                    2 -> showRemoveDeviceAndServerProfileDataDialog()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Opens the local Data Deletion policy page, with the web version available from
     * PolicyPageActivity's "Open web version" button.
     */
    private fun openDataDeletionPage() {
        PolicyPageActivity.open(
            context = this,
            title = getString(R.string.data_deletion_title),
            asset = "data_deletion.html",
            webUrl = WebPolicies.DATA_DELETION_URL
        )
    }

    private fun showRemoveThisDeviceDataDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.remove_this_device_data_title)
            .setMessage(R.string.remove_this_device_data_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove_this_device_data_confirm) { _, _ ->
                performRemoveThisDeviceData()
            }
            .show()
    }

    private fun showRemoveDeviceAndServerProfileDataDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.remove_device_server_profiles_title)
            .setMessage(R.string.remove_device_server_profiles_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove_device_server_profiles_confirm) { _, _ ->
                performRemoveDeviceAndServerProfileData()
            }
            .show()
    }

    private fun performRemoveThisDeviceData() {
        DeviceDataDeletionController.removeThisDeviceData(this) { result ->
            if (!result.ok) {
                Toast.makeText(
                    this,
                    getString(R.string.data_deletion_server_failed, result.message),
                    Toast.LENGTH_LONG
                ).show()
                return@removeThisDeviceData
            }

            Toast.makeText(
                this,
                getString(R.string.data_deletion_done),
                Toast.LENGTH_SHORT
            ).show()

            restartAppAfterDeletion()
        }
    }

    private fun performRemoveDeviceAndServerProfileData() {
        DeviceDataDeletionController.removeThisDeviceAndServerProfileData(this) { result ->
            if (!result.ok) {
                Toast.makeText(
                    this,
                    getString(R.string.data_deletion_server_failed, result.message),
                    Toast.LENGTH_LONG
                ).show()
                return@removeThisDeviceAndServerProfileData
            }

            Toast.makeText(
                this,
                getString(R.string.data_deletion_done),
                Toast.LENGTH_SHORT
            ).show()

            restartAppAfterDeletion()
        }
    }

    /**
     * Restarts the app after local data has been wiped.
     *
     * This prevents the current Activity/process from continuing to run with old
     * in-memory account/session state after SharedPreferences and Gecko data were
     * deleted.
     */
    private fun restartAppAfterDeletion() {
        val launchIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

        if (launchIntent != null) {
            startActivity(launchIntent)
        }

        finishAffinity()
    }
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SafetyPrivacyActivity::class.java))
        }
    }
}