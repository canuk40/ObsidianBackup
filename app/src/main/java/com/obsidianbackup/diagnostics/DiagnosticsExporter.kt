// diagnostics/DiagnosticsExporter.kt
package com.obsidianbackup.diagnostics

import android.content.Context
import android.os.Build
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.rootcore.busybox.BusyBoxManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger,
    private val busyBoxManager: BusyBoxManager
) {
    companion object {
        private const val TAG = "DiagnosticsExporter"
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }

    private val logsDir: File get() = context.getExternalFilesDir("logs") ?: context.filesDir
    private val appLogsDir: File get() = File(logsDir, "app_logs")
    private val shellAuditFile: File get() = File(logsDir, "shell_audit.log")
    private val exportDir: File get() = File(logsDir, "exports").also { it.mkdirs() }

    /** Returns app log file(s) ready to share. Null if no logs exist yet. */
    suspend fun getAppLogsFile(): File? = withContext(Dispatchers.IO) {
        val files = appLogsDir.listFiles()?.filter { it.isFile && it.length() > 0 }
        if (files.isNullOrEmpty()) {
            logger.w(TAG, "No app log files found at ${appLogsDir.absolutePath}")
            return@withContext null
        }
        if (files.size == 1) return@withContext files[0]

        val timestamp = DATE_FORMAT.format(Date())
        val zip = File(exportDir, "app_logs_$timestamp.zip")
        zipFiles(files, zip)
        zip
    }

    /** Returns the shell audit log file. Null if empty/missing. */
    suspend fun getShellAuditFile(): File? = withContext(Dispatchers.IO) {
        if (!shellAuditFile.exists() || shellAuditFile.length() == 0L) {
            logger.w(TAG, "Shell audit log empty or missing: ${shellAuditFile.absolutePath}")
            return@withContext null
        }
        shellAuditFile
    }

    /** Builds a full diagnostics ZIP: device info + app logs + shell audit. */
    suspend fun buildDiagnosticsBundle(): File = withContext(Dispatchers.IO) {
        val timestamp = DATE_FORMAT.format(Date())
        val bundleZip = File(exportDir, "obsidian_diagnostics_$timestamp.zip")

        ZipOutputStream(bundleZip.outputStream().buffered()).use { zip ->
            // Device info
            zip.putNextEntry(ZipEntry("device_info.txt"))
            zip.write(buildDeviceInfo().toByteArray())
            zip.closeEntry()

            // App logs
            appLogsDir.listFiles()?.filter { it.isFile && it.length() > 0 }?.forEach { f ->
                zip.putNextEntry(ZipEntry("app_logs/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }

            // Shell audit
            if (shellAuditFile.exists() && shellAuditFile.length() > 0) {
                zip.putNextEntry(ZipEntry("shell_audit.log"))
                shellAuditFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }

        logger.i(TAG, "Diagnostics bundle: ${bundleZip.name} (${bundleZip.length()} bytes)")
        bundleZip
    }

    private suspend fun buildDeviceInfo(): String = buildString {
        appendLine("=== ObsidianBackup Diagnostics ===")
        appendLine("Generated: ${Date()}")
        appendLine()
        appendLine("--- Device ---")
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        appendLine("Model: ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("ABI: ${Build.SUPPORTED_ABIS.firstOrNull()}")
        appendLine("Build: ${Build.DISPLAY}")
        appendLine()
        appendLine("--- App ---")
        appendLine("Package: ${context.packageName}")
        try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            appendLine("Version: ${pi.versionName} (${pi.longVersionCode})")
        } catch (_: Exception) {}
        appendLine()
        appendLine("--- BusyBox ---")
        val bbPath = busyBoxManager.getBusyBoxPath()
        if (bbPath != null) {
            appendLine("Path: $bbPath")
            appendLine("Version: ${busyBoxManager.getVersion() ?: "unknown"}")
            appendLine("Applets: ${busyBoxManager.listApplets().size}")
        } else {
            appendLine("Status: Not found")
        }
        appendLine()
        appendLine("--- Storage ---")
        appendLine("Internal: ${context.filesDir.absolutePath}")
        val ext = context.getExternalFilesDir(null)
        appendLine("External: ${ext?.absolutePath ?: "unavailable"}")
        if (ext != null) {
            appendLine("Free: ${ext.freeSpace / 1024 / 1024} MB")
            appendLine("Total: ${ext.totalSpace / 1024 / 1024} MB")
        }
    }

    private fun zipFiles(files: List<File>, output: File): File {
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            files.forEach { f ->
                zip.putNextEntry(ZipEntry(f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return output
    }
}
