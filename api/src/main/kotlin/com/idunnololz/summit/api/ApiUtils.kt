package com.idunnololz.summit.api

import android.util.Log
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONObject
import retrofit2.Call

private const val TAG = "ApiUtils"

internal suspend fun <T> retrofitErrorHandler(call: suspend () -> Call<T>): Result<T> {
  val res = try {
    val call = call()
    runInterruptible(Dispatchers.IO) {
      call.execute()
    }
  } catch (e: Exception) {
    if (e is SocketTimeoutException) {
      return Result.failure(com.idunnololz.summit.api.SocketTimeoutException())
    }
    if (e is UnknownHostException) {
      return Result.failure(NoInternetException())
    }
    if (e is CancellationException) {
      throw e
    }
    if (e is InterruptedIOException) {
      return Result.failure(e)
    }
    if (e is ConnectException) {
      return Result.failure(ConnectionException())
    }
    Log.e(TAG, "Exception fetching url", e)
    return Result.failure(e)
  }

  if (res.isSuccessful) {
    return Result.success(requireNotNull(res.body()))
  } else {
    val errorCode = res.code()
    val errorBody = res.errorBody()?.string()

    if (errorCode >= 500) {
      if (res.message().contains("only-if-cached", ignoreCase = true)) {
        // for some reason okhttp returns a 504 if we force cache with no internet
        return Result.failure(NoInternetException())
      }
      return Result.failure(
        ServerApiException(
          errorMessage = errorBody,
          errorCode = errorCode,
        ),
      )
    }

    if (errorCode == 429) {
      return Result.failure(RateLimitException(0L))
    }

    val errMsg = try {
      errorBody?.let {
        JSONObject(it).getString("error")
      } ?: run {
        res.code().toString()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception parsing body", e)
      errorBody
    }

    if (errMsg?.equals("incorrect_login", ignoreCase = true) == true) {
      return Result.failure(ClientApiException(errMsg, errorCode))
    }

    if (errorCode == 401) {
      return Result.failure(NotAuthenticatedException())
    }

    if (errMsg?.contains("not_logged_in", ignoreCase = true) == true) {
      return Result.failure(NotAuthenticatedException())
    }
    if (errMsg == "rate_limit_error") { // might be safe to delete
      return Result.failure(RateLimitException(0L))
    }
    if (errMsg == "not_a_mod_or_admin") {
      return Result.failure(NotAModOrAdmin())
    }
    if (errMsg == "couldnt_find_object" || errMsg == "couldnt_find_community") {
      return Result.failure(CouldntFindObjectError())
    }
    if (errMsg == "not_yet_implemented") {
      return Result.failure(NotYetImplemented())
    }
    if (errMsg == "language_not_allowed") {
      return Result.failure(LanguageNotAllowed())
    }
    // TODO: Remove these checks once v0.19 is out for everyone.
    if (errMsg?.contains("unknown variant") == true || (errorCode == 404 && res.raw().request.url.toString().contains("site/block"))) {
      return Result.failure(NewApiException("v0.19"))
    }

    if (errorCode == 403) {
      return Result.failure(ForbiddenException())
    }

    if (BuildConfig.DEBUG) {
      Log.e(
        "ApiError",
        "Code: $errorCode Error message: $errMsg Call: ${call().request().url}",
        RuntimeException(),
      )
    }

    if (errMsg?.contains("timeout", ignoreCase = true) == true) {
      return Result.failure(ServerTimeoutException(errorCode))
    }
    if (errMsg?.contains("the database system is not yet accepting connections", ignoreCase = true) == true) {
      // this is a 4xx error but it should be a 5xx error because it's server sided and retry-able
      return Result.failure(ServerApiException(null, 503))
    }

    return Result.failure(ClientApiException(errorMessage = errMsg, errorCode = errorCode))
  }
}
