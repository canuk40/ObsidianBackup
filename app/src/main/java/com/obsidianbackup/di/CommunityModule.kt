// di/CommunityModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.community.AnalyticsManager
import com.obsidianbackup.community.BetaProgramManager
import com.obsidianbackup.community.ChangelogManager
import com.obsidianbackup.community.CommunityForumManager
import com.obsidianbackup.community.ConfigSharingManager
import com.obsidianbackup.community.FeedbackManager
import com.obsidianbackup.community.OnboardingManager
import com.obsidianbackup.community.TipsManager
import com.obsidianbackup.logging.ObsidianLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommunityModule {

    @Provides
    @Singleton
    fun provideAnalyticsManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): AnalyticsManager {
        return AnalyticsManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideFeedbackManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        analyticsManager: AnalyticsManager
    ): FeedbackManager {
        return FeedbackManager(context, logger, analyticsManager)
    }

    @Provides
    @Singleton
    fun provideOnboardingManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        analyticsManager: AnalyticsManager
    ): OnboardingManager {
        return OnboardingManager(context, logger, analyticsManager)
    }

    @Provides
    @Singleton
    fun provideTipsManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): TipsManager {
        return TipsManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideChangelogManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): ChangelogManager {
        return ChangelogManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideCommunityForumManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): CommunityForumManager {
        return CommunityForumManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideBetaProgramManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        analyticsManager: AnalyticsManager
    ): BetaProgramManager {
        return BetaProgramManager(context, logger, analyticsManager)
    }

    @Provides
    @Singleton
    fun provideConfigSharingManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        analyticsManager: AnalyticsManager
    ): ConfigSharingManager {
        return ConfigSharingManager(context, logger, analyticsManager)
    }
}
