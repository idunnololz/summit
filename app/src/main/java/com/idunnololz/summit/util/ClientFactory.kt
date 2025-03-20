package com.idunnololz.summit.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.api.LemmyApi.Companion.CACHE_CONTROL_HEADER
import com.idunnololz.summit.api.LemmyApi.Companion.CACHE_CONTROL_NO_CACHE
import com.idunnololz.summit.cache.CachePolicy
import com.idunnololz.summit.cache.CachePolicyManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.UserAgentChoiceIds
import com.idunnololz.summit.util.ext.hasInternet
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Singleton
class ClientFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val cachePolicyManager: CachePolicyManager,
) {

    enum class Purpose {
        LemmyApiClient,
        SummitApiClient,
        BrowserLike,
    }

    private fun getUserAgent(purpose: Purpose): String {
        val purposeString = when (purpose) {
            Purpose.LemmyApiClient -> "lac"
            Purpose.SummitApiClient -> "sac"
            Purpose.BrowserLike -> "bl"
        }

        if (purpose == Purpose.BrowserLike) {
            return when (preferences.userAgentChoice) {
                UserAgentChoiceIds.UNSET,
                UserAgentChoiceIds.LEGACY_USER_AGENT,
                ->
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.3"
                else ->
                    "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; $purposeString) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/51.0.2704.91 Mobile Safari/537.36 SummitForLemmyAndroid"
            }
        }

        return when (preferences.userAgentChoice) {
            UserAgentChoiceIds.UNSET,
            UserAgentChoiceIds.LEGACY_USER_AGENT,
            ->
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0"
            UserAgentChoiceIds.NEW_USER_AGENT ->
                "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; $purposeString) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/51.0.2704.91 Mobile Safari/537.36 SummitForLemmyAndroid"
            UserAgentChoiceIds.NEW_USER_AGENT_2 ->
                "SummitForLemmyAndroid/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
            UserAgentChoiceIds.FLUTTER_USER_AGENT ->
                "Dart/3.7.0"
            UserAgentChoiceIds.OKHTTP_USER_AGENT ->
                "okhttp/4.12.0"
            else ->
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0"
        }
    }

    fun newClient(debugName: String, cacheDir: File, purpose: Purpose): OkHttpClient {

        val myCache = Cache(
            directory = cacheDir,
            maxSize = 20L * 1024L * 1024L, // 20MB
        )
        val okHttpClient = OkHttpClient.Builder()
            // Specify the cache we created earlier.
            .cache(myCache)
            // Add an Interceptor to the OkHttpClient.
            .addInterceptor a@{ chain ->
                // Get the request from the chain.
                var request = chain.request()

                val shouldUseCache =
                    request.header(CACHE_CONTROL_HEADER) != CACHE_CONTROL_NO_CACHE
                if (!shouldUseCache) {
                    return@a chain.proceed(request)
                }

                /*
                 *  Leveraging the advantage of using Kotlin,
                 *  we initialize the request and change its header depending on whether
                 *  the device is connected to Internet or not.
                 */
                val cachePolicy = cachePolicyManager.cachePolicy

                request = if (cachePolicy == CachePolicy.Minimum) {
                    request
                } else if (context.hasInternet()) {
                    val cacheControl = CacheControl.Builder()
                        .apply {
                            when (cachePolicy) {
                                CachePolicy.Aggressive -> {
                                    maxStale(30, TimeUnit.MINUTES)
                                }
                                CachePolicy.Moderate -> {
                                    maxStale(10, TimeUnit.MINUTES)
                                }
                                CachePolicy.Lite -> {
                                    maxStale(5, TimeUnit.MINUTES)
                                }
                                CachePolicy.Minimum -> error("Unreachable")
                            }
                        }
                        .build()
                    /*
                     *  If there is Internet, get the cache that was stored 5 seconds ago.
                     *  If the cache is older than 5 seconds, then discard it,
                     *  and indicate an error in fetching the response.
                     *  The 'max-age' attribute is responsible for this behavior.
                     */
                    request.newBuilder()
                        .header(
                            CACHE_CONTROL_HEADER,
                            cacheControl.toString(),
                        )
                        .removeHeader("Pragma")
                        .build()
                } else {
                    /*
                     *  If there is no Internet, get the cache that was stored 7 days ago.
                     *  If the cache is older than 7 days, then discard it,
                     *  and indicate an error in fetching the response.
                     *  The 'max-stale' attribute is responsible for this behavior.
                     *  The 'only-if-cached' attribute indicates to not retrieve new data;
                     *  fetch the cache only instead.
                     */
                    request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .removeHeader("Pragma")
                        .build()
                }
                // End of if-else statement

                Log.d(debugName, "headers: ${request.headers} url ${request.url}")

                // Add the modified request to the chain.
                val response = chain.proceed(request)

                Log.d(debugName, "header: ${request.headers}")
                Log.d(debugName, "Response 1 response:          $response")
                Log.d(debugName, "Response 1 cache response:    ${response.cacheResponse}")
                Log.d(debugName, "Response 1 network response:  ${response.networkResponse}")

                response
            }
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .header("User-Agent", getUserAgent(purpose))
                val newRequest = requestBuilder.build()

                chain.proceed(newRequest)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor()
                    loggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
                    addInterceptor(loggingInterceptor)
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .enableTls12()
            .build()

        return okHttpClient
    }
}
