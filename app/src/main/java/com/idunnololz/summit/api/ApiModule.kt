package com.idunnololz.summit.api

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.cache.CachePolicyManager
import com.idunnololz.summit.preferences.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {
    @Provides
    @Singleton
    fun provideSummitServerApi(
        @ApplicationContext context: Context,
        cachePolicyManager: CachePolicyManager,
    ): SummitServerApi =
        SummitServerApi.newInstance(
            context = context,
            userAgent = "Summit / ${BuildConfig.VERSION_NAME} ${BuildConfig.APPLICATION_ID}",
            cachePolicyManager = cachePolicyManager,
        )
}