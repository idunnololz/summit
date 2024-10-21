package com.idunnololz.summit.db

import android.content.Context
import com.idunnololz.summit.account.AccountDao
import com.idunnololz.summit.account.info.AccountInfoDao
import com.idunnololz.summit.actions.db.PostReadDao
import com.idunnololz.summit.drafts.DraftsDao
import com.idunnololz.summit.emoji.db.TextEmojiDao
import com.idunnololz.summit.filterLists.ContentFiltersDao
import com.idunnololz.summit.hidePosts.HiddenPostsDao
import com.idunnololz.summit.history.HistoryDao
import com.idunnololz.summit.lemmy.actions.LemmyActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyCompletedActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyFailedActionsDao
import com.idunnololz.summit.lemmy.inbox.InboxEntriesDao
import com.idunnololz.summit.lemmy.inbox.db.ConversationEntriesDao
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
    fun provideAccountDao(db: MainDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideUserCommunitiesDao(db: MainDatabase): UserCommunitiesDao = db.userCommunitiesDao()

    @Provides
    fun provideLemmyActionsDao(db: MainDatabase): LemmyActionsDao = db.lemmyActionsDao()

    @Provides
    fun provideLemmyFailedActionsDao(db: MainDatabase): LemmyFailedActionsDao =
        db.lemmyFailedActionsDao()

    @Provides
    fun provideLemmyCompletedActionsDao(db: MainDatabase): LemmyCompletedActionsDao =
        db.lemmyCompletedActionsDao()

    @Provides
    fun provideHistoryDao(db: MainDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideAccountInfoDao(db: MainDatabase): AccountInfoDao = db.accountInfoDao()

    @Provides
    fun provideHiddenPostsDao(db: MainDatabase): HiddenPostsDao = db.hiddenPostsDao()

    @Provides
    fun provideContentFiltersDao(db: MainDatabase): ContentFiltersDao = db.contentFiltersDao()

    @Provides
    fun provideDraftsDao(db: MainDatabase): DraftsDao = db.draftsDao()

    @Provides
    fun provideInboxEntriesDao(db: MainDatabase): InboxEntriesDao = db.inboxEntriesDao()

    @Provides
    fun provideConversationEntriesDao(db: MainDatabase): ConversationEntriesDao =
        db.conversationEntriesDao()

    @Provides
    fun providePostReadDao(db: MainDatabase): PostReadDao =
        db.postReadDao()

    @Provides
    fun provideTextEmojiDao(db: MainDatabase): TextEmojiDao =
        db.textEmojiDao()

    @Provides
    @Singleton
    fun provideMainDatabase(@ApplicationContext context: Context): MainDatabase =
        MainDatabase.getInstance(context)
}
