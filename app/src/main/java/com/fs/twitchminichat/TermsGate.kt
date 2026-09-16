package com.fs.twitchminichat

import android.app.Activity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog

/**
 * Decides whether the acceptance gate has to be put on screen.
 *
 * Kept apart from the dialog so the decision can be tested without a device. The
 * second term is the one that matters: the gate itself opens the policy pages in
 * their own activity, so the activity behind it resumes while the terms are still
 * unaccepted. Without it, every trip to a policy page would stack another dialog.
 */
internal object TermsGatePolicy {

    fun shouldShow(accepted: Boolean, alreadyOnScreen: Boolean): Boolean {
        return !accepted && !alreadyOnScreen
    }
}

/**
 * Builds the acceptance gate shown when the stored acceptance is older than the
 * current terms.
 *
 * The dialog is a gate rather than a prompt: it cannot be dismissed with back or
 * by touching outside, and refusing closes the application. Refusing must still
 * leave a way out with one's data, so it offers the Data Deletion page, which also
 * describes the request by email for someone who does not want to come back in.
 */
object TermsGateDialog {

    fun create(
        activity: Activity,
        onAccepted: () -> Unit,
        onDeclined: () -> Unit
    ): AlertDialog {
        val dialogView = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_terms_gate, null)

        val btnOpenTerms = dialogView.findViewById<Button>(R.id.btnOpenTermsFromGate)
        val btnOpenPrivacy = dialogView.findViewById<Button>(R.id.btnOpenPrivacyFromGate)
        val btnOpenDataDeletion =
            dialogView.findViewById<Button>(R.id.btnOpenDataDeletionFromGate)
        val checkAccept = dialogView.findViewById<CheckBox>(R.id.checkAcceptTerms)

        btnOpenTerms.setOnClickListener {
            PolicyPageActivity.open(
                context = activity,
                title = activity.getString(R.string.terms_of_use),
                asset = "terms.html",
                webUrl = WebPolicies.TERMS_URL
            )
        }

        btnOpenPrivacy.setOnClickListener {
            PolicyPageActivity.open(
                context = activity,
                title = activity.getString(R.string.privacy_policy),
                asset = "privacy.html",
                webUrl = WebPolicies.PRIVACY_URL
            )
        }

        btnOpenDataDeletion.setOnClickListener {
            PolicyPageActivity.open(
                context = activity,
                title = activity.getString(R.string.data_deletion),
                asset = "data_deletion.html",
                webUrl = WebPolicies.DATA_DELETION_URL
            )
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.terms_gate_title)
            .setView(dialogView)
            .setNegativeButton(R.string.terms_gate_decline, null)
            .setPositiveButton(R.string.terms_gate_accept, null)
            .create()

        /*
         * Both, and not one: touching outside was already refused, while back was
         * not, so the gate could be walked around with one more tap.
         */
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isEnabled = checkAccept.isChecked

            checkAccept.setOnCheckedChangeListener { _, isChecked ->
                positive.isEnabled = isChecked
            }

            positive.setOnClickListener {
                if (!checkAccept.isChecked) return@setOnClickListener

                TermsPrefs.markAcceptedCurrentVersion(activity)
                dialog.dismiss()
                onAccepted()
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
                onDeclined()
            }
        }

        return dialog
    }
}
