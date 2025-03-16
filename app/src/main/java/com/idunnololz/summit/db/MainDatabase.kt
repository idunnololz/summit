package com.idunnololz.summit.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountDao
import com.idunnololz.summit.account.info.AccountInfo
import com.idunnololz.summit.account.info.AccountInfoConverters
import com.idunnololz.summit.account.info.AccountInfoDao
import com.idunnololz.summit.actions.db.PostReadDao
import com.idunnololz.summit.actions.db.ReadPostEntry
import com.idunnololz.summit.drafts.DraftConverters
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftsDao
import com.idunnololz.summit.emoji.db.TextEmojiDao
import com.idunnololz.summit.emoji.db.TextEmojiEntry
import com.idunnololz.summit.filterLists.ContentFiltersDao
import com.idunnololz.summit.filterLists.FilterEntry
import com.idunnololz.summit.hidePosts.HiddenPostEntry
import com.idunnololz.summit.hidePosts.HiddenPostsDao
import com.idunnololz.summit.history.HistoryConverters
import com.idunnololz.summit.history.HistoryDao
import com.idunnololz.summit.history.HistoryEntry
import com.idunnololz.summit.lemmy.actions.LemmyActionConverters
import com.idunnololz.summit.lemmy.actions.LemmyActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.LemmyCompletedActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyFailedAction
import com.idunnololz.summit.lemmy.actions.LemmyFailedActionsDao
import com.idunnololz.summit.lemmy.actions.LemmyPendingAction
import com.idunnololz.summit.lemmy.inbox.InboxEntriesDao
import com.idunnololz.summit.lemmy.inbox.InboxEntry
import com.idunnololz.summit.lemmy.inbox.InboxEntryConverters
import com.idunnololz.summit.lemmy.inbox.db.ConversationEntriesDao
import com.idunnololz.summit.lemmy.inbox.db.ConversationEntry
import com.idunnololz.summit.lemmy.userTags.UserTagConverters
import com.idunnololz.summit.lemmy.userTags.UserTagEntry
import com.idunnololz.summit.lemmy.userTags.UserTagsDao
import com.idunnololz.summit.user.UserCommunitiesConverters
import com.idunnololz.summit.user.UserCommunitiesDao
import com.idunnololz.summit.user.UserCommunityEntry
import com.idunnololz.summit.util.dagger.json
import kotlinx.serialization.json.Json

/**
 * Db that contains actions taken by the user. This is necessary to cache all of the user's actions.
 */
@Database(
    entities = [
        UserCommunityEntry::class,
        HistoryEntry::class,
        Account::class,
        LemmyPendingAction::class,
        LemmyFailedAction::class,
        LemmyCompletedAction::class,
        AccountInfo::class,
        HiddenPostEntry::class,
        FilterEntry::class,
        DraftEntry::class,
        InboxEntry::class,
        ConversationEntry::class,
        ReadPostEntry::class,
        TextEmojiEntry::class,
        UserTagEntry::class,
    ],
    autoMigrations = [
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 29, to = 30),
        AutoMigration(from = 31, to = 32),
        AutoMigration(from = 32, to = 33),
        AutoMigration(from = 41, to = 42),
        AutoMigration(from = 42, to = 43),
        AutoMigration(from = 43, to = 44),
    ],
    version = 44,
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
    abstract fun inboxEntriesDao(): InboxEntriesDao
    abstract fun conversationEntriesDao(): ConversationEntriesDao
    abstract fun postReadDao(): PostReadDao
    abstract fun textEmojiDao(): TextEmojiDao
    abstract fun userTagsDao(): UserTagsDao

    companion object {

        @Volatile
        private var INSTANCE: MainDatabase? = null

        const val DATABASE_NAME = "main.db"

        fun getInstance(context: Context, json: Json): MainDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, DATABASE_NAME, json).also { INSTANCE = it }
            }

        fun buildDatabase(context: Context, dbName: String, json: Json): MainDatabase {
//            if (BuildConfig.DEBUG) {
//                context.deleteDatabase("main.db")
//                val db = context.openOrCreateDatabase("main.db", 0, null)
//                db.execSQL("DROP TABLE drafts;")
//            }

            return Room
                .databaseBuilder(
                    context.applicationContext,
                    MainDatabase::class.java,
                    dbName,
                )
                .apply {
                    if (!BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration()
                    }
                }
                .addTypeConverter(LemmyActionConverters(json))
                .addTypeConverter(UserCommunitiesConverters(json))
                .addTypeConverter(AccountInfoConverters(json))
                .addTypeConverter(DraftConverters(json))
                .addTypeConverter(InboxEntryConverters(json))
                .addTypeConverter(UserTagConverters(json))
                .addMigrations(MIGRATION_19_20)
                .addMigrations(MIGRATION_21_22)
                .addMigrations(MIGRATION_22_24)
                .addMigrations(MIGRATION_23_24)
                .addMigrations(MIGRATION_24_26)
                .addMigrations(MIGRATION_25_26)
                .addMigrations(MIGRATION_28_29)
                .addMigrations(MIGRATION_30_31)
                .addMigrations(MIGRATION_33_34)
                .addMigrations(MIGRATION_34_37)
                .addMigrations(MIGRATION_36_37)
                .addMigrations(MIGRATION_37_38)
                .addMigrations(MIGRATION_38_39)
                .addMigrations(MIGRATION_39_38)
                .addMigrations(MIGRATION_39_40)
                .addMigrations(MIGRATION_40_41)
                .build()
        }
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS tabs;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_communities` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sortOrder` INTEGER NOT NULL, `communitySortOrder` TEXT NOT NULL, `ref` TEXT)",
        )
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS lemmy_failed_actions;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `lemmy_failed_actions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ts` INTEGER NOT NULL, `cts` INTEGER NOT NULL, `fts` INTEGER NOT NULL, `error` TEXT NOT NULL, `info` TEXT)",
        )
    }
}

