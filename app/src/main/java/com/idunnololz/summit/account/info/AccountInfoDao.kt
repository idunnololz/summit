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
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.InstanceId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.util.crashLogger.crashLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

@Serializable
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

@Serializable
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

@Serializable
data class BlockedPerson(
    val personId: PersonId,
    val personRef: PersonRef,
)

@Serializable
data class BlockedCommunity(
    val communityId: CommunityId,
    val communityRef: CommunityRef.CommunityRefByName,
)

@Serializable
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
class AccountInfoConverters(private val json: Json) {

    companion object {
        private const val TAG = "AccountInfoConverters"
    }

    @TypeConverter
    fun subscriptionsToString(value: List<AccountSubscription>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun stringToSubscriptions(value: String): List<AccountSubscription>? = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        crashLogger?.recordException(e)
        null
    }

    @TypeConverter
    fun miscAccountInfoToString(miscAccountInfo: MiscAccountInfo): String {
        return json.encodeToString(miscAccountInfo)
    }

    @TypeConverter
    fun stringToMiscAccountInfo(value: String): MiscAccountInfo? = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        Log.e(TAG, "", e)
        crashLogger?.recordException(e)
        null
    }
}

fun Community.toAccountSubscription() = AccountSubscription(
    this.id,
    this.name,
    this.title,
    this.removed,
    this.published,
    this.updated,
    this.deleted,
    this.nsfw,
    this.actor_id,
    this.local,
    this.icon,
    this.banner,
    this.hidden,
    this.posting_restricted_to_mods,
    this.instance_id,
)
