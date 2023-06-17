package com.idunnololz.summit.main

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.idunnololz.summit.R
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.reddit.RecentSubredditsManager
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.reddit_objects.ListingObject
import com.idunnololz.summit.reddit_objects.SubredditItem
import com.idunnololz.summit.reddit_objects.SubredditObject
import com.idunnololz.summit.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

typealias SubredditSelectedListener = ((controller: SubredditSelectorController, url: String) -> Unit)

class SubredditSelectorController(
    val context: Context
) {
    private val inflater = LayoutInflater.from(context)

    private lateinit var rootView: View

    private lateinit var motionLayout: MotionLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: EditText

    private val offlineManager = OfflineManager.instance
    private val recentSubredditsManager = RecentSubredditsManager.instance

    private val adapter = SubredditsAdapter()

    private val disposables = CompositeDisposable()

    var isVisible: Boolean = false

    var onVisibilityChangedCallback: ((Boolean) -> Unit)? = null
    var onSubredditSelectedListener: SubredditSelectedListener? = null

    fun inflate(container: ViewGroup) {
        rootView = inflater.inflate(R.layout.subreddit_selector_view, container, false)
        rootView.visibility = View.GONE

        motionLayout = rootView.findViewById(R.id.motionLayout)
        recyclerView = rootView.findViewById(R.id.subredditsRecyclerView)
        searchView = rootView.findViewById(R.id.searchView)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        searchView.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                onSubredditSelectedListener?.invoke(this@SubredditSelectorController, "r/${searchView.text}")
                searchView.setText("")
                return@OnKeyListener true
            }
            false
        })

        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.setQuery(s?.toString())
                doQueryAsync(s)
                recyclerView.scrollToPosition(0)
            }
        })

        container.addView(rootView)

        motionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}

            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}

            override fun onTransitionCompleted(moitonLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.collapsed) {
                    isVisible = false
                    rootView.visibility = View.GONE
                }
            }
        })
    }

    private fun doQueryAsync(query: CharSequence?) {
        disposables.clear()

        adapter.setQueryServerResultsInProgress()

        val d = Single
            .fromCallable {
                val result = LinkUtils.downloadSite(LinkUtils.subredditsSearch(query))
                val listingObject = Utils.gson.fromJson(result, ListingObject::class.java)
                listingObject.data?.children?.filterIsInstance<SubredditObject>()
                    ?.mapNotNull { it.data }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it != null) {
                    adapter.setQueryServerResults(it)
                }
            }, {})
        disposables.add(d)
    }

    fun show() {
        isVisible = true
        rootView.visibility = View.VISIBLE
        motionLayout.transitionToState(R.id.expanded)
    }

    fun hide() {
        motionLayout.transitionToState(R.id.collapsed)
        disposables.clear()
    }

    fun setSubredditsState(it: StatefulData<List<SubredditItem>>) {
        when (it) {
            is StatefulData.Error -> {}
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                adapter.setData(it.data)
            }
        }
    }

    fun setTopMargin(height: Int) {
        rootView.layoutParams = (rootView.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin = height
        }
    }

    private sealed class Item(
        val id: String
    ) {
        class GroupHeaderItem(
            val text: String,
            val stillLoading: Boolean = false
        ) : Item(text)

        class NoResultsItem(
            val text: String
        ) : Item(text)

        class SubredditChildItem(
            val text: String,
            val subredditItem: SubredditItem
        ) : Item(subredditItem.id)

        class StaticChildItem(
            val text: String,
            val url: String,
            @DrawableRes val iconResId: Int
        ) : Item(url)

        class RecentChildItem(val text: String, val url: String) : Item("r:${url}")
    }

    private class GroupItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val titleTextView: TextView = v.findViewById(R.id.titleTextView)
        val progressBar: ProgressBar = v.findViewById(R.id.progressBar)
    }

    private class NoResultsItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textView: TextView = v.findViewById(R.id.text)
    }

    private class SubredditItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val textView: TextView = v.findViewById(R.id.textView)
    }

    private inner class SubredditsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        private var rawData: List<SubredditItem> = listOf()
        private var serverResultsInProgress = false
        private var serverQueryResults: List<SubredditItem> = listOf()
        private var items: List<Item> = listOf()

        private var query: String? = null

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is Item.GroupHeaderItem -> R.layout.subreddit_selector_group_item
            is Item.SubredditChildItem -> R.layout.subreddit_selector_subreddit_item
            is Item.StaticChildItem -> R.layout.subreddit_selector_static_subreddit_item
            is Item.RecentChildItem -> R.layout.subreddit_selector_static_subreddit_item
            is Item.NoResultsItem -> R.layout.subreddit_selector_no_results_item
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = inflater.inflate(viewType, parent, false)
            return when (viewType) {
                R.layout.subreddit_selector_group_item -> {
                    GroupItemViewHolder(v)
                }
                R.layout.subreddit_selector_no_results_item -> {
                    return NoResultsItemViewHolder(v)
                }
                else -> SubredditItemViewHolder(v)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            when (item) {
                is Item.GroupHeaderItem -> {
                    val h = holder as GroupItemViewHolder
                    h.titleTextView.text = item.text

                    if (item.stillLoading) {
                        h.progressBar.visibility = View.VISIBLE
                    } else {
                        h.progressBar.visibility = View.GONE
                    }
                }
                is Item.NoResultsItem -> {
                    val h = holder as NoResultsItemViewHolder
                    h.textView.text = item.text
                }
                is Item.SubredditChildItem -> {
                    val h = holder as SubredditItemViewHolder
                    val subredditItem = item.subredditItem

                    h.icon.setImageResource(R.drawable.ic_subreddit_default)
                    offlineManager.fetchImage(h.itemView, subredditItem.iconImg) {
                        Glide.with(context)
                            .load(it)
                            .into(h.icon)
                    }

                    h.textView.text = item.text

                    h.itemView.setOnClickListener {
                        onSubredditSelectedListener?.invoke(
                            this@SubredditSelectorController,
                            RedditUtils.normalizeSubredditPath(
                                subredditItem.url
                            )
                        )
                    }
                }
                is Item.StaticChildItem -> {
                    val h = holder as SubredditItemViewHolder
                    h.icon.setImageResource(item.iconResId)
                    h.textView.text = item.text

                    h.itemView.setOnClickListener {
                        onSubredditSelectedListener?.invoke(this@SubredditSelectorController, item.url)
                    }
                }
                is Item.RecentChildItem -> {
                    val h = holder as SubredditItemViewHolder
                    h.icon.setImageResource(0)
                    h.textView.text = item.text
                    h.itemView.setOnClickListener {
                        onSubredditSelectedListener?.invoke(this@SubredditSelectorController, item.url)
                    }
                }
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            offlineManager.cancelFetch(holder.itemView)
        }

        override fun getItemCount(): Int = items.size

        private fun refreshItems() {

            fun makeRecentItems(query: String?): List<Item> =
                if (!query.isNullOrBlank()) listOf()
                else arrayListOf<Item>().apply {
                    val recents = recentSubredditsManager.getRecentSubreddits()
                    if (recents.isNotEmpty()) {
                        add(Item.GroupHeaderItem(context.getString(R.string.recents)))
                        recents.forEach {
                            add(Item.RecentChildItem(it, it))
                        }
                    }
                }

            val subredditIds = hashSetOf<String>()
            val query = query
            val oldItems = items
            val newItems =
                listOf(
                    Item.GroupHeaderItem(context.getString(R.string.feeds)),
                    Item.StaticChildItem(
                        context.getString(R.string.home),
                        "",
                        R.drawable.ic_subreddit_home
                    ),
                    Item.StaticChildItem(
                        context.getString(R.string.popular),
                        "r/popular",
                        R.drawable.ic_subreddit_popular
                    ),
                    Item.StaticChildItem(
                        context.getString(R.string.all),
                        "r/all",
                        R.drawable.ic_subreddit_all
                    )
                )
                    .plus(makeRecentItems(query))
                    .plus(Item.GroupHeaderItem(context.getString(R.string.communities)))
                    .plus(rawData
                        .sortedBy { it.displayName }
                        .map {
                            Item.SubredditChildItem(it.displayNamePrefixed, it)
                                .also { subredditIds.add(it.id) }
                        })
                    .let a@{ intermediateList ->
                        if (query.isNullOrBlank()) {
                            return@a intermediateList
                        }

                        fun getText(item: Item): String = when (item) {
                            is Item.StaticChildItem -> item.text
                            is Item.SubredditChildItem -> item.text
                            else -> ""
                        }

                        val serverQueryItems = serverQueryResults
                            .map { Item.SubredditChildItem(it.displayNamePrefixed, it) }
                            .filter {
                                !subredditIds.contains(it.id) &&
                                        getText(it).contains(query, ignoreCase = true)
                            }

                        intermediateList.filter { getText(it).contains(query, ignoreCase = true) }
                            .sortedByDescending { StringSearchUtils.similarity(query, getText(it)) }
                            .let {
                                if (it.isEmpty()) {
                                    it.plus(Item.NoResultsItem(context.getString(R.string.no_results_found)))
                                } else it
                            }
                            .let {
                                if (serverQueryItems.isNotEmpty() || serverResultsInProgress) {
                                    it.plus(
                                        Item.GroupHeaderItem(
                                            context.getString(R.string.server_results),
                                            serverResultsInProgress
                                        )
                                    )
                                        .plus(serverQueryItems)
                                } else it
                            }
                    }

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]

                    return oldItem.id == newItem.id
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]
                    return areItemsTheSame(oldItemPosition, newItemPosition) && when (oldItem) {
                        is Item.GroupHeaderItem ->
                            oldItem.stillLoading == (newItem as Item.GroupHeaderItem).stillLoading
                        is Item.NoResultsItem -> true
                        is Item.SubredditChildItem -> true
                        is Item.StaticChildItem -> true
                        is Item.RecentChildItem -> true
                    }
                }

            })
            items = newItems
            diff.dispatchUpdatesTo(this)
        }

        fun setQuery(query: String?) {
            this.query = query

            refreshItems()
        }

        fun setData(newData: List<SubredditItem>) {
            rawData = newData

            refreshItems()
        }

        fun setQueryServerResults(serverQueryResults: List<SubredditItem>) {
            this.serverQueryResults = serverQueryResults
            serverResultsInProgress = false

            refreshItems()
        }

        fun setQueryServerResultsInProgress() {
            serverQueryResults = listOf()
            serverResultsInProgress = true

            refreshItems()
        }

    }
}