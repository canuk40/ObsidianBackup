// di/TaskerModule.kt
package com.obsidianbackup.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object TaskerModule {
    // TaskerStatusProvider and TaskerIntegration use field injection via @Inject
    // and EntryPoint pattern for ContentProvider/BroadcastReceiver
}
