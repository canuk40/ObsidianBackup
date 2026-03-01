package com.obsidianbackup.wear

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp

/**
 * Wear OS Application class with Hilt support
 */
@HiltAndroidApp
class WearApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
    }
}
