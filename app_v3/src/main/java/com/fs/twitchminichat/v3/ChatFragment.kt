package com.fs.twitchminichat.v3

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class ChatFragment : Fragment(R.layout.fragment_chat) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val accountId = requireArguments().getString(ARG_ID).orEmpty()
        val repo = AccountRepository(requireContext())
        val cfg = repo.getById(accountId)

        view.findViewById<TextView>(R.id.tvChatTitle).text =
            if (cfg != null) "Chat placeholder: @${cfg.username} su #${cfg.channel}"
            else "Chat placeholder: account non trovato"
    }

    companion object {
        private const val ARG_ID = "id"

        fun newInstance(accountId: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply { putString(ARG_ID, accountId) }
            }
        }
    }
}
