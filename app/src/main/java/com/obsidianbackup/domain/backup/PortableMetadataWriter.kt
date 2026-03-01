package com.obsidianbackup.domain.backup

import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.model.BackupId
import timber.log.Timber
import java.io.File
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes portable .properties metadata alongside Room DB entries.
 * Inspired by Titanium Backup's .properties files — makes backups
 * readable/importable without the Room database.
 *
 * Format: standard Java Properties file with key=value pairs.
 */
@Singleton
class PortableMetadataWriter @Inject constructor(
    private val catalog: BackupCatalog,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "PortableMetadata"
        private const val METADATA_FILENAME = "backup.properties"
    }

    /**
     * Write a .properties file alongside the backup snapshot.
     */
    suspend fun writeMetadata(snapshotId: BackupId, backupDir: File) {
        try {
            val metadata = catalog.getSnapshot(snapshotId) ?: return

            val props = Properties()
            props.setProperty("snapshot.id", metadata.snapshotId.value)
            props.setProperty("snapshot.timestamp", metadata.timestamp.toString())
            props.setProperty("snapshot.description", metadata.description ?: "")
            props.setProperty("snapshot.encrypted", metadata.encrypted.toString())
            props.setProperty("snapshot.totalSize", metadata.totalSize.toString())
            props.setProperty("snapshot.compressionLevel", metadata.compressionLevel.toString())
            props.setProperty("snapshot.permissionMode", metadata.permissionMode)
            props.setProperty("snapshot.merkleRootHash", metadata.merkleRootHash ?: "")

            // Device info
            props.setProperty("device.model", metadata.deviceInfo.model)
            props.setProperty("device.manufacturer", metadata.deviceInfo.manufacturer)
            props.setProperty("device.apiLevel", metadata.deviceInfo.androidVersion.toString())
            props.setProperty("device.fingerprint", metadata.deviceInfo.buildFingerprint)

            // Apps list
            props.setProperty("apps.count", metadata.apps.size.toString())
            metadata.apps.forEachIndexed { index, appId ->
                props.setProperty("apps.$index", appId.value)
            }

            // Components
            props.setProperty("components", metadata.components.joinToString(",") { it.name })

            // Checksums
            metadata.checksums.forEach { (key, value) ->
                props.setProperty("checksum.$key", value)
            }

            // Write to file
            val propsFile = File(backupDir, METADATA_FILENAME)
            propsFile.outputStream().use { output ->
                props.store(output, "ObsidianBackup Snapshot Metadata — v1")
            }

            Timber.d("$TAG Written metadata to: ${propsFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "$TAG Failed to write portable metadata for ${snapshotId.value}")
        }
    }

    /**
     * Read a .properties metadata file from a backup directory.
     * Returns the properties map, or null if not found.
     */
    fun readMetadata(backupDir: File): Properties? {
        val propsFile = File(backupDir, METADATA_FILENAME)
        if (!propsFile.exists()) return null

        return try {
            Properties().apply {
                propsFile.inputStream().use { load(it) }
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG Failed to read metadata from: ${propsFile.absolutePath}")
            null
        }
    }
}
