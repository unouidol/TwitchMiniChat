package com.fs.twitchminichat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class BlockedUsersFragment : Fragment(R.layout.fragment_blocked_users) {

    private lateinit var txtEmpty: TextView
    private lateinit var usersContainer: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtEmpty = view.findViewById(R.id.txtBlockedUsersEmpty)
        usersContainer = view.findViewById(R.id.blockedUsersListContainer)

        refreshBlockedUsers()
    }

    private fun refreshBlockedUsers() {
        val ctx = requireContext()
        val users = HiddenUsersStore.getAll(ctx).sorted()

        usersContainer.removeAllViews()

        txtEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE

        for (user in users) {
            val row = layoutInflater.inflate(
                R.layout.row_hidden_user,
                usersContainer,
                false
            )

            val txtUser = row.findViewById<TextView>(R.id.txtHiddenUser)
            val btnUnhide = row.findViewById<Button>(R.id.btnUnhideUser)

            txtUser.text = "@$user"

            btnUnhide.setOnClickListener {
                val removed = HiddenUsersStore.remove(requireContext(), user)

                Toast.makeText(
                    requireContext(),
                    if (removed) {
                        "User unhidden: @$user"
                    } else {
                        "User not found: @$user"
                    },
                    Toast.LENGTH_SHORT
                ).show()

                refreshBlockedUsers()
            }

            usersContainer.addView(row)
        }
    }
}