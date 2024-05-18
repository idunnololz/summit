package com.idunnololz.summit.util

import com.idunnololz.summit.api.ApiException
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.RateLimitException
import com.idunnololz.summit.api.ServerApiException
import kotlinx.coroutines.delay

suspend fun <T> retry(
    times: Int = 6,
    // 0.5 second
    initialDelay: Long = 500,
    // 16 second
    maxDelay: Long = 16_000,
    factor: Double = 2.0,
    retry: (Throwable) -> Boolean = {
        // Only retry server exceptions
        if (it is ApiException) {
            when (it) {
                is ClientApiException -> false
                is ServerApiException -> true
            }
        } else {
            false
        }
    },
    block: suspend () -> Result<T>,
): Result<T> {
    var currentDelay = initialDelay
    repeat(times - 1) {
        val result = block()
        if (result.isSuccess) {
            return result
        }
        val exception = requireNotNull(result.exceptionOrNull())
        if (exception is RateLimitException && exception.timeout > 0L) {
            delay(exception.timeout)
        } else {
            if (!retry(exception)) {
                return result
            }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}
