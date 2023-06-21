package com.idunnololz.summit.api

import java.lang.RuntimeException

class ServerApiException(val errorCode: Int) : ApiException(
    "Server error. Code: ${errorCode}"
)
open class ClientApiException(val errorMessage: String?, val errorCode: Int) : ApiException(
    "Client error. Code: ${errorCode}. Message: ${errorMessage}."
)

class NotAuthenticatedException(): ClientApiException("Not signed in", 401)
class AccountInstanceMismatchException(
    val accountInstance: String,
    val apiInstance: String
): ClientApiException(
    "Attempted to call an auth'd endpoint with the wrong account. " +
            "Account instance: $accountInstance Api instance: $apiInstance", 401
)

/**
 * This is 99% a server error. For client side timeout errors, use a different error.
 */
class ServerTimeoutException(errorCode: Int):
    ClientApiException("Timed out waiting for server to respond", errorCode)

sealed class ApiException(msg: String) : RuntimeException(msg)