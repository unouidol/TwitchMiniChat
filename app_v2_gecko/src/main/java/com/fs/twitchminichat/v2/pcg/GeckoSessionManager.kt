package com.fs.twitchminichat.v2.pcg

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import java.util.concurrent.ConcurrentHashMap

object GeckoSessionManager {

    @Volatile private var runtime: GeckoRuntime? = null
    private val sessions = ConcurrentHashMap<String, GeckoSession>()

    private fun getRuntime(appContext: Context): GeckoRuntime {
        val existing = runtime
        if (existing != null) return existing

        synchronized(this) {
            val again = runtime
            if (again != null) return again

            val rt = GeckoRuntime.create(appContext.applicationContext)
            runtime = rt
            return rt
        }
    }

    fun getOrCreateSession(context: Context, accountId: String): GeckoSession {
        sessions[accountId]?.let { return it }

        val ctx = context.applicationContext
        val rt = getRuntime(ctx)

        // 🔥 Isolamento storage per account:
        // contextId partiziona cookie jar + localStorage per ID
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .contextId("acc_$accountId")
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
            .build()

        val session = GeckoSession(settings)
        session.open(rt)

        sessions[accountId] = session
        return session
    }

    fun destroy(accountId: String) {
        sessions.remove(accountId)?.let { s ->
            try { s.close() } catch (_: Throwable) {}
        }
    }
}
