package com.idunnololz.summit.api

import com.idunnololz.summit.api.LemmyApi.Companion.CACHE_CONTROL_HEADER
import com.idunnololz.summit.api.LemmyApi.Companion.CACHE_CONTROL_NO_CACHE
import com.idunnololz.summit.api.summit.CommunitySuggestionsDto
import com.idunnololz.summit.api.summit.PresetDto
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface SummitServerApi {

    @GET("/v1/community-suggestions")
    fun communitySuggestions(): Call<CommunitySuggestionsDto>

    @GET("/v1/community-suggestions")
    @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
    fun communitySuggestionsNoCache(): Call<CommunitySuggestionsDto>

    @Multipart
    @POST("/v1/preset")
    fun submitPreset(
        @Part("preset") preset: PresetDto,
        @Part phoneScreenshot: MultipartBody.Part?,
        @Part tabletScreenshot: MultipartBody.Part?,
    ): Call<PresetDto>

    @GET("/v1/preset")
    fun getPresets(): Call<List<PresetDto>>

    @GET("/v1/preset")
    @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
    fun getPresetsNoCache(): Call<List<PresetDto>>

    @GET("/v1/preset/all")
    fun getAllPresets(): Call<List<PresetDto>>

    @GET("/v1/preset/all")
    @Headers("$CACHE_CONTROL_HEADER: $CACHE_CONTROL_NO_CACHE")
    fun getAllPresetsNoCache(): Call<List<PresetDto>>
}
