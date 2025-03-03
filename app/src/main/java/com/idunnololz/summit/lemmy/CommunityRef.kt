package com.idunnololz.summit.lemmy

import android.content.Context
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.account.info.instance
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.CommunityRef.All
import com.idunnololz.summit.lemmy.CommunityRef.AllSubscribed
import com.idunnololz.summit.lemmy.CommunityRef.CommunityRefByName
import com.idunnololz.summit.lemmy.CommunityRef.Local
import com.idunnololz.summit.lemmy.CommunityRef.ModeratedCommunities
import com.idunnololz.summit.lemmy.CommunityRef.MultiCommunity
import com.idunnololz.summit.lemmy.CommunityRef.Subscribed
import com.idunnololz.summit.util.ext.getColorCompat
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Instances that don't support self references by fully qualified names.
 *
 * Eg. 'mycommunity' works but 'mycommunity@burggit.moe' doesn't work.
 */
private val CommunitiesNoAt = setOf(
    "burggit.moe",
    "lemmy.thesanewriter.com",
)

@Serializable
@JsonClassDiscriminator("t")
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

    @Serializable
    @SerialName("2")
    @Parcelize
    data class Local(
        val instance: String?,
        val rawUrl: String? = null,
    ) : CommunityRef

    @Serializable
    @SerialName("3")
    @Parcelize
    data class All(
        @SerialName("site")
        val instance: String? = null,
    ) : CommunityRef

    @Serializable
    @SerialName("4")
    @Parcelize
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

    @Serializable
    @SerialName("5")
    @Parcelize
    data class Subscribed(
        val instance: String?,
    ) : CommunityRef

    @Serializable
    @SerialName("6")
    @Parcelize
    data class MultiCommunity(
        val name: String,
        val icon: String?,
        val communities: List<CommunityRefByName>,
    ) : CommunityRef

    @Serializable
    @SerialName("7")
    @Parcelize
    data class ModeratedCommunities(
        val instance: String?,
    ) : CommunityRef

    @Serializable
    @SerialName("8")
    @Parcelize
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
        is Local ->
            if (this.instance != null) {
                "${context.getString(R.string.local)}@${this.instance}"
            } else {
                context.getString(R.string.local)
            }
        is All ->
            if (this.instance != null) {
                "${context.getString(R.string.all)}@${this.instance}"
            } else {
                context.getString(R.string.all)
            }
        is CommunityRefByName -> this.fullName
        is Subscribed ->
            if (this.instance != null) {
                "${context.getString(R.string.subscribed)}@${this.instance}"
            } else {
                context.getString(R.string.subscribed)
            }
        is MultiCommunity -> this.name
        is ModeratedCommunities ->
            if (this.instance != null) {
                "${context.getString(R.string.moderated_communities)}@${this.instance}"
            } else {
                context.getString(R.string.moderated_communities)
            }
        is AllSubscribed -> context.getString(R.string.all_subscribed)
    }
    fun getLocalizedFullNameSpannable(context: Context): Spannable {
        val text = when (this) {
            is Local ->
                if (this.instance != null) {
                    "${context.getString(R.string.local)}@${this.instance}"
                } else {
                    context.getString(R.string.local)
                }
            is All ->
                if (this.instance != null) {
                    "${context.getString(R.string.all)}@${this.instance}"
                } else {
                    context.getString(R.string.all)
                }
            is CommunityRefByName -> this.fullName
            is Subscribed ->
                if (this.instance != null) {
                    "${context.getString(R.string.subscribed)}@${this.instance}"
                } else {
                    context.getString(R.string.subscribed)
                }
            is MultiCommunity -> this.name
            is ModeratedCommunities ->
                if (this.instance != null) {
                    "${context.getString(R.string.moderated_communities)}@${this.instance}"
                } else {
                    context.getString(R.string.moderated_communities)
                }
            is AllSubscribed -> context.getString(R.string.all_subscribed)
        }

        return SpannableStringBuilder().apply {
            append(text)

            val atIndex = text.indexOf('@')
            val end = length

            if (atIndex >= 0) {
                setSpan(
                    ForegroundColorSpan(context.getColorCompat(R.color.colorTextFaint)),
                    atIndex,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
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

val CommunityRef.instance: String?
    get() = when (this) {
        is All -> this.instance
        is AllSubscribed -> null
        is CommunityRefByName -> this.instance
        is Local -> this.instance
        is ModeratedCommunities -> this.instance
        is MultiCommunity -> null
        is Subscribed -> this.instance
    }
