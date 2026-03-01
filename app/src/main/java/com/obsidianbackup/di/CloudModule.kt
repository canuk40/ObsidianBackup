// di/CloudModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.cloud.CloudProvider
import com.obsidianbackup.cloud.GoogleDriveProvider
import com.obsidianbackup.cloud.OAuth2Manager
import com.obsidianbackup.cloud.WebDavCloudProvider
import com.obsidianbackup.cloud.WebDavConfig
import com.obsidianbackup.cloud.FilecoinCloudProvider
import com.obsidianbackup.cloud.FilecoinConfig
import com.obsidianbackup.crypto.KeystoreManager
import com.obsidianbackup.data.repository.CloudProviderRepository
import com.obsidianbackup.logging.ObsidianLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 50L * 1024 * 1024) // 50MB cache
        
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideKeystoreManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): KeystoreManager {
        return KeystoreManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideOAuth2Manager(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager,
        logger: ObsidianLogger
    ): OAuth2Manager {
        return OAuth2Manager(context, keystoreManager, logger)
    }

    @Provides
    @Singleton
    @Named("GoogleDrive")
    fun provideGoogleDriveProvider(
        @ApplicationContext context: Context,
        oauthManager: OAuth2Manager,
        logger: ObsidianLogger
    ): CloudProvider {
        return GoogleDriveProvider(context, oauthManager, logger)
    }

    @Provides
    @Singleton
    @Named("WebDAV")
    fun provideWebDavProvider(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        webDavConfig: dagger.Lazy<WebDavConfig>
    ): CloudProvider {
        // Lazy load config on first access to avoid blocking
        return WebDavCloudProvider(context, logger, webDavConfig.get())
    }

    @Provides
    @Singleton
    fun provideWebDavConfig(
        cloudProviderRepository: CloudProviderRepository
    ): WebDavConfig {
        // Load config with defaults, will be refreshed asynchronously
        return WebDavConfig(
            baseUrl = "",
            username = "",
            password = "",
            useDigestAuth = false
        )
    }

    @Provides
    @Singleton
    @Named("Filecoin")
    fun provideFilecoinProvider(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        filecoinConfig: FilecoinConfig
    ): CloudProvider {
        return FilecoinCloudProvider(context, logger, filecoinConfig)
    }

    @Provides
    @Singleton
    fun provideFilecoinConfig(
        cloudProviderRepository: CloudProviderRepository
    ): FilecoinConfig {
        // Load config with defaults, will be refreshed asynchronously
        return FilecoinConfig(
            web3StorageToken = "",
            ipfsGateways = emptyList(),
            enableFilecoinDeals = false,
            pinningService = "web3.storage"
        )
    }

    @Provides
    @Singleton
    @Named("default")
    fun provideDefaultCloudProvider(
        @Named("GoogleDrive") googleDriveProvider: CloudProvider
    ): CloudProvider {
        // Default to Google Drive for now
        return googleDriveProvider
    }
}
