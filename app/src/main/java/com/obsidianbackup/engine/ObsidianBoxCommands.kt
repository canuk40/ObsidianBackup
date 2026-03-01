// Forwarding alias to the original BusyBox implementation located in
// com.example.titanbackup.engine. Per project safety rules, the original
// BusyBox files must not be modified; this file provides a non-invasive
// way to reference BusyBox types from the `com.titanbackup` package.

package com.obsidianbackup.engine

import android.content.Context
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import com.obsidianbackup.model.BackupComponent
import java.io.File
import kotlinx.coroutines.delay

object BusyBoxCommands {

    /**
     * Create tar archive with compression
     * @param sourceDir Directory to backup
     * @param outputFile Output tar.gz file
     * @param useZstd Use zstd instead of gzip for better compression
     */
    fun createTarArchive(
        sourceDir: String,
        outputFile: String,
        useZstd: Boolean = true,
        compressionLevel: Int = 6
    ): String {
        return if (useZstd) {
            "busybox tar -cf - -C $sourceDir . | zstd -$compressionLevel -T0 > $outputFile"
        } else {
            "busybox tar -czf $outputFile -C $sourceDir ."
        }
    }

    /**
     * Extract tar archive
     */
    fun extractTarArchive(
        archiveFile: String,
        destDir: String,
        isZstd: Boolean = true
    ): String {
        return if (isZstd) {
            "zstd -d -c $archiveFile | busybox tar -xf - -C $destDir"
        } else {
            "busybox tar -xzf $archiveFile -C $destDir"
        }
    }

    /**
     * Incremental backup using rsync
     */
    fun rsyncIncremental(
        sourceDir: String,
        destDir: String,
        linkDest: String? = null
    ): String {
        val linkDestArg = linkDest?.let { "--link-dest=$it" } ?: ""
        return "busybox rsync -aAX $linkDestArg $sourceDir/ $destDir/"
    }

    /**
     * Calculate SHA256 checksum
     */
    fun calculateSha256(filePath: String): String {
        return "busybox sha256sum $filePath"
    }

    /**
     * Verify checksum
     */
    fun verifySha256(checksumFile: String, directory: String): String {
        return "cd $directory && busybox sha256sum -c $checksumFile"
    }

    /**
     * Restore SELinux context
     */
    fun restoreSelinuxContext(path: String): String {
        return "restorecon -R $path"
    }

    /**
     * Copy file preserving permissions
     */
    fun copyWithPermissions(source: String, dest: String): String {
        return "busybox cp -a $source $dest"
    }

    /**
     * Change ownership (requires root)
     */
    fun changeOwnership(path: String, uid: Int, gid: Int): String {
        return "busybox chown -R $uid:$gid $path"
    }

    /**
     * Get directory size
     */
    fun getDirectorySize(path: String): String {
        return "busybox du -sb $path | busybox cut -f1"
    }

    /**
     * Create directory with parents
     */
    fun createDirectory(path: String, mode: String = "755"): String {
        return "busybox mkdir -p $path && busybox chmod $mode $path"
    }

    /**
     * Remove directory recursively
     */
    fun removeDirectory(path: String): String {
        return "busybox rm -rf $path"
    }

    /**
     * List files with details
     */
    fun listFiles(directory: String): String {
        return "busybox ls -la $directory"
    }
}
class IOThrottler(context: Context) {
    private val storageManager = context.getSystemService(StorageManager::class.java)

    suspend fun shouldThrottle(): Boolean {
        val stats = storageManager?.getStorageVolumes()?.firstOrNull()
        // Check if device is under I/O pressure
        return stats?.let { checkIOPressure(it) } ?: false
    }

    suspend fun executeWithThrottling(block: suspend () -> Unit) {
        while (shouldThrottle()) {
            delay(100) // Back off
        }
        block()
    }

    private fun checkIOPressure(volume: android.os.storage.StorageVolume): Boolean {
        return try {
            // Check disk I/O pressure using /proc/pressure/io if available (Android 10+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val pressureFile = File("/proc/pressure/io")
                if (pressureFile.exists()) {
                    val content = pressureFile.readText()
                    // Parse pressure metrics (format: some avg10=X.XX avg60=X.XX avg300=X.XX total=XXXXX)
                    val avg10Regex = "avg10=(\\d+\\.\\d+)".toRegex()
                    val match = avg10Regex.find(content)
                    if (match != null) {
                        val pressure = match.groupValues[1].toFloatOrNull() ?: 0f
                        // Throttle if 10-second average pressure > 50%
                        return pressure > 50.0f
                    }
                }
            }
            
            // Fallback: Check disk usage as proxy for I/O pressure
            val statFs = android.os.StatFs(volume.directory?.path ?: "/data")
            val availableBytes = statFs.availableBytes
            val totalBytes = statFs.totalBytes
            val usagePercent = ((totalBytes - availableBytes).toFloat() / totalBytes) * 100
            
            // Throttle if disk usage > 90%
            usagePercent > 90.0f
        } catch (e: Exception) {
            // On error, don't throttle
            false
        }
    }
}
object OptimizedBusyBoxCommands {
    fun streamingBackup(sourceDir: String, outputFile: String): String {
        // Use process substitution to avoid temp files
        return """
            busybox tar -c -C $sourceDir . | \
            zstd -6 -T0 --rsyncable -o $outputFile
        """.trimIndent()
    }

    // For very large backups, use chunked archives
    fun chunkedBackup(sourceDir: String, outputPrefix: String, chunkSizeMB: Int = 500): String {
        return """
            busybox tar -c -C $sourceDir . | \
            split -b ${chunkSizeMB}M - $outputPrefix.tar.zst.
        """.trimIndent()
    }
}
