// logging/ObsidianLogger.kt
package com.obsidianbackup.logging

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

sealed class LogLevel(val priority: Int, val tag: String) {
    object VERBOSE : LogLevel(Log.VERBOSE, "V")
    object DEBUG : LogLevel(Log.DEBUG, "D")
    object INFO : LogLevel(Log.INFO, "I")
    object WARN : LogLevel(Log.WARN, "W")
    object ERROR : LogLevel(Log.ERROR, "E")
}

@Serializable
data class LogEntry(
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String,
    val thread: String,
    val processId: Int,
    val stackTrace: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

interface LogSink {
    suspend fun write(entry: LogEntry)
    suspend fun flush()
}

class FileLogSink(
    private val logDir: File,
    private val maxFileSizeMB: Int = 10,
    private val maxFiles: Int = 5
) : LogSink {

    private val json = Json { prettyPrint = false }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val buffer = ConcurrentLinkedQueue<LogEntry>()
    private var currentFile: File? = null
    private var currentFileSize = 0L

    init {
        logDir.mkdirs()
        rotateIfNeeded()
    }

    override suspend fun write(entry: LogEntry) {
        buffer.offer(entry)

        if (buffer.size > 100) {
            flush()
        }
    }

    override suspend fun flush() {
        val entries = mutableListOf<LogEntry>()
        while (buffer.isNotEmpty()) {
            buffer.poll()?.let { entries.add(it) }
        }

        if (entries.isEmpty()) return

        rotateIfNeeded()

        val file = getCurrentLogFile()
        file.appendText(entries.joinToString("\n") { json.encodeToString(it) } + "\n")
        currentFileSize = file.length()
    }

    private fun getCurrentLogFile(): File {
        if (currentFile == null || currentFileSize > maxFileSizeMB * 1024 * 1024) {
            rotateIfNeeded()
            val date = dateFormat.format(Date())
            val timestamp = System.currentTimeMillis()
            currentFile = File(logDir, "titan_${date}_$timestamp.log")
            currentFileSize = 0
        }
        return currentFile!!
    }

    private fun rotateIfNeeded() {
        val files = logDir.listFiles()
            ?.filter { it.extension == "log" }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (files.size >= maxFiles) {
            files.drop(maxFiles - 1).forEach { it.delete() }
        }
    }

    fun exportLogs(): List<File> {
        return logDir.listFiles()?.filter { it.extension == "log" } ?: emptyList()
    }
}

class ConsoleLogSink : LogSink {
    override suspend fun write(entry: LogEntry) {
        val priority = when (entry.level) {
            "V" -> Log.VERBOSE
            "D" -> Log.DEBUG
            "I" -> Log.INFO
            "W" -> Log.WARN
            "E" -> Log.ERROR
            else -> Log.INFO
        }

        Log.println(priority, entry.tag, entry.message)
    }

    override suspend fun flush() {
        // Console doesn't need flushing
    }
}

class ObsidianLogger(
    private val minLevel: LogLevel = LogLevel.INFO,
    private val sinks: List<LogSink> = listOf(ConsoleLogSink()),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    fun v(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.VERBOSE, tag, message, metadata = metadata)
    }

    fun d(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.DEBUG, tag, message, metadata = metadata)
    }

    fun i(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.INFO, tag, message, metadata = metadata)
    }

    fun w(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.WARN, tag, message, metadata = metadata)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.ERROR, tag, message, throwable, metadata)
    }

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        if (level.priority < minLevel.priority) return

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level.tag,
            tag = tag,
            message = message,
            thread = Thread.currentThread().name,
            processId = android.os.Process.myPid(),
            stackTrace = throwable?.stackTraceToString(),
            metadata = metadata
        )

        scope.launch {
            sinks.forEach { sink ->
                try {
                    sink.write(entry)
                } catch (e: Exception) {
                    // Fail silently - don't crash due to logging
                }
            }
        }
    }

    suspend fun flush() {
        sinks.forEach { it.flush() }
    }

    companion object {
        private const val TAG = "ObsidianLogger"
    }
}

// Extension functions for easier use
inline fun <reified T> T.getLogger(): ObsidianLogger {
    return ObsidianLogger() // Should be injected via DI in real app
}
