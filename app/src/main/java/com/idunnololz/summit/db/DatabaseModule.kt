package com.idunnololz.summit.db

import android.content.Context
import com.idunnololz.summit.account.AccountDao
import com.idunnololz.summit.history.HistoryDao
import com.idunnololz.summit.lemmy.actions.LemmyActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyFailedActionsDao
import com.idunnololz.summit.user.UserCommunitiesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Provides
    fun provideAccountDao(db: MainDatabase): AccountDao =
        db.accountDao()

    @Provides
    fun provideUserCommunitiesDao(db: MainDatabase): UserCommunitiesDao =
        db.userCommunitiesDao()

    @Provides
    fun provideLemmyActionsDao(db: MainDatabase): LemmyActionsDao =
        db.lemmyActionsDao()

    @Provides
    fun provideLemmyFailedActionsDao(db: MainDatabase): LemmyFailedActionsDao =
        db.lemmyFailedActionsDao()

    @Provides
    fun provideHistoryDao(db: MainDatabase): HistoryDao =
        db.historyDao()

    @Provides
    @Singleton
    fun provideMainDatabase(@ApplicationContext context: Context): MainDatabase =
        MainDatabase.getInstance(context)
}