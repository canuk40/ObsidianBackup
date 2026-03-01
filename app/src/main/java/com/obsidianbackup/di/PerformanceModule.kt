// di/PerformanceModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.performance.BatteryOptimizationManager
import com.obsidianbackup.performance.ImageOptimizationManager
import com.obsidianbackup.performance.MemoryOptimizationManager
import com.obsidianbackup.performance.NetworkOptimizationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {

    @Provides
    @Singleton
    fun provideBatteryOptimizationManager(
        @ApplicationContext context: Context
    ): BatteryOptimizationManager {
        return BatteryOptimizationManager(context)
    }

    @Provides
    @Singleton
    fun provideMemoryOptimizationManager(
        @ApplicationContext context: Context
    ): MemoryOptimizationManager {
        return MemoryOptimizationManager(context)
    }

    @Provides
    @Singleton
    fun provideNetworkOptimizationManager(
        @ApplicationContext context: Context
    ): NetworkOptimizationManager {
        return NetworkOptimizationManager(context)
    }

    @Provides
    @Singleton
    fun provideImageOptimizationManager(
        @ApplicationContext context: Context
    ): ImageOptimizationManager {
        return ImageOptimizationManager(context)
    }
}
