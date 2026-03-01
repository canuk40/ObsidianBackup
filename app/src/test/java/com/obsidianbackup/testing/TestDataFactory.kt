package com.obsidianbackup.testing

import com.obsidianbackup.testing.TestFixtures.faker
import com.obsidianbackup.testing.TestFixtures.randomBoolean
import com.obsidianbackup.testing.TestFixtures.randomInt
import com.obsidianbackup.testing.TestFixtures.randomLong
import com.obsidianbackup.testing.TestFixtures.randomString
import com.obsidianbackup.testing.TestFixtures.randomUUID
import java.time.Instant

/**
 * Factory for creating test data objects with sensible defaults.
 */
object TestDataFactory {
    
    data class TestBackupConfig(
        val id: String = randomUUID(),
        val name: String = "Test Backup ${randomString(5)}",
        val sourcePath: String = "/storage/emulated/0/test",
        val destinationPath: String = "/backups/${randomString()}",
        val isEncrypted: Boolean = randomBoolean(),
        val compressionEnabled: Boolean = true,
        val scheduleEnabled: Boolean = false,
        val createdAt: Long = System.currentTimeMillis()
    )
    
    data class TestBackupSnapshot(
        val id: String = randomUUID(),
        val backupConfigId: String = randomUUID(),
        val timestamp: Long = System.currentTimeMillis(),
        val totalFiles: Int = randomInt(10, 1000),
        val totalSize: Long = randomLong(1024L, 1024L * 1024L * 100L),
        val status: String = "COMPLETED",
        val errorMessage: String? = null
    )
    
    data class TestAppInfo(
        val packageName: String = "com.example.${randomString()}",
        val appName: String = faker.app.name(),
        val versionName: String = "1.${randomInt(0, 99)}.${randomInt(0, 99)}",
        val versionCode: Int = randomInt(1, 1000),
        val installTime: Long = System.currentTimeMillis() - randomLong(0, 86400000L * 30),
        val updateTime: Long = System.currentTimeMillis(),
        val isSystemApp: Boolean = randomBoolean(),
        val dataSize: Long = randomLong(1024L, 1024L * 1024L * 100L)
    )
    
    data class TestCloudAccount(
        val id: String = randomUUID(),
        val provider: String = listOf("GoogleDrive", "Dropbox", "WebDAV", "S3").random(),
        val accountName: String = faker.internet.email(),
        val isConnected: Boolean = true,
        val lastSyncTime: Long = System.currentTimeMillis()
    )
    
    data class TestLogEntry(
        val id: Long = randomLong(1, 10000),
        val timestamp: Long = System.currentTimeMillis(),
        val level: String = listOf("DEBUG", "INFO", "WARN", "ERROR").random(),
        val tag: String = "Test",
        val message: String = faker.lorem.words(),
        val throwable: String? = null
    )
    
    data class TestAutomationRule(
        val id: String = randomUUID(),
        val name: String = "Rule ${randomString(5)}",
        val triggerType: String = listOf("SCHEDULED", "ON_INSTALL", "ON_UNINSTALL", "MANUAL").random(),
        val enabled: Boolean = true,
        val cronExpression: String? = "0 0 2 * * ?",
        val targetPackages: List<String> = List(randomInt(1, 5)) { "com.example.${randomString()}" }
    )
    
    fun createBackupConfig(
        id: String = randomUUID(),
        name: String = "Test Backup ${randomString(5)}",
        sourcePath: String = "/storage/emulated/0/test",
        destinationPath: String = "/backups/${randomString()}",
        isEncrypted: Boolean = randomBoolean(),
        compressionEnabled: Boolean = true,
        scheduleEnabled: Boolean = false
    ) = TestBackupConfig(
        id = id,
        name = name,
        sourcePath = sourcePath,
        destinationPath = destinationPath,
        isEncrypted = isEncrypted,
        compressionEnabled = compressionEnabled,
        scheduleEnabled = scheduleEnabled
    )
    
    fun createBackupSnapshot(
        id: String = randomUUID(),
        backupConfigId: String = randomUUID(),
        status: String = "COMPLETED"
    ) = TestBackupSnapshot(
        id = id,
        backupConfigId = backupConfigId,
        status = status
    )
    
    fun createAppInfo(
        packageName: String = "com.example.${randomString()}",
        isSystemApp: Boolean = false
    ) = TestAppInfo(
        packageName = packageName,
        isSystemApp = isSystemApp
    )
    
    fun createCloudAccount(
        provider: String = "GoogleDrive",
        isConnected: Boolean = true
    ) = TestCloudAccount(
        provider = provider,
        isConnected = isConnected
    )
    
    fun createMultipleBackupConfigs(count: Int): List<TestBackupConfig> {
        return List(count) { createBackupConfig() }
    }
    
    fun createMultipleAppInfos(count: Int): List<TestAppInfo> {
        return List(count) { createAppInfo() }
    }
}
