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

    private val rootView: View = v
    private val customActionBar: View = v.findViewById(R.id.customActionBar)
    private val subredditTextView: TextView = v.findViewById(R.id.subredditTextView)
    private val pageTextView: TextView = v.findViewById(R.id.pageTextView)
    private val abTabsImageView: TabsImageButton = v.findViewById(R.id.abTabsImageView)
    private val abOverflowButton: ImageButton = v.findViewById(R.id.abOverflowButton)
    private val sortOrderView: RedditSortOrderView = v.findViewById(R.id.redditSortOrderView)

    fun setup(
        subredditSelectedListener: SubredditSelectedListener,
        abOverflowClickListener: View.OnClickListener
    ) {
        subredditTextView.setOnClickListener {
            val controller = mainActivity.showSubredditSelector()
            controller.onSubredditSelectedListener = subredditSelectedListener
        }
        customActionBar.setOnClickListener {
            val controller = mainActivity.showSubredditSelector()
            controller.onSubredditSelectedListener = subredditSelectedListener
        }
        abOverflowButton.setOnClickListener(abOverflowClickListener)
    }

    fun setSubreddit(subreddit: String) {
        subredditTextView.text = subreddit
    }

    fun setPageIndex(
        pageIndex: Int,
        onPageSelectedListener: (pageIndex: Int) -> Unit
    ) {
        pageTextView.text = context.getString(R.string.page_format, pageIndex + 1)

        pageTextView.setOnClickListener {
            PopupMenu(context, it).apply {
                menu.apply {
                    for (i in 0..pageIndex) {
                        add(0, i, 0, context.getString(R.string.page_format, i + 1))
                    }
                }
                setOnMenuItemClickListener {
                    Log.d(TAG, "Page selected: ${it.itemId}")
                    onPageSelectedListener(it.itemId)
                    true
                }
            }.show()
        }
    }

    fun clearPageIndex() {
        pageTextView.text = ""
        pageTextView.setOnClickListener(null)
    }

    fun setupSortOrderSelector(
        lifecycleOwner: LifecycleOwner,
        currentSortOrder: RedditSortOrder,
        onSortOrderChangedListener: RedditSortOrderView.OnSortOrderChangedListener
    ) {
        sortOrderView.registerOnSortOrderChangedListener(onSortOrderChangedListener)
        if (sortOrderView.getSelection() != currentSortOrder) {
            sortOrderView.setSelection(currentSortOrder)
        }

        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                sortOrderView.unregisterOnSortOrderChangedListener(onSortOrderChangedListener)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                sortOrderView.registerOnSortOrderChangedListener(onSortOrderChangedListener)
            }
        })
    }
}