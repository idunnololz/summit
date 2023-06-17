package com.idunnololz.summit.main

import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.idunnololz.summit.R
import com.idunnololz.summit.reddit.RedditSortOrder
import com.idunnololz.summit.view.RedditSortOrderView
import com.idunnololz.summit.view.TabsImageButton

class RedditAppBarController(private val mainActivity: MainActivity, v: View) {

    private val TAG = "RedditAppBarController"

    private val context = mainActivity


    fun setup(
        subredditSelectedListener: SubredditSelectedListener,
        abOverflowClickListener: View.OnClickListener
    ) {
    }

    fun setSubreddit(subreddit: String) {
    }

    fun setPageIndex(
        pageIndex: Int,
        onPageSelectedListener: (pageIndex: Int) -> Unit
    ) {
    }

    fun clearPageIndex() {
    }

    fun setupSortOrderSelector(
        lifecycleOwner: LifecycleOwner,
        currentSortOrder: RedditSortOrder,
        onSortOrderChangedListener: RedditSortOrderView.OnSortOrderChangedListener
    ) {
    }
}