package com.idunnololz.summit.account.info

import android.net.Uri
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.InstanceId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "account_info")
@TypeConverters(AccountInfoConverters::class)
data class AccountInfo(
    @PrimaryKey
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "subscriptions")
    val subscriptions: List<AccountSubscription>?,
    @ColumnInfo(name = "misc_account_info")
    val miscAccountInfo: MiscAccountInfo?,
)

@JsonClass(generateAdapter = true)
data class MiscAccountInfo(
    val avatar: String? = null,
    val defaultCommunitySortType: SortType? = null,
    val showReadPosts: Boolean? = null,
    /**
     * List of community ids that this account is the mod of.
     */
    val modCommunityIds: List<Int>? = null,
    val blockedPersons: List<BlockedPerson>? = null,
    val blockedCommunities: List<BlockedCommunity>? = null,
    val blockedInstances: List<BlockedInstance>? = null,
    val isAdmin: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class AccountSubscription(
    val id: CommunityId,
    val name: String,
    val title: String,
    val removed: Boolean,
    val published: String,
    val updated: String? = null,
    val deleted: Boolean,
    val nsfw: Boolean,
    val actorId: String,
    val local: Boolean,
    val icon: String? = null,
    val banner: String? = null,
    val hidden: Boolean,
    val posting_restricted_to_mods: Boolean,
    val instanceId: InstanceId,
)

@JsonClass(generateAdapter = true)
data class BlockedPerson(
    val personId: PersonId,
    val personRef: PersonRef,
)

@JsonClass(generateAdapter = true)
data class BlockedCommunity(
    val communityId: CommunityId,
    val communityRef: CommunityRef.CommunityRefByName,
)

@JsonClass(generateAdapter = true)
data class BlockedInstance(
    val instanceId: InstanceId,
    val instanceName: String,
)

val AccountSubscription.instance: String?
    get() = Uri.parse(this.actorId).host

@Dao
abstract class AccountInfoDao {
    @Query("SELECT * FROM account_info WHERE account_id = :accountId")
    abstract suspend fun getAccountInfo(accountId: Long): AccountInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = AccountInfo::class)
    abstract suspend fun insert(accountInfo: AccountInfo)

    @Query("DELETE FROM account_info WHERE account_id = :accountId")
    abstract suspend fun delete(accountId: Long)

    @Query("SELECT COUNT(*) FROM account_info")
    abstract suspend fun count(): Int
}

@ProvidedTypeConverter
class AccountInfoConverters(private val moshi: Moshi) {

    companion object {
        private const val TAG = "AccountInfoConverters"
    }

    @TypeConverter
    fun subscriptionsToString(value: List<AccountSubscription>): String {
        return moshi.adapter<List<AccountSubscription>>(
            Types.newParameterizedType(List::class.java, AccountSubscription::class.java),
        )
            .toJson(value)
    }

    @TypeConverter
    fun stringToSubscriptions(value: String): List<AccountSubscription>? =
        try {
            moshi.adapter<List<AccountSubscription>>(
                Types.newParameterizedType(List::class.java, AccountSubscription::class.java),
            )
                .fromJson(value)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }

    @TypeConverter
    fun miscAccountInfoToString(miscAccountInfo: MiscAccountInfo): String {
        return moshi.adapter<MiscAccountInfo>(MiscAccountInfo::class.java)
            .toJson(miscAccountInfo)
    }

    @TypeConverter
    fun stringToMiscAccountInfo(value: String): MiscAccountInfo? =
        try {
            moshi.adapter(MiscAccountInfo::class.java)
                .fromJson(value)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
}
