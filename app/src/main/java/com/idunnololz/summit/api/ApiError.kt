package com.idunnololz.summit.api

import kotlin.RuntimeException

class ServerApiException(val errorMessage: String?, val errorCode: Int) : ApiException(
    "Server error. Code: $errorCode. Message: $errorMessage.",
)
open class ClientApiException(val errorMessage: String?, val errorCode: Int) : ApiException(
    "Client error. Code: $errorCode. Message: $errorMessage.",
)

class IncorrectLoginException() : ClientApiException("Incorrect username or password.", 401)

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

class ForbiddenException : ClientApiException("Rate limit timed out.", 403)

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

/**
 * 50/50 could be the server or the user network
 */
class ConnectionException() : NetworkException("Unable to connect to host")

sealed class NetworkException(msg: String) : RuntimeException(msg)

class GetNetworkException(msg: String) : NetworkException(msg)

class NotAModOrAdmin() : ClientApiException("Not a mod or admin", 400)

class CouldntFindObjectError() : ClientApiException("Couldn't find object", 400)

class FileTooLargeError() : ClientApiException("File is over the size limit", 413)

class CommunityBlockedError() : ClientApiException(
    "The community you are trying to access is blocked",
    400,
)
