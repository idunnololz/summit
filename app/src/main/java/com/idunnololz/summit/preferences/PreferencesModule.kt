package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import com.idunnololz.summit.util.PreferenceUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AccountIdsSharedPreference

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NotificationsSharedPreference

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StateSharedPreference

@InstallIn(SingletonComponent::class)
@Module
class PreferencesModule {
    @Provides
    @Singleton
    fun providePreferences(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences,
    ): Preferences = Preferences(context, sharedPreferences)

    @Provides
    @Singleton
    fun provideSharedPreferences(): SharedPreferences = PreferenceUtil.preferences

    @AccountIdsSharedPreference
    @Provides
    fun provideAccountIdsSharedPreference(preferenceManager: PreferenceManager): SharedPreferences =
        preferenceManager.getAccountIdSharedPreferences()

    @NotificationsSharedPreference
    @Provides
    fun provideNotificationsSharedPreference(
        preferenceManager: PreferenceManager,
    ): SharedPreferences = preferenceManager.getAccountIdSharedPreferences()

    @StateSharedPreference
    @Provides
    fun provideStateSharedPreference(preferenceManager: PreferenceManager): SharedPreferences =
        preferenceManager.getGlobalStateSharedPreferences()
}
