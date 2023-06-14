package com.idunnololz.summit.auth

import android.os.Build
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.reddit.RateLimitManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import kotlin.math.roundToInt

class RedditInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = updateRequestIfNeeded(chain.request())
        val response = chain.proceed(request)
        processResponse(response)
        return response
    }

    private fun processResponse(response: Response) {
        val remainingTokens =
            response.headers.get("x-ratelimit-remaining")?.toDouble()?.roundToInt() ?: -1
        val tokensUsed = response.headers.get("x-ratelimit-used")?.toDouble()?.roundToInt() ?: -1
        val timeUntilReset =
            response.headers.get("x-ratelimit-reset")?.toDouble()?.roundToInt() ?: -1

        if (remainingTokens != -1 && tokensUsed != -1 && timeUntilReset != -1) {
            RateLimitManager.onRateLimitInfoReceived(remainingTokens, tokensUsed, timeUntilReset)
        }
    }

    private fun updateRequestIfNeeded(request: Request): Request {
        val requestBuilder = request.newBuilder()

        if (request.url.host == "oauth.reddit.com") {
            val authToken = RedditAuthManager.instance.authToken
            if (authToken != null) {
                requestBuilder
                    .header("Authorization", "Bearer $authToken")
            } else {
                requestBuilder
                    .url(request.url.newBuilder().host("www.reddit.com").build())
            }
        }

        return requestBuilder
            .header(
                "User-Agent",
                "com.idunnololz.Summit/${BuildConfig.VERSION_NAME} (Linux;Android ${Build.VERSION.RELEASE}) by u/rumias"
            )
            .build()
    }
}