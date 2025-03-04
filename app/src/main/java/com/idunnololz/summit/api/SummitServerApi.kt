package com.idunnololz.summit.api

import com.idunnololz.summit.api.LemmyApi.Companion.CACHE_CONTROL_HEADER
import com.idunnololz.summit.api.LemmyApi.Companion.CACHE_CONTROL_NO_CACHE
import com.idunnololz.summit.api.summit.CommunitySuggestionsDto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers

interface SummitServerApi {

    @GET("/v1/community-suggestions")
    fun communitySuggestions(): Call<CommunitySuggestionsDto>

    @GET("/v1/community-suggestions")
    @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
    fun communitySuggestionsNoCache(): Call<CommunitySuggestionsDto>
}
