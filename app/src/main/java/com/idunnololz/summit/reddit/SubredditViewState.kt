package com.idunnololz.summit.reddit

import android.content.Context
import android.os.Bundle
import com.idunnololz.summit.R
import com.idunnololz.summit.subreddit.RedditViewModel

data class SubredditViewState(
    val subredditState: SubredditState,
    val pageScrollStates: List<RedditViewModel.PageScrollState>
) {
    companion object {

        private const val SIS_PAGE_POSITIONS = "RedditViewModel_pp"

        fun restoreFromState(inState: Bundle?): SubredditViewState? {
            val subredditState = SubredditState.restoreFromState(inState)
            return if (inState != null && subredditState != null) {
                SubredditViewState(
                    subredditState = subredditState,
                    pageScrollStates = inState.getParcelableArrayList(SIS_PAGE_POSITIONS)
                        ?: arrayListOf()
                )
            } else {
                null
            }
        }
    }

    fun writeToBundle(outState: Bundle) {
        subredditState.writeToBundle(outState)
        outState.putParcelableArrayList(SIS_PAGE_POSITIONS, ArrayList(pageScrollStates))
    }
}

fun SubredditViewState.getShortDesc(context: Context): String =
    context.getString(
        R.string.subreddit_state_format,
        subredditState.subreddit,
        subredditState.currentPageIndex + 1
    )