package com.idunnololz.summit.lemmy

import android.content.Context
import android.os.Parcelable
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.account.info.instance
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.utils.instance
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.parcelize.Parcelize

/**
 * Instances that don't support self references by fully qualified names.
 *
 * Eg. 'mycommunity' works but 'mycommunity@burggit.moe' doesn't work.
 */
private val CommunitiesNoAt = setOf(
    "burggit.moe",
    "lemmy.thesanewriter.com",
)

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface CommunityRef : PageRef, Parcelable {

//    @Parcelize
//    @JsonClass(generateAdapter = true)
//    @TypeLabel("1")
//    data class CommunityRefByObj(
//        val community: Community,
//        val instance: String?,
//    ) : CommunityRef {
//
//        fun getServerId(): String {
//            if (instance == null) {
//                return "${community.name}@"
//            }
//            return "${community.name}@${instance}"
//        }
//    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    @TypeLabel("2")
    data class Local(
        val instance: String?,
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    @TypeLabel("3")
    data class All(
        @Json(name = "site")
        val instance: String? = null,
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    @TypeLabel("4")
    data class CommunityRefByName(
        val name: String,
        val instance: String?,
    ) : CommunityRef {

        val fullName: String
            get() {
                if (instance == null) {
                    return "$name@"
                }
                return "$name@$instance"
            }

        fun getServerId(apiInstance: String): String {
            if (CommunitiesNoAt.contains(apiInstance) && instance == apiInstance) {
                return name
            }
            if (instance == null) {
                return name
            }
            return "$name@$instance"
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    @TypeLabel("5")
    data class Subscribed(
        val instance: String?,
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    @TypeLabel("6")
    data class MultiCommunity(
        val name: String,
        val icon: String?,
        val communities: List<CommunityRefByName>,
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    @TypeLabel("7")
    data class ModeratedCommunities(
        val instance: String?,
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    @TypeLabel("8")
    class AllSubscribed() : CommunityRef

    fun getName(context: Context): String = when (this) {
        is Local -> context.getString(R.string.local)
        is All -> context.getString(R.string.all)
        is CommunityRefByName -> this.name
        is Subscribed -> context.getString(R.string.subscribed)
        is MultiCommunity -> this.name
        is ModeratedCommunities -> context.getString(R.string.moderated_communities)
        is AllSubscribed -> context.getString(R.string.all_subscribed)
    }

    fun getLocalizedFullName(context: Context): String = when (this) {
        is Local -> "${context.getString(R.string.local)}@${this.instance}"
        is All -> "${context.getString(R.string.all)}@${this.instance}"
        is CommunityRefByName -> this.fullName
        is Subscribed -> "${context.getString(R.string.subscribed)}@${this.instance}"
        is MultiCommunity -> this.name
        is ModeratedCommunities -> "${context.getString(
            R.string.moderated_communities,
        )}@${this.instance}"
        is AllSubscribed -> context.getString(R.string.all_subscribed)
    }

    fun getKey(): String = when (this) {
        is Local ->
            if (this.instance != null) {
                "local@${this.instance}"
            } else {
                "local"
            }
        is All ->
            if (this.instance != null) {
                "all@${this.instance}"
            } else {
                "all"
            }
        is CommunityRefByName -> this.fullName
        is Subscribed ->
            if (this.instance != null) {
                "subscribed@${this.instance}"
            } else {
                "subscribed"
            }
        is MultiCommunity ->
            "multicommunity@${this.name}"

        is ModeratedCommunities ->
            if (this.instance != null) {
                "mc@${this.instance}"
            } else {
                "mc"
            }

        is AllSubscribed -> "allSubscribed"
    }
}

fun Community.toCommunityRef(): CommunityRef.CommunityRefByName {
    return CommunityRef.CommunityRefByName(this.name, this.instance)
}

fun AccountSubscription.toCommunityRef(): CommunityRef.CommunityRefByName {
    return CommunityRef.CommunityRefByName(this.name, this.instance)
}
