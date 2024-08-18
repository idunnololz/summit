package com.idunnololz.summit.lemmy.inbox.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_entries")
data class ConversationEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "ts")
    val ts: Long,
    @ColumnInfo(name = "account_full_name")
    val accountStableId: String,
    @ColumnInfo(name = "person_id")
    val personId: Long, // the person you are conversing with
    @ColumnInfo(name = "person_instance")
    val personInstance: String,
    @ColumnInfo(name = "person_name")
    val personName: String?,
    @ColumnInfo(name = "title")
    val title: String, // Usually the name of the person you are conversing with
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
    @ColumnInfo(name = "content")
    val content: String?, // Usually the last message sent
    @ColumnInfo(name = "is_read")
    val isRead: Boolean,
    @ColumnInfo(name = "most_recent_message_id")
    val mostRecentMessageId: Long?,
)
