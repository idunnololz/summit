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
import com.idunnololz.summit.history.HistoryConverters
import com.idunnololz.summit.history.HistoryDao
import com.idunnololz.summit.history.HistoryEntry
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionConverters
import com.idunnololz.summit.lemmy.actions.LemmyActionsDao
import com.idunnololz.summit.user.UserCommunityEntry
import com.idunnololz.summit.user.UserCommunitiesConverters
import com.idunnololz.summit.user.UserCommunitiesDao
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
    ],
    autoMigrations = [
        AutoMigration (
            from = 20,
            to = 21
        ),
    ],
    version = 21,
    exportSchema = true,
)
@TypeConverters(HistoryConverters::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun lemmyActionsDao(): LemmyActionsDao
    abstract fun userCommunitiesDao(): UserCommunitiesDao
    abstract fun historyDao(): HistoryDao
    abstract fun accountDao(): AccountDao

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
                    MainDatabase::class.java, "main.db"
                )
                .fallbackToDestructiveMigration()
                .addTypeConverter(LemmyActionConverters(moshi))
                .addTypeConverter(UserCommunitiesConverters(moshi))
                .addMigrations(MIGRATION_19_20)
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