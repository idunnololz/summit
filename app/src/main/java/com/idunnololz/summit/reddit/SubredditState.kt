package com.idunnololz.summit.reddit

import android.os.Bundle

data class SubredditState(
    val pages: List<RedditPageLoader.PageInfo>,
    val currentPageIndex: Int,
    val subreddit: String,
    val currentUrl: String
) {
    companion object {
        private const val SIS_PAGES = "RedditPageLoader_pages"
        private const val SIS_PAGES_FLAGS = "RedditPageLoader_pages_flags"
        private const val SIS_CURRENT_PAGE_INDEX = "RedditPageLoader_index"
        private const val SIS_SUBREDDIT = "RedditPageLoader_subreddit"
        private const val SIS_URL = "RedditPageLoader_url"

        fun restoreFromState(inState: Bundle?): SubredditState? =
            if (inState != null && inState.containsKey(SIS_PAGES)) {
                val pages: List<String> = inState.getString(SIS_PAGES)?.split(',') ?: listOf()
                val pagesFlagsStr = inState.getString(SIS_PAGES_FLAGS)
                val pagesFlags: List<Int> =
                    pagesFlagsStr?.split(',')?.map { it.toInt() } ?: listOf()
                val currentPageIndex = inState.getInt(SIS_CURRENT_PAGE_INDEX)
                val subreddit = inState.getString(SIS_SUBREDDIT) ?: ""
                val url = inState.getString(SIS_URL) ?: ""

                SubredditState(
                    pages = pages.zip(pagesFlags).map { (pageId, flags) ->
                        RedditPageLoader.PageInfo(pageId, flags)
                    },
                    currentPageIndex = currentPageIndex,
                    subreddit = subreddit,
                    currentUrl = url
                )
            } else {
                null
            }
    }

    fun writeToBundle(outState: Bundle) {
        outState.putString(SIS_PAGES, pages.map { it.pageId }.joinToString(separator = ","))
        outState.putString(SIS_PAGES_FLAGS, pages.map { it.flags }.joinToString(separator = ","))
        outState.putInt(SIS_CURRENT_PAGE_INDEX, currentPageIndex)
        outState.putString(SIS_SUBREDDIT, subreddit)
        outState.putString(SIS_URL, currentUrl)
    }
}