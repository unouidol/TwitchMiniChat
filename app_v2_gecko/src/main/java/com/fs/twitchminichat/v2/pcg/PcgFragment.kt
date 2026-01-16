package com.fs.twitchminichat.v2.pcg

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.fs.twitchminichat.v2.AccountRepository
import com.fs.twitchminichat.v2.R
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class PcgFragment : Fragment(R.layout.fragment_pcg) {

    private lateinit var geckoView: GeckoView
    private lateinit var session: GeckoSession

    private var channel: String = ""

    private val PCG_EXTENSION_ID = "pm0qkv9g4h87t5y6lg329oam8j7ze9"
    private fun pcgUrl(): String =
        "https://www.twitch.tv/popout/$channel/extensions/$PCG_EXTENSION_ID/panel"

    private val TWITCH_LOGIN_URL = "https://www.twitch.tv/login"

    // Stato: stiamo aspettando che il login finisca?
    private var waitingForLogin = true
    private var alreadyJumpedToPcg = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geckoView = view.findViewById(R.id.geckoView)

        val accountId = requireArguments().getString(ARG_ACCOUNT_ID).orEmpty()

        val cfg = AccountRepository(requireContext()).getById(accountId)
        channel = cfg?.channel?.trim().orEmpty()
            .removePrefix("#")
            .lowercase()
            .ifBlank { "unouidol" }

        session = GeckoSessionManager.getOrCreateSession(requireContext(), accountId)
        geckoView.setSession(session)

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                p0: GeckoSession,
                p1: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny?>? {

                val uri = p1.uri ?: return GeckoResult.fromValue(AllowOrDeny.DENY)

                // resta dentro GeckoView solo per http/https
                val isWeb = uri.startsWith("http://") || uri.startsWith("https://")
                if (!isWeb) return GeckoResult.fromValue(AllowOrDeny.DENY)

                val lower = uri.lowercase()

                // Se siamo nel flusso login, quando Twitch tenta di portarci "fuori" dal login
                // (home / canale / ecc.), noi blocchiamo e saltiamo a PCG.
                if (waitingForLogin && !alreadyJumpedToPcg) {
                    val stillLoginFlow =
                        lower.startsWith("https://www.twitch.tv/login") ||
                                lower.contains("passport.twitch.tv") ||
                                lower.contains("id.twitch.tv") ||
                                lower.contains("accounts.twitch.tv")

                    val leavingToTwitchSite =
                        lower.startsWith("https://www.twitch.tv/") && !stillLoginFlow

                    if (leavingToTwitchSite) {
                        alreadyJumpedToPcg = true
                        waitingForLogin = false

                        // blocca il load della home e vai dritto al pannello PCG
                        session.loadUri(pcgUrl())
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                }

                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }
        }

        // ✅ apri login; se eri già loggato, Twitch proverà ad andare sulla home -> noi intercettiamo e apriamo PCG
        waitingForLogin = true
        alreadyJumpedToPcg = false
        session.loadUri(TWITCH_LOGIN_URL)
    }

    companion object {
        private const val ARG_ACCOUNT_ID = "account_id"

        fun newInstance(accountId: String): PcgFragment {
            return PcgFragment().apply {
                arguments = Bundle().apply { putString(ARG_ACCOUNT_ID, accountId) }
            }
        }
    }
}
