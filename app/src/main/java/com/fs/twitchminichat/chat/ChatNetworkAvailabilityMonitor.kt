package com.fs.twitchminichat.chat

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Tells one chat page, on the main thread, each time the phone's default network
 * becomes available, between [start] and [stop].
 *
 * The page starts it in onStart and stops it in onStop, so a stopped page holds no
 * callback. It reads only that a default network exists: no address, no network name,
 * nothing that leaves the phone. It needs `android.permission.ACCESS_NETWORK_STATE`, a
 * normal permission granted at install with no prompt.
 *
 * Android calls [ConnectivityManager.NetworkCallback.onAvailable] once at registration
 * when a default network already exists, and again each time the default network
 * changes. The page acts on it only after a transient failure
 * ([ChatReconnectController.onNetworkAvailable]).
 */
class ChatNetworkAvailabilityMonitor(
    context: Context,
    private val onNetworkAvailable: () -> Unit
) {

    private val connectivityManager: ConnectivityManager? =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val mainHandler = Handler(Looper.getMainLooper())

    /* The registered callback; null while stopped. */
    private var callback: ConnectivityManager.NetworkCallback? = null

    /** Registers the callback, once; a failure is logged and leaves the backoff alone in charge. */
    fun start() {
        if (callback != null) return
        val manager = connectivityManager ?: return

        val registered = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                /* A callback already queued when stop() ran is not this page's any more. */
                if (callback !== this) return
                onNetworkAvailable()
            }
        }

        try {
            manager.registerDefaultNetworkCallback(registered, mainHandler)
            callback = registered
        } catch (error: RuntimeException) {
            Log.w(
                LOG_TAG,
                "network callback not registered error=${error.javaClass.simpleName}"
            )
        }
    }

    /** Unregisters the callback, if registered. */
    fun stop() {
        val registered = callback ?: return
        callback = null

        try {
            connectivityManager?.unregisterNetworkCallback(registered)
        } catch (_: IllegalArgumentException) {
            /* Already unregistered by the system: nothing left to do. */
        }
    }

    private companion object {

        /** The chat connection's log tag. */
        const val LOG_TAG = "TWITCH_IRC"
    }
}
