// error/ErrorHandler.kt
package com.obsidianbackup.error

import kotlinx.coroutines.delay

sealed class ObsidianError(
    open val message: String,
    open val cause: Throwable? = null,
    open val recoverable: Boolean = false
) {
    data class PermissionDenied(
        override val message: String,
        val requiredPermission: String
    ) : ObsidianError(message, recoverable = true)

    data class InsufficientStorage(
        override val message: String,
        val requiredBytes: Long,
        val availableBytes: Long
    ) : ObsidianError(message, recoverable = true)

    data class CorruptedBackup(
        override val message: String,
        val snapshotId: String
    ) : ObsidianError(message, recoverable = false)

    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ObsidianError(message, cause, recoverable = true)

    data class IOError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ObsidianError(message, cause, recoverable = true)

    data class AppNotFound(
        override val message: String,
        val packageName: String
    ) : ObsidianError(message, recoverable = false)

    data class BackupFailed(
        override val message: String,
        override val cause: Throwable? = null,
        val partialData: Any? = null
    ) : ObsidianError(message, cause, recoverable = true)

    data class RestoreFailed(
        override val message: String,
        override val cause: Throwable? = null,
        val rolledBack: Boolean = false
    ) : ObsidianError(message, cause, recoverable = true)

    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : ObsidianError(message, cause, recoverable = false)
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: ObsidianError) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (ObsidianError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }
}
