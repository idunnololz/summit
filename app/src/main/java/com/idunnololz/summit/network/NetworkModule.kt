package com.idunnololz.summit.network

import com.idunnololz.summit.BuildConfig
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
  @BrowserLikeAuthed
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
  @BrowserLikeUnauthed
  fun provideBrowserLikeUnauthedOkHttpClient(
    clientFactory: ClientFactory,
    directoryHelper: DirectoryHelper,
  ): OkHttpClient = clientFactory.newClient(
    debugName = "BrowserLike",
    cacheDir = directoryHelper.okHttpCacheDir,
    purpose = ClientFactory.Purpose.BrowserLikeUnauthed,
  )

  @Provides
  @Singleton
  @LemmyApi
  fun provideApiOkHttpClient(
    clientFactory: ClientFactory,
    directoryHelper: DirectoryHelper,
  ): OkHttpClient = clientFactory.newClient(
    debugName = "Api",
    cacheDir = directoryHelper.okHttpCacheDir,
    purpose = ClientFactory.Purpose.LemmyApiClient,
  )

  @Provides
  @Singleton
  @SummitApi
  fun provideSummitApiOkHttpClient(
    clientFactory: ClientFactory,
    directoryHelper: DirectoryHelper,
  ): OkHttpClient = clientFactory
    .newClient(
      "SummitApi",
      directoryHelper.okHttpCacheDir,
      ClientFactory.Purpose.SummitApiClient,
    )
    .newBuilder()
    .addNetworkInterceptor {
      val requestBuilder = it.request().newBuilder()
      requestBuilder.header(
        "Authorization",
        "Bearer ${BuildConfig.SUMMIT_JWT}",
      )
      it.proceed(requestBuilder.build())
    }
    .build()
}

/**
 * Used to make HTTP calls under the guise of a browser.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BrowserLikeAuthed

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BrowserLikeUnauthed

/**
 * Used to make API calls.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class LemmyApi

/**
 * Used to make API calls for the Summit server.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SummitApi
