package com.obsidianbackup.cloud

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multithreaded and resumable cloud download manager.
 *
 * Features:
 * - Split large files into chunks downloaded in parallel
 * - Resume interrupted downloads from last byte position
 * - Progress tracking per file and overall
 * - Configurable thread count and chunk size
 */
@Singleton
class MultiThreadedDownloadManager @Inject constructor() {

    companion object {
        private const val TAG = "[MTDownload]"
        private const val DEFAULT_THREADS = 4
        private const val DEFAULT_CHUNK_SIZE = 4L * 1024 * 1024 // 4MB chunks
        private const val PROGRESS_FILE_SUFFIX = ".obsidian_progress"
    }

    data class DownloadConfig(
        val threadCount: Int = DEFAULT_THREADS,
        val chunkSize: Long = DEFAULT_CHUNK_SIZE,
        val resumeEnabled: Boolean = true,
        val maxRetries: Int = 3
    )

    data class DownloadProgress(
        val fileName: String,
        val totalBytes: Long,
        val downloadedBytes: Long,
        val speed: Long, // bytes per second
        val activeThreads: Int,
        val isComplete: Boolean = false,
        val error: String? = null
    ) {
        val percentage: Float get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes * 100) else 0f
    }

    data class ChunkInfo(
        val index: Int,
        val startByte: Long,
        val endByte: Long,
        var downloadedBytes: Long = 0,
        var complete: Boolean = false
    )

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: Flow<DownloadProgress?> = _progress.asStateFlow()

    /**
     * Download a file using multiple threads with resume support.
     *
     * @param downloadUrl The URL to download from
     * @param outputFile Local file to save to
     * @param totalSize Total file size (must be known for chunked download)
     * @param config Download configuration
     * @param downloadChunk Lambda that downloads a byte range to a specific file position
     */
    suspend fun download(
        downloadUrl: String,
        outputFile: File,
        totalSize: Long,
        config: DownloadConfig = DownloadConfig(),
        downloadChunk: suspend (url: String, startByte: Long, endByte: Long, output: RandomAccessFile) -> Long
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val progressFile = File(outputFile.absolutePath + PROGRESS_FILE_SUFFIX)

            // Calculate chunks
            val chunks = calculateChunks(totalSize, config.chunkSize, progressFile, config.resumeEnabled)
            val incompleteChunks = chunks.filter { !it.complete }

            if (incompleteChunks.isEmpty()) {
                Timber.d("$TAG File already complete: ${outputFile.name}")
                cleanupProgressFile(progressFile)
                return@runCatching outputFile
            }

            Timber.d("$TAG Starting download: ${outputFile.name} (${totalSize / 1024}KB, ${incompleteChunks.size} chunks, ${config.threadCount} threads)")

            // Pre-allocate file
            if (!outputFile.exists()) {
                RandomAccessFile(outputFile, "rw").use { it.setLength(totalSize) }
            }

            val startTime = System.currentTimeMillis()
            var totalDownloaded = chunks.filter { it.complete }.sumOf { it.endByte - it.startByte + 1 }

            // Download chunks in parallel
            coroutineScope {
                val semaphore = kotlinx.coroutines.sync.Semaphore(config.threadCount)
                val jobs = incompleteChunks.map { chunk ->
                    async {
                        semaphore.acquire()
                        try {
                            var retries = 0
                            while (retries < config.maxRetries) {
                                try {
                                    val startByte = chunk.startByte + chunk.downloadedBytes
                                    RandomAccessFile(outputFile, "rw").use { raf ->
                                        raf.seek(startByte)
                                        val bytesRead = downloadChunk(downloadUrl, startByte, chunk.endByte, raf)
                                        chunk.downloadedBytes += bytesRead
                                        totalDownloaded += bytesRead
                                        chunk.complete = true
                                    }

                                    // Update progress
                                    val elapsed = System.currentTimeMillis() - startTime
                                    val speed = if (elapsed > 0) totalDownloaded * 1000 / elapsed else 0
                                    _progress.value = DownloadProgress(
                                        outputFile.name, totalSize, totalDownloaded, speed,
                                        config.threadCount - semaphore.availablePermits
                                    )
                                    break
                                } catch (e: Exception) {
                                    retries++
                                    if (retries >= config.maxRetries) throw e
                                    Timber.w("$TAG Chunk ${chunk.index} retry $retries: ${e.message}")
                                    delay(1000L * retries) // Exponential backoff
                                }
                            }
                        } finally {
                            semaphore.release()
                        }

                        // Save progress for resume
                        saveProgress(progressFile, chunks)
                    }
                }
                jobs.awaitAll()
            }

            // Cleanup
            cleanupProgressFile(progressFile)

            val elapsed = System.currentTimeMillis() - startTime
            _progress.value = DownloadProgress(outputFile.name, totalSize, totalSize, totalSize * 1000 / maxOf(elapsed, 1), 0, isComplete = true)
            Timber.d("$TAG Download complete: ${outputFile.name} in ${elapsed}ms")
            outputFile
        }
    }

    /**
     * Check if a download can be resumed (progress file exists).
     */
    fun canResume(outputFile: File): Boolean {
        return File(outputFile.absolutePath + PROGRESS_FILE_SUFFIX).exists()
    }

    /**
     * Cancel and clean up a partial download.
     */
    fun cancelDownload(outputFile: File) {
        cleanupProgressFile(File(outputFile.absolutePath + PROGRESS_FILE_SUFFIX))
        outputFile.delete()
        _progress.value = null
    }

    private fun calculateChunks(totalSize: Long, chunkSize: Long, progressFile: File, resumeEnabled: Boolean): List<ChunkInfo> {
        val numChunks = ((totalSize + chunkSize - 1) / chunkSize).toInt()
        val chunks = (0 until numChunks).map { i ->
            val start = i * chunkSize
            val end = minOf(start + chunkSize - 1, totalSize - 1)
            ChunkInfo(i, start, end)
        }

        // Restore progress if resumable
        if (resumeEnabled && progressFile.exists()) {
            try {
                progressFile.readLines().forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 3) {
                        val idx = parts[0].toInt()
                        val downloaded = parts[1].toLong()
                        val complete = parts[2].toBoolean()
                        if (idx < chunks.size) {
                            chunks[idx].downloadedBytes = downloaded
                            chunks[idx].complete = complete
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "$TAG Failed to read progress file")
            }
        }

        return chunks
    }

    private fun saveProgress(progressFile: File, chunks: List<ChunkInfo>) {
        try {
            progressFile.writeText(chunks.joinToString("\n") { "${it.index},${it.downloadedBytes},${it.complete}" })
        } catch (e: Exception) {
            Timber.w(e, "$TAG Failed to save progress")
        }
    }

    private fun cleanupProgressFile(progressFile: File) {
        progressFile.delete()
    }
}
