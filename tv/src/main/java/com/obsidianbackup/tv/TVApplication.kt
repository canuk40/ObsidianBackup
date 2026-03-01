package com.obsidianbackup.tv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TVApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
