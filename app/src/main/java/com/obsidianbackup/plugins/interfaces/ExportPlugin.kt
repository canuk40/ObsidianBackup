// plugins/interfaces/ExportPlugin.kt
package com.obsidianbackup.plugins.interfaces

import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.api.PluginMetadata
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ExportPlugin {

    val metadata: PluginMetadata

    /**
     * Get supported export formats
     */
    fun getSupportedFormats(): List<ExportFormat>

    /**
     * Export snapshot data
     */
    suspend fun exportSnapshot(
        snapshotId: SnapshotId,
        format: ExportFormat,
        destination: File
    ): ExportResult

    /**
     * Export catalog data
     */
    suspend fun exportCatalog(
        format: ExportFormat,
        destination: File,
        filter: CatalogFilter? = null
    ): ExportResult

    /**
     * Stream export for large datasets
     */
    fun exportStream(
        format: ExportFormat,
        filter: CatalogFilter? = null
    ): Flow<ExportChunk>

    /**
     * Validate export configuration
     */
    suspend fun validateConfig(config: ExportConfig): ValidationResult
}

data class ExportFormat(
    val id: String,
    val name: String,
    val description: String,
    val fileExtension: String,
    val supportsStreaming: Boolean = false,
    val compressionSupported: Boolean = true
)

data class ExportConfig(
    val format: ExportFormat,
    val includeMetadata: Boolean = true,
    val includeChecksums: Boolean = true,
    val compressionLevel: Int = 6,
    val customOptions: Map<String, Any> = emptyMap()
)

data class CatalogFilter(
    val dateRange: DateRange? = null,
    val appIds: Set<AppId>? = null,
    val snapshotIds: Set<SnapshotId>? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null
)

data class DateRange(
    val startDate: Long,
    val endDate: Long
)

data class ExportResult(
    val success: Boolean,
    val exportedFile: java.io.File? = null,
    val recordCount: Int = 0,
    val totalSize: Long = 0,
    val error: String? = null
)

data class ExportChunk(
    val data: ByteArray,
    val sequenceNumber: Int,
    val isLast: Boolean,
    val checksum: String
)

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)
