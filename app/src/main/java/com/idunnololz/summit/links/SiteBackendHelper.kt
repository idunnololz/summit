package com.idunnololz.summit.links

import android.util.Log
import com.google.gson.Gson
import com.idunnololz.summit.api.ApiFeature
import com.idunnololz.summit.api.ApiInfo
import com.idunnololz.summit.api.ApiType
import com.idunnololz.summit.api.NoInternetException
import com.idunnololz.summit.api.dto.lemmy.GetSiteResponse
import com.idunnololz.summit.network.BrowserLikeUnauthed
import com.idunnololz.summit.util.LinkFetcher
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Singleton
class SiteBackendHelper @Inject constructor(
  private val json: Json,
  private val linkFetcher: LinkFetcher,
  @BrowserLikeUnauthed private val okHttpClient: OkHttpClient,
) {

  companion object {
    private const val TAG = "SiteBackendHelper"
  }

  private val mutex = Mutex()
  private val instanceApiInfoCache = mutableMapOf<String, ApiInfo>()
  private val gson = Gson()

  fun getApiInfoFromCache(instance: String): ApiInfo? =
    instanceApiInfoCache[instance]

  suspend fun fetchApiInfo(instance: String): Result<ApiInfo> {
    withContext(Dispatchers.Main) {
      instanceApiInfoCache[instance]?.let {
        return@withContext it
      }
    }?.let {
      return Result.success(it)
    }

    mutex.withLock {
      withContext(Dispatchers.Main) {
        instanceApiInfoCache[instance]?.let {
          return@withContext it
        }
      }?.let {
        return Result.success(it)
      }

      val result = withContext(Dispatchers.Default) {
        val v3Job = async {
          linkFetcher.downloadSite(
            url = "https://$instance/api/v3/site",
            cache = true,
            client = okHttpClient,
          )
        }
//        val v4Job = async {
//          runCatching {
//            linkFetcher.downloadSite(
//              url = "https://$instance/api/v4/site",
//              cache = true,
//              client = okHttpClient
//            )
//          }
//        }
        val alphaJob = async {
          linkFetcher.downloadSite(
            url = "https://$instance/api/alpha/site",
            cache = true,
            client = okHttpClient,
          )
        }
        val homePageJob = async {
          linkFetcher.downloadSite(
            url = "https://$instance/",
            cache = true,
            client = okHttpClient,
          )
        }

        val allJobs = listOf(
          v3Job,
          alphaJob,
          homePageJob,
        )

//        if (v4Job.isSiteView()) {
//          Log.d(TAG, "instance: ${instance} is type V4")
//          return@withContext Result.success(ApiInfo(ApiType.LemmyV4))
//        } else

        alphaJob.await().let { result ->
          if (result.isSiteView()) {
            try {
              Log.d(TAG, "instance: $instance is type alpha")
              val site = gson.fromJson(
                result.getOrNull(),
                com.idunnololz.summit.api.dto.piefed.GetSiteResponse::class.java
              )

              return@withContext Result.success(
                ApiInfo(
                  backendType = ApiType.PieFedAlpha,
                  downvoteAllowed = site?.site?.enableDownvotes != false,
                )
              )
            } catch (e: Exception) {
              Log.e(TAG, "Error parsing site object", e)
            }
          }
        }
        v3Job.await().let { result ->
          if (result.isSiteView()) {
            try {
              Log.d(TAG, "instance: $instance is type V3")
              val site = gson.fromJson(result.getOrNull(), GetSiteResponse::class.java)

              return@withContext Result.success(
                ApiInfo(
                  backendType = ApiType.LemmyV3,
                  downvoteAllowed = site?.site_view?.local_site?.enable_downvotes != false,
                )
              )
            } catch (e: Exception) {
              Log.e(TAG, "Error parsing site object", e)
            }
          }
        }

        if (homePageJob.await().isSuccess) {
          Log.d(TAG, "instance: $instance is not a lemmy site")

          return@withContext Result.success(ApiInfo(backendType = null, downvoteAllowed = false))
        }

        // All 3 network calls failed. This likely means either the site is down or we are down.

        val errors = allJobs.mapNotNull { it.await().exceptionOrNull() }

        if (errors.isEmpty()) {
          return@withContext Result.failure(RuntimeException("No errors! Impossible!"))
        }

        if (errors.any { it is SocketTimeoutException }) {
          return@withContext Result.failure(SocketTimeoutException())
        }
        if (errors.any { it is UnknownHostException }) {
          return@withContext Result.failure(NoInternetException())
        }
        if (errors.any { it is InterruptedIOException }) {
          return@withContext Result.failure(CancellationException())
        }
        Log.e(TAG, "error", errors.first())
        Result.failure(errors.first())
      }

      val apiInfo = result.getOrNull()
      if (apiInfo != null) {
        withContext(Dispatchers.Main) {
          instanceApiInfoCache[instance] = apiInfo
        }
      }

      return result
    }
  }

  private suspend fun Result<String>.isSiteView(): Boolean = this.fold(
    onSuccess = {
      Log.d(TAG, "onSuccess")
      it.length > 100 && it.firstOrNull() == '{'
    },
    onFailure = {
      Log.d(TAG, "onFailure", it)
      false
    },
  )
}

val ApiType.isLemmy
  get() = when (this) {
    ApiType.LemmyV3 -> true
//    ApiType.LemmyV4 -> true
    ApiType.PieFedAlpha -> false
  }