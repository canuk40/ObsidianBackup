// di/SyncModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.sync.SyncthingApiClient
import com.obsidianbackup.sync.SyncthingConflictResolver
import com.obsidianbackup.sync.SyncthingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncthingApiClient(): SyncthingApiClient {
        return SyncthingApiClient()
    }

    @Provides
    @Singleton
    fun provideSyncthingConflictResolver(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): SyncthingConflictResolver {
        return SyncthingConflictResolver(context, logger)
    }

    @Provides
    @Singleton
    fun provideSyncthingManager(
        @ApplicationContext context: Context,
        syncthingApi: SyncthingApiClient,
        conflictResolver: SyncthingConflictResolver,
        logger: ObsidianLogger
    ): SyncthingManager {
        return SyncthingManager(context, syncthingApi, conflictResolver, logger)
    }
}
