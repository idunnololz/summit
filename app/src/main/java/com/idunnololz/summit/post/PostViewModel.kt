package com.idunnololz.summit.post

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.reddit.CommentsSortOrder
import com.idunnololz.summit.reddit.RedditPageLoader
import com.idunnololz.summit.reddit_objects.*
import com.idunnololz.summit.reddit_website_adapter.MoreChildrenWebsiteAdapter
import com.idunnololz.summit.scrape.LoaderException
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils.hashSha256

class PostViewModel : ViewModel() {
    companion object {
        private const val TAG = "PostViewModel"
    }

    private var loader: WebsiteAdapterLoader? = null

    private var commentLoaders = ArrayList<WebsiteAdapterLoader>()

    var commentsSortOrder: CommentsSortOrder? = null
        private set

    val redditObjects = StatefulLiveData<List<RedditObject>>()
    val redditMoreComments = MutableLiveData<HashMap<String, List<CommentItemObject>>>()

    val redditPageLoader = RedditPageLoader().apply {
        onPostLoadStartListener = {
            redditObjects.setIsLoading()
        }
        onPostLoadedListener = {
            if (it.isSuccess()) {
                redditObjects.postValue(it.get())
            } else {
                redditObjects.postError(LoaderException(it.error))
            }
        }
    }

    init {
        redditMoreComments.postValue(HashMap())
    }

    fun fetchPostData(url: String, force: Boolean = false) {
        redditPageLoader.fetchPost(
            url = url,
            sortOrder = commentsSortOrder,
            force = force
        )
    }

    fun fetchMoreComments(url: String, parentId: String, force: Boolean = false) {
        commentLoaders.add(
            WebsiteAdapterLoader().apply {
                add(
                    MoreChildrenWebsiteAdapter(),
                    url,
                    "morechildren:${hashSha256(url)}"
                )
                setOnEachAdapterLoadedListener {
                    if (it is MoreChildrenWebsiteAdapter) {
                        if (it.isSuccess()) {

                            val map = redditMoreComments.value ?: HashMap()

                            it.get().json?.data?.things?.filterIsInstance<CommentItemObject>()
                                ?.let { commentItemObjects ->

                                    // result is flattened... try to unflatten

                                    val dict = commentItemObjects.associateBy { it.data?.name }
                                    val topLevel: MutableList<CommentItemObject> =
                                        commentItemObjects.toMutableList()

                                    commentItemObjects.forEach {
                                        val p = dict[it.data?.parentId]
                                        if (p != null) {
                                            topLevel.remove(it)

                                            p.data?.replies =
                                                ListingObject(
                                                    kind = "Listing",
                                                    data = ListingData(
                                                        modHash = "clientSided",
                                                        dist = 0,
                                                        children = ((p.data?.replies as? ListingObject)
                                                            ?.data?.children ?: listOf()) + listOf(
                                                            it
                                                        )
                                                    )
                                                )
                                        }
                                    }


                                    map[parentId] = topLevel
                                }

                            redditMoreComments.postValue(map)
                        } else {
                            Log.e(TAG, "Error loading more comments: ${it.error}")
                        }
                    }
                }
            }.load(forceRefetch = force)
        )
    }

    override fun onCleared() {
        super.onCleared()

        loader?.destroy()

        for (l in commentLoaders) {
            l.destroy()
        }
    }

    fun setCommentsSortOrder(sortOrder: CommentsSortOrder) {
        commentsSortOrder = sortOrder
    }
}