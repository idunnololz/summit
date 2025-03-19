package com.idunnololz.summit.util.imgur

import com.idunnololz.summit.network.BrowserLike
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class ImgurModule {

    @Provides
    fun provideImgurApi(
        json: Json,
        @BrowserLike okHttpClient: OkHttpClient
    ): ImgurApi = Retrofit.Builder()
        .baseUrl("https://api.imgur.com")
        .client(okHttpClient)
        .addConverterFactory(
            json.asConverterFactory(
                "application/json; charset=UTF8".toMediaType(),
            ),
        )
        .build()
        .create(ImgurApi::class.java)
}
