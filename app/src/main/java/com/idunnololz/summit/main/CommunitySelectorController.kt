package com.idunnololz.summit.main

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.CommonLemmyInstance
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.databinding.CommunitySelectorCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorGroupItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorStaticCommunityItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StringSearchUtils
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

typealias CommunitySelectedListener =
        ((controller: CommunitySelectorController, communityRef: CommunityRef) -> Unit)

class CommunitySelectorController @AssistedInject constructor(
    @Assisted private val context: Context,
    private val lemmyApiClient: AccountAwareLemmyClient,
    private val recentCommunityManager: RecentCommunityManager,
) {
    @AssistedFactory
    interface Factory {
        fun create(context: Context): CommunitySelectorController
    }

    private val inflater = LayoutInflater.from(context)

    private var rootView: View? = null

    private lateinit var motionLayout: MotionLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: EditText

    private val offlineManager = OfflineManager.instance

    private var coroutineScope = createCoroutineScope()

    private val adapter = SubredditsAdapter()

    private var topInset = MutableLiveData(0)
    private var bottomInset = MutableLiveData(0)

    var isVisible: Boolean = false

    var onCommunitySelectedListener: CommunitySelectedListener? = null

    fun inflate(container: ViewGroup) {
        rootView?.let {
            return
        }

        val rootView = inflater.inflate(R.layout.subreddit_selector_view, container, false)
        this.rootView = rootView
        rootView.visibility = View.GONE

        motionLayout = rootView.findViewById(R.id.motionLayout)
        recyclerView = rootView.findViewById(R.id.subredditsRecyclerView)
        searchView = rootView.findViewById(R.id.searchView)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        searchView.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                onCommunitySelectedListener?.invoke(
                    this@CommunitySelectorController,
                    CommunityRef.CommunityRefByName(searchView.text.toString())
                )
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
        query ?: return

        adapter.setQueryServerResultsInProgress()

        coroutineScope.launch {
            lemmyApiClient.search(
                sortType = SortType.Active,
                listingType = ListingType.All,
                searchType = SearchType.Communities,
                query = query.toString(),
            ).onSuccess {
                adapter.setQueryServerResults(it.communities)
            }
        }
    }

    fun show(insets: MainActivityInsets) {

        isVisible = true
        rootView?.visibility = View.VISIBLE
        motionLayout.transitionToState(R.id.expanded)

        setInsets(insets.topInset, insets.bottomInset)

        bottomInset.observeForever {
            recyclerView.updatePadding(bottom = it)
        }
        topInset.observeForever {
            rootView?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = it
            }
        }
    }

    fun onBackPressed() {
        if (searchView.text.isNullOrBlank()) {
            hide()
        } else {
            searchView.setText("")
        }
    }

    fun hide() {
        motionLayout.transitionToState(R.id.collapsed)
        coroutineScope.cancel()
        coroutineScope = createCoroutineScope()
    }

    fun setCommunities(it: StatefulData<List<CommunityView>>) {
        when (it) {
            is StatefulData.Error -> {}
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                adapter.setData(it.data)
            }
        }
    }

    private fun setInsets(topInset: Int, bottomInset: Int) {
        this.topInset.value = topInset
        this.bottomInset.value = bottomInset
    }

    private fun createCoroutineScope() =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

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

        class CommunityChildItem(
            val text: String,
            val community: CommunityView
        ) : Item(community.community.name)

        class StaticChildItem(
            val text: String,
            @DrawableRes val iconResId: Int,
            val communityRef: CommunityRef,
        ) : Item(communityRef.getKey())

        class RecentChildItem(
            val text: String,
            val communityRef: CommunityRef
        ) : Item("r:${communityRef.getKey()}")
    }

    private inner class SubredditsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var rawData: List<CommunityView> = listOf()
        private var serverResultsInProgress = false
        private var serverQueryResults: List<CommunityView> = listOf()

        private var query: String? = null

        private val adapterHelper = AdapterHelper<Item> (
            areItemsTheSame = { old, new ->
                old.id == new.id
            },
            areContentsTheSame = {old, new ->
                when (old) {
                    is Item.GroupHeaderItem ->
                        old.stillLoading == (new as Item.GroupHeaderItem).stillLoading
                    is Item.NoResultsItem -> true
                    is Item.CommunityChildItem -> true
                    is Item.StaticChildItem -> true
                    is Item.RecentChildItem -> true
                }
            }
        ).apply {
            addItemType(
                clazz = Item.GroupHeaderItem::class,
                inflateFn = CommunitySelectorGroupItemBinding::inflate
            ) { item, b, _ ->
                b.titleTextView.text = item.text

                if (item.stillLoading) {
                    b.progressBar.visibility = View.VISIBLE
                } else {
                    b.progressBar.visibility = View.GONE
                }
            }

            addItemType(
                clazz = Item.CommunityChildItem::class,
                inflateFn = CommunitySelectorCommunityItemBinding::inflate
            ) { item, b, h ->
                val community = item.community

                b.icon.setImageResource(R.drawable.ic_subreddit_default)
                offlineManager.fetchImage(h.itemView, community.community.icon) {
                    b.icon.load(it)
                }

                b.textView.text = item.text

                h.itemView.setOnClickListener {
                    onCommunitySelectedListener?.invoke(
                        this@CommunitySelectorController,
                        CommunityRef.CommunityRefByObj(community.community)
                    )
                }

            }
            addItemType(
                clazz = Item.StaticChildItem::class,
                inflateFn = CommunitySelectorStaticCommunityItemBinding::inflate
            ) { item, b, h ->
                b.icon.setImageResource(item.iconResId)
                b.textView.text = item.text

                h.itemView.setOnClickListener {
                    onCommunitySelectedListener?.invoke(
                        this@CommunitySelectorController, item.communityRef)
                }
            }
            addItemType(
                clazz = Item.RecentChildItem::class,
                inflateFn = CommunitySelectorStaticCommunityItemBinding::inflate
            ) { item, b, h ->
                b.icon.setImageResource(0)
                b.icon.visibility = View.GONE

                b.textView.text = item.text
                h.itemView.setOnClickListener {
                    onCommunitySelectedListener?.invoke(
                        this@CommunitySelectorController, item.communityRef)
                }
            }
            addItemType(
                clazz = Item.NoResultsItem::class,
                inflateFn = CommunitySelectorNoResultsItemBinding::inflate
            ) { item, b, _ ->
                b.text.text = item.text
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            adapterHelper.onBindViewHolder(holder, position)
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            offlineManager.cancelFetch(holder.itemView)
        }

        override fun getItemCount(): Int = adapterHelper.itemCount

        private fun refreshItems() {
            fun makeRecentItems(query: String?): List<Item> =
                if (!query.isNullOrBlank()) listOf()
                else arrayListOf<Item>().apply {
                    val recents = recentCommunityManager.getRecentCommunities()
                    if (recents.isNotEmpty()) {
                        add(Item.GroupHeaderItem(context.getString(R.string.recents)))
                        recents.forEach {
                            add(Item.RecentChildItem(it.key, it.communityRef))
                        }
                    }
                }

            val subredditIds = hashSetOf<String>()
            val query = query
            val newItems =
                listOf(
                    Item.GroupHeaderItem(context.getString(R.string.feeds)),
                    Item.StaticChildItem(
                        context.getString(R.string.all),
                        R.drawable.ic_subreddit_all,
                        CommunityRef.All(),
                    ),
                    Item.StaticChildItem(
                        CommonLemmyInstance.LemmyMl.site,
                        R.drawable.ic_subreddit_home,
                        CommunityRef.Local(CommonLemmyInstance.LemmyMl.site),
                    ),
                    Item.StaticChildItem(
                        CommonLemmyInstance.LemmyWorld.site,
                        R.drawable.ic_subreddit_home,
                        CommunityRef.Local(CommonLemmyInstance.LemmyWorld.site),
                    ),
                    Item.StaticChildItem(
                        CommonLemmyInstance.Beehaw.site,
                        R.drawable.ic_subreddit_home,
                        CommunityRef.Local(CommonLemmyInstance.Beehaw.site),
                    ),
                )
                    .plus(makeRecentItems(query))
                    .plus(Item.GroupHeaderItem(context.getString(R.string.communities)))
                    .plus(rawData
                        .sortedBy { it.counts.users_active_month }
                        .map {
                            Item.CommunityChildItem(it.community.name, it)
                                .also { subredditIds.add(it.id) }
                        })
                    .let a@{ intermediateList ->
                        if (query.isNullOrBlank()) {
                            return@a intermediateList
                        }

                        fun getText(item: Item): String = when (item) {
                            is Item.StaticChildItem -> item.text
                            is Item.CommunityChildItem -> item.text
                            else -> ""
                        }

                        val serverQueryItems = serverQueryResults
                            .map { Item.CommunityChildItem(it.community.name, it) }
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

            adapterHelper.setItems(newItems, this)
        }

        fun setQuery(query: String?) {
            this.query = query

            refreshItems()
        }

        fun setData(newData: List<CommunityView>) {
            rawData = newData

            refreshItems()
        }

        fun setQueryServerResults(serverQueryResults: List<CommunityView>) {
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