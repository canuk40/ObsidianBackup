// di/GamingModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.gaming.EmulatorDetector
import com.obsidianbackup.gaming.GamingBackupManager
import com.obsidianbackup.gaming.PlayGamesCloudSync
import com.obsidianbackup.gaming.RomScanner
import com.obsidianbackup.gaming.SaveStateManager
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.storage.BackupCatalog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GamingModule {

    @Provides
    @Singleton
    fun provideEmulatorDetector(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): EmulatorDetector {
        return EmulatorDetector(context, logger)
    }

    @Provides
    @Singleton
    fun provideSaveStateManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): SaveStateManager {
        return SaveStateManager(context, logger)
    }

    @Provides
    @Singleton
    fun providePlayGamesCloudSync(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): PlayGamesCloudSync {
        return PlayGamesCloudSync(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideRomScanner(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): RomScanner {
        return RomScanner(context, logger)
    }

    @Provides
    @Singleton
    fun provideGamingBackupManager(
        @ApplicationContext context: Context,
        emulatorDetector: EmulatorDetector,
        saveStateManager: SaveStateManager,
        playGamesSync: PlayGamesCloudSync,
        backupCatalog: BackupCatalog,
        logger: ObsidianLogger
    ): GamingBackupManager {
        return GamingBackupManager(
            context,
            emulatorDetector,
            saveStateManager,
            playGamesSync,
            backupCatalog,
            logger
        )
    }
}
