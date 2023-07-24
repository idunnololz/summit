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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Either
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.CommonLemmyInstance
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.dto.SubscribedType
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.CommunitySelectorCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorCurrentCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorGroupItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorStaticCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorViewBinding
import com.idunnololz.summit.databinding.DummyTopItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StringSearchUtils
import com.idunnololz.summit.util.ext.runAfterLayout
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.toErrorMessage
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
    @Assisted private val viewModel: MainActivityViewModel,
    @Assisted private val viewLifecycleOwner: LifecycleOwner,
    private val offlineManager: OfflineManager,
    private val accountManager: AccountManager,
    private val lemmyApiClient: AccountAwareLemmyClient,
    private val recentCommunityManager: RecentCommunityManager,
) {
    @AssistedFactory
    interface Factory {
        fun create(
            context: Context,
            viewModel: MainActivityViewModel,
            viewLifecycleOwner: LifecycleOwner,
        ): CommunitySelectorController
    }

    private val inflater = LayoutInflater.from(context)

    private var rootView: View? = null

    private lateinit var binding: CommunitySelectorViewBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: EditText

    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    private var coroutineScope = createCoroutineScope()

    private val adapter = SubredditsAdapter()

    private var currentCommunity: CommunityRef? = null

    var onCommunitySelectedListener: CommunitySelectedListener? = null
    var onCommunityInfoClick: ((CommunityRef) -> Unit)? = null

    private val onBackPressedHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    init {
        viewModel.siteOrCommunity.observe(viewLifecycleOwner) {
            adapter.setCurrentCommunity(currentCommunity, it, {})
        }
    }

    fun inflate(container: ViewGroup) {
        rootView?.let {
            return
        }

        val binding = CommunitySelectorViewBinding.inflate(inflater, container, false)
        this.binding = binding
        this.rootView = binding.root
        val rootView = binding.root
        rootView.visibility = View.GONE

        recyclerView = binding.recyclerView
        searchView = binding.searchView

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        BottomSheetBehavior.from(binding.root).apply {
            peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            skipCollapsed = true
        }.also {
            bottomSheetBehavior = it
        }

        searchView.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                val tokens = searchView.text.toString().split("@")
                val communityName = tokens.getOrNull(0) ?: return@OnKeyListener true
                val instance = tokens.getOrNull(1)
                    ?: lemmyApiClient.instance

                onCommunitySelectedListener?.invoke(
                    this@CommunitySelectorController,
                    CommunityRef.CommunityRefByName(communityName, instance)
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
    }

    private fun doQueryAsync(query: CharSequence?) {
        query ?: return

        adapter.setQueryServerResultsInProgress()

        coroutineScope.launch {
            lemmyApiClient.search(
                sortType = SortType.TopMonth,
                listingType = ListingType.All,
                searchType = SearchType.Communities,
                query = query.toString(),
                limit = 20,
            ).onSuccess {
                adapter.setQueryServerResults(it.communities)
            }
        }
    }

    fun show(container: ViewGroup, activity: MainActivity, lifecycleOwner: LifecycleOwner) {
        container.addView(rootView)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.scrollToPosition(0)

        rootView?.visibility = View.VISIBLE

        activity.insetViewExceptTopAutomaticallyByMargins(lifecycleOwner, recyclerView)
        activity.insetViewExceptBottomAutomaticallyByPadding(lifecycleOwner, binding.coordinatorLayout)

        activity.onBackPressedDispatcher.addCallback(onBackPressedHandler)

        bottomSheetBehavior?.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet1: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        rootView?.visibility = View.GONE
                        container.removeView(rootView)
                        onBackPressedHandler.remove()
                    }
                }

                override fun onSlide(bottomSheet1: View, slideOffset: Float) {
                }
            },
        )

        rootView?.runAfterLayout {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
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
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        coroutineScope.cancel()
        coroutineScope = createCoroutineScope()
        binding.recyclerView.adapter = null
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

    private fun createCoroutineScope() =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun setCurrentCommunity(communityRef: CommunityRef?) {
        currentCommunity = communityRef
        adapter.setCurrentCommunity(communityRef, null) {}

        if (communityRef != null) {
            viewModel.onCommunityChanged(communityRef)
        }
    }

    private sealed class Item(
        val id: String
    ) {
        object TopItem : Item("#always_the_top")

        data class CurrentCommunity(
            val communityRef: CommunityRef,
            val siteResponse: GetSiteResponse?,
            val communityView: CommunityView?,
            val isLoading: Boolean,
            val error: Throwable?,
        ) : Item("#current_community")

        class GroupHeaderItem(
            val text: String,
            val stillLoading: Boolean = false
        ) : Item(text)

        class NoResultsItem(
            val text: String
        ) : Item(text)

        class CommunityChildItem(
            val text: String,
            val community: CommunityView,
            val monthlyActiveUsers: Int,
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
        private var currentCommunityRef: CommunityRef? = null
        private var currentCommunityData: StatefulData<Either<GetSiteResponse, CommunityView>>? = null

        private val adapterHelper = AdapterHelper<Item> (
            areItemsTheSame = { old, new ->
                old.id == new.id
            },
            areContentsTheSame = {old, new ->
                when (old) {
                    is Item.TopItem -> true
                    is Item.GroupHeaderItem ->
                        old.stillLoading == (new as Item.GroupHeaderItem).stillLoading
                    is Item.NoResultsItem -> true
                    is Item.CommunityChildItem -> true
                    is Item.StaticChildItem -> true
                    is Item.RecentChildItem -> true
                    is Item.CurrentCommunity -> old == new
                }
            }
        ).apply {
            addItemType(
                clazz = Item.TopItem::class,
                inflateFn = DummyTopItemBinding::inflate
            ) { _, _, _ -> }
            addItemType(
                clazz = Item.CurrentCommunity::class,
                inflateFn = CommunitySelectorCurrentCommunityItemBinding::inflate
            ) { item, b, _ ->

                when (item.communityRef) {
                    is CommunityRef.All -> {
                        b.communityName.text = context.getString(R.string.all)
                        item.communityRef.instance?.let {
                            b.instance.text = it
                        }
                    }
                    is CommunityRef.CommunityRefByName -> {
                        b.communityName.text = item.communityRef.name
                        item.communityRef.instance?.let {
                            b.instance.text = it
                        }
                    }
                    is CommunityRef.Local -> {
                        b.communityName.text = context.getString(R.string.local)
                        item.communityRef.instance?.let {
                            b.instance.text = it
                        }
                    }
                    is CommunityRef.Subscribed -> {
                        b.communityName.text = context.getString(R.string.subscribed)
                        item.communityRef.instance?.let {
                            b.instance.text = it
                        }
                    }
                }

                b.moreInfo.visibility = View.GONE
                b.moreInfo.setOnClickListener {
                    onCommunityInfoClick?.let {
                        it(item.communityRef)
                        hide()
                    }
                }
                b.cardView.setOnClickListener {
                    onCommunityInfoClick?.let {
                        it(item.communityRef)
                        hide()
                    }
                }

                var icon: String? = null
                if (item.error != null) {
                    b.instance.text = item.error.toErrorMessage(context)
                    b.subscribe.visibility = View.VISIBLE
                    b.subscribe.text = context.getString(R.string.retry)
                    b.subscribe.setOnClickListener {
                        viewModel.refetchCommunityOrSite(force = true)
                    }
                    b.progressBar.visibility = View.GONE
                } else {
                    if (item.isLoading) {
                        b.progressBar.visibility = View.VISIBLE
                    } else {
                        b.progressBar.visibility = View.GONE
                    }

                    b.subscribe.visibility = View.GONE
                    if (item.siteResponse != null) {
                        icon = item.siteResponse.site_view.site.icon
                        b.instance.text = item.siteResponse.site_view.site.instance
                        b.moreInfo.visibility = View.VISIBLE
                    } else if (item.communityView != null) {
                        icon = item.communityView.community.icon
                        b.instance.text = item.communityView.community.instance

                        b.subscribe.visibility = View.VISIBLE
                        if (item.communityView.subscribed != SubscribedType.Subscribed) {
                            b.subscribe.text = context.getString(R.string.subscribe)
                            b.subscribe.setOnClickListener {
                                viewModel.updateSubscriptionStatus(item.communityView.community.id, true)
                            }
                        } else {
                            b.subscribe.text = context.getString(R.string.unsubscribe)
                            b.subscribe.setOnClickListener {
                                viewModel.updateSubscriptionStatus(item.communityView.community.id, false)
                            }
                        }
                        b.moreInfo.visibility = View.VISIBLE
                    }
                }

                b.image.load(icon) {
                    placeholder(R.drawable.ic_subreddit_default)
                    fallback(R.drawable.ic_subreddit_default)
                }
            }

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

                b.icon.load(R.drawable.ic_subreddit_default)
                offlineManager.fetchImage(h.itemView, community.community.icon) {
                    b.icon.load(it)
                }

                b.title.text = item.text
                val mauString = LemmyUtils.abbrevNumber(item.monthlyActiveUsers.toLong())

                @Suppress("SetTextI18n")
                b.monthlyActives.text = "(${context.getString(R.string.mau_format, mauString)}) " +
                        "(${item.community.community.instance})"

                h.itemView.setOnClickListener {
                    onCommunitySelectedListener?.invoke(
                        this@CommunitySelectorController,
                        community.community.toCommunityRef()
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

        private fun refreshItems(cb: () -> Unit) {
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

            val communityRefs = hashSetOf<CommunityRef>()
            val query = query
            val isQueryActive = !query.isNullOrBlank()

            val newItems = mutableListOf<Item>()

            newItems += Item.TopItem

            if (!isQueryActive) {
                val currentCommunityData = currentCommunityData
                currentCommunityRef?.let {
                    when (currentCommunityData) {
                        is StatefulData.Error ->
                            newItems.add(Item.CurrentCommunity(
                                communityRef = it,
                                siteResponse = null,
                                communityView = null,
                                isLoading = false,
                                error = currentCommunityData.error,
                            ))
                        is StatefulData.Loading ->
                            newItems.add(Item.CurrentCommunity(
                                communityRef = it,
                                siteResponse = null,
                                communityView = null,
                                isLoading = true,
                                error = null,
                            ))
                        is StatefulData.NotStarted ->
                            newItems.add(Item.CurrentCommunity(
                                communityRef = it,
                                siteResponse = null,
                                communityView = null,
                                isLoading = false,
                                error = null,
                            ))
                        is StatefulData.Success ->
                            newItems.add(Item.CurrentCommunity(
                                communityRef = it,
                                siteResponse = currentCommunityData.data.leftOrNull(),
                                communityView = currentCommunityData.data.getOrNull(),
                                isLoading = false,
                                error = null,
                            ))
                        null ->
                            newItems.add(Item.CurrentCommunity(
                                communityRef = it,
                                siteResponse = null,
                                communityView = null,
                                isLoading = false,
                                error = null,
                            ))
                    }
                }

                newItems.add(Item.GroupHeaderItem(context.getString(R.string.feeds)))
                newItems.add(
                    Item.StaticChildItem(
                        context.getString(R.string.all),
                        R.drawable.ic_subreddit_all,
                        CommunityRef.All(),
                    )
                )

                val account = accountManager.currentAccount.value
                if (account != null) {
                    newItems.add(
                        Item.StaticChildItem(
                            context.getString(R.string.subscribed),
                            R.drawable.baseline_dynamic_feed_24,
                            CommunityRef.Subscribed(account.instance),
                        )
                    )
                    newItems.add(
                        Item.StaticChildItem(
                            context.getString(R.string.local),
                            R.drawable.ic_subreddit_home,
                            CommunityRef.Local(account.instance),
                        )
                    )
                } else {
                    newItems.addAll(
                        listOf(
                            Item.StaticChildItem(
                                CommonLemmyInstance.LemmyMl.instance,
                                R.drawable.ic_subreddit_home,
                                CommunityRef.Local(CommonLemmyInstance.LemmyMl.instance),
                            ),
                            Item.StaticChildItem(
                                CommonLemmyInstance.LemmyWorld.instance,
                                R.drawable.ic_subreddit_home,
                                CommunityRef.Local(CommonLemmyInstance.LemmyWorld.instance),
                            ),
                            Item.StaticChildItem(
                                CommonLemmyInstance.Beehaw.instance,
                                R.drawable.ic_subreddit_home,
                                CommunityRef.Local(CommonLemmyInstance.Beehaw.instance),
                            ),
                        )
                    )
                }

                newItems.addAll(makeRecentItems(query))
                newItems.add(Item.GroupHeaderItem(context.getString(R.string.communities)))

                val filteredPopularCommunities = rawData.filter {
                    query.isNullOrBlank() || it.community.name.contains(query, ignoreCase = true)
                }

                filteredPopularCommunities
                    .sortedByDescending { it.counts.users_active_month }
                    .mapTo(newItems) {
                        Item.CommunityChildItem(
                            text = it.community.name,
                            community = it,
                            monthlyActiveUsers = it.counts.users_active_month
                        ).also {
                            communityRefs.add(
                                CommunityRef.CommunityRefByName(
                                    name = it.community.community.name,
                                    instance = it.community.community.instance
                                )
                            )
                        }
                    }
            }

            if (!query.isNullOrBlank()) {
                fun getText(item: Item): String = when (item) {
                    is Item.StaticChildItem -> item.text
                    is Item.CommunityChildItem -> item.text
                    else -> ""
                }

                val serverQueryItems = serverQueryResults
                    .map { Item.CommunityChildItem(
                        text = it.community.name,
                        community = it,
                        monthlyActiveUsers = it.counts.users_active_month
                    ) }
                    .filter {
                        !communityRefs.contains(
                            CommunityRef.CommunityRefByName(
                                it.community.community.name,
                                it.community.community.instance,
                            )
                        ) && it.community.community.name.contains(query, ignoreCase = true)
                    }

                newItems.sortByDescending { StringSearchUtils.similarity(query, getText(it)) }

                newItems.add(
                    Item.GroupHeaderItem(
                        context.getString(R.string.server_results),
                        serverResultsInProgress
                    )
                )

                if (serverQueryItems.isNotEmpty() || serverResultsInProgress) {
                    newItems.addAll(serverQueryItems)
                } else {
                    if (newItems.isEmpty()) {
                        newItems.add(Item.NoResultsItem(context.getString(R.string.no_results_found)))
                    }
                }
            }

            adapterHelper.setItems(newItems, this, cb)
        }

        fun setQuery(query: String?) {
            this.query = query

            refreshItems({})
        }

        fun setData(newData: List<CommunityView>) {
            rawData = newData

            refreshItems({})
        }

        fun setQueryServerResults(serverQueryResults: List<CommunityView>) {
            this.serverQueryResults = serverQueryResults
            serverResultsInProgress = false

            refreshItems({})
        }

        fun setQueryServerResultsInProgress() {
            serverQueryResults = listOf()
            serverResultsInProgress = true

            refreshItems({})
        }

        fun setCurrentCommunity(
            currentCommunityRef: CommunityRef?,
            data: StatefulData<Either<GetSiteResponse, CommunityView>>?,
            cb: () -> Unit,
        ) {
            this.currentCommunityRef = currentCommunityRef
            this.currentCommunityData = data

            refreshItems(cb)
        }

    }
}