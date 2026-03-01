// di/HealthModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.health.HealthConnectManager
import com.obsidianbackup.health.HealthDataExporter
import com.obsidianbackup.health.HealthDataStore
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.security.audit.SecurityAuditLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthModule {

    @Provides
    @Singleton
    fun provideHealthDataStore(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): HealthDataStore {
        return HealthDataStore(context, logger)
    }

    @Provides
    @Singleton
    fun provideHealthDataExporter(
        @ApplicationContext context: Context,
        securityAuditLogger: SecurityAuditLogger,   // ADD THIS PARAMETER — fixes H-10
        logger: ObsidianLogger
    ): HealthDataExporter {
        return HealthDataExporter(context, securityAuditLogger, logger)
    }

    @Provides
    @Singleton
    fun provideHealthConnectManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        healthDataExporter: HealthDataExporter,
        healthDataStore: HealthDataStore
    ): HealthConnectManager {
        return HealthConnectManager(
            context,
            logger,
            healthDataExporter,
            healthDataStore
        )
    }
}
