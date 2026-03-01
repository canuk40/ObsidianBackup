package com.obsidianbackup.model

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>() {
        constructor(exception: Throwable) : this(exception.message ?: "Unknown error", exception)
    }
    object Loading : Result<Nothing>()
    
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, exception)
        return this
    }
}
