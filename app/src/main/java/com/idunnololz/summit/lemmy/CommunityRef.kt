package com.idunnololz.summit.lemmy

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunitySafe
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import kotlinx.parcelize.Parcelize
import java.util.Locale

sealed interface CommunityRef : PageRef, Parcelable {

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
        val community: CommunitySafe,
        val instance: String?,
    ) : CommunityRef {

        fun getServerId(): String {
            if (instance == null) {
                return "${community.name}@"
            }
            return "${community.name}@${instance}"
        }
    }

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class Local(
        val instance: String?
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class All(
        @Json(name = "site")
        val instance: String? = null
    ) : CommunityRef

    @Parcelize
    @JsonClass(generateAdapter = true)
    data class CommunityRefByName(
        val name: String,
        val instance: String?,
    ) : CommunityRef {

        fun getServerId(): String {
            if (instance == null) {
                return "${name}@"
            }
            return "${name}@${instance}"
        }
    }

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
            is Local -> this.instance ?: "local@auto"
            is All -> "all@${this.instance}"
            is CommunityRefByName -> "cname@${this.name}".lowercase(Locale.US)
        }
}

fun CommunitySafe.toCommunityRef(): CommunityRef.CommunityRefByObj {
    val uri = Uri.parse(this.actor_id)
    return CommunityRef.CommunityRefByObj(this, uri.host)
}

fun CommunityRef.toInstanceAgnosticCommunityRef(): CommunityRef =
    when (this) {
        is CommunityRef.All -> this
        is CommunityRef.CommunityRefByName -> this
        is CommunityRef.CommunityRefByObj -> CommunityRef.CommunityRefByName(
            this.community.name,
            this.instance,
        )
        is CommunityRef.Local -> this
    }