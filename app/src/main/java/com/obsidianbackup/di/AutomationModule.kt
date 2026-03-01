// di/AutomationModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.automation.BackupScheduler
import com.obsidianbackup.automation.ScheduleManager
import com.obsidianbackup.data.repository.ScheduleRepository
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.storage.BackupScheduleDao
import com.obsidianbackup.work.WorkManagerScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AutomationModule {

    @Provides
    @Singleton
    fun provideBackupScheduler(
        @ApplicationContext context: Context
    ): BackupScheduler {
        return BackupScheduler(context)
    }

    @Provides
    @Singleton
    fun provideScheduleRepository(
        backupCatalog: BackupCatalog
    ): ScheduleRepository {
        // Access DAO internally, not exposed in DI
        return ScheduleRepository(backupCatalog.getScheduleDao())
    }

    @Provides
    @Singleton
    fun provideScheduleManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): ScheduleManager {
        return ScheduleManager(context, logger)
    }
}
