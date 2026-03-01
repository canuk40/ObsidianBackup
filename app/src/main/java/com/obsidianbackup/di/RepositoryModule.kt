// di/RepositoryModule.kt
package com.obsidianbackup.di

import com.obsidianbackup.data.repository.AppRepository
import com.obsidianbackup.data.repository.BackupRepository
import com.obsidianbackup.data.repository.BackupProfileRepository
import com.obsidianbackup.data.repository.BackupProfileRepositoryImpl
import com.obsidianbackup.data.repository.CatalogRepository
import com.obsidianbackup.domain.repository.ICatalogRepository
import com.obsidianbackup.engine.BackupEngine
import com.obsidianbackup.scanner.AppScanner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        impl: CatalogRepository
    ): ICatalogRepository
    
    @Binds
    @Singleton
    abstract fun bindBackupProfileRepository(
        impl: BackupProfileRepositoryImpl
    ): BackupProfileRepository

    companion object {
        @Provides
        @Singleton
        fun provideBackupRepository(
            backupEngine: BackupEngine
        ): BackupRepository {
            return BackupRepository(backupEngine)
        }

        @Provides
        @Singleton
        fun provideAppRepository(
            appScanner: AppScanner
        ): AppRepository {
            return AppRepository(appScanner)
        }
    }
}
