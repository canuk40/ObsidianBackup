// di/MLModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.ml.SmartScheduler
import com.obsidianbackup.ml.analytics.BackupAnalytics
import com.obsidianbackup.ml.context.ContextAwareManager
import com.obsidianbackup.ml.models.UserHabitModel
import com.obsidianbackup.ml.nlp.NaturalLanguageProcessor
import com.obsidianbackup.ml.prediction.BackupPredictor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides
    @Singleton
    fun provideContextDetector(
        @ApplicationContext context: Context
    ): ContextAwareManager {
        return ContextAwareManager(context)
    }

    @Provides
    @Singleton
    fun provideBackupPatternAnalyzer(
        @ApplicationContext context: Context
    ): BackupAnalytics {
        return BackupAnalytics(context)
    }

    @Provides
    @Singleton
    fun provideBackupIntentParser(
        @ApplicationContext context: Context
    ): NaturalLanguageProcessor {
        return NaturalLanguageProcessor(context)
    }

    @Provides
    @Singleton
    fun provideBackupPredictionModel(
        @ApplicationContext context: Context
    ): UserHabitModel {
        return UserHabitModel(context)
    }

    @Provides
    @Singleton
    fun provideOptimalTimePredictor(
        @ApplicationContext context: Context,
        contextDetector: ContextAwareManager,
        patternAnalyzer: BackupAnalytics
    ): BackupPredictor {
        return BackupPredictor(context)
    }

    @Provides
    @Singleton
    fun provideSmartScheduler(
        @ApplicationContext context: Context,
        contextAwareManager: ContextAwareManager,    // M-5: Already a singleton in the DI graph
        backupPredictor: BackupPredictor,            // M-5: Already provided by MLModule
        backupAnalytics: BackupAnalytics,            // M-5: Already a singleton
        naturalLanguageProcessor: NaturalLanguageProcessor  // M-5: Already provided
    ): SmartScheduler {
        return SmartScheduler(
            context = context,
            contextManager = contextAwareManager,
            backupPredictor = backupPredictor,
            analytics = backupAnalytics,
            nlpProcessor = naturalLanguageProcessor
        )
    }
}
