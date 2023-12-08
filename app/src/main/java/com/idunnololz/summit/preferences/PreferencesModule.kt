package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.util.PreferenceUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class PreferencesModule {
    @Provides
    @Singleton
    fun providePreferences(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences
    ): Preferences =
        Preferences(context, sharedPreferences)

    @Provides
    @Singleton
    fun provideSharedPreferences(): SharedPreferences =
        PreferenceUtil.preferences
}