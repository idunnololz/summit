package com.idunnololz.summit.api.openai

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface OpenAiApi {
  @Multipart
  @POST("/v1/content_provenance_checks")
  fun createContentProvenanceCheck(
    @Header("Authorization") authorization: String,
    @Part file: MultipartBody.Part,
  ): Call<ContentProvenanceCheck>
}
