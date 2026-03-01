// di/RcloneModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.cloud.rclone.RcloneConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RcloneModule {

    @Provides
    @Singleton
    fun provideRcloneConfigManager(
        @ApplicationContext context: Context
    ): RcloneConfigManager {
        val configFile = File(context.filesDir, "rclone/rclone.conf")
        return RcloneConfigManager(context, configFile)
    }
}
