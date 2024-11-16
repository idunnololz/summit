package com.idunnololz.summit.api

import android.content.Context
import android.util.Log
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.api.LemmyApiClient.Companion
import com.idunnololz.summit.api.summit.CommunitySuggestionsDto
import com.idunnololz.summit.cache.CachePolicyManager
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONObject
import retrofit2.Call
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class SummitServerClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val summitServerApi: SummitServerApi,
) {

    companion object {
        private const val TAG = "SummitServerClient"
    }

    suspend fun communitySuggestions(
        force: Boolean,
    ): Result<CommunitySuggestionsDto> {
        return retrofitErrorHandler {
            if (force) {
                summitServerApi.communitySuggestionsNoCache()
            } else {
                summitServerApi.communitySuggestions()
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it) },
        )
    }

    private suspend fun <T> retrofitErrorHandler(call: () -> Call<T>): Result<T> {
        val res = try {
            runInterruptible(Dispatchers.IO) {
                call().execute()
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
            Log.e(TAG, "Exception fetching url", e)
            return Result.failure(e)
        }

        if (res.isSuccessful) {
            return Result.success(requireNotNull(res.body()))
        } else {
            val errorCode = res.code()

            if (errorCode >= 500) {
                if (res.message().contains("only-if-cached", ignoreCase = true)) {
                    // for some reason okhttp returns a 504 if we force cache with no internet
                    return Result.failure(NoInternetException())
                }
                return Result.failure(ServerApiException(errorCode))
            }

            if (errorCode == 401) {
                return Result.failure(NotAuthenticatedException())
            }

            val errorBody = res.errorBody()?.string()
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

            if (errMsg?.contains("not_logged_in", ignoreCase = true) == true) {
                return Result.failure(NotAuthenticatedException())
            }
            if (errMsg == "rate_limit_error") {
                return Result.failure(RateLimitException(0L))
            }
            if (errMsg == "not_a_mod_or_admin") {
                return Result.failure(NotAModOrAdmin())
            }
            if (errMsg == "couldnt_find_object") {
                return Result.failure(CouldntFindObjectError())
            }
            // TODO: Remove these checks once v0.19 is out for everyone.
            if (errMsg?.contains("unknown variant") == true || (errorCode == 404 && res.raw().request.url.toString().contains("site/block"))) {
                return Result.failure(NewApiException("v0.19"))
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
                return Result.failure(ServerApiException(503))
            }

            return Result.failure(ClientApiException(errMsg, errorCode))
        }
    }
}