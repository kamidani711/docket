package com.docket.domain.common

/**
 * Wraps the outcome of an operation (use case, repository call, etc.) so callers handle
 * success/failure/in-flight explicitly instead of throwing across layers.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val throwable: Throwable, val message: String? = throwable.message) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Throwable, String?) -> Unit): Result<T> {
    if (this is Result.Error) action(throwable, message)
    return this
}
