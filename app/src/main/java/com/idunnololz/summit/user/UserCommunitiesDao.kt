package com.idunnololz.summit.user

import android.os.Parcelable
import androidx.room.*
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.squareup.moshi.Moshi
import kotlinx.parcelize.Parcelize

/**
 * Data Access Object
 */
@Dao
interface UserCommunitiesDao {

    @Query("SELECT * FROM user_communities ORDER BY id")
    suspend fun getAllCommunities(): List<UserCommunityEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunity(userCommunityEntry: UserCommunityEntry): Long

    @Query("DELETE FROM user_communities WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM user_communities")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM user_communities")
    suspend fun count(): Int
}

@Entity(tableName = "user_communities")
@TypeConverters(UserCommunitiesConverters::class)
data class UserCommunityEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "sortOrder")
    val sortOrder: Long,
    @ColumnInfo(name = "communitySortOrder")
    val communitySortOrder: CommunitySortOrder,
    @ColumnInfo(name = "ref")
    val communityRef: CommunityRef?,
    @ColumnInfo(name = "iconUrl")
    val iconUrl: String?,
)

@Parcelize
data class UserCommunityItem(
    val id: Long = 0L,
    val sortOrder: Long = 0L,
    val communitySortOrder: CommunitySortOrder = CommunitySortOrder.Active,
    val communityRef: CommunityRef,
    val iconUrl: String? = null,
) : Parcelable

fun UserCommunityItem.toEntry() =
    UserCommunityEntry(
        id,
        sortOrder,
        communitySortOrder,
        communityRef,
        iconUrl,
    )

fun UserCommunityEntry.toItem(): UserCommunityItem? {
    return UserCommunityItem(
        id,
        sortOrder,
        communitySortOrder,
        communityRef ?: return null,
        iconUrl,
    )
}

@ProvidedTypeConverter
class UserCommunitiesConverters(private val moshi: Moshi) {
    @TypeConverter
    fun communitySortOrderToString(value: CommunitySortOrder): String {
        return moshi.adapter(CommunitySortOrder::class.java).toJson(value)
    }

    @TypeConverter
    fun stringToCommunitySortOrder(value: String): CommunitySortOrder =
        try {
            moshi.adapter(CommunitySortOrder::class.java).fromJson(value)
                ?: CommunitySortOrder.Active
        } catch (e: Exception) {
            CommunitySortOrder.Active
        }

    @TypeConverter
    fun communityRefToString(value: CommunityRef): String {
        return moshi.adapter(CommunityRef::class.java).toJson(value)
    }

    @TypeConverter
    fun stringToCommunityRef(value: String): CommunityRef? =
        try {
            moshi.adapter(CommunityRef::class.java).fromJson(value)
        } catch (e: Exception) {
            null
        }
}
