// test/java/com.titanbackup/TestUtils.kt
package com.titanbackup

import com.titanbackup.model.*
import java.io.File

object TestUtils {

    fun createTestAppInfo(
        packageName: String = "com.example.test",
        appName: String = "Test App",
        dataSize: Long = 1024L * 1024 * 50, // 50MB
        apkSize: Long = 1024L * 1024 * 10   // 10MB
    ): AppInfo {
        return AppInfo(
            appId = AppId(packageName),
            packageName = packageName,
            appName = appName,
            versionName = "1.0.0",
            versionCode = 1L,
            isSystemApp = false,
            dataSize = dataSize,
            apkSize = apkSize,
            lastUpdateTime = System.currentTimeMillis(),
            icon = null
        )
    }

    fun createTestBackupRequest(
        appIds: List<AppId> = listOf(AppId("com.example.test")),
        components: Set<BackupComponent> = setOf(BackupComponent.APK, BackupComponent.DATA),
        compressionLevel: Int = 6,
        encryptionEnabled: Boolean = false
    ): BackupRequest {
        return BackupRequest(
            appIds = appIds,
            components = components,
            incremental = false,
            compressionLevel = compressionLevel,
            encryptionEnabled = encryptionEnabled,
            description = "Test backup"
        )
    }

    fun createTestBackupMetadata(
        snapshotId: String = "test_snapshot_${System.currentTimeMillis()}",
        apps: List<AppId> = listOf(AppId("com.example.test")),
        totalSize: Long = 1024L * 1024 * 100 // 100MB
    ): BackupMetadata {
        return BackupMetadata(
            snapshotId = BackupId(snapshotId),
            timestamp = System.currentTimeMillis(),
            description = "Test backup",
            apps = apps,
            components = setOf(BackupComponent.APK, BackupComponent.DATA),
            compressionLevel = 6,
            encrypted = false,
            permissionMode = "ROOT",
            deviceInfo = DeviceInfo("TestDevice", "TestManufacturer", 30, "test_fingerprint"),
            totalSize = totalSize,
            checksums = mapOf("data.tar.zst" to "test_checksum"),
            merkleRootHash = "test_root_hash",
            merkleTreeJson = "{}"
        )
    }

    fun createTempDirectory(prefix: String = "titan_test"): File {
        return File(System.getProperty("java.io.tmpdir"), prefix).apply {
            mkdirs()
            deleteOnExit()
        }
    }

    fun createTempFile(directory: File, name: String, size: Long = 1024): File {
        return File(directory, name).apply {
            writeBytes(ByteArray(size.toInt()) { it.toByte() })
            deleteOnExit()
        }
    }
}
