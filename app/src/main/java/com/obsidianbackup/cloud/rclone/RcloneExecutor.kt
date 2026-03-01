// cloud/rclone/RcloneExecutor.kt
package com.obsidianbackup.cloud.rclone

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Executes rclone commands via native binary
 * 
 * Integration approach:
 * - Uses rclone binary placed in jniLibs or extracted from assets
 * - Executes via ProcessBuilder (bypassing SafeShellExecutor for custom binaries)
 * - Supports both direct execution and RC (Remote Control) server mode
 */
class RcloneExecutor(
    private val context: Context,
    private val configDir: File = File(context.filesDir, "rclone")
) {
    
    @PublishedApi
    internal val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private var rcloneBinaryPath: String? = null
    private val binaryManager = RcloneBinaryManager(context)
    
    companion object {
        private const val RCLONE_BINARY_NAME = "rclone"
        const val DEFAULT_TIMEOUT_MS = 60000L
    }
    
    init {
        configDir.mkdirs()
    }
    
    /**
     * Initialize rclone executor and locate binary
     */
    suspend fun initialize(): RcloneResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Use binary manager to locate rclone
            val binaryLocation = binaryManager.locateBinary()
            val binaryPath = when (binaryLocation) {
                is RcloneBinaryManager.BinaryLocation.NativeLib -> binaryLocation.path
                is RcloneBinaryManager.BinaryLocation.Extracted -> binaryLocation.path
                is RcloneBinaryManager.BinaryLocation.SystemPath -> binaryLocation.path
                is RcloneBinaryManager.BinaryLocation.NotFound -> null
            }
            
            if (binaryPath == null) {
                return@withContext RcloneResult.Error(
                    RcloneError(
                        code = RcloneErrorCode.BINARY_NOT_FOUND,
                        message = "rclone binary not found. Please install rclone or package it with the app.",
                        retryable = false
                    )
                )
            }
            
            rcloneBinaryPath = binaryPath
            
            // Test binary execution
            val versionResult = executeCommand(listOf("version", "--check=false"), timeout = 5000)
            
            when (versionResult) {
                is RcloneResult.Success -> {
                    RcloneResult.Success(Unit)
                }
                is RcloneResult.Error -> {
                    RcloneResult.Error(
                        RcloneError(
                            code = RcloneErrorCode.BINARY_EXECUTION_FAILED,
                            message = "Failed to execute rclone: ${versionResult.error.message}",
                            retryable = false
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            RcloneResult.Error(
                RcloneError(
                    code = RcloneErrorCode.INITIALIZATION_FAILED,
                    message = e.message ?: "Unknown initialization error",
                    cause = e,
                    retryable = false
                )
            )
        }
    }
    
    /**
     * Execute rclone command and return output
     */
    suspend fun executeCommand(
        args: List<String>,
        timeout: Long = DEFAULT_TIMEOUT_MS,
        progressCallback: ((String) -> Unit)? = null
    ): RcloneResult<String> = withContext(Dispatchers.IO) {
        
        val binary = rcloneBinaryPath 
            ?: return@withContext RcloneResult.Error(
                RcloneError(
                    code = RcloneErrorCode.BINARY_NOT_FOUND,
                    message = "Rclone not initialized",
                    retryable = false
                )
            )
        
        try {
            val command = listOf(binary) + args + listOf(
                "--config", getConfigPath()
            )
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(false)
            
            val process = processBuilder.start()
            
            val output = StringBuilder()
            val error = StringBuilder()
            
            // Read output stream
            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read output in separate thread
            val outputThread = Thread {
                outputReader.forEachLine { line ->
                    output.append(line).append("\n")
                    progressCallback?.invoke(line)
                }
            }
            
            val errorThread = Thread {
                errorReader.forEachLine { line ->
                    error.append(line).append("\n")
                }
            }
            
            outputThread.start()
            errorThread.start()
            
            // Wait for process with timeout
            val completed = process.waitFor()
            outputThread.join(timeout)
            errorThread.join(timeout)
            
            if (completed == 0) {
                RcloneResult.Success(output.toString())
            } else {
                RcloneResult.Error(
                    RcloneError(
                        code = mapExitCodeToError(completed),
                        message = error.toString().ifEmpty { "Command failed with exit code $completed" },
                        retryable = isRetryableError(completed)
                    )
                )
            }
            
        } catch (e: Exception) {
            RcloneResult.Error(
                RcloneError(
                    code = RcloneErrorCode.EXECUTION_ERROR,
                    message = e.message ?: "Unknown error",
                    cause = e,
                    retryable = false
                )
            )
        }
    }
    
    /**
     * Execute rclone command and parse JSON output
     */
    suspend inline fun <reified T> executeJsonCommand(
        args: List<String>,
        timeout: Long = Companion.DEFAULT_TIMEOUT_MS
    ): RcloneResult<T> {
        val jsonParser = this.json
        return when (val result = executeCommand(args, timeout)) {
            is RcloneResult.Success -> {
                try {
                    val parsed = jsonParser.decodeFromString<T>(result.data)
                    RcloneResult.Success(parsed)
                } catch (e: Exception) {
                    RcloneResult.Error(
                        RcloneError(
                            code = RcloneErrorCode.PARSE_ERROR,
                            message = "Failed to parse JSON output: ${e.message}",
                            cause = e,
                            retryable = false
                        )
                    )
                }
            }
            is RcloneResult.Error -> RcloneResult.Error(result.error)
        }
    }
    
    /**
     * Get path to rclone config file
     */
    fun getConfigPath(): String = File(configDir, "rclone.conf").absolutePath
    
    /**
     * Get rclone config directory
     */
    fun getConfigDir(): File = configDir
    
    /**
     * Get rclone version
     */
    suspend fun getVersion(): RcloneResult<String> {
        return executeCommand(listOf("version", "--check=false"))
    }
    
    private fun mapExitCodeToError(exitCode: Int): RcloneErrorCode {
        return when (exitCode) {
            1 -> RcloneErrorCode.SYNTAX_ERROR
            2 -> RcloneErrorCode.FILE_NOT_FOUND
            3 -> RcloneErrorCode.DIRECTORY_NOT_FOUND
            4 -> RcloneErrorCode.FILE_EXISTS
            5 -> RcloneErrorCode.TEMPORARY_ERROR
            6 -> RcloneErrorCode.LESS_SERIOUS_ERROR
            7 -> RcloneErrorCode.FATAL_ERROR
            8 -> RcloneErrorCode.TRANSFER_EXCEEDED
            9 -> RcloneErrorCode.NO_FILES_TRANSFERRED
            else -> RcloneErrorCode.UNKNOWN_ERROR
        }
    }
    
    private fun isRetryableError(exitCode: Int): Boolean {
        return exitCode in listOf(5, 6) // Temporary errors
    }
}

/**
 * Rclone operation result
 */
sealed class RcloneResult<out T> {
    data class Success<T>(val data: T) : RcloneResult<T>()
    data class Error(val error: RcloneError) : RcloneResult<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): RcloneResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
}

data class RcloneError(
    val code: RcloneErrorCode,
    val message: String,
    val cause: Throwable? = null,
    val retryable: Boolean = false
)

enum class RcloneErrorCode {
    BINARY_NOT_FOUND,
    BINARY_EXECUTION_FAILED,
    INITIALIZATION_FAILED,
    SYNTAX_ERROR,
    FILE_NOT_FOUND,
    DIRECTORY_NOT_FOUND,
    FILE_EXISTS,
    TEMPORARY_ERROR,
    LESS_SERIOUS_ERROR,
    FATAL_ERROR,
    TRANSFER_EXCEEDED,
    NO_FILES_TRANSFERRED,
    EXECUTION_ERROR,
    PARSE_ERROR,
    CONFIG_ERROR,
    AUTHENTICATION_ERROR,
    NETWORK_ERROR,
    UNKNOWN_ERROR
}
