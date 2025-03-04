package com.idunnololz.summit.api

import android.content.Context
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.api.LemmyApi.Companion.getOkHttpClient
import com.idunnololz.summit.cache.CachePolicyManager
import com.idunnololz.summit.util.DirectoryHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {
    @Provides
    @Singleton
    fun provideSummitServerApi(
        @ApplicationContext context: Context,
        cachePolicyManager: CachePolicyManager,
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
                getOkHttpClient(
                    context = context,
                    userAgent = "Summit / ${BuildConfig.VERSION_NAME} ${BuildConfig.APPLICATION_ID}",
                    cachePolicyManager = cachePolicyManager,
                    cacheDir = directoryHelper.okHttpCacheDir,
                ),
            )
            .build()
            .create(SummitServerApi::class.java)
    }
}
