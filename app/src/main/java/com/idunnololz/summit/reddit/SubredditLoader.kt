package com.idunnololz.summit.reddit

import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.reddit_objects.SubredditItem
import com.idunnololz.summit.reddit_objects.SubredditObject
import com.idunnololz.summit.reddit_website_adapter.RedditListingWebsiteAdapter
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.Utils

class SubredditLoader {

    private var loader: WebsiteAdapterLoader? = null

    fun fetchSubreddits(
        force: Boolean,
        onLoadStartCallback: () -> Unit,
        onLoadedCallback: (List<SubredditItem>) -> Unit,
        onErrorCallback: (Int) -> Unit
    ) {
        loader?.destroy()

        onLoadStartCallback()

        val url = LinkUtils.subreddits(limit = 100, showAll = true)
        loader = WebsiteAdapterLoader().apply {
            add(
                RedditListingWebsiteAdapter(),
                url,
                Utils.hashSha256(url)
            )
            setOnEachAdapterLoadedListener {
                if (it is RedditListingWebsiteAdapter) {
                    if (it.isSuccess()) {
                        onLoadedCallback.invoke(
                            it.get().data?.getChildrenAs<SubredditObject>()?.mapNotNull { it.data }
                                ?: listOf())
                    } else {
                        onErrorCallback(it.error)
                    }
                }
            }
        }.load(forceRefetch = force)
    }

    fun destroy() {
        loader?.destroy()
    }
}