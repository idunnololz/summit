package com.idunnololz.summit.api

import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.network.SummitApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {
    @Provides
    @Singleton
    fun provideSummitServerApi(
        @SummitApi okHttpClient: OkHttpClient,
        json: Json,
    ): SummitServerApi {
        return Retrofit.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    baseUrl("http://10.0.2.2:8080")
                } else {
                    baseUrl("https://summitforlemmyserver.idunnololz.com")
                }
            }
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json; charset=UTF8".toMediaType(),
                ),
            )
            .client(okHttpClient)
            .build()
            .create(SummitServerApi::class.java)
    }
}
