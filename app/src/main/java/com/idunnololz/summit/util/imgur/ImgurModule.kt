package com.idunnololz.summit.util.imgur

import com.idunnololz.summit.util.Client.get
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class ImgurModule {

    @Provides
    fun provideImgurApi(json: Json): ImgurApi = Retrofit.Builder()
        .baseUrl("https://api.imgur.com")
        .client(get())
        .addConverterFactory(
            json.asConverterFactory(
                "application/json; charset=UTF8".toMediaType(),
            ),
        )
        .build()
        .create(ImgurApi::class.java)
}
