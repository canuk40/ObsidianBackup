package com.obsidianbackup.rootcore.di

import com.obsidianbackup.rootcore.busybox.BusyBoxManager
import com.obsidianbackup.rootcore.detection.RootDetector
import com.obsidianbackup.rootcore.detection.RootDetectorImpl
import com.obsidianbackup.rootcore.magisk.MagiskDetector
import com.obsidianbackup.rootcore.permissions.RootPermissionGranter
import com.obsidianbackup.rootcore.selinux.SELinuxHelper
import com.obsidianbackup.rootcore.selinux.SELinuxRestoreHelper
import com.obsidianbackup.rootcore.shell.PersistentShellSession
import com.obsidianbackup.rootcore.shell.ShellExecutor
import com.obsidianbackup.rootcore.storage.StorageDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module providing root-core infrastructure singletons.
 *
 * Provides:
 * - [RootDetector] — 4-phase root detection with Magisk/KernelSU/APatch/SELinux
 * - [ShellExecutor] — Validated shell execution with timeouts
 * - [PersistentShellSession] — HyperShell-style persistent root shell for batch ops
 * - [MagiskDetector] — Magisk module management
 * - [SELinuxHelper] — SELinux context operations
 * - [SELinuxRestoreHelper] — Post-restore SELinux context fixup
 * - [BusyBoxManager] — BusyBox binary discovery and extraction
 * - [StorageDetector] — 3-tier storage detection
 * - [RootPermissionGranter] — Auto-grant permissions via root
 */
@Module
@InstallIn(SingletonComponent::class)
object RootCoreModule {

    @Provides
    @Singleton
    fun provideRootDetector(impl: RootDetectorImpl): RootDetector = impl

    @Provides
    @Singleton
    fun provideShellExecutor(rootDetector: RootDetector): ShellExecutor =
        ShellExecutor(rootDetector)

    @Provides
    @Singleton
    fun providePersistentShellSession(rootDetector: RootDetector): PersistentShellSession =
        PersistentShellSession(rootDetector)

    @Provides
    @Singleton
    fun provideMagiskDetector(shellExecutor: ShellExecutor): MagiskDetector =
        MagiskDetector(shellExecutor)

    @Provides
    @Singleton
    fun provideSELinuxHelper(shellExecutor: ShellExecutor): SELinuxHelper =
        SELinuxHelper(shellExecutor)

    @Provides
    @Singleton
    fun provideSELinuxRestoreHelper(
        shellExecutor: ShellExecutor,
        seLinuxHelper: SELinuxHelper
    ): SELinuxRestoreHelper =
        SELinuxRestoreHelper(shellExecutor, seLinuxHelper)

    @Provides
    @Singleton
    fun provideRootPermissionGranter(shellExecutor: ShellExecutor): RootPermissionGranter =
        RootPermissionGranter(shellExecutor)
}
