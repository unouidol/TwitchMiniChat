package com.fs.twitchminichat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Shows the list of locally hidden chat users and lets the user unhide them.
 *
 * When a user is unhidden, the parent BlockedUsersActivity is notified so the
 * chat screen can refresh message visibility after returning from this screen.
 */
class BlockedUsersFragment : Fragment(R.layout.fragment_blocked_users) {

    private lateinit var txtEmpty: TextView
    private lateinit var usersContainer: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtEmpty = view.findViewById(R.id.txtBlockedUsersEmpty)
        usersContainer = view.findViewById(R.id.blockedUsersListContainer)

        refreshBlockedUsers()
    }

    /**
     * Rebuilds the visible blocked-users list from HiddenUsersStore.
     */
    private fun refreshBlockedUsers() {
        val ctx = requireContext()
        val users = HiddenUsersStore.getAll(ctx).sorted()

        usersContainer.removeAllViews()

        txtEmpty.visibility = if (users.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        for (user in users) {
            val row = layoutInflater.inflate(
                R.layout.row_hidden_user,
                usersContainer,
                false
            )

            val txtUser = row.findViewById<TextView>(R.id.txtHiddenUser)
            val btnUnhide = row.findViewById<Button>(R.id.btnUnhideUser)

            txtUser.text = txtUser.context.getString(
                R.string.mention_username,
                user
            )

            btnUnhide.setOnClickListener {
                unhideUser(user)
            }

            usersContainer.addView(row)
        }
    }

    /**
     * Removes one user from the local hidden-users store.
     *
     * If the removal succeeds, the parent activity is marked as changed. This lets
     * ChatFragment re-apply HiddenUsersStore state when the user returns to chat.
     */
    private fun unhideUser(user: String) {
        val ctx = requireContext()
        val removed = HiddenUsersStore.remove(ctx, user)

        if (removed) {
            (activity as? BlockedUsersActivity)?.markBlockedUsersChanged()
        }

        Toast.makeText(
            ctx,
            ctx.getString(
                if (removed) {
                    R.string.hidden_user_unhidden
                } else {
                    R.string.hidden_user_not_found
                },
                user
            ),
            Toast.LENGTH_SHORT
        ).show()

        refreshBlockedUsers()
    }
}