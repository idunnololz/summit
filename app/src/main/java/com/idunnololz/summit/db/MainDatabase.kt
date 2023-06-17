package com.idunnololz.summit.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountDao
import com.idunnololz.summit.history.HistoryConverters
import com.idunnololz.summit.history.HistoryDao
import com.idunnololz.summit.history.HistoryEntry
import com.idunnololz.summit.reddit_actions.RedditAction
import com.idunnololz.summit.reddit_actions.RedditActionDao
import com.idunnololz.summit.tabs.TabEntry
import com.idunnololz.summit.tabs.TabsDao

/**
 * Db that contains actions taken by the user. This is necessary to cache all of the user's actions.
 */
@Database(
    entities = [
        RedditAction::class,
        TabEntry::class,
        HistoryEntry::class,
        Account::class
    ],
    version = 16
)
@TypeConverters(HistoryConverters::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun redditActionDao(): RedditActionDao
    abstract fun tabsDao(): TabsDao
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
            val db = Room
                .databaseBuilder(
                    context.applicationContext,
                    MainDatabase::class.java, "main.db"
                )
                .fallbackToDestructiveMigration()
                .build()
            return db
        }
    }
}