package com.substream

import android.app.Application
import com.substream.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Global application class. It runs before any Activity.
 */
class SubstreamApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin Dependency Injection
        startKoin {
            androidContext(this@SubstreamApp)
            modules(appModule)
        }
    }
}