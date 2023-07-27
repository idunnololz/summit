package com.idunnololz.summit.hidePosts

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idunnololz.summit.api.dto.PostId

@Entity(tableName = "hidden_posts")
data class HiddenPostEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val ts: Long,
    val instance: String,
    val postId: PostId,
)
