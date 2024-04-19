package com.idunnololz.summit.lemmy

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.community.CommunityViewModel
import com.idunnololz.summit.util.moshi
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CommunityViewState(
    val communityState: CommunityState,
    val pageScrollStates: List<CommunityViewModel.PageScrollState>,
) {
    companion object {

        private const val SIS_KEY = "LemmyViewState_pp"

        fun restoreFromBundle(inState: Bundle): CommunityViewState? {
            val json = inState.getString(SIS_KEY, null) ?: return null
            return try {
                moshi.adapter(CommunityViewState::class.java).fromJson(json)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun writeToBundle(outState: Bundle) {
        outState.putString(SIS_KEY, moshi.adapter(CommunityViewState::class.java).toJson(this))
    }
}

fun CommunityViewState.toUrl(apiInstance: String): String =
    communityState.communityRef.toUri(apiInstance)
        .buildUpon()
        .appendQueryParameter("page", this.communityState.currentPageIndex.toString())
        .build()
        .toString()

fun CommunityRef.toUrl(apiInstance: String): String = toUri(apiInstance).toString()

fun CommunityRef.toUri(apiInstance: String): Uri {
    val url = when (val community = this) {
        is CommunityRef.All ->
            "https://${community.instance ?: apiInstance}/?dataType=Post&listingType=All"
        is CommunityRef.Local -> "https://${community.instance ?: apiInstance}/?dataType=Post&listingType=Local"
        is CommunityRef.CommunityRefByName -> "https://${community.instance}/c/${community.name}?dataType=Post"
        is CommunityRef.Subscribed -> "https://${community.instance ?: apiInstance}/?dataType=Post&listingType=Subscribed"
        is CommunityRef.MultiCommunity -> "https://${apiInstance}/#!${moshi.adapter(CommunityRef::class.java).toJson(community)}"
        is CommunityRef.ModeratedCommunities -> "https://${community.instance ?: apiInstance}/?dataType=Post&listingType=ModeratorView"
    }

    return Uri.parse(url)
}

@JsonClass(generateAdapter = true)
data class CommunityState(
    val communityRef: CommunityRef,
    val pages: List<PageInfo>,
    val currentPageIndex: Int,
)

@JsonClass(generateAdapter = true)
data class PageInfo(
    val pageIndex: Int,
    var flags: Int,
)

fun CommunityViewState.getShortDesc(context: Context): String = context.getString(
    R.string.community_state_format,
    communityState.communityRef.getName(context),
    (communityState.currentPageIndex + 1).toString(),
)

class MultiCommunityException : Exception()
class ModeratedCommunitiesException : Exception()
