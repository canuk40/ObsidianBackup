// error/RetryStrategy.kt
package com.obsidianbackup.error

import kotlinx.coroutines.delay

class RetryStrategy(
    private val maxAttempts: Int = 3,
    private val initialDelay: Long = 1000,
    private val maxDelay: Long = 10000,
    private val factor: Double = 2.0,
    private val retryIf: (ObsidianError) -> Boolean = { it.recoverable }
) {
    suspend fun <T> execute(block: suspend () -> Result<T>): Result<T> {
        var currentDelay = initialDelay
        var lastError: ObsidianError? = null

        repeat(maxAttempts) { attempt ->
            when (val result = block()) {
                is Result.Success -> return result
                is Result.Error -> {
                    lastError = result.error

                    if (!retryIf(result.error)) {
                        return result // Don't retry if error is not recoverable
                    }

                    if (attempt < maxAttempts - 1) {
                        delay(currentDelay)
                        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                    }
                }
            }
        }

        return Result.Error(lastError ?: ObsidianError.Unknown("Unknown error after retry"))
    }
}
