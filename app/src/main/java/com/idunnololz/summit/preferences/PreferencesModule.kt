package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.lemmy.utils.stateStorage.StateStorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json

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
    sharedPreferencesManager: SharedPreferencesManager,
    coroutineScopeFactory: CoroutineScopeFactory,
    json: Json,
  ): Preferences = Preferences(
    context = context,
    sharedPreferencesManager = sharedPreferencesManager,
    sharedPreferences = sharedPreferencesManager.defaultSharedPreferences,
    coroutineScopeFactory = coroutineScopeFactory,
    json = json,
  )

  @AccountIdsSharedPreference
  @Provides
  fun provideAccountIdsSharedPreference(
    preferenceManager: SharedPreferencesManager,
  ): SharedPreferences = preferenceManager.getAccountIdSharedPreferences()

  @NotificationsSharedPreference
  @Provides
  fun provideNotificationsSharedPreference(
    preferenceManager: SharedPreferencesManager,
  ): SharedPreferences = preferenceManager.getAccountIdSharedPreferences()

  @StateSharedPreference
  @Provides
  fun provideStateSharedPreference(
    preferenceManager: SharedPreferencesManager,
  ): SharedPreferences = preferenceManager.getGlobalStateSharedPreferences()

  @Provides
  @Singleton
  fun provideGlobalStateStorage(stateStorageManager: StateStorageManager): GlobalStateStorage =
    stateStorageManager.globalStateStorage
}
