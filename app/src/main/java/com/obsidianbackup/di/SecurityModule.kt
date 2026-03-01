// di/SecurityModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.crypto.KeystoreManager
import com.obsidianbackup.crypto.ZeroKnowledgeManager
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.security.BiometricAuthManager
import com.obsidianbackup.security.CertificatePinningManager
import com.obsidianbackup.security.PasskeyManager
import com.obsidianbackup.security.RootDetectionManager
import com.obsidianbackup.security.SecureStorageManager
import com.obsidianbackup.security.TaskerSecurityValidator
import com.obsidianbackup.security.WebViewSecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecureStorageManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): SecureStorageManager {
        return SecureStorageManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideBiometricAuthManager(
        @ApplicationContext context: Context
    ): BiometricAuthManager {
        return BiometricAuthManager(context)
    }

    @Provides
    @Singleton
    fun provideRootDetectionManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): RootDetectionManager {
        return RootDetectionManager(context, logger)
    }

    @Provides
    @Singleton
    fun providePasskeyManager(
        @ApplicationContext context: Context
    ): PasskeyManager {
        return PasskeyManager(context)
    }

    @Provides
    @Singleton
    fun provideCertificatePinningManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): CertificatePinningManager {
        return CertificatePinningManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideWebViewSecurityManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): WebViewSecurityManager {
        return WebViewSecurityManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideZeroKnowledgeManager(
        @ApplicationContext context: Context
    ): ZeroKnowledgeManager {
        return ZeroKnowledgeManager(context)
    }
    
    // TaskerSecurityValidator uses @Inject constructor
}
