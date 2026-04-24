package com.fs.twitchminichat.pcg

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.fs.twitchminichat.AccountRepository
import com.fs.twitchminichat.ProfileIdUtil
import com.fs.twitchminichat.R
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class PcgFragment : Fragment(R.layout.fragment_pcg) {

    private lateinit var geckoView: GeckoView
    private lateinit var session: GeckoSession

    private var accountId: String = ""
    private var channel: String = ""

    private fun pcgUrl(): String =
        "https://www.twitch.tv/popout/$channel/extensions/$PCG_EXTENSION_ID/panel"

    // Stato: stiamo aspettando che il login finisca?
    private var waitingForLogin = true
    private var alreadyJumpedToPcg = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geckoView = view.findViewById(R.id.geckoView)

        accountId = requireArguments().getString(ARG_ACCOUNT_ID).orEmpty()
        val cfg = AccountRepository(requireContext()).getById(accountId) ?: return

        channel = cfg.channel.trim()
            .removePrefix("#")
            .lowercase()
            .ifBlank { "unouidol" }

        val profileId = ProfileIdUtil.fromUsername(cfg.username)
        val profileLabel = cfg.username.trim().ifBlank { profileId }

        session = GeckoSessionManager.attachPcgSessionToView(
            context = requireContext(),
            geckoView = geckoView,
            profileId = profileId,
            profileLabel = profileLabel,
            accountId = accountId
        )

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny> {
                val uri = request.uri

                // Resta dentro GeckoView solo per http/https.
                val isWeb = uri.startsWith("http://") || uri.startsWith("https://")
                if (!isWeb) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                val lower = uri.lowercase()

                // Se siamo nel flusso login, quando Twitch tenta di portarci "fuori" dal login
                // home / canale / ecc., blocchiamo e saltiamo a PCG.
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

                        GeckoSessionManager.loadPcgUriIfNeeded(
                            accountId = accountId,
                            session = this@PcgFragment.session,
                            url = pcgUrl()
                        )

                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                }

                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }
        }

        val targetPcgUrl = pcgUrl()

        val pcgAlreadyLoaded = GeckoSessionManager.isPcgUriAlreadyLoaded(
            accountId = accountId,
            url = targetPcgUrl
        )

        if (pcgAlreadyLoaded) {
            waitingForLogin = false
            alreadyJumpedToPcg = true
            return
        }

        // Primo avvio o sessione non ancora arrivata al pannello PCG:
        // apri login; se eri già loggato, Twitch proverà ad andare sulla home
        // e il navigationDelegate sopra intercetterà il passaggio aprendo PCG.
        waitingForLogin = true
        alreadyJumpedToPcg = false

        GeckoSessionManager.loadPcgUriIfNeeded(
            accountId = accountId,
            session = session,
            url = TWITCH_LOGIN_URL
        )
    }

    override fun onStop() {
        if (this::geckoView.isInitialized && accountId.isNotBlank()) {
            runCatching {
                GeckoSessionManager.detachPcgSessionFromView(
                    geckoView = geckoView,
                    accountId = accountId
                )
            }
        }

        super.onStop()
    }

    override fun onDestroyView() {
        if (this::geckoView.isInitialized && accountId.isNotBlank()) {
            runCatching {
                GeckoSessionManager.detachPcgSessionFromView(
                    geckoView = geckoView,
                    accountId = accountId
                )
            }
        }

        super.onDestroyView()
    }

    companion object {
        private const val ARG_ACCOUNT_ID = "account_id"
        private const val PCG_EXTENSION_ID = "pm0qkv9g4h87t5y6lg329oam8j7ze9"
        private const val TWITCH_LOGIN_URL = "https://www.twitch.tv/login"
    }
}