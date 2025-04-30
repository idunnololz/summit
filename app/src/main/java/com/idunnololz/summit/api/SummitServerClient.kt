package com.idunnololz.summit.api

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.api.summit.CommunitySuggestionsDto
import com.idunnololz.summit.api.summit.PresetDto
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Call

@Singleton
class SummitServerClient @Inject constructor(
  private val summitServerApi: SummitServerApi,
) {

  companion object {
    private const val TAG = "SummitServerClient"
  }

  fun getAuthPublicFileUrl(fileName: String) = if (BuildConfig.DEBUG) {
    "http://10.0.2.2:8080"
  } else {
    "https://summitforlemmyserver.idunnololz.com"
  } + "/v1/auth/public/$fileName"

  suspend fun communitySuggestions(force: Boolean): Result<CommunitySuggestionsDto> {
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

  suspend fun submitPreset(
    presetName: String,
    presetDescription: String,
    presetData: String,
    phoneScreenshot: Uri?,
    tabletScreenshot: Uri?,
  ): Result<PresetDto> {
    return retrofitErrorHandler {
      val ts = System.currentTimeMillis()
      summitServerApi.submitPreset(
        preset = PresetDto(
          id = null,
          presetName = presetName,
          presetDescription = presetDescription,
          presetData = presetData,
          createTs = ts,
          updateTs = ts,
        ),
        phoneScreenshot = phoneScreenshot?.toFile()?.let {
          MultipartBody.Part.createFormData(
            "file",
            "phoneScreenshot",
            it.asRequestBody("image/jpeg".toMediaType()),
          )
        },
        tabletScreenshot = tabletScreenshot?.toFile()?.let {
          MultipartBody.Part.createFormData(
            "file",
            "tabletScreenshot",
            it.asRequestBody("image/jpeg".toMediaType()),
          )
        },
      )
    }
  }

  suspend fun getPresets(force: Boolean): Result<List<PresetDto>> {
    return retrofitErrorHandler {
      if (force) {
        summitServerApi.getPresetsNoCache()
      } else {
        summitServerApi.getPresets()
      }
    }
  }

  suspend fun getAllPresets(force: Boolean): Result<List<PresetDto>> {
    return retrofitErrorHandler {
      if (force) {
        summitServerApi.getAllPresetsNoCache()
      } else {
        summitServerApi.getAllPresets()
      }
    }
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

      if (errorCode >= 500) {
        if (res.message().contains("only-if-cached", ignoreCase = true)) {
          // for some reason okhttp returns a 504 if we force cache with no internet
          return Result.failure(NoInternetException())
        }
        return Result.failure(ServerApiException(null, errorCode))
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

      return Result.failure(ClientApiException(errMsg, errorCode))
    }
  }
}
