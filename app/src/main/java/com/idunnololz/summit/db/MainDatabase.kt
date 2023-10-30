package com.idunnololz.summit.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountDao
import com.idunnololz.summit.account.info.AccountInfo
import com.idunnololz.summit.account.info.AccountInfoConverters
import com.idunnololz.summit.account.info.AccountInfoDao
import com.idunnololz.summit.drafts.DraftConverters
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftsDao
import com.idunnololz.summit.filterLists.ContentFiltersDao
import com.idunnololz.summit.filterLists.FilterEntry
import com.idunnololz.summit.hidePosts.HiddenPostEntry
import com.idunnololz.summit.hidePosts.HiddenPostsDao
import com.idunnololz.summit.history.HistoryConverters
import com.idunnololz.summit.history.HistoryDao
import com.idunnololz.summit.history.HistoryEntry
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionConverters
import com.idunnololz.summit.lemmy.actions.LemmyActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.LemmyCompletedActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyFailedAction
import com.idunnololz.summit.lemmy.actions.LemmyFailedActionsDao
import com.idunnololz.summit.user.UserCommunitiesConverters
import com.idunnololz.summit.user.UserCommunitiesDao
import com.idunnololz.summit.user.UserCommunityEntry
import com.idunnololz.summit.util.moshi

/**
 * Db that contains actions taken by the user. This is necessary to cache all of the user's actions.
 */
@Database(
    entities = [
        UserCommunityEntry::class,
        HistoryEntry::class,
        Account::class,
        LemmyAction::class,
        LemmyFailedAction::class,
        LemmyCompletedAction::class,
        AccountInfo::class,
        HiddenPostEntry::class,
        FilterEntry::class,
        DraftEntry::class,
    ],
    autoMigrations = [
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 29, to = 30),
        AutoMigration(from = 31, to = 32),
    ],
    version = 32,
    exportSchema = true,
)
@TypeConverters(HistoryConverters::class, DraftConverters::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun lemmyActionsDao(): LemmyActionsDao
    abstract fun lemmyFailedActionsDao(): LemmyFailedActionsDao
    abstract fun lemmyCompletedActionsDao(): LemmyCompletedActionsDao
    abstract fun userCommunitiesDao(): UserCommunitiesDao
    abstract fun historyDao(): HistoryDao
    abstract fun accountDao(): AccountDao
    abstract fun accountInfoDao(): AccountInfoDao
    abstract fun hiddenPostsDao(): HiddenPostsDao
    abstract fun contentFiltersDao(): ContentFiltersDao
    abstract fun draftsDao(): DraftsDao

    companion object {

        @Volatile
        private var INSTANCE: MainDatabase? = null

        fun getInstance(context: Context): MainDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): MainDatabase {
            val moshi = moshi

            return Room
                .databaseBuilder(
                    context.applicationContext,
                    MainDatabase::class.java,
                    "main.db",
                )
                .fallbackToDestructiveMigration()
                .addTypeConverter(LemmyActionConverters(moshi))
                .addTypeConverter(UserCommunitiesConverters(moshi))
                .addTypeConverter(AccountInfoConverters(moshi))
                .addTypeConverter(DraftConverters(moshi))
                .addMigrations(MIGRATION_19_20)
                .addMigrations(MIGRATION_21_22)
                .addMigrations(MIGRATION_22_24)
                .addMigrations(MIGRATION_23_24)
                .addMigrations(MIGRATION_24_26)
                .addMigrations(MIGRATION_25_26)
                .addMigrations(MIGRATION_28_29)
                .addMigrations(MIGRATION_30_31)
                .build()
        }
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS tabs;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `user_communities` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sortOrder` INTEGER NOT NULL, `communitySortOrder` TEXT NOT NULL, `ref` TEXT)")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS lemmy_failed_actions;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `lemmy_failed_actions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ts` INTEGER NOT NULL, `cts` INTEGER NOT NULL, `fts` INTEGER NOT NULL, `error` TEXT NOT NULL, `info` TEXT)")
    }
}

val MIGRATION_22_24 = object : Migration(22, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS account_info;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `account_info` (`account_id` INTEGER NOT NULL, `subscriptions` TEXT, PRIMARY KEY(`account_id`))")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS account_info;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `account_info` (`account_id` INTEGER NOT NULL, `subscriptions` TEXT, PRIMARY KEY(`account_id`))")
    }
}

val MIGRATION_24_26 = object : Migration(24, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS account_info;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `account_info` (`account_id` INTEGER NOT NULL, `subscriptions` TEXT, `misc_account_info` TEXT, PRIMARY KEY(`account_id`))")
    }
}

val MIGRATION_25_26 = object : Migration(24, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS account_info;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `account_info` (`account_id` INTEGER NOT NULL, `subscriptions` TEXT, `misc_account_info` TEXT, PRIMARY KEY(`account_id`))")
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS drafts;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `drafts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cts` INTEGER NOT NULL, `uts` INTEGER NOT NULL, `draft_type` INTEGER NOT NULL, `data` TEXT)")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS lemmy_completed_actions;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `lemmy_completed_actions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ts` INTEGER NOT NULL, `cts` INTEGER NOT NULL, `info` TEXT)")
    }
}
