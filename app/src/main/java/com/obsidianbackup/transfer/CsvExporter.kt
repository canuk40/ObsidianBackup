package com.obsidianbackup.transfer

import android.content.Context
import com.obsidianbackup.storage.BackupCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CSV export of backup database for external analysis.
 *
 * Exports backup snapshots, app metadata, labels, and schedules
 * as CSV files for import into spreadsheets or data tools.
 */
@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupCatalog: BackupCatalog
) {
    companion object {
        private const val TAG = "[CsvExport]"
    }

    data class ExportResult(
        val file: File,
        val rowCount: Int,
        val exportType: String
    )

    /**
     * Export all backup snapshots as CSV.
     */
    suspend fun exportSnapshots(outputFile: File): ExportResult = withContext(Dispatchers.IO) {
        val dao = backupCatalog.getAppBackupDao()
        val snapshots = dao.getAllDistinctAppIds()
        var rowCount = 0

        FileWriter(outputFile).use { writer ->
            writer.appendLine("package_name,snapshot_count")
            for (appId in snapshots) {
                val snapshotIds = dao.getSnapshotIdsForApp(appId)
                writer.appendLine("${escapeCsv(appId)},${snapshotIds.size}")
                rowCount++
            }
        }

        Timber.d("$TAG Exported $rowCount app snapshot records")
        ExportResult(outputFile, rowCount, "snapshots")
    }

    /**
     * Export labels and assignments as CSV.
     */
    suspend fun exportLabels(outputFile: File): ExportResult = withContext(Dispatchers.IO) {
        val labelDao = backupCatalog.getLabelDao()
        val labels = labelDao.getAllLabels().first()
        var rowCount = 0

        FileWriter(outputFile).use { writer ->
            writer.appendLine("label_id,label_name,color,assigned_apps")
            for (label in labels) {
                val apps = labelDao.getAppIdsForLabelSync(label.id)
                writer.appendLine("${label.id},${escapeCsv(label.name)},${label.color},${apps.joinToString(";")}")
                rowCount++
            }
        }

        Timber.d("$TAG Exported $rowCount labels")
        ExportResult(outputFile, rowCount, "labels")
    }

    /**
     * Export blacklist as CSV.
     */
    suspend fun exportBlacklist(outputFile: File): ExportResult = withContext(Dispatchers.IO) {
        val labelDao = backupCatalog.getLabelDao()
        val blacklist = labelDao.getAllBlacklisted().first()
        var rowCount = 0

        FileWriter(outputFile).use { writer ->
            writer.appendLine("package_name,mode,created_at")
            for (entry in blacklist) {
                writer.appendLine("${escapeCsv(entry.appId)},${entry.mode},${entry.createdAt}")
                rowCount++
            }
        }

        Timber.d("$TAG Exported $rowCount blacklist entries")
        ExportResult(outputFile, rowCount, "blacklist")
    }

    /**
     * Export everything to a directory: snapshots.csv, labels.csv, blacklist.csv.
     */
    suspend fun exportAll(outputDir: File): List<ExportResult> {
        outputDir.mkdirs()
        return listOf(
            exportSnapshots(File(outputDir, "snapshots.csv")),
            exportLabels(File(outputDir, "labels.csv")),
            exportBlacklist(File(outputDir, "blacklist.csv"))
        )
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
