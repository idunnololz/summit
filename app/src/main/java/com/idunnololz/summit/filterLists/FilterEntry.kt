package com.idunnololz.summit.filterLists

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "content_filters")
@Parcelize
data class FilterEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    /**
     * What this filter is for. Eg. post list, comments, etc.
     */
    val contentType: ContentTypeId,
    /**
     * What this filter is against. Eg. post title, instance, etc.
     */
    val filterType: FilterTypeId,
    val filter: String,
    val isRegex: Boolean,
) : Parcelable

typealias FilterTypeId = Int

object FilterTypes {
    const val KeywordFilter = 1
    const val InstanceFilter = 2
    const val CommunityFilter = 3
    const val UserFilter = 4
}

typealias ContentTypeId = Int

object ContentTypes {
    const val PostListType = 1
}