val MIGRATION_22_24 = object : Migration(22, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS account_info;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `account_info` (`account_id` INTEGER NOT NULL, `subscriptions` TEXT, PRIMARY KEY(`account_id`))",
        )
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS account_info;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `account_info` (`account_id` INTEGER NOT NULL, `subscriptions` TEXT, PRIMARY KEY(`account_id`))",
        )
    }
}

val MIGRATION_24_26 = object : Migration(24, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS account_info;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `account_info` (`account_id` INTEGER NOT NULL, `subscriptions` TEXT, `misc_account_info` TEXT, PRIMARY KEY(`account_id`))",
        )
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS account_info;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `account_info` (`account_id` INTEGER NOT NULL, `subscriptions` TEXT, `misc_account_info` TEXT, PRIMARY KEY(`account_id`))",
        )
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS drafts;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `drafts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cts` INTEGER NOT NULL, `uts` INTEGER NOT NULL, `draft_type` INTEGER NOT NULL, `data` TEXT)",
        )
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS lemmy_completed_actions;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `lemmy_completed_actions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ts` INTEGER NOT NULL, `cts` INTEGER NOT NULL, `info` TEXT)",
        )
    }
}

val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS inbox_entries;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `inbox_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ts` INTEGER NOT NULL, `item_id` INTEGER NOT NULL, `notification_id` INTEGER NOT NULL, `account_full_name` TEXT NOT NULL, `inbox_item` TEXT)",
        )
    }
}

val MIGRATION_34_37 = object : Migration(34, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createConversationsEntriesTable(db)
    }
}

val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createConversationsEntriesTable(db)
    }
}

val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createConversationsEntriesTable(db)
    }
}

val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = db.query("SELECT id, data FROM drafts;")
        val toExecute = mutableListOf<String>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val data = cursor.getString(1)

            val draftData = json.decodeFromString<DraftData>(data)
                ?: continue
            val accountId = draftData.accountId
            val accountInstance = draftData.accountInstance

            toExecute += "UPDATE drafts SET account_id = $accountId, " +
                "account_instance = '$accountInstance' WHERE id = $id"
        }

        db.execSQL("ALTER TABLE drafts ADD COLUMN account_id INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE drafts ADD COLUMN account_instance TEXT NOT NULL DEFAULT ''")

        for (c in toExecute) {
            db.execSQL(c)
        }
    }
}

val MIGRATION_39_38 = object : Migration(39, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE drafts DROP COLUMN account_id;")
        db.execSQL("ALTER TABLE drafts DROP COLUMN account_instance;")
    }
}

val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS conversation_entries;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `conversation_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ts` INTEGER NOT NULL, `account_full_name` TEXT NOT NULL, `person_id` INTEGER NOT NULL, `person_instance` TEXT NOT NULL, `person_name` TEXT, `title` TEXT NOT NULL, `icon_url` TEXT, `content` TEXT, `is_read` INTEGER NOT NULL, `most_recent_message_id` INTEGER)",
        )
    }
}

val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS read_posts;")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `read_posts` (`post_key` TEXT NOT NULL, `read` INTEGER NOT NULL, PRIMARY KEY(`post_key`))",
        )
    }
}

private fun createConversationsEntriesTable(db: SupportSQLiteDatabase) {
    db.execSQL("DROP TABLE IF EXISTS conversation_entries;")
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `conversation_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ts` INTEGER NOT NULL, `account_full_name` TEXT NOT NULL, `person_id` INTEGER NOT NULL, `person_instance` TEXT, `person_name` TEXT, `title` TEXT NOT NULL, `icon_url` TEXT, `content` TEXT, `is_read` INTEGER NOT NULL, `most_recent_message_id` INTEGER)",
    )
}
