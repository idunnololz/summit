package com.idunnololz.summit.api

import android.content.Context
import com.idunnololz.summit.api.LemmyApi.Companion.CACHE_CONTROL_HEADER
import com.idunnololz.summit.api.LemmyApi.Companion.CACHE_CONTROL_NO_CACHE
import com.idunnololz.summit.api.LemmyApi.Companion.getOkHttpClient
import com.idunnololz.summit.api.summit.CommunitySuggestionsDto
import com.idunnololz.summit.cache.CachePolicyManager
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers

interface SummitServerApi {

    @GET("/v1/community-suggestions")
    fun communitySuggestions(): Call<CommunitySuggestionsDto>

    @GET("/v1/community-suggestions")
    @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
    fun communitySuggestionsNoCache(): Call<CommunitySuggestionsDto>

    companion object {

        fun newInstance(
            context: Context,
            userAgent: String,
            cachePolicyManager: CachePolicyManager,
        ): SummitServerApi {
            return Retrofit.Builder()
                .baseUrl("https://summitforlemmyserver.idunnololz.com")
                .addConverterFactory(GsonConverterFactory.create())
                .client(getOkHttpClient(context, userAgent, cachePolicyManager))
                .build()
                .create(SummitServerApi::class.java)
        }
    }
}
