// storage/BackupProfileEntity.kt
package com.obsidianbackup.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import com.obsidianbackup.model.BackupProfile
import com.obsidianbackup.model.BackupProfileId
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "backup_profiles")
data class BackupProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val appIds: String, // JSON array of app IDs
    val components: String, // JSON array of component names
    val incremental: Boolean,
    val compressionLevel: Int,
    val encryptionEnabled: Boolean,
    val isEnabled: Boolean,
    val scheduleEnabled: Boolean,
    val scheduleCron: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastBackupTimestamp: Long?,
    val cloudSyncEnabled: Boolean,
    val cloudProviderId: String?
)

/**
 * Convert entity to domain model
 */
fun BackupProfileEntity.toDomain(): BackupProfile {
    val json = Json { ignoreUnknownKeys = true }
    val appIdList = json.decodeFromString<List<String>>(appIds).map { AppId(it) }
    val componentSet = json.decodeFromString<List<String>>(components)
        .mapNotNull { componentName ->
            try {
                BackupComponent.valueOf(componentName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        .toSet()
    
    return BackupProfile(
        id = BackupProfileId(id),
        name = name,
        appIds = appIdList,
        components = componentSet,
        incremental = incremental,
        compressionLevel = compressionLevel,
        encryptionEnabled = encryptionEnabled,
        isEnabled = isEnabled,
        scheduleEnabled = scheduleEnabled,
        scheduleCron = scheduleCron,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastBackupTimestamp = lastBackupTimestamp,
        cloudSyncEnabled = cloudSyncEnabled,
        cloudProviderId = cloudProviderId
    )
}

/**
 * Convert domain model to entity
 */
fun BackupProfile.toEntity(): BackupProfileEntity {
    val json = Json { ignoreUnknownKeys = true }
    val appIdsJson = json.encodeToString(appIds.map { it.value })
    val componentsJson = json.encodeToString(components.map { it.name })
    
    return BackupProfileEntity(
        id = id.value,
        name = name,
        appIds = appIdsJson,
        components = componentsJson,
        incremental = incremental,
        compressionLevel = compressionLevel,
        encryptionEnabled = encryptionEnabled,
        isEnabled = isEnabled,
        scheduleEnabled = scheduleEnabled,
        scheduleCron = scheduleCron,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastBackupTimestamp = lastBackupTimestamp,
        cloudSyncEnabled = cloudSyncEnabled,
        cloudProviderId = cloudProviderId
    )
}
