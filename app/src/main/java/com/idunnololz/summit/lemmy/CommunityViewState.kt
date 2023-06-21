package com.idunnololz.summit.lemmy

import android.content.Context
import android.os.Bundle
import com.idunnololz.summit.R
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_INSTANCE
import com.idunnololz.summit.lemmy.community.CommunityViewModel
import com.idunnololz.summit.util.moshi
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CommunityViewState(
    val communityState: CommunityState,
    val pageScrollStates: List<CommunityViewModel.PageScrollState>
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

fun CommunityViewState.toUrl(): String {
    val baseUrl = when (val community = this.communityState.communityRef) {
        is CommunityRef.All -> "https://lemmy.world/home/data_type/Post/listing_type/All/data_type/Post"
        is CommunityRef.CommunityRefByObj -> "${community.community.communityUrl}/data_type/Post"
        is CommunityRef.Local -> "${community.site}/home/data_type/Post/listing_type/Local"
        is CommunityRef.CommunityRefByName -> "https://${DEFAULT_INSTANCE}/c/${community.name}"
    }
    return "${baseUrl}/sort/Active/page/${this.communityState.currentPageIndex}"
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
    var flags: Int
)

fun CommunityViewState.getShortDesc(context: Context): String =
    context.getString(
        R.string.subreddit_state_format,
        communityState.communityRef.getName(context),
        (communityState.currentPageIndex + 1).toString()
    )