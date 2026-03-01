package com.obsidianbackup.rootcore.shell

/**
 * Sealed class representing the result of a root shell command.
 * Provides clear error differentiation for UI feedback.
 *
 * Ported from ObsidianBox v31 production code.
 */
sealed class RootCommandResult<out T> {
    data class Success<T>(val data: T) : RootCommandResult<T>()
    data object RootNotAvailable : RootCommandResult<Nothing>()
    data class CommandFailed(val exitCode: Int, val stderr: String) : RootCommandResult<Nothing>()
    data class Error(val message: String) : RootCommandResult<Nothing>()

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /** Returns a user-friendly error message for display. */
    fun errorMessage(): String = when (this) {
        is Success -> ""
        is RootNotAvailable -> "Root access is not available. Please grant root permission."
        is CommandFailed -> "Command failed (exit code $exitCode): $stderr"
        is Error -> message
    }
}
