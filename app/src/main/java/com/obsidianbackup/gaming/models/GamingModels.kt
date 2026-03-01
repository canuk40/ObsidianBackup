// gaming/models/GamingModels.kt
package com.obsidianbackup.gaming.models

data class DetectedEmulator(
    val type: EmulatorType,
    val packageName: String,
    val name: String,
    val version: String,
    val isInstalled: Boolean,
    val savePaths: List<String>,
    val saveStatePaths: List<String>,
    val romPaths: List<String>,
    val supportedPlatforms: List<String>,
    val installedApkPath: String
)

enum class EmulatorType {
    RETROARCH,
    DOLPHIN,
    PPSSPP,
    DRASTIC,
    CITRA,
    AETHERSX2
}

data class GameInfo(
    val name: String,
    val platform: String,
    val romPath: String? = null,
    val romChecksum: String? = null,
    val region: String? = null,
    val playTimeMinutes: Long = 0
)

data class BackupOptions(
    val includeSaves: Boolean = true,
    val includeRoms: Boolean = false,
    val includeSaveStates: Boolean = true,
    val cloudSync: Boolean = false,
    val compression: Boolean = true,
    val encryption: Boolean = false
)

data class BackupResult(
    val backupId: String,
    val emulatorName: String,
    val totalGames: Int,
    val successfulBackups: Int,
    val failedBackups: Int,
    val backupPath: String,
    val timestamp: Long,
    val results: List<GameBackupResult>
)

data class GameBackupResult(
    val gameName: String,
    val success: Boolean,
    val error: String? = null,
    val savePath: String? = null,
    val romPath: String? = null,
    val metadata: Map<String, String>? = null
)

data class RestoreResult(
    val success: Boolean,
    val message: String
)

sealed class GamingBackupProgress {
    object Idle : GamingBackupProgress()
    object Scanning : GamingBackupProgress()
    data class Backing(val current: Int, val total: Int) : GamingBackupProgress()
    data class Completed(val successful: Int, val total: Int) : GamingBackupProgress()
    data class Error(val message: String) : GamingBackupProgress()
}

data class SaveState(
    val path: String,
    val label: String,
    val timestamp: Long,
    val screenshot: String? = null,
    val checksum: String
)

data class SpeedrunProfile(
    val gameName: String,
    val maxSaveStates: Int,
    val saveStates: List<SaveState>,
    val createdAt: Long,
    val lastUsed: Long = createdAt
)

data class MultiProfile(
    val gameName: String,
    val profiles: Map<Int, ProfileData>
)

data class ProfileData(
    val slotNumber: Int,
    val name: String,
    val savePath: String,
    val createdAt: Long,
    val lastModified: Long,
    val playTime: Long = 0
)

data class CloudSaveData(
    val gameName: String,
    val saveData: ByteArray,
    val metadata: Map<String, String>,
    val uploadedAt: Long,
    val version: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CloudSaveData

        if (gameName != other.gameName) return false
        if (!saveData.contentEquals(other.saveData)) return false
        if (metadata != other.metadata) return false
        if (uploadedAt != other.uploadedAt) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gameName.hashCode()
        result = 31 * result + saveData.contentHashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + uploadedAt.hashCode()
        result = 31 * result + version
        return result
    }
}
