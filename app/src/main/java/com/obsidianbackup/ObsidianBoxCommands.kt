// engine/ObsidianBoxCommands.kt
package com.obsidianbackup

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import kotlinx.coroutines.delay

object ObsidianBoxCommands {

    /**
     * IMPORTANT: All path parameters passed to these functions MUST be pre-escaped
     * using the shellEscape() function to prevent command injection vulnerabilities.
     * 
     * Example: ObsidianBoxCommands.createTarArchive(shellEscape(sourceDir), shellEscape(outputFile))
     */

    /**
     * Create tar archive with compression
     * @param sourceDir Directory to backup (MUST be shell-escaped)
     * @param outputFile Output tar.gz file (MUST be shell-escaped)
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
     * @param archiveFile Archive file path (MUST be shell-escaped)
     * @param destDir Destination directory (MUST be shell-escaped)
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
     * @param sourceDir Source directory (MUST be shell-escaped)
     * @param destDir Destination directory (MUST be shell-escaped)
     * @param linkDest Link destination (MUST be shell-escaped)
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
     * @param filePath File path (MUST be shell-escaped)
     */
    fun calculateSha256(filePath: String): String {
        return "busybox sha256sum $filePath"
    }

    /**
     * Verify checksum
     * @param checksumFile Checksum file (MUST be shell-escaped)
     * @param directory Directory (MUST be shell-escaped)
     */
    fun verifySha256(checksumFile: String, directory: String): String {
        return "cd $directory && busybox sha256sum -c $checksumFile"
    }

    /**
     * Restore SELinux context
     * @param path Path (MUST be shell-escaped)
     */
    fun restoreSelinuxContext(path: String): String {
        return "restorecon -R $path"
    }

    /**
     * Copy file preserving permissions
     * @param source Source path (MUST be shell-escaped)
     * @param dest Destination path (MUST be shell-escaped)
     */
    fun copyWithPermissions(source: String, dest: String): String {
        return "busybox cp -a $source $dest"
    }

    /**
     * Change ownership (requires root)
     * @param path Path (MUST be shell-escaped)
     */
    fun changeOwnership(path: String, uid: Int, gid: Int): String {
        return "busybox chown -R $uid:$gid $path"
    }

    /**
     * Get directory size
     * @param path Directory path (MUST be shell-escaped)
     */
    fun getDirectorySize(path: String): String {
        return "busybox du -sb $path | busybox cut -f1"
    }

    /**
     * Create directory with parents
     * @param path Directory path (MUST be shell-escaped)
     */
    fun createDirectory(path: String, mode: String = "755"): String {
        return "busybox mkdir -p $path && busybox chmod $mode $path"
    }

    /**
     * Remove directory recursively
     * @param path Directory path (MUST be shell-escaped)
     */
    fun removeDirectory(path: String): String {
        return "busybox rm -rf $path"
    }

    /**
     * List files with details
     * @param directory Directory path (MUST be shell-escaped)
     */
    fun listFiles(directory: String): String {
        return "busybox ls -la $directory"
    }
}
class IOThrottler(context: Context) {
    private val storageManager = context.getSystemService(StorageManager::class.java)

    suspend fun shouldThrottle(): Boolean {
        return try {
            val pressureFile = java.io.File("/proc/pressure/io")
            if (!pressureFile.exists()) {
                return false
            }
            
            val content = pressureFile.readText()
            val someLine = content.lines().find { it.startsWith("some") }
            if (someLine != null) {
                val avg10Match = "avg10=([0-9.]+)".toRegex().find(someLine)
                val avg10 = avg10Match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                avg10 > 50.0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun executeWithThrottling(block: suspend () -> Unit) {
        while (shouldThrottle()) {
            delay(100) // Back off
        }
        block()
    }

    private fun checkIOPressure(volume: StorageVolume): Boolean {
        return try {
            val pressureFile = java.io.File("/proc/pressure/io")
            if (!pressureFile.exists()) {
                return false
            }
            
            val content = pressureFile.readText()
            val someLine = content.lines().find { it.startsWith("some") }
            if (someLine != null) {
                val avg10Match = "avg10=([0-9.]+)".toRegex().find(someLine)
                val avg10 = avg10Match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                avg10 > 50.0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
object OptimizedObsidianBoxCommands {
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
