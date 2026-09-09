package com.fs.twitchminichat.diagnostics

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Reads back the ranking Android assigned to this app's own spawn alerts.
 *
 * Every field recorded so far describes the request handed to the system, and on
 * the alerts that produced no sound all of them were permissive. The audio probe
 * established that the sound genuinely never played; it cannot say what decided
 * that. The ranking can: lastAudiblyAlertedMillis is the framework's own record
 * of whether a notification audibly alerted the user.
 *
 * Scope. A notification listener is bound to every notification on the device.
 * This one discards anything that is not this application's own PCG alert channel
 * before touching a single field, so nothing about any other app is read, kept or
 * written. That restraint is the reason the class can exist at all.
 *
 * Not for release. Even reading nothing, holding notification access is far more
 * than a chat app should ask for. This ships to the test device only and comes
 * out before users, ahead of the diagnostic journal itself.
 */
class NotificationAlertProbeService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onNotificationPosted(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?
    ) {
        val posted = sbn ?: return
        if (posted.packageName != packageName) return

        val channelId = runCatching { posted.notification?.channelId }.getOrNull()
        if (channelId == null || !channelId.startsWith(OWNED_CHANNEL_PREFIX)) return

        record("listener.posted", posted.key, channelId, rankingMap)

        /*
         * The audible alert is decided around the moment of posting, so reading
         * the ranking immediately would ask before the answer exists. A second
         * read once things have settled is the one that carries the verdict.
         */
        handler.postDelayed(
            { record("listener.settled", posted.key, channelId, currentRanking) },
            SETTLE_DELAY_MS
        )
    }

    private fun record(
        event: String,
        key: String?,
        channelId: String,
        rankingMap: RankingMap?
    ) {
        val ranking = Ranking()
        val found = runCatching {
            key != null && rankingMap?.getRanking(key, ranking) == true
        }.getOrDefault(false)

        if (!found) {
            HistoryDiagnosticsLog.record(
                applicationContext,
                event,
                "channelId" to channelId,
                "rankingFound" to false
            )
            return
        }

        /*
         * The instant the framework last alerted the user audibly for this
         * notification, or zero when it never did. This is the field the whole
         * investigation has been missing: a statement from the system about its
         * own decision, rather than an inference from what it was handed.
         */
        val lastAudiblyAlertedMs =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { ranking.lastAudiblyAlertedMillis }.getOrNull()
            } else {
                null
            }

        HistoryDiagnosticsLog.record(
            applicationContext,
            event,
            "channelId" to channelId,
            "audiblyAlerted" to lastAudiblyAlertedMs?.let { alerted -> alerted > 0L },
            "lastAudiblyAlertedMs" to lastAudiblyAlertedMs,
            "importance" to runCatching { ranking.importance }.getOrNull(),
            "matchesFilter" to runCatching { ranking.matchesInterruptionFilter() }.getOrNull(),
            "ambient" to runCatching { ranking.isAmbient }.getOrNull(),
            "suppressedEffects" to runCatching { ranking.suppressedVisualEffects }.getOrNull(),
            "suspended" to
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    runCatching { ranking.isSuspended }.getOrNull()
                } else {
                    null
                },
            "rank" to runCatching { ranking.rank }.getOrNull()
        )
    }

    private companion object {

        /** Namespace of the PCG alert channels; everything else is ignored. */
        const val OWNED_CHANNEL_PREFIX = "pcg_alerts_"

        /** Delay before the ranking is read back, past the alerting decision. */
        const val SETTLE_DELAY_MS = 2_500L
    }
}
