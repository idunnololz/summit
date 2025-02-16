package com.idunnololz.summit.network

import android.content.Context
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.api.LemmyApi
import com.idunnololz.summit.api.SummitServerApi
import com.idunnololz.summit.cache.CachePolicyManager
import com.idunnololz.summit.util.DirectoryHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        cachePolicyManager: CachePolicyManager,
        directoryHelper: DirectoryHelper,
    ): OkHttpClient =
        LemmyApi.okHttpClient(
            context = context,
            cachePolicyManager = cachePolicyManager,
            userAgent = "Summit / ${BuildConfig.VERSION_NAME} ${BuildConfig.APPLICATION_ID}",
            cacheDir = directoryHelper.okHttpCacheDir,
        )
}