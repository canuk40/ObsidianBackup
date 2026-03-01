package com.obsidianbackup.deeplink

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger module for deep linking dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DeepLinkModule {
    
    @Provides
    @Singleton
    fun provideDeepLinkParser(): DeepLinkParser {
        return DeepLinkParser()
    }
    
    @Provides
    @Singleton
    fun provideDeepLinkAuthenticator(
        @ApplicationContext context: Context
    ): DeepLinkAuthenticator {
        return DeepLinkAuthenticator(context)
    }
    
    @Provides
    @Singleton
    fun provideDeepLinkRouter(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): DeepLinkRouter {
        return DeepLinkRouter(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideDeepLinkAnalytics(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): DeepLinkAnalytics {
        return DeepLinkAnalytics(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideDeepLinkSecurityValidator(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): DeepLinkSecurityValidator {
        return DeepLinkSecurityValidator(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideDeepLinkHandler(
        @ApplicationContext context: Context,
        parser: DeepLinkParser,
        authenticator: DeepLinkAuthenticator,
        router: DeepLinkRouter,
        analytics: DeepLinkAnalytics,
        securityValidator: DeepLinkSecurityValidator,
        logger: ObsidianLogger
    ): DeepLinkHandler {
        return DeepLinkHandler(context, parser, authenticator, router, analytics, securityValidator, logger)
    }
}
