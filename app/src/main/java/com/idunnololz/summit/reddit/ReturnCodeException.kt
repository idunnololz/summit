package com.idunnololz.summit.reddit

/**
 * Exception when return code is 4xx or 5xx
 */
class ReturnCodeException(
    val errorCode: Int
) : Exception()