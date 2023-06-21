package com.idunnololz.summit.db

import android.content.Context
import com.idunnololz.summit.account.AccountDao
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
    @Singleton
    fun provideMainDatabase(@ApplicationContext context: Context): MainDatabase =
        MainDatabase.getInstance(context)
}