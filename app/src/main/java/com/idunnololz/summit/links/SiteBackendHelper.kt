package com.idunnololz.summit.links

import android.util.Log
import com.idunnololz.summit.api.NoInternetException
import com.idunnololz.summit.links.SiteBackendHelper.ApiType
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

@Singleton
class SiteBackendHelper @Inject constructor(
  private val json: Json,
  private val linkFetcher: LinkFetcher,
) {

  companion object {
    private const val TAG = "SiteBackendHelper"
  }

  enum class ApiType {
    LemmyV3,
    LemmyV4,
    PieFedAlpha,
  }

  data class ApiInfo(
    val backendType: ApiType?,
  )

  private val mutex = Mutex()
  private val instanceApiInfoCache = mutableMapOf<String, ApiInfo>()

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
          runCatching {
            linkFetcher.downloadSite("https://$instance/api/v3/site", cache = true)
          }
        }
        val v4Job = async {
          runCatching {
            linkFetcher.downloadSite("https://$instance/api/v4/site", cache = true)
          }
        }
        val alphaJob = async {
          runCatching {
            linkFetcher.downloadSite("https://$instance/api/alpha/site", cache = true)
          }
        }
        val homePageJob = async {
          runCatching {
            linkFetcher.downloadSite("https://$instance/", cache = true)
          }
        }

        if (v4Job.isSiteView()) {
          return@withContext Result.success(ApiInfo(ApiType.LemmyV4))
        } else if (alphaJob.isSiteView()) {
          return@withContext Result.success(ApiInfo(ApiType.PieFedAlpha))
        } else if (v3Job.isSiteView()) {
          return@withContext Result.success(ApiInfo(ApiType.LemmyV3))
        } else if (homePageJob.isSiteView()) {
          return@withContext Result.success(ApiInfo(null))
        }

        // All 3 network calls failed. This likely means either the site is down or we are down.

        val errors = listOfNotNull(
          v4Job.await().exceptionOrNull(),
          v3Job.await().exceptionOrNull(),
          alphaJob.await().exceptionOrNull(),
          homePageJob.await().exceptionOrNull(),
        )

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

  private suspend fun Deferred<Result<String>>.isSiteView(): Boolean {
    return this.await().fold(
      { it.length > 100 && it.firstOrNull() == '{' },
      { false },
    )
  }
}

val ApiType.isLemmy
  get() = when (this) {
    ApiType.LemmyV3 -> true
    ApiType.LemmyV4 -> true
    ApiType.PieFedAlpha -> false
  }
