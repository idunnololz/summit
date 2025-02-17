package com.idunnololz.summit.main

import android.content.Context
import android.net.Uri
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Either
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.dto.SubscribedType
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.databinding.CommunitySelectorCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorCurrentCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorGroupItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorRecentItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorStaticCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorViewBinding
import com.idunnololz.summit.databinding.CurrentInstanceItemBinding
import com.idunnololz.summit.databinding.DummyTopItemBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.LoadingViewItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.RecentCommunityManager
import com.idunnololz.summit.lemmy.appendNameWithInstance
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StringSearchUtils
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import com.idunnololz.summit.util.ext.runAfterLayout
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByMargins
import com.idunnololz.summit.util.newBottomSheetPredictiveBackBackPressHandler
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.toErrorMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias CommunitySelectedListener =
    ((controller: CommunitySelectorController, communityRef: CommunityRef) -> Unit)

class CommunitySelectorController @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val viewModel: MainActivityViewModel,
    @Assisted private val viewLifecycleOwner: LifecycleOwner,
    private val offlineManager: OfflineManager,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val lemmyApiClient: AccountAwareLemmyClient,
    private val recentCommunityManager: RecentCommunityManager,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val animationsHelper: AnimationsHelper,
    private val avatarHelper: AvatarHelper,
    private val preferences: Preferences,
) {

    companion object {
        private const val TAG = "CommunitySelectorController"
    }

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
    private lateinit var searchView: EditText

    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    private var coroutineScope = coroutineScopeFactory.create()

    private val adapter = CommunitiesAdapter(
        onCurrentInstanceClick = {
            onChangeInstanceClick?.invoke()
        },
    )

    private var currentCommunity: CommunityRef? = null

    var onCommunitySelectedListener: CommunitySelectedListener? = null
    var onCommunityInfoClick: ((CommunityRef) -> Unit)? = null
    var onChangeInstanceClick: (() -> Unit)? = null

    private val insetObserver: Observer<ActivityInsets> = Observer { insets ->
        binding.csBottomSheetContainerInner.updateLayoutParams<MarginLayoutParams> {
            topMargin = insets.topInset
            leftMargin = insets.leftInset
            rightMargin = insets.rightInset
        }
    }

    private var removeView: (() -> Unit)? = null

    private val onBackPressedHandler =
        newBottomSheetPredictiveBackBackPressHandler(
            context,
            { binding.csBottomSheetContainerInner },
        ) {
            onBackPressed()
        }

    init {
        viewModel.siteOrCommunity.observe(viewLifecycleOwner) {
            adapter.setCurrentCommunity(currentCommunity, it) {}
        }

        coroutineScope.launch {
            lemmyApiClient.instanceFlow.collect {
                adapter.refreshItems { }
            }
        }
    }

    fun setup(activity: MainActivity, container: ViewGroup) {
        rootView?.let {
            return
        }

        val binding = CommunitySelectorViewBinding.inflate(inflater, container, false)
        this.binding = binding
        this.rootView = binding.root

        searchView = binding.searchView

        binding.recyclerView.setup(animationsHelper)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        searchView.setOnKeyListener(
            View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    val tokens = searchView.text.toString().split("@")
                    val communityName = tokens.getOrNull(0) ?: return@OnKeyListener true
                    val instance = tokens.getOrNull(1)
                        ?: lemmyApiClient.instance

                    if (Uri.parse(instance) == null) {
                        MaterialAlertDialogBuilder(context)
                            .setMessage(R.string.error_invalid_instance_format)
                            .show()
                        return@OnKeyListener true
                    }

                    onCommunitySelectedListener?.invoke(
                        this@CommunitySelectorController,
                        CommunityRef.CommunityRefByName(communityName, instance),
                    )
                    searchView.setText("")
                    return@OnKeyListener true
                }
                false
            },
        )

        searchView.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    adapter.setQuery(s?.toString()) {
                        binding.recyclerView.scrollToPosition(0)
                    }
                    doQueryAsync(s)
                }
            },
        )

        activity.insets.observe(activity, insetObserver)
        activity.insetViewExceptTopAutomaticallyByMargins(activity, binding.recyclerView)
    }

    fun show(bottomSheetContainer: ViewGroup, activity: MainActivity) {
        Log.d(TAG, "show()")

        val rootView = rootView ?: return

        if (rootView.parent != null) {
            return
        }

        // reset any transforms by predictive back
        binding.csBottomSheetContainerInner.apply {
            scaleX = 1f
            scaleY = 1f
            translationX = 0f
            translationY = 0f
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.scrollToPosition(0)

        removeView = { bottomSheetContainer.removeView(rootView) }
        bottomSheetContainer.addView(rootView)

        activity.onBackPressedDispatcher.addCallback(activity, onBackPressedHandler)

        rootView.requestLayout()

        BottomSheetBehavior.from(binding.csCoordinatorLayout).apply {
            peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            skipCollapsed = true
        }.also {
            bottomSheetBehavior = it
        }
        val overlay = binding.csOverlay
        overlay.setOnClickListener {
            bottomSheetBehavior?.setState(
                BottomSheetBehavior.STATE_HIDDEN,
            )
        }

        bottomSheetBehavior?.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet1: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        adapter.stopLoading()
                        removeView?.invoke()
                        removeView = null
                        onBackPressedHandler.remove()
                    } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        adapter.startLoading()
                    }
                }

                override fun onSlide(bottomSheet1: View, slideOffset: Float) {
                    if (java.lang.Float.isNaN(slideOffset)) {
                        overlay.alpha = 1f
                    } else {
                        overlay.alpha = 1 + slideOffset
                    }
                }
            },
        )

        binding.csCoordinatorLayout.translationY = 0f

        rootView.runAfterLayout {
            rootView.postDelayed({
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }, 100)
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
        coroutineScope = coroutineScopeFactory.create()
        binding.recyclerView.adapter = null
    }

    fun setCommunities(communities: StatefulData<List<CommunityView>>) {
        when (communities) {
            is StatefulData.Error -> {}
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                adapter.setData(communities.data)
            }
        }
    }

    fun setCurrentCommunity(communityRef: CommunityRef?) {
        currentCommunity = communityRef
        adapter.setCurrentCommunity(communityRef, null) {}

        if (communityRef != null) {
            viewModel.onCommunityChanged(communityRef)
        }
    }

    private fun doQueryAsync(query: CharSequence?) {
        query ?: return

        adapter.setQueryServerResultsInProgress()

        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                lemmyApiClient.search(
                    sortType = SortType.TopMonth,
                    listingType = ListingType.All,
                    searchType = SearchType.Communities,
                    query = query.toString(),
                    limit = 20,
                )
            }
            result.onSuccess {
                adapter.setQueryServerResults(it.communities)
            }
        }
    }

    private sealed class Item(
        val id: String,
    ) {
        data object TopItem : Item("#always_the_top")

        data object LoadingItem : Item("#loading")

        data class CurrentCommunity(
            val communityRef: CommunityRef,
            val siteResponse: GetSiteResponse?,
            val communityView: CommunityView?,
            val isLoading: Boolean,
            val error: Throwable?,
        ) : Item("#current_community")

        data class CurrentInstance(
            val instance: String,
            val siteResponse: GetSiteResponse?,
            val isLoading: Boolean,
            val error: Throwable?,
        ) : Item("#current_instance")

        class GroupHeaderItem(
            val text: String,
            val stillLoading: Boolean = false,
        ) : Item(text)

        class NoResultsItem(
            val text: String,
        ) : Item(text)

        class CommunityChildItem(
            val text: CharSequence,
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
            val communityRef: CommunityRef,
            val iconUrl: String?,
        ) : Item("r:${communityRef.getKey()}")

        data object BottomSpacer : Item("bottom_spacer:")
    }

    private inner class CommunitiesAdapter(
        private val onCurrentInstanceClick: () -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var rawData: List<CommunityView> = listOf()
        private var serverResultsInProgress = false
        private var serverQueryResults: List<CommunityView> = listOf()

        private var query: String? = null
        private var currentCommunityRef: CommunityRef? = null
        private var currentCommunityData: StatefulData<Either<GetSiteResponse, CommunityView>>? = null

        private var refreshRequestFlow = MutableSharedFlow<Unit>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old.id == new.id
            },
            areContentsTheSame = { old, new ->
                when (old) {
                    is Item.TopItem -> true
                    is Item.GroupHeaderItem ->
                        old.stillLoading == (new as Item.GroupHeaderItem).stillLoading
                    is Item.NoResultsItem -> true
                    is Item.CommunityChildItem -> true
                    is Item.StaticChildItem -> true
                    is Item.RecentChildItem -> true
                    is Item.LoadingItem -> true
                    is Item.CurrentCommunity -> old == new
                    is Item.CurrentInstance -> old == new
                    Item.BottomSpacer -> true
                }
            },
        ).apply {
            addItemType(
                clazz = Item.LoadingItem::class,
                inflateFn = LoadingViewItemBinding::inflate,
            ) { _, b, _ ->
                b.loadingView.showProgressBar()
            }
            addItemType(
                clazz = Item.TopItem::class,
                inflateFn = DummyTopItemBinding::inflate,
            ) { _, _, _ -> }
            addItemType(
                clazz = Item.CurrentCommunity::class,
                inflateFn = CommunitySelectorCurrentCommunityItemBinding::inflate,
            ) { item, b, _ ->
                b.communityName.text = item.communityRef.getName(context)

                when (item.communityRef) {
                    is CommunityRef.All -> {
                        item.communityRef.instance?.let {
                            b.instance.text = it
                        }
                    }
                    is CommunityRef.CommunityRefByName -> {
                        item.communityRef.instance?.let {
                            b.instance.text = it
                        }
                    }
                    is CommunityRef.Local -> {
                        item.communityRef.instance?.let {
                            b.instance.text = it
                        }
                    }
                    is CommunityRef.Subscribed -> {
                        item.communityRef.instance?.let {
                            b.instance.text = it
                        }
                    }

                    is CommunityRef.MultiCommunity -> {
                        b.instance.text = context.getString(R.string.multicommunity)
                    }

                    is CommunityRef.ModeratedCommunities -> {
                        b.instance.text = item.communityRef.instance
                            ?: item.communityRef.getName(context)
                    }

                    is CommunityRef.AllSubscribed -> {
                        b.instance.text = context.getString(R.string.all_subscribed)
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
                    b.actionButton.visibility = View.VISIBLE
                    b.actionButton.text = context.getString(R.string.retry)
                    b.actionButton.setOnClickListener {
                        viewModel.refetchCommunityOrSite(force = true)
                    }
                    b.progressBar.visibility = View.GONE
                } else {
                    if (item.isLoading) {
                        b.progressBar.visibility = View.VISIBLE
                    } else {
                        b.progressBar.visibility = View.GONE
                    }

                    if (item.siteResponse != null) {
                        icon = item.siteResponse.site_view.site.icon
                        b.instance.text = item.siteResponse.site_view.site.instance
                        b.moreInfo.visibility = View.VISIBLE

                        b.actionButton.visibility = View.VISIBLE
                        b.actionButton.text = context.getString(R.string.instance_info)
                        b.actionButton.setOnClickListener {
                            b.cardView.performClick()
                        }
                    } else if (item.communityView != null) {
                        icon = item.communityView.community.icon
                        b.instance.text = item.communityView.community.instance

                        b.actionButton.visibility = View.VISIBLE
                        if (item.communityView.subscribed != SubscribedType.Subscribed) {
                            b.actionButton.text = context.getString(R.string.subscribe)
                            b.actionButton.setOnClickListener {
                                viewModel.updateSubscriptionStatus(
                                    item.communityView.community.id,
                                    true,
                                )

                                if (preferences.hapticsOnActions) {
                                    it.performHapticFeedbackCompat(
                                        HapticFeedbackConstantsCompat.CONFIRM,
                                    )
                                }
                            }
                        } else {
                            b.actionButton.text = context.getString(R.string.unsubscribe)
                            b.actionButton.setOnClickListener {
                                viewModel.updateSubscriptionStatus(
                                    item.communityView.community.id,
                                    false,
                                )

                                if (preferences.hapticsOnActions) {
                                    it.performHapticFeedbackCompat(
                                        HapticFeedbackConstantsCompat.CONFIRM,
                                    )
                                }
                            }
                        }
                        b.moreInfo.visibility = View.VISIBLE
                    } else if (item.communityRef is CommunityRef.MultiCommunity) {
                        icon = item.communityRef.icon

                        b.actionButton.visibility = View.VISIBLE
                        b.actionButton.text = context.getString(R.string.configure)
                        b.actionButton.setOnClickListener {
                            b.cardView.performClick()
                        }
                    }
                }

                avatarHelper.loadCommunityIcon(b.image, item.communityRef, icon)
            }

            addItemType(
                clazz = Item.CurrentInstance::class,
                inflateFn = CurrentInstanceItemBinding::inflate,
            ) { item, b, _ ->
                b.instanceName.text = item.instance
                b.cardView.setOnClickListener {
                    onCurrentInstanceClick()
                }
            }

            addItemType(
                clazz = Item.GroupHeaderItem::class,
                inflateFn = CommunitySelectorGroupItemBinding::inflate,
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
                inflateFn = CommunitySelectorCommunityItemBinding::inflate,
            ) { item, b, h ->
                val community = item.community

                avatarHelper.loadCommunityIcon(b.icon, community.community)

                b.title.text = item.text

                val mauString = LemmyUtils.abbrevNumber(item.monthlyActiveUsers.toLong())
                @Suppress("SetTextI18n")
                b.monthlyActives.text = context.getString(R.string.mau_format, mauString)

                h.itemView.setOnClickListener {
                    onCommunitySelectedListener?.invoke(
                        this@CommunitySelectorController,
                        community.community.toCommunityRef(),
                    )
                }
            }
            addItemType(
                clazz = Item.StaticChildItem::class,
                inflateFn = CommunitySelectorStaticCommunityItemBinding::inflate,
            ) { item, b, h ->
                b.icon.setImageResource(item.iconResId)
                b.textView.text = item.text

                h.itemView.setOnClickListener {
                    onCommunitySelectedListener?.invoke(
                        this@CommunitySelectorController,
                        item.communityRef,
                    )
                }
            }
            addItemType(
                clazz = Item.RecentChildItem::class,
                inflateFn = CommunitySelectorRecentItemBinding::inflate,
            ) { item, b, h ->
                avatarHelper.loadCommunityIcon(b.icon, item.communityRef, item.iconUrl)

                b.title.text = item.communityRef.getName(b.title.context)
                b.subtitle.text = item.text
                h.itemView.setOnClickListener {
                    onCommunitySelectedListener?.invoke(
                        this@CommunitySelectorController,
                        item.communityRef,
                    )
                }
            }
            addItemType(
                clazz = Item.NoResultsItem::class,
                inflateFn = CommunitySelectorNoResultsItemBinding::inflate,
            ) { item, b, _ ->
                b.text.text = item.text
            }
            addItemType(
                clazz = Item.BottomSpacer::class,
                inflateFn = GenericSpaceFooterItemBinding::inflate,
            ) { _, _, _ -> }
        }

        private var loadingJob: Job? = null

        private suspend fun refreshItemsImmediate() = withContext(Dispatchers.Default) {
            val newItems = mutableListOf<Item>()

            fun addRecentItems(query: String?) {
                if (!query.isNullOrBlank()) {
                    return
                }

                newItems.apply {
                    val recents = recentCommunityManager.getRecentCommunities()
                    if (recents.isNotEmpty()) {
                        add(Item.GroupHeaderItem(context.getString(R.string.recents)))
                        recents.forEach {
                            add(Item.RecentChildItem(it.key, it.communityRef, it.iconUrl))
                        }
                    }
                }
            }

            val communityRefs = hashSetOf<CommunityRef>()
            val query = query
            val isQueryActive = !query.isNullOrBlank()

            newItems += Item.TopItem

            val account = accountManager.currentAccount.asAccount

            if (!isQueryActive) {
                val currentCommunityData = currentCommunityData
                val currentCommunityRef = currentCommunityRef
                if (currentCommunityRef is CommunityRef.MultiCommunity ||
                    currentCommunityRef is CommunityRef.ModeratedCommunities
                ) {
                    newItems.add(
                        Item.CurrentCommunity(
                            communityRef = currentCommunityRef,
                            siteResponse = null,
                            communityView = null,
                            isLoading = false,
                            error = null,
                        ),
                    )
                } else {
                    currentCommunityRef?.let {
                        when (currentCommunityData) {
                            is StatefulData.Error ->
                                newItems.add(
                                    Item.CurrentCommunity(
                                        communityRef = it,
                                        siteResponse = null,
                                        communityView = null,
                                        isLoading = false,
                                        error = currentCommunityData.error,
                                    ),
                                )

                            is StatefulData.Loading ->
                                newItems.add(
                                    Item.CurrentCommunity(
                                        communityRef = it,
                                        siteResponse = null,
                                        communityView = null,
                                        isLoading = true,
                                        error = null,
                                    ),
                                )

                            is StatefulData.NotStarted ->
                                newItems.add(
                                    Item.CurrentCommunity(
                                        communityRef = it,
                                        siteResponse = null,
                                        communityView = null,
                                        isLoading = false,
                                        error = null,
                                    ),
                                )

                            is StatefulData.Success ->
                                newItems.add(
                                    Item.CurrentCommunity(
                                        communityRef = it,
                                        siteResponse = currentCommunityData.data.leftOrNull(),
                                        communityView = currentCommunityData.data.getOrNull(),
                                        isLoading = false,
                                        error = null,
                                    ),
                                )

                            null ->
                                newItems.add(
                                    Item.CurrentCommunity(
                                        communityRef = it,
                                        siteResponse = null,
                                        communityView = null,
                                        isLoading = false,
                                        error = null,
                                    ),
                                )
                        }
                    }

                    if (account == null) {
                        newItems.add(
                            Item.CurrentInstance(
                                instance = lemmyApiClient.instance,
                                siteResponse = null,
                                isLoading = false,
                                error = null,
                            ),
                        )
                    }
                }

                newItems.add(Item.GroupHeaderItem(context.getString(R.string.feeds)))
                newItems.add(
                    Item.StaticChildItem(
                        context.getString(R.string.all),
                        R.drawable.ic_feed_all,
                        CommunityRef.All(),
                    ),
                )

                if (account != null) {
                    newItems.add(
                        Item.StaticChildItem(
                            context.getString(R.string.subscribed),
                            R.drawable.baseline_subscriptions_24,
                            CommunityRef.Subscribed(null),
                        ),
                    )
                    newItems.add(
                        Item.StaticChildItem(
                            context.getString(R.string.local),
                            R.drawable.ic_feed_home,
                            CommunityRef.Local(null),
                        ),
                    )
                    if (accountInfoManager.currentFullAccount.value
                            ?.accountInfo
                            ?.miscAccountInfo
                            ?.modCommunityIds
                            ?.isNotEmpty() == true
                    ) {
                        newItems.add(
                            Item.StaticChildItem(
                                context.getString(R.string.moderated_communities),
                                R.drawable.outline_shield_24,
                                CommunityRef.ModeratedCommunities(null),
                            ),
                        )
                    }

                    newItems.add(
                        Item.StaticChildItem(
                            context.getString(R.string.all_subscribed),
                            R.drawable.baseline_groups_24,
                            CommunityRef.AllSubscribed(),
                        ),
                    )
                } else {
                    newItems.add(
                        Item.StaticChildItem(
                            context.getString(R.string.local),
                            R.drawable.ic_feed_home,
                            CommunityRef.Local(null),
                        ),
                    )
                }

                addRecentItems(query)
                newItems.add(Item.GroupHeaderItem(context.getString(R.string.communities)))

                val filteredPopularCommunities = rawData.filter {
                    query.isNullOrBlank() || it.community.name.contains(query, ignoreCase = true)
                }

                filteredPopularCommunities
                    .sortedByDescending { it.counts.users_active_month }
                    .mapTo(newItems) {
                        Item.CommunityChildItem(
                            text = SpannableStringBuilder().apply {
                                appendNameWithInstance(
                                    context,
                                    it.community.name,
                                    it.community.instance,
                                )
                            },
                            community = it,
                            monthlyActiveUsers = it.counts.users_active_month,
                        ).also {
                            communityRefs.add(
                                CommunityRef.CommunityRefByName(
                                    name = it.community.community.name,
                                    instance = it.community.community.instance,
                                ),
                            )
                        }
                    }
            }

            if (!query.isNullOrBlank()) {
                fun getText(item: Item): String = when (item) {
                    is Item.StaticChildItem -> item.text
                    is Item.CommunityChildItem -> item.text.toString()
                    else -> ""
                }

                val serverQueryItems = serverQueryResults
                    .map {
                        Item.CommunityChildItem(
                            text = it.community.name,
                            community = it,
                            monthlyActiveUsers = it.counts.users_active_month,
                        )
                    }
                    .filter {
                        !communityRefs.contains(
                            CommunityRef.CommunityRefByName(
                                it.community.community.name,
                                it.community.community.instance,
                            ),
                        ) && it.community.community.name.contains(query, ignoreCase = true)
                    }

                newItems.sortByDescending { StringSearchUtils.similarity(query, getText(it)) }

                newItems.add(
                    Item.GroupHeaderItem(
                        context.getString(R.string.server_results),
                        serverResultsInProgress,
                    ),
                )

                if (serverQueryItems.isNotEmpty() || serverResultsInProgress) {
                    newItems.addAll(serverQueryItems)
                } else {
                    newItems.add(Item.NoResultsItem(context.getString(R.string.no_results_found)))
                }
            }
            newItems.add(Item.BottomSpacer)
            withContext(Dispatchers.Main) {
                adapterHelper.setItems(newItems, this@CommunitiesAdapter, {})
            }
        }

        init {
            adapterHelper.setItems(listOf(Item.LoadingItem), this)
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

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

        fun refreshItems(cb: () -> Unit) {
            coroutineScope.launch {
                refreshRequestFlow.emit(Unit)
            }
        }

        fun setQuery(query: String?, cb: () -> Unit) {
            this.query = query

            refreshItems(cb)
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
//            serverQueryResults = listOf()
            serverResultsInProgress = true

            refreshItems({})
        }

        fun setCurrentCommunity(
            currentCommunityRef: CommunityRef?,
            data: StatefulData<Either<GetSiteResponse, CommunityView>>?,
            cb: () -> Unit,
        ) {
            if (this.currentCommunityRef == currentCommunityRef) {
                if (currentCommunityData !is StatefulData.Success) {
                    this.currentCommunityData = data
                } else if (data is StatefulData.Success) {
                    this.currentCommunityData = data
                }
            } else {
                this.currentCommunityRef = currentCommunityRef
                this.currentCommunityData = data
            }

            refreshItems(cb)
        }

        fun startLoading() {
            loadingJob?.cancel()
            loadingJob =
                coroutineScope.launch {
                    refreshRequestFlow
                        .collect {
                            refreshItemsImmediate()
                        }
                }
        }

        fun stopLoading() {
            loadingJob?.cancel()
            adapterHelper.setItems(listOf(Item.LoadingItem), this@CommunitiesAdapter, {})
        }
    }
}
