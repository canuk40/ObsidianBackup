// di/AccessibilityModule.kt
package com.obsidianbackup.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AccessibilityModule {
    // VoiceControlHandler uses @Inject constructor, so Hilt handles it automatically
}
