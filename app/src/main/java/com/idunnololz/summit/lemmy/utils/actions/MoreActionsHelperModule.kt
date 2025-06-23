package com.idunnololz.summit.lemmy.utils.actions

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MoreActionsHelperModule {

  @Provides
  @Singleton
  fun provideMoreActionsHelper(moreActionsHelperManager: MoreActionsHelperManager) =
    moreActionsHelperManager.getDefaultInstance()
}
