package com.obsidianbackup.rootcore.shell

import kotlinx.serialization.Serializable

/**
 * Result of shell command execution.
 *
 * Ported from ObsidianBox v31 production code.
 */
@Serializable
data class ShellResult(
    val success: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long
) {
    val isSuccess: Boolean get() = success && exitCode == 0
    val output: String get() = stdout.ifEmpty { stderr }

    companion object {
        fun success(output: String = "", executionTimeMs: Long = 0) = ShellResult(
            success = true,
            exitCode = 0,
            stdout = output,
            stderr = "",
            executionTimeMs = executionTimeMs
        )

        fun failure(exitCode: Int, error: String, executionTimeMs: Long = 0) = ShellResult(
            success = false,
            exitCode = exitCode,
            stdout = "",
            stderr = error,
            executionTimeMs = executionTimeMs
        )
    }
}
