package com.fs.twitchminichat.probe

import android.app.Application
import android.content.Context

/**
 * SCAFFOLD for the 5d closing measurement. Never merged. As at a091809.
 *
 * Exists only to take [StartupProbe]'s anchor as early as the process runs any code of
 * this app, and to time Application.onCreate.
 */
class ProbeApplication : Application() {

    override fun attachBaseContext(base: Context) {
        StartupProbe.onAttach(base)
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        StartupProbe.boundary(StartupProbe.B_APP_IN)
        StartupProbe.inject("app")
        super.onCreate()
        StartupProbe.boundary(StartupProbe.B_APP_OUT)
        StartupProbe.start(this)
    }
}
