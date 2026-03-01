package com.obsidianbackup.transfer

import com.obsidianbackup.rootcore.shell.ShellExecutor
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TWRP-flashable update.zip creator.
 *
 * Creates a flashable ZIP that can restore apps from TWRP recovery.
 * The ZIP contains an updater-script and backed-up APKs + data that get
 * restored to the correct locations during flash.
 *
 * Inspired by Titanium Backup's "Create update.zip" feature.
 */
@Singleton
class RecoveryZipCreator @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    companion object {
        private const val TAG = "[RecoveryZip]"
        private const val UPDATER_BINARY = "META-INF/com/google/android/update-binary"
        private const val UPDATER_SCRIPT = "META-INF/com/google/android/updater-script"
    }

    data class ZipEntry(
        val packageName: String,
        val apkPath: String,
        val dataPath: String?
    )

    data class CreateResult(
        val outputFile: File,
        val appsIncluded: Int,
        val sizeBytes: Long
    )

    /**
     * Create a TWRP-flashable ZIP from a list of backed-up apps.
     *
     * @param entries List of apps to include (APK path + optional data path)
     * @param outputFile Output ZIP file
     * @param includeData Whether to include app data (requires root on restore)
     */
    suspend fun createRecoveryZip(
        entries: List<ZipEntry>,
        outputFile: File,
        includeData: Boolean = true
    ): Result<CreateResult> = runCatching {
        outputFile.parentFile?.mkdirs()

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // Write updater-script (the shell script that runs during flash)
            writeUpdaterScript(zos, entries, includeData)

            // Write update-binary (shell-based, uses /sbin/sh)
            writeUpdateBinary(zos)

            // Include APKs
            for (entry in entries) {
                val apkFile = File(entry.apkPath)
                if (apkFile.exists()) {
                    addFileToZip(zos, apkFile, "system/app/${entry.packageName}/base.apk")
                }

                // Include data tarballs
                if (includeData && entry.dataPath != null) {
                    val dataFile = File(entry.dataPath)
                    if (dataFile.exists()) {
                        addFileToZip(zos, dataFile, "data/${entry.packageName}.tar.gz")
                    }
                }
            }
        }

        Timber.d("$TAG Created recovery ZIP: ${outputFile.absolutePath} (${entries.size} apps)")
        CreateResult(outputFile, entries.size, outputFile.length())
    }

    private fun writeUpdaterScript(zos: ZipOutputStream, entries: List<ZipEntry>, includeData: Boolean) {
        val script = buildString {
            appendLine("#!/sbin/sh")
            appendLine("# ObsidianBackup Recovery Restore Script")
            appendLine("# Generated: ${java.util.Date()}")
            appendLine()
            appendLine("OUTFD=/proc/self/fd/\$2")
            appendLine("ZIPFILE=\"\$3\"")
            appendLine()
            appendLine("ui_print() { echo -e \"ui_print \$1\\nui_print\" >> \$OUTFD; }")
            appendLine()
            appendLine("ui_print \"ObsidianBackup Recovery Restore\"")
            appendLine("ui_print \"Restoring ${entries.size} apps...\"")
            appendLine()

            // Extract ZIP contents
            appendLine("TMPDIR=/tmp/obsidian_restore")
            appendLine("rm -rf \$TMPDIR")
            appendLine("mkdir -p \$TMPDIR")
            appendLine("unzip -o \"\$ZIPFILE\" -d \$TMPDIR")
            appendLine()

            // Install APKs
            for (entry in entries) {
                appendLine("# Install ${entry.packageName}")
                appendLine("if [ -f \"\$TMPDIR/system/app/${entry.packageName}/base.apk\" ]; then")
                appendLine("  mkdir -p /data/app/${entry.packageName}")
                appendLine("  cp \"\$TMPDIR/system/app/${entry.packageName}/base.apk\" /data/app/${entry.packageName}/base.apk")
                appendLine("  chmod 644 /data/app/${entry.packageName}/base.apk")
                appendLine("  ui_print \"  ✓ ${entry.packageName}\"")
                appendLine("fi")

                if (includeData && entry.dataPath != null) {
                    appendLine("if [ -f \"\$TMPDIR/data/${entry.packageName}.tar.gz\" ]; then")
                    appendLine("  mkdir -p /data/data/${entry.packageName}")
                    appendLine("  tar -xzf \"\$TMPDIR/data/${entry.packageName}.tar.gz\" -C /data/data/${entry.packageName}/")
                    appendLine("  restorecon -R /data/data/${entry.packageName}/ 2>/dev/null")
                    appendLine("  ui_print \"  ✓ ${entry.packageName} data restored\"")
                    appendLine("fi")
                }
                appendLine()
            }

            appendLine("# Cleanup")
            appendLine("rm -rf \$TMPDIR")
            appendLine("ui_print \"Restore complete!\"")
            appendLine("ui_print \"Reboot to apply changes.\"")
        }

        zos.putNextEntry(java.util.zip.ZipEntry(UPDATER_SCRIPT))
        zos.write(script.toByteArray())
        zos.closeEntry()
    }

    private fun writeUpdateBinary(zos: ZipOutputStream) {
        // Minimal update-binary that invokes updater-script as a shell script
        val dollar = '$'
        val binary = "#!/sbin/sh\n" +
            "# Minimal update-binary for shell-based updater-script\n" +
            "OUTFD=${dollar}2\n" +
            "ZIP=${dollar}3\n" +
            "DIR=${dollar}(dirname \"${dollar}0\")\n" +
            "sh \"${dollar}DIR/updater-script\" \"${dollar}@\"\n"
        zos.putNextEntry(java.util.zip.ZipEntry(UPDATER_BINARY))
        zos.write(binary.toByteArray())
        zos.closeEntry()
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryPath: String) {
        zos.putNextEntry(java.util.zip.ZipEntry(entryPath))
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }
}
