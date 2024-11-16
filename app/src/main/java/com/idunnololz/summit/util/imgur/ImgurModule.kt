package com.idunnololz.summit.util.imgur

import com.idunnololz.summit.util.Client.get
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class ImgurModule {

    @Provides
    fun provideImgurApi(): ImgurApi = Retrofit.Builder()
        .baseUrl("https://api.imgur.com")
        .client(get())
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(ImgurApi::class.java)
}
