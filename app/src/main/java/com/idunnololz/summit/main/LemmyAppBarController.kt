package com.idunnololz.summit.main

import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunitySafe
import com.idunnololz.summit.lemmy.Community
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.reddit.RedditSortOrder
import com.idunnololz.summit.view.LemmySortOrderView
import com.idunnololz.summit.view.RedditSortOrderView
import com.idunnololz.summit.view.TabsImageButton

class LemmyAppBarController(private val mainActivity: MainActivity, v: View) {

    private val TAG = "RedditAppBarController"

    private val context = mainActivity

    private val rootView: View = v
    private val customActionBar: View = v.findViewById(R.id.customActionBar)
    private val subredditTextView: TextView = v.findViewById(R.id.subredditTextView)
    private val pageTextView: TextView = v.findViewById(R.id.pageTextView)
    private val abTabsImageView: TabsImageButton = v.findViewById(R.id.abTabsImageView)
    private val abOverflowButton: ImageButton = v.findViewById(R.id.abOverflowButton)
    private val sortOrderView: LemmySortOrderView = v.findViewById(R.id.sort_order_view)

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

    fun setCommunity(community: Community?) {
        subredditTextView.text = community?.getName(context) ?: ""
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
        currentSortOrder: CommunitySortOrder,
        onSortOrderChangedListener: LemmySortOrderView.OnSortOrderChangedListener
    ) {
        sortOrderView.registerOnSortOrderChangedListener(onSortOrderChangedListener)
        if (sortOrderView.getSelection() != currentSortOrder) {
            sortOrderView.setSelection(currentSortOrder)
        }

        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                sortOrderView.unregisterOnSortOrderChangedListener(onSortOrderChangedListener)
            }

            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                sortOrderView.registerOnSortOrderChangedListener(onSortOrderChangedListener)
            }
        })
    }
}