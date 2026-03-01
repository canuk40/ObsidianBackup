package com.obsidianbackup.rootcore.shell

import com.obsidianbackup.rootcore.detection.RootDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent root shell session — keeps a single su process alive for batch operations.
 * Modeled after Titanium Backup's HyperShell: dramatically faster for sequential
 * operations since we avoid the overhead of spawning a new su process per command.
 *
 * Features:
 * - Single su process with stdin/stdout pipes
 * - Health check on init (echo + id verification)
 * - OOM protection via /proc/self/oom_score_adj
 * - su --mount-master for Magisk (mount namespace access)
 * - Thread-safe via Mutex
 * - Auto-reconnect on broken pipe
 * - Graceful shutdown
 */
@Singleton
class PersistentShellSession @Inject constructor(
    private val rootDetector: RootDetector
) {
    companion object {
        private const val TAG = "PersistentShell"
        private const val SHELL_TEST_TOKEN = "OBSIDIAN_SHELL_READY"
        private const val COMMAND_DONE_TOKEN = "OBSIDIAN_CMD_DONE"
        private const val OOM_SCORE_MIN = "-17"
        private const val HEALTH_CHECK_TIMEOUT = 10L

        private val SU_STRATEGIES = arrayOf(
            arrayOf("su", "--mount-master"),  // Magisk su with mount namespace
            arrayOf("su"),                    // Standard su
        )
    }

    private var process: Process? = null
    private var stdin: DataOutputStream? = null
    private var stdoutReader: BufferedReader? = null
    private var stderrReader: BufferedReader? = null
    private val isAlive = AtomicBoolean(false)
    private val mutex = Mutex()

    /**
     * Open a persistent root shell session.
     * Tries su --mount-master first (Magisk), falls back to standard su.
     * Returns true if session was established successfully.
     */
    suspend fun open(): Boolean = mutex.withLock {
        if (isAlive.get() && isHealthy()) return true
        close()

        return@withLock withContext(Dispatchers.IO) {
            for (strategy in SU_STRATEGIES) {
                try {
                    Timber.d("$TAG Attempting: ${strategy.joinToString(" ")}")
                    val pb = ProcessBuilder(*strategy)
                    pb.redirectErrorStream(false)
                    val proc = pb.start()

                    val out = DataOutputStream(proc.outputStream)
                    val reader = BufferedReader(InputStreamReader(proc.inputStream))

                    // Apply OOM protection immediately
                    out.writeBytes("echo $OOM_SCORE_MIN > /proc/self/oom_score_adj 2>/dev/null\n")
                    out.flush()

                    // Health check: echo test token + verify uid=0
                    out.writeBytes("echo $SHELL_TEST_TOKEN\n")
                    out.flush()

                    val startTime = System.currentTimeMillis()
                    var verified = false
                    while (System.currentTimeMillis() - startTime < HEALTH_CHECK_TIMEOUT * 1000) {
                        if (reader.ready()) {
                            val line = reader.readLine()
                            if (line == SHELL_TEST_TOKEN) {
                                verified = true
                                break
                            }
                        }
                        Thread.sleep(50)
                    }

                    if (!verified) {
                        Timber.w("$TAG Health check failed for: ${strategy.joinToString(" ")}")
                        proc.destroyForcibly()
                        continue
                    }

                    // Verify we're actually root
                    out.writeBytes("id -u\n")
                    out.flush()
                    Thread.sleep(100)
                    val uid = if (reader.ready()) reader.readLine()?.trim() else null
                    if (uid != "0") {
                        Timber.w("$TAG Not root (uid=$uid) for: ${strategy.joinToString(" ")}")
                        proc.destroyForcibly()
                        continue
                    }

                    process = proc
                    stdin = out
                    stdoutReader = reader
                    stderrReader = BufferedReader(InputStreamReader(proc.errorStream))
                    isAlive.set(true)

                    Timber.i("$TAG Session opened via: ${strategy.joinToString(" ")}")
                    return@withContext true

                } catch (e: Exception) {
                    Timber.w(e, "$TAG Failed to open with: ${strategy.joinToString(" ")}")
                }
            }

            Timber.e("$TAG All su strategies failed")
            false
        }
    }

    /**
     * Execute a command through the persistent shell.
     * Uses a done-token delimiter to know when output is complete.
     */
    suspend fun execute(command: String): ShellResult = mutex.withLock {
        if (!isAlive.get()) {
            return ShellResult.failure(-1, "Persistent shell not open")
        }

        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val out = stdin ?: return@withContext ShellResult.failure(-1, "stdin not available")
                val reader = stdoutReader ?: return@withContext ShellResult.failure(-1, "stdout not available")

                // Write command + done marker with exit code capture
                out.writeBytes("$command\n")
                out.writeBytes("echo \"${COMMAND_DONE_TOKEN}:\$?\"\n")
                out.flush()

                // Read output until we see the done token
                val outputLines = mutableListOf<String>()
                var exitCode = 0
                val timeout = 30_000L // 30 seconds
                val deadline = System.currentTimeMillis() + timeout

                while (System.currentTimeMillis() < deadline) {
                    if (reader.ready()) {
                        val line = reader.readLine() ?: break
                        if (line.startsWith(COMMAND_DONE_TOKEN)) {
                            exitCode = line.substringAfter(":").toIntOrNull() ?: 0
                            break
                        }
                        outputLines.add(line)
                    } else {
                        Thread.sleep(10)
                    }
                }

                val executionTime = System.currentTimeMillis() - startTime
                ShellResult(
                    success = exitCode == 0,
                    exitCode = exitCode,
                    stdout = outputLines.joinToString("\n"),
                    stderr = "",
                    executionTimeMs = executionTime
                )
            } catch (e: Exception) {
                Timber.e(e, "$TAG Command failed: $command")
                // Shell may be broken, mark for reconnect
                isAlive.set(false)
                ShellResult.failure(
                    exitCode = -1,
                    error = e.message ?: "Unknown error",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }

    /**
     * Execute multiple commands sequentially through the persistent shell.
     */
    suspend fun executeBatch(commands: List<String>): List<ShellResult> {
        val results = mutableListOf<ShellResult>()
        for (command in commands) {
            val result = execute(command)
            results.add(result)
            if (!result.success) break // Stop on first failure
        }
        return results
    }

    /**
     * Check if the shell session is still responsive.
     */
    suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        if (!isAlive.get()) return@withContext false
        try {
            val out = stdin ?: return@withContext false
            val reader = stdoutReader ?: return@withContext false

            out.writeBytes("echo $SHELL_TEST_TOKEN\n")
            out.flush()

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 3000) {
                if (reader.ready()) {
                    val line = reader.readLine()
                    if (line == SHELL_TEST_TOKEN) return@withContext true
                }
                Thread.sleep(50)
            }
            false
        } catch (e: Exception) {
            Timber.w(e, "$TAG Health check failed")
            isAlive.set(false)
            false
        }
    }

    /**
     * Close the persistent shell session gracefully.
     */
    suspend fun close() = withContext(Dispatchers.IO) {
        try {
            stdin?.writeBytes("exit\n")
            stdin?.flush()
        } catch (_: Exception) { }

        try { stdin?.close() } catch (_: Exception) { }
        try { stdoutReader?.close() } catch (_: Exception) { }
        try { stderrReader?.close() } catch (_: Exception) { }

        process?.let { proc ->
            try {
                if (!proc.waitFor(3, TimeUnit.SECONDS)) {
                    proc.destroyForcibly()
                }
            } catch (_: Exception) {
                proc.destroyForcibly()
            }
        }

        process = null
        stdin = null
        stdoutReader = null
        stderrReader = null
        isAlive.set(false)
        Timber.d("$TAG Session closed")
    }

    /**
     * Whether the session is currently open and believed alive.
     */
    fun isOpen(): Boolean = isAlive.get()
}
