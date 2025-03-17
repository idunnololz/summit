package com.idunnololz.summit.network

import com.idunnololz.summit.util.ClientFactory
import com.idunnololz.summit.util.DirectoryHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Provides
    @Singleton
    @BrowserLike
    fun provideBrowserLikeOkHttpClient(
        clientFactory: ClientFactory,
        directoryHelper: DirectoryHelper,
    ): OkHttpClient = clientFactory.newClient(
        debugName = "BrowserLike",
        cacheDir = directoryHelper.okHttpCacheDir,
        purpose = ClientFactory.Purpose.BrowserLike,
    )

    @Provides
    @Singleton
    @Api
    fun provideApiOkHttpClient(
        clientFactory: ClientFactory,
        directoryHelper: DirectoryHelper,
    ): OkHttpClient = clientFactory.newClient(
        debugName = "Api",
        cacheDir = directoryHelper.okHttpCacheDir,
        purpose = ClientFactory.Purpose.LemmyApiClient,
    )
}

/**
 * Used to make HTTP calls under the guise of a browser.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BrowserLike

/**
 * Used to make API calls.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Api
