package com.idunnololz.summit.api

import kotlin.RuntimeException

class ServerApiException(val errorCode: Int) : ApiException(
    "Server error. Code: $errorCode",
)
open class ClientApiException(val errorMessage: String?, val errorCode: Int) : ApiException(
    "Client error. Code: $errorCode. Message: $errorMessage.",
)

class NotAuthenticatedException() : ClientApiException("Not signed in", 401)
class AccountInstanceMismatchException(
    val accountInstance: String,
    val apiInstance: String,
) : ClientApiException(
    "Attempted to call an auth'd endpoint with the wrong account. " +
        "Account instance: $accountInstance Api instance: $apiInstance",
    401,
)
class RateLimitException(val timeout: Long) : ClientApiException("Rate limit timed out.", 429)
class NewApiException(val minVersion: String) : ClientApiException(
    "Server version is too low and does not support this API. API version required: $minVersion.",
    400,
)

/**
 * This is 99% a server error. For client side timeout errors, use a different error.
 */
class ServerTimeoutException(errorCode: Int) :
    ClientApiException("Timed out waiting for server to respond", errorCode)
sealed class ApiException(msg: String) : RuntimeException(msg)

/**
 * 50/50 could be the server or the user network
 */
class SocketTimeoutException() :
    NetworkException("Timed out waiting for server to respond")

class NoInternetException() : NetworkException("No internet")

sealed class NetworkException(msg: String) : RuntimeException(msg)

class NotAModOrAdmin() : ClientApiException("Rate limit timed out.", 400)
