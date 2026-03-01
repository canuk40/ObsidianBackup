// api/plugin/ExportPlugin.kt
package com.obsidianbackup.api.plugin

import java.io.File
import com.obsidianbackup.model.BackupId

/**
 * Plugin that provides custom export formats
 * Examples: CSV reports, NDJSON logs, custom org schemas
 */
interface ExportPlugin : ObsidianBackupPlugin {

    /**
     * Supported export formats
     */
    val supportedFormats: List<ExportFormat>

    /**
     * Export catalog to custom format
     */
    suspend fun exportCatalog(
        format: ExportFormat,
        destination: File,
        options: ExportOptions = ExportOptions()
    ): PluginResult<ExportResult>

    /**
     * Export specific snapshots
     */
    suspend fun exportSnapshots(
        snapshotIds: List<BackupId>,
        format: ExportFormat,
        destination: File,
        options: ExportOptions = ExportOptions()
    ): PluginResult<ExportResult>

    /**
     * Export logs
     */
    suspend fun exportLogs(
        startTime: Long,
        endTime: Long,
        format: ExportFormat,
        destination: File
    ): PluginResult<ExportResult>
}

/**
 * Export format descriptor
 */
data class ExportFormat(
    val id: String,
    val name: String,
    val extension: String,
    val mimeType: String,
    val description: String
)

/**
 * Options for export operations
 */
data class ExportOptions(
    val includeMetadata: Boolean = true,
    val includeChecksums: Boolean = true,
    val prettyPrint: Boolean = false,
    val customOptions: Map<String, String> = emptyMap()
)

/**
 * Result of an export operation
 */
data class ExportResult(
    val outputFile: File,
    val recordsExported: Int,
    val sizeBytes: Long,
    val durationMillis: Long
)
