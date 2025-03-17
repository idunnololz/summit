package com.idunnololz.summit.api

import com.idunnololz.summit.util.ClientFactory
import com.idunnololz.summit.util.DirectoryHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {
    @Provides
    @Singleton
    fun provideSummitServerApi(
        clientFactory: ClientFactory,
        directoryHelper: DirectoryHelper,
        json: Json,
    ): SummitServerApi {
        return Retrofit.Builder()
            .baseUrl("https://summitforlemmyserver.idunnololz.com")
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json; charset=UTF8".toMediaType(),
                ),
            )
            .client(
                clientFactory.newClient(
                    "SummitApi",
                    directoryHelper.okHttpCacheDir,
                    ClientFactory.Purpose.SummitApiClient,
                ),
            )
            .build()
            .create(SummitServerApi::class.java)
    }
}
