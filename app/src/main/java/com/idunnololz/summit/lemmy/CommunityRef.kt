package com.idunnololz.summit.lemmy

import android.content.Context
import android.os.Parcelable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_INSTANCE
import com.idunnololz.summit.api.dto.CommunitySafe
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import kotlinx.parcelize.Parcelize
import java.util.Locale

sealed interface CommunityRef : Parcelable {

    companion object {
        fun adapter(): PolymorphicJsonAdapterFactory<CommunityRef> =
            PolymorphicJsonAdapterFactory.of(CommunityRef::class.java, "t")
                .withSubtype(CommunityRefByObj::class.java, "1")
                .withSubtype(Local::class.java, "2")
                .withSubtype(All::class.java, "3")
                .withSubtype(CommunityRefByName::class.java, "4")
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class CommunityRefByObj(
        val community: CommunitySafe
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Local(
        val site: String?
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class All(
        val site: String? = null
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class CommunityRefByName(
        val name: String
    ) : CommunityRef

    fun getName(context: Context): String =
        when (this) {
            is CommunityRefByObj -> this.community.name
            is Local -> context.getString(R.string.local)
            is All -> context.getString(R.string.all)
            is CommunityRefByName -> this.name
        }

    fun getKey(): String =
        when (this) {
            is CommunityRefByObj -> this.community.actor_id
            is Local -> this.site ?: "local@auto"
            is All -> "all@${this.site}"
            is CommunityRefByName -> "cname@${this.name}".lowercase(Locale.US)
        }
}

fun CommunitySafe.toCommunity(): CommunityRef.CommunityRefByObj {
    return CommunityRef.CommunityRefByObj(this)
}