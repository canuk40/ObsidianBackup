// engine/ArchiveFormat.kt
package com.obsidianbackup.engine

import com.obsidianbackup.model.PermissionMode
import java.io.File
import javax.inject.Inject

interface ArchiveFormat {
    suspend fun create(sourceDir: File, destFile: File)
    suspend fun extract(archiveFile: File, destDir: File)
    val extension: String
    val compressionRatio: Float
}

class ZstdTarFormat @Inject constructor(
    private val shellExecutor: ShellExecutor
) : ArchiveFormat {
    override val extension = ".tar.zst"
    override val compressionRatio = 0.65f

    override suspend fun create(sourceDir: File, destFile: File) {
        shellExecutor.execute("busybox tar -cf - -C $sourceDir . | zstd -6 -T0 > $destFile")
    }

    override suspend fun extract(archiveFile: File, destDir: File) {
        shellExecutor.execute("zstd -d -c $archiveFile | busybox tar -xf - -C $destDir")
    }
}

class SquashFSFormat @Inject constructor(
    private val shellExecutor: ShellExecutor
) : ArchiveFormat {
    override val extension = ".sqfs"
    override val compressionRatio = 0.55f

    override suspend fun create(sourceDir: File, destFile: File) {
        shellExecutor.execute("mksquashfs $sourceDir $destFile -comp zstd")
    }

    override suspend fun extract(archiveFile: File, destDir: File) {
        shellExecutor.execute("unsquashfs -d $destDir $archiveFile")
    }
}

class ArchiveFormatRegistry @Inject constructor(
    private val zstdTarFormat: ZstdTarFormat,
    private val squashFSFormat: SquashFSFormat
) {
    private val formats = mutableMapOf<String, ArchiveFormat>()

    init {
        register(zstdTarFormat)
        register(squashFSFormat)
    }

    fun register(format: ArchiveFormat) {
        formats[format.extension] = format
    }

    fun detect(file: File): ArchiveFormat? {
        return formats[file.extension]
    }

    fun getAllFormats(): List<ArchiveFormat> {
        return formats.values.toList()
    }
}
