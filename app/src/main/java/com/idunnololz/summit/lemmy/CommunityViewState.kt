package com.idunnololz.summit.lemmy

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.community.CommunityViewModel
import com.idunnololz.summit.util.dagger.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CommunityViewState(
    val communityState: CommunityState,
    val pageScrollStates: List<CommunityViewModel.PageScrollState>,
) {
    companion object {

        private const val SIS_KEY = "LemmyViewState_pp"

        fun restoreFromBundle(inState: Bundle, json: Json): CommunityViewState? {
            val jsonStr = inState.getString(SIS_KEY, null) ?: return null
            return try {
                json.decodeFromString<CommunityViewState?>(jsonStr)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun writeToBundle(outState: Bundle, json: Json) {
        outState.putString(SIS_KEY, json.encodeToString<CommunityViewState?>(this))
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
        is CommunityRef.MultiCommunity -> "https://$apiInstance/#!mc=${json.encodeToString(
            community,
        )}"
        is CommunityRef.ModeratedCommunities -> "https://${community.instance ?: apiInstance}/?dataType=Post&listingType=ModeratorView"
        is CommunityRef.AllSubscribed -> "https://$apiInstance/#!as="
    }

    return Uri.parse(url)
}

@Serializable
data class CommunityState(
    val communityRef: CommunityRef,
    val pages: List<PageInfo>,
    val currentPageIndex: Int,
)

@Serializable
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
