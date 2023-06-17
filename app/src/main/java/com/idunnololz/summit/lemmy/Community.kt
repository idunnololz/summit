package com.idunnololz.summit.lemmy

import android.content.Context
import android.os.Parcelable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunitySafe
import com.idunnololz.summit.api.dto.CommunityView
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import kotlinx.parcelize.Parcelize

sealed interface Community : Parcelable {

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class CommunityWrapper(
        val community: CommunitySafe
    ) : Community

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Local(
        val site: String
    ) : Community

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class All(
        val site: String = "https://lemmy.world/"
    ) : Community

    fun getName(context: Context): String =
        when (this) {
            is CommunityWrapper -> this.community.name
            is Local -> context.getString(R.string.local)
            is All -> context.getString(R.string.all)
        }

    fun getKey(): String =
        when (this) {
            is CommunityWrapper -> this.community.actor_id
            is Local -> this.site
            is All -> "all"
        }

    companion object {
        fun adapter(): PolymorphicJsonAdapterFactory<Community> =
            PolymorphicJsonAdapterFactory.of(Community::class.java, "t")
                .withSubtype(CommunityWrapper::class.java, "1")
                .withSubtype(Local::class.java, "2")
                .withSubtype(All::class.java, "3")
    }
}

fun CommunitySafe.toCommunity(): Community.CommunityWrapper {
    return Community.CommunityWrapper(this)
}