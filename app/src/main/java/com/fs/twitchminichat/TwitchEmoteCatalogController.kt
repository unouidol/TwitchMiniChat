package com.fs.twitchminichat

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.concurrent.thread

/** Coordinates cached catalogs and bounded read-only refreshes for one chat account. */
class TwitchEmoteCatalogController(
    context: Context,
    private val accountId: String,
    private val loader: TwitchEmoteCatalogLoader = TwitchEmoteCatalogApi
) {
    private val store = TwitchEmoteCatalogStore(context)
    private val lock = Any()

    /** Main-thread dispatcher used for catalog UI notifications. */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Optional observer owned by the current chat view. */
    @Volatile
    private var catalogListener: ((TwitchEmoteCatalog) -> Unit)? = null

    @Volatile
    private var currentChannel: String = ""

    @Volatile
    private var currentCatalog: TwitchEmoteCatalog = TwitchEmoteCatalog.EMPTY

    @Volatile
    private var isClosed = false

    private var inFlightKey: String? = null
    private var lastSuccessfulKey: String? = null

    /** Selects a channel and primes its catalog synchronously from local storage. */
    fun selectChannel(channel: String) {
        val normalizedChannel = normalizeChannel(channel)
        currentChannel = normalizedChannel
        currentCatalog = store.load(accountId, normalizedChannel)
            ?: TwitchEmoteCatalog.EMPTY

        notifyCatalogChanged(currentCatalog)
    }

    /**
     * Refreshes one catalog once per account/channel/broadcaster tuple.
     *
     * This is a read-only operation. It never sends, queues, or retries a chat command.
     */
    fun refresh(
        accessToken: String,
        channel: String,
        broadcasterId: String
    ) {
        val normalizedChannel = normalizeChannel(channel)
        val normalizedBroadcasterId = broadcasterId.trim()
        if (
            isClosed ||
            accessToken.isBlank() ||
            normalizedChannel.isBlank() ||
            normalizedBroadcasterId.isBlank()
        ) {
            return
        }

        val requestKey = "$normalizedChannel:$normalizedBroadcasterId"
        synchronized(lock) {
            if (inFlightKey == requestKey) return

            val catalogAgeMs = System.currentTimeMillis() - currentCatalog.fetchedAtMs
            if (
                lastSuccessfulKey == requestKey &&
                catalogAgeMs in 0 until REFRESH_INTERVAL_MS
            ) {
                return
            }

            inFlightKey = requestKey
        }

        thread(name = "tmc-emotes-$normalizedChannel") {
            val result = try {
                loader.load(
                    accessToken = accessToken,
                    broadcasterId = normalizedBroadcasterId
                )
            } catch (error: Exception) {
                Log.w(TAG, "unexpected catalog refresh failure accountId=$accountId", error)
                TwitchEmoteCatalogLoadResult.Failed()
            }

            synchronized(lock) {
                if (inFlightKey == requestKey) {
                    inFlightKey = null
                }
            }

            if (isClosed) return@thread

            when (result) {
                is TwitchEmoteCatalogLoadResult.Success -> {
                    store.save(
                        accountId = accountId,
                        channel = normalizedChannel,
                        catalog = result.catalog
                    )

                    if (currentChannel == normalizedChannel) {
                        currentCatalog = result.catalog
                        notifyCatalogChanged(result.catalog)
                    }

                    synchronized(lock) {
                        lastSuccessfulKey = requestKey
                    }

                    Log.d(
                        TAG,
                        "catalog ready accountId=$accountId channel=$normalizedChannel " +
                                "count=${result.catalog.entries.size}"
                    )
                }

                TwitchEmoteCatalogLoadResult.MissingScope -> {
                    Log.i(
                        TAG,
                        "catalog unavailable accountId=$accountId missingScope=user:read:emotes"
                    )
                }

                is TwitchEmoteCatalogLoadResult.Failed -> {
                    Log.w(
                        TAG,
                        "catalog refresh failed accountId=$accountId " +
                                "status=${result.httpStatus ?: "network"}"
                    )
                }
            }
        }
    }

    /** Resolves outgoing emotes immediately without any network operation. */
    fun buildOutgoingIrcTag(message: String): String? {
        return TwitchOutgoingEmoteResolver.buildIrcTag(
            message = message,
            catalog = currentCatalog
        )
    }

    /**
     * Registers the observer used by the emote picker.
     *
     * Notifications are dispatched on the Android main thread. Passing null removes
     * the observer without changing the cached catalog.
     */
    fun setOnCatalogChangedListener(
        listener: ((TwitchEmoteCatalog) -> Unit)?
    ) {
        catalogListener = listener

        if (listener != null) {
            notifyCatalogChanged(currentCatalog)
        }
    }

    /**
     * Returns the immutable catalog currently selected for this account and channel.
     *
     * The picker receives only catalog data and never obtains the OAuth access token.
     */
    fun currentCatalogSnapshot(): TwitchEmoteCatalog {
        return currentCatalog
    }
    /** Invalidates future asynchronous results when the chat view is destroyed. */
    fun close() {
        isClosed = true
        catalogListener = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Delivers one catalog only while it is still the selected snapshot.
     */
    private fun notifyCatalogChanged(catalog: TwitchEmoteCatalog) {
        mainHandler.post {
            if (!isClosed && currentCatalog == catalog) {
                catalogListener?.invoke(catalog)
            }
        }
    }

    /** Normalizes one channel exactly as the chat and history layers do. */
    private fun normalizeChannel(channel: String): String {
        return channel.trim().removePrefix("#").lowercase()
    }

    companion object {
        private const val TAG = "TWITCH_EMOTES"
        private const val REFRESH_INTERVAL_MS = 15 * 60 * 1000L
    }
}
